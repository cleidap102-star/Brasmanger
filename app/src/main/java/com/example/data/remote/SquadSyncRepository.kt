package com.example.data.remote

import android.util.Log
import com.example.data.local.SquadCacheDao
import com.example.data.local.SyncLogEntity
import com.example.data.local.SyncedClubMetaEntity
import com.example.data.local.SyncedPlayerEntity
import com.example.data.seed.makePlayer
import com.example.domain.model.Club
import com.example.domain.model.Division
import com.example.domain.model.Player
import com.example.domain.model.PlayerPosition
import com.example.domain.model.SquadSyncEntry
import com.example.domain.model.SquadSyncReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.text.Normalizer
import kotlin.math.abs

class SquadSyncRepository(
    private val api: EspnSoccerApi,
    private val cacheDao: SquadCacheDao
) {
    val syncLogs: Flow<List<SyncLogEntity>> = cacheDao.observeSyncLogs()

    suspend fun applyCachedUpdates(clubs: List<Club>): List<Club> = withContext(Dispatchers.IO) {
        val metaMap = cacheDao.getAllClubMeta().associateBy { it.clubId }
        val syncedPlayersByClub = cacheDao.getAllSyncedPlayers().groupBy { it.clubId }

        if (metaMap.isEmpty() && syncedPlayersByClub.isEmpty()) return@withContext clubs

        clubs.map { club ->
            val meta = metaMap[club.id]
            val cachedPlayers = syncedPlayersByClub[club.id].orEmpty()
            val mergedPlayers = if (cachedPlayers.isEmpty()) {
                club.players
            } else {
                mergePlayersPreservingState(club, cachedPlayers)
            }
            club.copy(
                crestUrl = meta?.crestUrl ?: club.crestUrl,
                espnTeamId = meta?.espnTeamId ?: club.espnTeamId,
                players = mergedPlayers
            )
        }
    }

    suspend fun syncLiveRosters(
        currentClubs: List<Club>,
        preserveUserSignings: Boolean = true,
        priorityDivision: Division? = null
    ): Pair<List<Club>, SquadSyncReport> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val entries = mutableListOf<SquadSyncEntry>()
        var addedCount = 0
        var updatedCount = 0
        var removedCount = 0
        var clubsSyncedCount = 0

        val targetDivisions = buildList {
            if (priorityDivision != null && priorityDivision.espnSlug.isNotBlank()) {
                add(priorityDivision)
            }
            if (Division.SERIE_A !in this) add(Division.SERIE_A)
            if (priorityDivision == null && Division.SERIE_B !in this) add(Division.SERIE_B)
            if (priorityDivision == null) {
                Division.entries.filter { it.espnSlug.isNotBlank() && it !in this }.take(3).forEach { add(it) }
            }
        }
        val targetSlugs = targetDivisions.map { it.espnSlug }.distinct()

        val espnTeamsByLeague = mutableMapOf<String, List<EspnTeamDto>>()
        coroutineScope {
            targetSlugs.map { slug ->
                async {
                    try {
                        val resp = api.getLeagueTeams(slug)
                        val list = resp.sports
                            ?.firstOrNull()?.leagues?.firstOrNull()?.teams
                            ?.mapNotNull { it.team } ?: emptyList()
                        slug to list
                    } catch (e: Exception) {
                        Log.w(TAG, "Falha ao buscar times $slug na ESPN: ${e.message}")
                        slug to emptyList()
                    }
                }
            }.awaitAll().forEach { (slug, list) ->
                if (list.isNotEmpty()) espnTeamsByLeague[slug] = list
            }
        }

        val allRemoteTeams = espnTeamsByLeague.flatMap { (league, list) ->
            list.map { league to it }
        }

        if (allRemoteTeams.isEmpty()) {
            val offlineReport = SquadSyncReport(
                timestamp = now,
                sourceLabel = "Base Local 2026 (Sem conexão)",
                isLiveNetwork = false,
                clubsSynced = 0,
                playersAdded = 0,
                playersUpdated = 0,
                playersRemoved = 0,
                entries = emptyList(),
                summary = "Não foi possível conectar à API da ESPN no momento. Elencos locais preservados."
            )
            return@withContext currentClubs to offlineReport
        }

        val clubsToFetchRoster = currentClubs.filter { it.division in targetDivisions }.map { it.id }.toSet()
        val semaphore = Semaphore(8)
        val metaToSave = mutableListOf<SyncedClubMetaEntity>()
        val clubResults = coroutineScope {
            currentClubs.map { club ->
                async {
                    if (club.id !in clubsToFetchRoster || club.division.espnSlug.isBlank()) {
                        return@async club to emptyList<SyncedPlayerEntity>()
                    }
                    val primaryLeague = club.division.espnSlug
                    val matchedRemote = findMatchingEspnTeam(club, allRemoteTeams, primaryLeague)
                    if (matchedRemote == null) {
                        club to emptyList<SyncedPlayerEntity>()
                    } else {
                        val (league, remoteTeam) = matchedRemote
                        val teamId = remoteTeam.id.orEmpty()
                        val crestUrl = remoteTeam.logos?.firstOrNull()?.href
                        synchronized(metaToSave) {
                            metaToSave.add(
                                SyncedClubMetaEntity(
                                    clubId = club.id,
                                    espnTeamId = teamId,
                                    crestUrl = crestUrl,
                                    lastSyncedAtMs = now
                                )
                            )
                        }
                        val roster = semaphore.withPermit {
                            try {
                                if (teamId.isNotBlank()) {
                                    api.getTeamRoster(league, teamId).athletes.orEmpty()
                                } else emptyList()
                            } catch (e: Exception) {
                                Log.w(TAG, "Falha ao buscar elenco de ${club.name}: ${e.message}")
                                emptyList()
                            }
                        }

                        if (roster.isEmpty()) {
                            club.copy(crestUrl = crestUrl ?: club.crestUrl, espnTeamId = teamId.ifBlank { club.espnTeamId }) to emptyList()
                        } else {
                            val syncedEntities = mutableListOf<SyncedPlayerEntity>()
                            val existingByNorm = club.players.associateBy { normalizeName(it.name) }
                            val updatedPlayers = club.players.toMutableList()
                            val matchedExistingIds = mutableSetOf<String>()

                            roster.take(30).forEachIndexed { idx, athlete ->
                                val rawName = athlete.displayName ?: athlete.fullName ?: athlete.shortName ?: return@forEachIndexed
                                val norm = normalizeName(rawName)
                                val pos = mapEspnPosition(athlete.position?.abbreviation, athlete.position?.displayName, idx)
                                val age = (athlete.age ?: 24).coerceIn(17, 45)
                                val shirt = athlete.jersey?.toIntOrNull()?.coerceIn(1, 99) ?: ((idx + 1) % 99 + 1)
                                val nat = athlete.citizenship?.takeIf { it.isNotBlank() } ?: "Brasil"

                                val existing = existingByNorm[norm]
                                    ?: club.players.firstOrNull { isSimilarPlayerName(it.name, rawName) }

                                val resolvedOverall: Int
                                if (existing != null) {
                                    matchedExistingIds.add(existing.id)
                                    resolvedOverall = existing.overall
                                    val changed = existing.shirtNumber != shirt || existing.espnAthleteId != athlete.id
                                    if (changed) {
                                        val pIdx = updatedPlayers.indexOfFirst { it.id == existing.id }
                                        if (pIdx >= 0) {
                                            updatedPlayers[pIdx] = existing.copy(
                                                shirtNumber = shirt,
                                                age = age,
                                                espnAthleteId = athlete.id
                                            )
                                        }
                                        synchronized(entries) {
                                            updatedCount++
                                            if (entries.size < 40) {
                                                entries.add(
                                                    SquadSyncEntry(
                                                        clubName = club.name,
                                                        action = "ATUALIZADO",
                                                        playerName = existing.name,
                                                        position = existing.position.code,
                                                        detail = "Camisa #$shirt • Dados da temporada sincronizados"
                                                    )
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val baseOvr = when (club.prestige) {
                                        5 -> 76
                                        4 -> 74
                                        3 -> 71
                                        else -> 68
                                    }
                                    val variance = abs(rawName.hashCode()) % 5 - 2
                                    resolvedOverall = (baseOvr + variance).coerceIn(64, 81)
                                    val newPlayer = makePlayer(
                                        clubId = club.id,
                                        index = 200 + idx,
                                        name = rawName,
                                        pos = pos,
                                        ovr = resolvedOverall,
                                        age = age,
                                        shirt = shirt,
                                        nationality = nat,
                                        isStarter = false
                                    ).copy(espnAthleteId = athlete.id)
                                    updatedPlayers.add(newPlayer)
                                    synchronized(entries) {
                                        addedCount++
                                        if (entries.size < 40) {
                                            entries.add(
                                                SquadSyncEntry(
                                                    clubName = club.name,
                                                    action = "NOVO",
                                                    playerName = rawName,
                                                    position = pos.code,
                                                    detail = "Reforço detectado na janela (Força $resolvedOverall)"
                                                )
                                            )
                                        }
                                    }
                                }

                                syncedEntities.add(
                                    SyncedPlayerEntity(
                                        id = "${club.id}_espn_${athlete.id ?: norm}",
                                        clubId = club.id,
                                        name = existing?.name ?: rawName,
                                        positionCode = (existing?.position ?: pos).code,
                                        overall = resolvedOverall,
                                        age = age,
                                        nationality = nat,
                                        shirtNumber = shirt,
                                        espnAthleteId = athlete.id,
                                        updatedAtEpochMs = now
                                    )
                                )
                            }

                            if (!preserveUserSignings && updatedPlayers.size > 24) {
                                val trimmed = updatedPlayers
                                    .sortedByDescending { if (it.id in matchedExistingIds || it.isStarter) 1 else 0 }
                                    .take(24)
                                removedCount += (updatedPlayers.size - trimmed.size)
                                updatedPlayers.clear()
                                updatedPlayers.addAll(trimmed)
                            }

                            synchronized(entries) { clubsSyncedCount++ }
                            club.copy(
                                crestUrl = crestUrl ?: club.crestUrl,
                                espnTeamId = teamId.ifBlank { club.espnTeamId },
                                players = updatedPlayers.distinctBy { normalizeName(it.name) }
                            ) to syncedEntities
                        }
                    }
                }
            }.awaitAll()
        }

        val updatedClubs = clubResults.map { it.first }
        val allSyncedEntities = clubResults.flatMap { it.second }

        if (metaToSave.isNotEmpty()) {
            cacheDao.upsertClubMeta(metaToSave)
        }
        if (allSyncedEntities.isNotEmpty()) {
            allSyncedEntities.groupBy { it.clubId }.forEach { (clubId, list) ->
                cacheDao.deleteSyncedPlayersForClub(clubId)
                cacheDao.upsertSyncedPlayers(list)
            }
        }

        val summary = "Sincronização ao vivo concluída: $clubsSyncedCount clubes verificados, " +
            "$addedCount reforços adicionados e $updatedCount atletas atualizados."

        val slugLabel = targetSlugs.joinToString(", ")
        cacheDao.insertSyncLog(
            SyncLogEntity(
                timestampMs = now,
                sourceLabel = "ESPN Live API ($slugLabel)",
                clubsSynced = clubsSyncedCount,
                playersAdded = addedCount,
                playersUpdated = updatedCount,
                summary = summary
            )
        )

        val report = SquadSyncReport(
            timestamp = now,
            sourceLabel = "ESPN Live API ($slugLabel)",
            isLiveNetwork = true,
            clubsSynced = clubsSyncedCount,
            playersAdded = addedCount,
            playersUpdated = updatedCount,
            playersRemoved = removedCount,
            entries = entries,
            summary = summary
        )
        updatedClubs to report
    }

    private fun mergePlayersPreservingState(
        club: Club,
        cached: List<SyncedPlayerEntity>
    ): List<Player> {
        val currentMap = club.players.associateBy { normalizeName(it.name) }.toMutableMap()
        val result = club.players.toMutableList()
        cached.forEachIndexed { idx, entity ->
            val norm = normalizeName(entity.name)
            val existing = currentMap[norm]
            if (existing != null) {
                val pIdx = result.indexOfFirst { it.id == existing.id }
                if (pIdx >= 0) {
                    result[pIdx] = existing.copy(
                        shirtNumber = entity.shirtNumber,
                        espnAthleteId = entity.espnAthleteId
                    )
                }
            } else {
                val pos = PlayerPosition.entries.firstOrNull { it.code == entity.positionCode } ?: PlayerPosition.MEI
                result.add(
                    makePlayer(
                        clubId = club.id,
                        index = 300 + idx,
                        name = entity.name,
                        pos = pos,
                        ovr = entity.overall,
                        age = entity.age,
                        shirt = entity.shirtNumber,
                        nationality = entity.nationality,
                        isStarter = false
                    ).copy(espnAthleteId = entity.espnAthleteId)
                )
            }
        }
        return result.distinctBy { normalizeName(it.name) }
    }

    private fun findMatchingEspnTeam(
        club: Club,
        remoteTeams: List<Pair<String, EspnTeamDto>>,
        preferredLeague: String
    ): Pair<String, EspnTeamDto>? {
        val byId = remoteTeams.firstOrNull { (_, team) ->
            club.espnTeamId.isNotBlank() && team.id == club.espnTeamId
        }
        if (byId != null) return byId

        val normClub = normalizeName(club.name)
        return remoteTeams
            .sortedByDescending { if (it.first == preferredLeague) 1 else 0 }
            .firstOrNull { (_, team) ->
                val tName = normalizeName(team.displayName.orEmpty())
                val tShort = normalizeName(team.shortDisplayName.orEmpty())
                tName == normClub || tShort == normClub ||
                    tName.contains(normClub) || normClub.contains(tShort.ifBlank { "___" })
            }
    }

    private fun mapEspnPosition(abbr: String?, display: String?, index: Int): PlayerPosition {
        val a = (abbr ?: "").uppercase()
        val d = (display ?: "").lowercase()
        return when {
            a == "G" || a == "GK" || d.contains("goal") || d.contains("goleir") -> PlayerPosition.GOL
            a == "D" || a == "DF" || a == "CB" || d.contains("defend") || d.contains("back") -> {
                if (index % 3 == 0) PlayerPosition.LAT else PlayerPosition.ZAG
            }
            a == "M" || a == "MF" || d.contains("mid") -> PlayerPosition.MEI
            a == "F" || a == "FW" || a == "ST" || d.contains("forw") || d.contains("atac") -> PlayerPosition.ATA
            else -> PlayerPosition.MEI
        }
    }

    private fun isSimilarPlayerName(a: String, b: String): Boolean {
        val na = normalizeName(a)
        val nb = normalizeName(b)
        if (na == nb) return true
        val lastA = na.split(" ").lastOrNull().orEmpty()
        val lastB = nb.split(" ").lastOrNull().orEmpty()
        return lastA.length >= 5 && lastA == lastB && na.firstOrNull() == nb.firstOrNull()
    }

    private fun normalizeName(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private companion object {
        const val TAG = "SquadSyncRepo"
    }
}
