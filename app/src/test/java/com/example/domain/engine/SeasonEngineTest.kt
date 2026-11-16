package com.example.domain.engine

import com.example.data.seed.SerieASeedData
import com.example.data.seed.SerieBSeedData
import com.example.domain.model.Division
import com.example.domain.model.Formation
import com.example.domain.model.MarkingStyle
import com.example.domain.model.Mentality
import com.example.domain.model.TicketPriceLevel
import com.example.domain.model.TrainingFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonEngineTest {

    @Test
    fun generateLeagueFixtures_serieA_produces38RoundsAnd380Matches() {
        val clubs = SerieASeedData.clubs()
        assertEquals(20, clubs.size)

        val fixtures = SeasonEngine.generateLeagueFixtures(clubs, Division.SERIE_A)
        assertEquals(380, fixtures.size)
        for (round in 1..38) {
            val roundFixtures = fixtures.filter { it.round == round }
            assertEquals("Round $round should have 10 matches", 10, roundFixtures.size)
            val playingIds = roundFixtures.flatMap { listOf(it.homeClubId, it.awayClubId) }.toSet()
            assertEquals("All 20 clubs play in round $round", 20, playingIds.size)
        }
    }

    @Test
    fun simulateMatchAndAdvanceRound_updatesTableAndPromotesTop4() {
        val clubs = (SerieASeedData.clubs() + SerieBSeedData.clubs()).map { club ->
            club.copy(players = SeasonEngine.autoSelectBestLineup(club.players, Formation.F_4_3_3))
        }
        val userClub = clubs.first { it.id == "flamengo" }
        val fixturesA = SeasonEngine.generateLeagueFixtures(clubs, Division.SERIE_A)
        val fixturesB = SeasonEngine.generateLeagueFixtures(clubs, Division.SERIE_B)
        val allFixtures = fixturesA + fixturesB

        var live = SeasonEngine.prepareLiveMatch(
            round = 1,
            userClub = userClub,
            allClubs = clubs,
            roundFixtures = fixturesA.filter { it.round == 1 },
            ticketPriceLevel = TicketPriceLevel.NORMAL
        )
        assertEquals(9, live.otherMatches.size)

        while (!live.isFinished) {
            live = live.copy(isPaused = false)
            live = SeasonEngine.simulateTick(
                state = live,
                userClub = userClub,
                allClubs = clubs,
                formation = Formation.F_4_3_3,
                mentality = Mentality.OFENSIVO,
                marking = MarkingStyle.ZONAL
            )
        }
        assertEquals(90, live.minute)

        val (updatedClubs, updatedFixtures) = SeasonEngine.applyRoundResults(
            clubs = clubs,
            fixtures = allFixtures,
            liveState = live,
            userClubId = userClub.id,
            trainingFocus = TrainingFocus.EQUILIBRADO,
            marking = MarkingStyle.ZONAL
        )
        assertTrue(updatedFixtures.filter { it.round == 1 }.all { it.played })
        assertTrue(updatedClubs.first { it.id == "flamengo" }.played == 1)
    }
}
