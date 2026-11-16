package com.example.domain.engine

import com.example.domain.model.Club
import com.example.domain.model.Fixture
import com.example.domain.model.IncomingTransferOffer
import com.example.domain.model.LiveMatchState
import com.example.domain.model.MatchEventType
import com.example.domain.model.NewsCategory
import com.example.domain.model.NewsItem
import com.example.domain.model.SquadSyncReport
import com.example.ui.components.formatCurrency
import java.util.UUID
import kotlin.random.Random

object NewsGenerator {

    fun buildInitialNews(
        userClub: Club,
        allClubs: List<Club>,
        seasonYear: Long,
        managerName: String
    ): List<NewsItem> {
        val items = mutableListOf<NewsItem>()

        items.add(
            NewsItem(
                id = "news_welcome_${UUID.randomUUID().toString().take(8)}",
                category = NewsCategory.CLUB_BUZZ,
                round = 1,
                headline = "${userClub.name} apresenta $managerName para a temporada $seasonYear",
                body = "A diretoria confia no novo comandante para conduzir o clube na ${userClub.division.label}. " +
                    "Orçamento inicial de ${formatCurrency(userClub.budget)} e ${userClub.players.size} atletas no elenco.",
                clubId = userClub.id,
                clubName = userClub.name,
                clubShortName = userClub.shortName,
                clubColorHex = userClub.primaryColorHex,
                clubSecondaryColorHex = userClub.secondaryColorHex,
                isBreaking = true,
                tagLabel = "OFICIAL"
            )
        )

        // Pre-season simulated transfer rumors / deals across leagues
        val otherClubs = allClubs.filter { it.id != userClub.id && it.players.isNotEmpty() }
        val shuffledClubs = otherClubs.shuffled().take(6)
        if (shuffledClubs.size >= 4) {
            val seller1 = shuffledClubs[0]
            val buyer1 = shuffledClubs[1]
            val star1 = seller1.players.sortedByDescending { it.overall }.drop(1).firstOrNull() ?: seller1.players.first()
            items.add(
                NewsItem(
                    id = "news_pre_tr1_${UUID.randomUUID().toString().take(8)}",
                    category = NewsCategory.TRANSFER,
                    round = 1,
                    headline = "${buyer1.name} fecha contratação de ${star1.name}",
                    body = "O ${buyer1.name} (${buyer1.division.shortLabel}) acertou a chegada de ${star1.name} (${star1.position.code}, OVR ${star1.overall}) " +
                        "junto ao ${seller1.name} por ${formatCurrency(star1.marketValue)}.",
                    clubId = buyer1.id,
                    clubName = buyer1.name,
                    clubShortName = buyer1.shortName,
                    clubColorHex = buyer1.primaryColorHex,
                    clubSecondaryColorHex = buyer1.secondaryColorHex,
                    isBreaking = false,
                    tagLabel = formatCurrency(star1.marketValue)
                )
            )

            val seller2 = shuffledClubs[2]
            val buyer2 = shuffledClubs[3]
            val star2 = seller2.players.sortedByDescending { it.overall }.firstOrNull()
            if (star2 != null) {
                items.add(
                    NewsItem(
                        id = "news_pre_tr2_${UUID.randomUUID().toString().take(8)}",
                        category = NewsCategory.TRANSFER,
                        round = 1,
                        headline = "Sondagem: ${buyer2.name} observa ${star2.name}",
                        body = "Olheiros do ${buyer2.name} monitoram a situação de ${star2.name} no ${seller2.name} " +
                            "antes do fechamento da janela na ${buyer2.division.shortLabel}.",
                        clubId = buyer2.id,
                        clubName = buyer2.name,
                        clubShortName = buyer2.shortName,
                        clubColorHex = buyer2.primaryColorHex,
                        clubSecondaryColorHex = buyer2.secondaryColorHex,
                        isBreaking = false,
                        tagLabel = "RUMOR"
                    )
                )
            }
        }

        // Same-division title rival preview
        val divRival = allClubs
            .filter { it.division == userClub.division && it.id != userClub.id }
            .maxByOrNull { it.averageOverall }
        if (divRival != null) {
            items.add(
                NewsItem(
                    id = "news_preview_${UUID.randomUUID().toString().take(8)}",
                    category = NewsCategory.CLUB_BUZZ,
                    round = 1,
                    headline = "Favoritos ao título na ${userClub.division.shortLabel}",
                    body = "Especialistas apontam ${divRival.name} (OVR ${divRival.averageOverall}) e ${userClub.name} " +
                        "entre os principais postulantes nesta temporada de ${userClub.division.totalRounds} rodadas.",
                    clubId = divRival.id,
                    clubName = divRival.name,
                    clubShortName = divRival.shortName,
                    clubColorHex = divRival.primaryColorHex,
                    clubSecondaryColorHex = divRival.secondaryColorHex,
                    isBreaking = false,
                    tagLabel = userClub.division.shortLabel.uppercase()
                )
            )
        }

        return items
    }

    fun buildRoundNews(
        round: Int,
        userClub: Club,
        liveState: LiveMatchState,
        allClubs: List<Club>,
        updatedFixtures: List<Fixture>,
        incomingOffers: List<IncomingTransferOffer>,
        seasonSummary: String?
    ): List<NewsItem> {
        val items = mutableListOf<NewsItem>()
        val clubMap = allClubs.associateBy { it.id }

        if (seasonSummary != null) {
            items.add(
                NewsItem(
                    id = "news_season_end_${round}_${UUID.randomUUID().toString().take(6)}",
                    category = NewsCategory.CLUB_BUZZ,
                    round = round,
                    headline = "Fechamento da Temporada na ${userClub.division.shortLabel}",
                    body = seasonSummary,
                    clubId = userClub.id,
                    clubName = userClub.name,
                    clubShortName = userClub.shortName,
                    clubColorHex = userClub.primaryColorHex,
                    clubSecondaryColorHex = userClub.secondaryColorHex,
                    isBreaking = true,
                    tagLabel = "TEMPORADA"
                )
            )
        }

        // User match result headline
        val um = liveState.userMatch
        val goalsSummary = liveState.events
            .filter { it.type == MatchEventType.GOAL }
            .reversed()
            .joinToString(", ") { "${it.playerName} (${it.minute}')" }
            .ifBlank { "Sem gols marcados" }
        val userWin = (liveState.userIsHome && um.homeScore > um.awayScore) ||
            (!liveState.userIsHome && um.awayScore > um.homeScore)
        val userDraw = um.homeScore == um.awayScore

        val matchHeadline = when {
            userWin -> "${userClub.name} vence na Rodada $round: ${um.homeShort} ${um.homeScore} x ${um.awayScore} ${um.awayShort}"
            userDraw -> "Tudo igual em ${um.homeShort} ${um.homeScore} x ${um.awayScore} ${um.awayShort}"
            else -> "${userClub.name} tropeça na Rodada $round (${um.homeShort} ${um.homeScore} x ${um.awayScore} ${um.awayShort})"
        }
        items.add(
            NewsItem(
                id = "news_user_match_${round}_${UUID.randomUUID().toString().take(6)}",
                category = NewsCategory.MATCH_RESULT,
                round = round,
                headline = matchHeadline,
                body = "Público de ${liveState.attendance} torcedores • Posse ${liveState.possessionHome}%-${100 - liveState.possessionHome}% • " +
                    "Gols: $goalsSummary. Craque: ${liveState.manOfTheMatch ?: "Equipe"}.",
                clubId = userClub.id,
                clubName = userClub.name,
                clubShortName = userClub.shortName,
                clubColorHex = userClub.primaryColorHex,
                clubSecondaryColorHex = userClub.secondaryColorHex,
                isBreaking = userWin && kotlin.math.abs(um.homeScore - um.awayScore) >= 2,
                tagLabel = "PLACAR FINAL"
            )
        )

        // Incoming transfer offer buzz
        incomingOffers.forEach { offer ->
            items.add(
                NewsItem(
                    id = "news_offer_${offer.id}",
                    category = NewsCategory.TRANSFER,
                    round = round,
                    headline = "${offer.offeringClubName} faz proposta por ${offer.player.name}",
                    body = "Oferta oficial de ${formatCurrency(offer.offerAmount)} pelo atleta (${offer.player.position.code}, OVR ${offer.player.overall}). " +
                        "Confira e responda na tela do Clube!",
                    clubId = userClub.id,
                    clubName = userClub.name,
                    clubShortName = userClub.shortName,
                    clubColorHex = userClub.primaryColorHex,
                    clubSecondaryColorHex = userClub.secondaryColorHex,
                    isBreaking = true,
                    tagLabel = "PROPOSTA"
                )
            )
        }

        // Highlights from other matches in the same round (same division + top global leagues)
        val roundPlayed = updatedFixtures.filter {
            it.round == round && it.played && it.id != um.fixtureId
        }
        val sameDivHighlights = roundPlayed
            .filter { it.division == userClub.division }
            .sortedByDescending { kotlin.math.abs(it.homeScore - it.awayScore) + it.homeScore + it.awayScore }
            .take(2)
        val worldHighlights = roundPlayed
            .filter { it.division != userClub.division }
            .sortedByDescending { kotlin.math.abs(it.homeScore - it.awayScore) + it.homeScore + it.awayScore }
            .take(2)

        (sameDivHighlights + worldHighlights).forEach { f ->
            val h = clubMap[f.homeClubId]
            val a = clubMap[f.awayClubId]
            if (h != null && a != null) {
                val winner = when {
                    f.homeScore > f.awayScore -> h
                    f.awayScore > f.homeScore -> a
                    else -> h
                }
                val isBlowout = kotlin.math.abs(f.homeScore - f.awayScore) >= 3
                val title = when {
                    isBlowout -> "Goleada na ${f.division.shortLabel}: ${h.shortName} ${f.homeScore} x ${f.awayScore} ${a.shortName}"
                    f.homeScore == f.awayScore -> "Empate movimentado: ${h.name} ${f.homeScore} x ${f.awayScore} ${a.name}"
                    else -> "${winner.name} soma 3 pontos diante do ${if (winner.id == h.id) a.name else h.name}"
                }
                items.add(
                    NewsItem(
                        id = "news_fx_${f.id}",
                        category = NewsCategory.MATCH_RESULT,
                        round = round,
                        headline = title,
                        body = "Pela Rodada $round da ${f.division.label}, ${h.name} ${f.homeScore} x ${f.awayScore} ${a.name}.",
                        clubId = winner.id,
                        clubName = winner.name,
                        clubShortName = winner.shortName,
                        clubColorHex = winner.primaryColorHex,
                        clubSecondaryColorHex = winner.secondaryColorHex,
                        isBreaking = false,
                        tagLabel = f.division.shortLabel.uppercase()
                    )
                )
            }
        }

        // Simulated AI-club transfer buzz for the round
        if (Random.nextDouble() < 0.85) {
            val candidates = allClubs.filter { it.id != userClub.id && it.players.isNotEmpty() }.shuffled()
            if (candidates.size >= 2) {
                val seller = candidates[0]
                val buyer = candidates[1]
                val player = seller.players.randomOrNull()
                if (player != null) {
                    val fee = ((player.marketValue * Random.nextDouble(0.95, 1.25)) / 100_000L).toLong() * 100_000L
                    items.add(
                        NewsItem(
                            id = "news_ai_tr_${round}_${UUID.randomUUID().toString().take(6)}",
                            category = NewsCategory.TRANSFER,
                            round = round,
                            headline = "Vai e Vem: ${buyer.name} contrata ${player.name}",
                            body = "Negociação concluída entre ${seller.name} e ${buyer.name} (${buyer.division.shortLabel}) " +
                                "por ${formatCurrency(fee.coerceAtLeast(300_000L))}.",
                            clubId = buyer.id,
                            clubName = buyer.name,
                            clubShortName = buyer.shortName,
                            clubColorHex = buyer.primaryColorHex,
                            clubSecondaryColorHex = buyer.secondaryColorHex,
                            isBreaking = false,
                            tagLabel = formatCurrency(fee.coerceAtLeast(300_000L))
                        )
                    )
                }
            }
        }

        return items
    }

    fun buildTransferNews(
        round: Int,
        headline: String,
        body: String,
        club: Club,
        feeLabel: String
    ): NewsItem = NewsItem(
        id = "news_tr_${round}_${UUID.randomUUID().toString().take(8)}",
        category = NewsCategory.TRANSFER,
        round = round,
        headline = headline,
        body = body,
        clubId = club.id,
        clubName = club.name,
        clubShortName = club.shortName,
        clubColorHex = club.primaryColorHex,
        clubSecondaryColorHex = club.secondaryColorHex,
        isBreaking = true,
        tagLabel = feeLabel
    )

    fun buildSyncNews(
        round: Int,
        report: SquadSyncReport,
        userClub: Club?
    ): NewsItem = NewsItem(
        id = "news_sync_${report.timestamp}_${UUID.randomUUID().toString().take(5)}",
        category = NewsCategory.SYNC_UPDATE,
        round = round,
        headline = if (report.isLiveNetwork) {
            "Atualização ao vivo da janela (${report.clubsSynced} clubes)"
        } else {
            "Verificação local de elencos concluída"
        },
        body = report.summary,
        clubId = userClub?.id,
        clubName = userClub?.name,
        clubShortName = userClub?.shortName,
        clubColorHex = userClub?.primaryColorHex,
        clubSecondaryColorHex = userClub?.secondaryColorHex,
        isBreaking = report.playersAdded > 0,
        tagLabel = "ESPN SYNC"
    )

    fun buildCupNews(
        round: Int,
        cupTitle: String,
        stageLabel: String,
        userClub: Club,
        homeShort: String,
        homeScore: Int,
        awayShort: String,
        awayScore: Int,
        shootoutSummary: String?,
        userWon: Boolean,
        isChampion: Boolean
    ): NewsItem {
        val headline = when {
            isChampion -> "CAMPEÃO DA ${cupTitle.uppercase()}! ${userClub.name} levanta a taça!"
            userWon -> "${userClub.name} avança na $cupTitle ($stageLabel)"
            else -> "${userClub.name} se despede da $cupTitle nas $stageLabel"
        }
        val body = buildString {
            append("Placar: $homeShort $homeScore x $awayScore $awayShort. ")
            if (shootoutSummary != null) append("$shootoutSummary ")
            if (isChampion) append("Festa histórica da torcida do ${userClub.shortName}!")
        }
        return NewsItem(
            id = "news_cup_${round}_${UUID.randomUUID().toString().take(6)}",
            category = NewsCategory.MATCH_RESULT,
            round = round,
            headline = headline,
            body = body.trim(),
            clubId = userClub.id,
            clubName = userClub.name,
            clubShortName = userClub.shortName,
            clubColorHex = userClub.primaryColorHex,
            clubSecondaryColorHex = userClub.secondaryColorHex,
            isBreaking = isChampion || userWon,
            tagLabel = if (isChampion) "CAMPEÃO" else cupTitle.uppercase()
        )
    }
}

