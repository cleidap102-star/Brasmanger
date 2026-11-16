package com.example.domain.engine

import com.example.data.seed.LeagueCatalog
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
    fun leagueCatalog_containsAll10DivisionsWithUniqueClubIdsAndValidSchedules() {
        val allClubs = LeagueCatalog.allClubs()
        assertEquals(196, allClubs.size)
        assertEquals(196, allClubs.map { it.id }.toSet().size)

        Division.entries.forEach { division ->
            val divClubs = allClubs.filter { it.division == division }
            val fixtures = SeasonEngine.generateLeagueFixtures(allClubs, division)
            val expectedMatchesPerRound = divClubs.size / 2
            assertEquals(
                "Division ${division.code} should have ${division.totalRounds * expectedMatchesPerRound} matches",
                division.totalRounds * expectedMatchesPerRound,
                fixtures.size
            )
            for (round in 1..division.totalRounds) {
                val roundFixtures = fixtures.filter { it.round == round }
                assertEquals(expectedMatchesPerRound, roundFixtures.size)
            }
        }
    }

    @Test
    fun simulateMatchAndAdvanceRound_updatesTableAcrossAllLeagues() {
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

    @Test
    fun advanceToNextSeason_cascadesPromotionAndRelegationAcrossBrazilPyramid() {
        val allClubs = LeagueCatalog.allClubs().mapIndexed { index, club ->
            club.copy(points = 100 - (index % 20) * 3, wins = 20 - (index % 20))
        }
        val nextSeason = SeasonEngine.advanceToNextSeason(allClubs)

        val serieATop4B = SeasonEngine.sortStandings(allClubs, Division.SERIE_B).take(4).map { it.id }
        val serieBBottom4A = SeasonEngine.sortStandings(allClubs, Division.SERIE_A).takeLast(4).map { it.id }
        val serieBTop4C = SeasonEngine.sortStandings(allClubs, Division.SERIE_C).take(4).map { it.id }
        val serieCBottom4D = SeasonEngine.sortStandings(allClubs, Division.SERIE_D).take(4).map { it.id }

        serieATop4B.forEach { id ->
            assertEquals(Division.SERIE_A, nextSeason.first { it.id == id }.division)
        }
        serieBBottom4A.forEach { id ->
            assertEquals(Division.SERIE_B, nextSeason.first { it.id == id }.division)
        }
        serieBTop4C.forEach { id ->
            assertEquals(Division.SERIE_B, nextSeason.first { it.id == id }.division)
        }
        serieCBottom4D.forEach { id ->
            assertEquals(Division.SERIE_C, nextSeason.first { it.id == id }.division)
        }
    }
}
