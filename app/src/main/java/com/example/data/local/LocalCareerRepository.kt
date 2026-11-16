package com.example.data.local

import android.util.Base64
import android.util.Log
import com.example.data.firebase.CareerSave
import com.example.data.firebase.ManagerProfile
import com.example.data.firebase.TransferRecord
import com.example.data.firebase.TrophyRecord
import com.example.domain.model.Club
import com.example.domain.model.CupCompetitionState
import com.example.domain.model.Fixture
import com.example.domain.model.ManagerJobOffer
import com.example.domain.model.MonthlyLedgerRecord
import com.example.domain.model.SeasonSnapshot
import com.example.domain.model.SponsorDeal
import com.example.domain.model.YouthProspect
import com.google.firebase.Timestamp
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

internal data class PersistedSeasonSnapshot(
    val clubs: List<Club>,
    val fixtures: List<Fixture>,
    val monthlyLedgerList: List<MonthlyLedgerRecord>,
    val seasonTicketRevenue: Long,
    val cups: List<CupCompetitionState> = emptyList(),
    val activeSponsor: SponsorDeal? = null,
    val youthProspects: List<YouthProspect> = emptyList(),
    val pendingJobOffers: List<ManagerJobOffer> = emptyList(),
    val isSacked: Boolean = false
)

class LocalCareerRepository(
    private val dao: CareerLocalDao,
    private val localUserId: String = DEFAULT_LOCAL_USER_ID
) {
    private val moshi: Moshi by lazy {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }
    private val snapshotAdapter by lazy {
        moshi.adapter(PersistedSeasonSnapshot::class.java)
    }
    fun observeManagerProfile(userId: String = localUserId): Flow<ManagerProfile?> =
        dao.observeManagerProfile(userId).map { entity -> entity?.toDomain() }

    suspend fun ensureManagerProfile(defaultName: String): Result<ManagerProfile> = runCatching {
        val existing = dao.getManagerProfile(localUserId)
        if (existing != null) {
            existing.toDomain()
        } else {
            val now = System.currentTimeMillis()
            val created = LocalManagerProfileEntity(
                userId = localUserId,
                managerName = defaultName.trim().ifBlank { "Técnico Brasileiro" }.take(60),
                autoSyncEnabled = true,
                preferredSimSpeed = 2L,
                totalTitles = 0L,
                totalWins = 0L,
                lastSquadSyncAt = null,
                createdAtMs = now,
                updatedAtMs = now
            )
            dao.upsertManagerProfile(created)
            created.toDomain()
        }
    }

    suspend fun updateManagerSettings(
        managerName: String,
        autoSyncEnabled: Boolean,
        preferredSimSpeed: Long,
        totalTitlesDelta: Long = 0L,
        totalWinsDelta: Long = 0L,
        lastSquadSyncAt: Long? = null
    ): Result<Unit> = runCatching {
        val existing = dao.getManagerProfile(localUserId) ?: run {
            val now = System.currentTimeMillis()
            LocalManagerProfileEntity(
                userId = localUserId,
                managerName = managerName.trim().ifBlank { "Técnico Brasileiro" }.take(60),
                autoSyncEnabled = autoSyncEnabled,
                preferredSimSpeed = preferredSimSpeed,
                totalTitles = 0L,
                totalWins = 0L,
                lastSquadSyncAt = lastSquadSyncAt,
                createdAtMs = now,
                updatedAtMs = now
            )
        }
        val updated = existing.copy(
            managerName = managerName.trim().ifBlank { "Técnico Brasileiro" }.take(60),
            autoSyncEnabled = autoSyncEnabled,
            preferredSimSpeed = preferredSimSpeed.let { if (it in listOf(1L, 2L, 4L)) it else 2L },
            totalTitles = (existing.totalTitles + totalTitlesDelta).coerceAtLeast(0L),
            totalWins = (existing.totalWins + totalWinsDelta).coerceAtLeast(0L),
            lastSquadSyncAt = lastSquadSyncAt ?: existing.lastSquadSyncAt,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertManagerProfile(updated)
    }

    suspend fun updateManagerName(newName: String): Result<Unit> = runCatching {
        val trimmed = newName.trim().ifBlank { "Técnico Brasileiro" }.take(60)
        val existing = dao.getManagerProfile(localUserId)
        val now = System.currentTimeMillis()
        if (existing != null) {
            dao.upsertManagerProfile(existing.copy(managerName = trimmed, updatedAtMs = now))
        } else {
            dao.upsertManagerProfile(
                LocalManagerProfileEntity(
                    userId = localUserId,
                    managerName = trimmed,
                    autoSyncEnabled = true,
                    preferredSimSpeed = 2L,
                    totalTitles = 0L,
                    totalWins = 0L,
                    lastSquadSyncAt = null,
                    createdAtMs = now,
                    updatedAtMs = now
                )
            )
        }
    }

    suspend fun saveSeasonSnapshot(careerId: String, snapshot: SeasonSnapshot): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(careerId.isNotBlank()) { "Career ID must not be blank for snapshot" }
                val payload = PersistedSeasonSnapshot(
                    clubs = snapshot.clubs,
                    fixtures = snapshot.fixtures,
                    monthlyLedgerList = snapshot.monthlyLedger.values.sortedBy { it.monthIndex },
                    seasonTicketRevenue = snapshot.seasonTicketRevenue,
                    cups = snapshot.cups,
                    activeSponsor = snapshot.activeSponsor,
                    youthProspects = snapshot.youthProspects,
                    pendingJobOffers = snapshot.pendingJobOffers,
                    isSacked = snapshot.isSacked
                )
                val json = snapshotAdapter.toJson(payload)
                val encoded = compressSnapshot(json)
                dao.upsertSeasonSnapshot(
                    LocalSeasonSnapshotEntity(
                        careerId = careerId,
                        snapshotJson = encoded,
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
            }
        }

    suspend fun loadSeasonSnapshot(careerId: String): SeasonSnapshot? =
        withContext(Dispatchers.IO) {
            runCatching {
                val entity = dao.getSeasonSnapshot(careerId) ?: return@runCatching null
                val rawJson = decompressSnapshot(entity.snapshotJson)
                val payload = snapshotAdapter.fromJson(rawJson) ?: return@runCatching null
                SeasonSnapshot(
                    clubs = payload.clubs,
                    fixtures = payload.fixtures,
                    monthlyLedger = payload.monthlyLedgerList.associateBy { it.monthIndex },
                    seasonTicketRevenue = payload.seasonTicketRevenue,
                    cups = payload.cups,
                    activeSponsor = payload.activeSponsor,
                    youthProspects = payload.youthProspects,
                    pendingJobOffers = payload.pendingJobOffers,
                    isSacked = payload.isSacked
                )
            }.onFailure { e ->
                Log.w("LocalCareerRepository", "Failed to load season snapshot for $careerId", e)
            }.getOrNull()
        }

    private fun compressSnapshot(json: String): String {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(json.toByteArray(Charsets.UTF_8))
        }
        return GZIP_PREFIX + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
    }

    private fun decompressSnapshot(stored: String): String {
        if (!stored.startsWith(GZIP_PREFIX)) return stored
        val bytes = Base64.decode(stored.removePrefix(GZIP_PREFIX), Base64.NO_WRAP)
        return GZIPInputStream(ByteArrayInputStream(bytes)).use { gzip ->
            gzip.readBytes().toString(Charsets.UTF_8)
        }
    }

    fun observeCareers(userId: String = localUserId): Flow<List<CareerSave>> =
        dao.observeCareers(userId).map { list -> list.map { it.toDomain() } }

    suspend fun createCareer(career: CareerSave): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val careerId = career.id.ifBlank {
            "car_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        }
        val entity = career.copy(id = careerId, userId = localUserId).toEntity(
            createdAtMs = now,
            updatedAtMs = now
        )
        dao.upsertCareer(entity)
        careerId
    }

    suspend fun updateCareer(career: CareerSave): Result<Unit> = runCatching {
        require(career.id.isNotBlank()) { "Career ID must not be blank for update" }
        val now = System.currentTimeMillis()
        val createdAtMs = career.createdAt?.seconds?.times(1000L) ?: now
        dao.upsertCareer(
            career.copy(userId = localUserId).toEntity(
                createdAtMs = createdAtMs,
                updatedAtMs = now
            )
        )
    }

    suspend fun deleteCareer(careerId: String): Result<Unit> = runCatching {
        dao.deleteTransfersForCareer(careerId)
        dao.deleteTrophiesForCareer(careerId)
        dao.deleteSeasonSnapshot(careerId)
        dao.deleteCareer(careerId)
    }

    fun observeTransfers(careerId: String): Flow<List<TransferRecord>> =
        dao.observeTransfers(careerId).map { list -> list.map { it.toDomain() } }

    suspend fun addTransfer(transfer: TransferRecord): Result<String> = runCatching {
        require(transfer.careerId.isNotBlank()) { "Career ID must not be blank" }
        val transferId = "tr_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val now = System.currentTimeMillis()
        dao.insertTransfer(
            LocalTransferEntity(
                id = transferId,
                userId = localUserId,
                careerId = transfer.careerId,
                playerName = transfer.playerName.take(80),
                position = transfer.position,
                fromClubName = transfer.fromClubName.take(64),
                toClubName = transfer.toClubName.take(64),
                fee = transfer.fee.coerceAtLeast(0L),
                transferType = transfer.transferType,
                seasonYear = transfer.seasonYear,
                roundNumber = transfer.roundNumber,
                createdAtMs = now
            )
        )
        transferId
    }

    fun observeTrophies(careerId: String): Flow<List<TrophyRecord>> =
        dao.observeTrophies(careerId).map { list -> list.map { it.toDomain() } }

    suspend fun addTrophy(trophy: TrophyRecord): Result<String> = runCatching {
        require(trophy.careerId.isNotBlank()) { "Career ID must not be blank" }
        val trophyId = "cup_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val now = System.currentTimeMillis()
        dao.insertTrophy(
            LocalTrophyEntity(
                id = trophyId,
                userId = localUserId,
                careerId = trophy.careerId,
                competitionName = trophy.competitionName.take(80),
                clubName = trophy.clubName.take(64),
                seasonYear = trophy.seasonYear,
                finalPosition = trophy.finalPosition.coerceIn(1L, 20L),
                createdAtMs = now
            )
        )
        trophyId
    }

    private fun LocalManagerProfileEntity.toDomain(): ManagerProfile = ManagerProfile(
        id = userId,
        userId = userId,
        managerName = managerName,
        autoSyncEnabled = autoSyncEnabled,
        preferredSimSpeed = preferredSimSpeed,
        totalTitles = totalTitles,
        totalWins = totalWins,
        lastSquadSyncAt = lastSquadSyncAt,
        createdAt = Timestamp(createdAtMs / 1000L, 0),
        updatedAt = Timestamp(updatedAtMs / 1000L, 0)
    )

    private fun LocalCareerEntity.toDomain(): CareerSave = CareerSave(
        id = id,
        userId = userId,
        clubId = clubId,
        clubName = clubName,
        division = division,
        seasonYear = seasonYear,
        currentRound = currentRound,
        budget = budget,
        stadiumCapacity = stadiumCapacity,
        ticketPriceLevel = ticketPriceLevel,
        trainingFocus = trainingFocus,
        formation = formation,
        mentality = mentality,
        marking = marking,
        boardConfidence = boardConfidence,
        points = points,
        leaguePosition = leaguePosition,
        wins = wins,
        draws = draws,
        losses = losses,
        goalsFor = goalsFor,
        goalsAgainst = goalsAgainst,
        copaStage = copaStage,
        activeStarterIds = activeStarterIdsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() },
        status = status,
        createdAt = Timestamp(createdAtMs / 1000L, 0),
        updatedAt = Timestamp(updatedAtMs / 1000L, 0)
    )

    private fun CareerSave.toEntity(createdAtMs: Long, updatedAtMs: Long): LocalCareerEntity =
        LocalCareerEntity(
            id = id,
            userId = userId,
            clubId = clubId,
            clubName = clubName.take(64),
            division = division,
            seasonYear = seasonYear,
            currentRound = currentRound,
            budget = budget,
            stadiumCapacity = stadiumCapacity,
            ticketPriceLevel = ticketPriceLevel,
            trainingFocus = trainingFocus,
            formation = formation,
            mentality = mentality,
            marking = marking,
            boardConfidence = boardConfidence.coerceIn(0L, 100L),
            points = points.coerceAtLeast(0L),
            leaguePosition = leaguePosition.coerceIn(1L, 20L),
            wins = wins.coerceAtLeast(0L),
            draws = draws.coerceAtLeast(0L),
            losses = losses.coerceAtLeast(0L),
            goalsFor = goalsFor.coerceAtLeast(0L),
            goalsAgainst = goalsAgainst.coerceAtLeast(0L),
            copaStage = copaStage.take(48),
            activeStarterIdsCsv = activeStarterIds.take(11).joinToString(","),
            status = status,
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs
        )

    private fun LocalTransferEntity.toDomain(): TransferRecord = TransferRecord(
        id = id,
        userId = userId,
        careerId = careerId,
        playerName = playerName,
        position = position,
        fromClubName = fromClubName,
        toClubName = toClubName,
        fee = fee,
        transferType = transferType,
        seasonYear = seasonYear,
        roundNumber = roundNumber,
        createdAt = Timestamp(createdAtMs / 1000L, 0)
    )

    private fun LocalTrophyEntity.toDomain(): TrophyRecord = TrophyRecord(
        id = id,
        userId = userId,
        careerId = careerId,
        competitionName = competitionName,
        clubName = clubName,
        seasonYear = seasonYear,
        finalPosition = finalPosition,
        createdAt = Timestamp(createdAtMs / 1000L, 0)
    )

    companion object {
        const val DEFAULT_LOCAL_USER_ID = "local_manager"
        private const val GZIP_PREFIX = "GZIP:"
    }
}
