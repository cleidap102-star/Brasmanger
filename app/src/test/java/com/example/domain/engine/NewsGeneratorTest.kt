package com.example.domain.engine

import com.example.data.seed.SerieASeedData
import com.example.data.seed.SerieBSeedData
import com.example.domain.model.Formation
import com.example.domain.model.MarkingStyle
import com.example.domain.model.Mentality
import com.example.domain.model.NewsCategory
import com.example.domain.model.SquadSyncReport
import com.example.domain.model.TicketPriceLevel
import com.example.domain.model.TrainingFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsGeneratorTest {

    @Test
    fun buildInitialNews_generatesWelcomeTransferAndTitlePreviewItems() {
        val clubs = (SerieASeedData.clubs() + SerieBSeedData.clubs()).map { club ->
            club.copy(players = SeasonEngine.autoSelectBestLineup(club.players, Formation.F_4_3_3))
        }
        val userClub = clubs.first { it.id == "flamengo" }

        val news = NewsGenerator.buildInitialNews(
            userClub = userClub,
            allClubs = clubs,
            seasonYear = 2026L,
            managerName = "Tecnico Teste"
        )

        assertTrue(news.isNotEmpty())
        assertEquals(news.size, news.map { it.id }.toSet().size)
        assertTrue(news.any { it.category == NewsCategory.CLUB_BUZZ && it.isBreaking })
        assertTrue(news.any { it.category == NewsCategory.TRANSFER })
    }

    @Test
    fun buildRoundNews_andTransferAndSyncNews_produceDistinctCategorizedItems() {
        val clubs = (SerieASeedData.clubs() + SerieBSeedData.clubs()).map { club ->
            club.copy(players = SeasonEngine.autoSelectBestLineup(club.players, Formation.F_4_3_3))
        }
        val userClub = clubs.first { it.id == "flamengo" }
        val fixtures = SeasonEngine.generateLeagueFixtures(clubs, userClub.division)

        var live = SeasonEngine.prepareLiveMatch(
            round = 1,
            userClub = userClub,
            allClubs = clubs,
            roundFixtures = fixtures.filter { it.round == 1 },
            ticketPriceLevel = TicketPriceLevel.NORMAL
        )
        while (!live.isFinished) {
            live = SeasonEngine.simulateTick(
                state = live.copy(isPaused = false),
                userClub = userClub,
                allClubs = clubs,
                formation = Formation.F_4_3_3,
                mentality = Mentality.EQUILIBRADO,
                marking = MarkingStyle.ZONAL
            )
        }

        val (updatedClubs, updatedFixtures) = SeasonEngine.applyRoundResults(
            clubs = clubs,
            fixtures = fixtures,
            liveState = live,
            userClubId = userClub.id,
            trainingFocus = TrainingFocus.EQUILIBRADO,
            marking = MarkingStyle.ZONAL
        )
        val updatedUser = updatedClubs.first { it.id == userClub.id }

        val roundNews = NewsGenerator.buildRoundNews(
            round = 1,
            userClub = updatedUser,
            liveState = live,
            allClubs = updatedClubs,
            updatedFixtures = updatedFixtures,
            incomingOffers = emptyList(),
            seasonSummary = null
        )
        assertTrue(roundNews.any { it.category == NewsCategory.MATCH_RESULT && it.round == 1 })

        val transferItem = NewsGenerator.buildTransferNews(
            round = 1,
            headline = "Reforço anunciado",
            body = "Novo jogador chegou ao elenco.",
            club = updatedUser,
            feeLabel = "R$ 5,0M"
        )
        assertEquals(NewsCategory.TRANSFER, transferItem.category)

        val syncItem = NewsGenerator.buildSyncNews(
            round = 1,
            report = SquadSyncReport(
                timestamp = 123456L,
                sourceLabel = "ESPN Soccer",
                isLiveNetwork = true,
                clubsSynced = 10,
                playersAdded = 2,
                playersUpdated = 5,
                playersRemoved = 1,
                entries = emptyList(),
                summary = "Sincronização concluída."
            ),
            userClub = updatedUser
        )
        assertEquals(NewsCategory.SYNC_UPDATE, syncItem.category)
        assertTrue(syncItem.isBreaking)
    }
}
