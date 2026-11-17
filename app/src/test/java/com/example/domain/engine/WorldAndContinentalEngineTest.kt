package com.example.domain.engine

import com.example.data.seed.LeagueCatalog
import com.example.domain.model.ChampionsLeaguePhase
import com.example.domain.model.ContinentalCupType
import com.example.domain.model.ContinentalPhase
import com.example.domain.model.Division
import com.example.domain.model.FairPlayState
import com.example.domain.model.FfpComplianceLevel
import com.example.domain.model.InjurySeverity
import com.example.domain.model.MedicalTreatmentType
import com.example.domain.model.MonthlyLedgerRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WorldAndContinentalEngineTest {

    @Test
    fun championsLeague_36ClubsLeaguePhaseToFinalProducesChampion() {
        val allClubs = LeagueCatalog.allClubs()
        val euroClub = allClubs.first { it.division == Division.PREMIER_LEAGUE }
        val clubMap = allClubs.associateBy { it.id }

        var ucl = ChampionsLeagueEngine.buildInitialChampionsLeague(euroClub, allClubs)
        assertEquals(36, ucl.standings.size)
        assertEquals(ChampionsLeaguePhase.LEAGUE_PHASE, ucl.phase)
        assertEquals(8 * 18, ucl.leagueFixtures.size)

        // Resolve all 8 league matchdays
        repeat(8) {
            ucl = ChampionsLeagueEngine.resolveLeagueMatchday(
                ucl = ucl,
                userFixtureId = null,
                userHomeScore = null,
                userAwayScore = null,
                clubMap = clubMap,
                random = Random(10 + it)
            )
        }
        assertEquals(ChampionsLeaguePhase.PLAYOFFS, ucl.phase)
        assertEquals(8, ucl.knockoutTies.size)

        // Resolve Playoffs -> R16 -> QF -> SF -> Final
        repeat(5) { stage ->
            if (ucl.phase != ChampionsLeaguePhase.FINISHED) {
                ucl = ChampionsLeagueEngine.resolveKnockoutStage(
                    ucl = ucl,
                    userTieId = null,
                    userHomeScore = null,
                    userAwayScore = null,
                    userShootout = null,
                    clubMap = clubMap,
                    random = Random(200 + stage)
                )
            }
        }
        assertEquals(ChampionsLeaguePhase.FINISHED, ucl.phase)
        assertNotNull(ucl.championClubId)
    }

    @Test
    fun continentalTournaments_libertadoresAndSulAmericanaGroupsAdvanceToChampions() {
        val allClubs = LeagueCatalog.allClubs()
        val brClub = allClubs.first { it.division == Division.SERIE_A }
        val clubMap = allClubs.associateBy { it.id }

        val tournaments = ContinentalGroupEngine.buildInitialContinentalTournaments(brClub, allClubs)
        assertTrue(tournaments.any { it.cupType == ContinentalCupType.LIBERTADORES })
        assertTrue(tournaments.any { it.cupType == ContinentalCupType.SUL_AMERICANA })

        tournaments.forEach { initial ->
            var state = initial
            assertEquals(ContinentalPhase.GROUP_STAGE, state.phase)
            val groups = state.standings.map { it.groupName }.distinct()
            assertEquals(8, groups.size)

            repeat(6) { md ->
                state = ContinentalGroupEngine.resolveGroupMatchday(
                    tournament = state,
                    userFixtureId = null,
                    userHomeScore = null,
                    userAwayScore = null,
                    clubMap = clubMap,
                    random = Random(30 + md)
                )
            }
            assertEquals(ContinentalPhase.ROUND_OF_16, state.phase)
            assertEquals(8, state.knockoutTies.size)

            repeat(4) { stage ->
                if (state.phase != ContinentalPhase.FINISHED) {
                    state = ContinentalGroupEngine.resolveKnockoutStage(
                        tournament = state,
                        userTieId = null,
                        userHomeScore = null,
                        userAwayScore = null,
                        userShootout = null,
                        clubMap = clubMap,
                        random = Random(300 + stage)
                    )
                }
            }
            assertEquals(ContinentalPhase.FINISHED, state.phase)
            assertNotNull(state.championClubId)
        }
    }

    @Test
    fun transferWindowEngine_respectsOpenAndClosedRangesAndNews() {
        val total = 38
        assertTrue(TransferWindowEngine.windowStatus(1, total).isOpen)
        assertTrue(TransferWindowEngine.windowStatus(6, total).isOpen)
        assertFalse(TransferWindowEngine.windowStatus(7, total).isOpen)
        assertFalse(TransferWindowEngine.windowStatus(18, total).isOpen)
        assertTrue(TransferWindowEngine.windowStatus(19, total).isOpen)
        assertTrue(TransferWindowEngine.windowStatus(24, total).isOpen)
        assertFalse(TransferWindowEngine.windowStatus(25, total).isOpen)

        val club = LeagueCatalog.allClubs().first()
        assertNotNull(TransferWindowEngine.transitionNewsForRound(7, total, club))
        assertNotNull(TransferWindowEngine.transitionNewsForRound(19, total, club))
        assertNotNull(TransferWindowEngine.transitionNewsForRound(25, total, club))
        assertNull(TransferWindowEngine.transitionNewsForRound(10, total, club))
    }

    @Test
    fun ballonDorEngine_appliesUserVoteAndComputesAnnualAwards() {
        val allClubs = LeagueCatalog.allClubs()
        val club = allClubs.first { it.division == Division.SERIE_A }
        val star = club.players.maxBy { it.overall }

        val withoutVote = BallonDorEngine.computeLiveCandidates(allClubs, null, limit = 30)
        val baseScore = withoutVote.firstOrNull { it.playerId == star.id }?.score

        val withVote = BallonDorEngine.computeLiveCandidates(allClubs, null, limit = 30, userVotedPlayerId = star.id)
        val votedScore = withVote.first { it.playerId == star.id }.score
        if (baseScore != null) {
            assertEquals(baseScore + 22, votedScore)
        }

        val awards = BallonDorEngine.computeAnnualAwards(
            clubs = allClubs,
            championsLeague = null,
            seasonYear = 2026L,
            userClub = club,
            managerName = "Técnico Teste",
            userVotedPlayerId = star.id
        )
        assertEquals(3, awards.ballonDorTop3.size)
        assertNotNull(awards.goldenBoot)
        assertNotNull(awards.topPlaymaker)
        assertNotNull(awards.bestGoalkeeper)
        assertTrue(awards.teamOfTheYear.isNotEmpty())
    }

    @Test
    fun medicalEngine_inflictsTracksHistoryAndAcceleratesRecovery() {
        val club = LeagueCatalog.allClubs().first()
        val player = club.players.first()

        val injured = MedicalEngine.inflictInjury(
            player = player,
            seasonYear = 2026L,
            round = 5,
            severity = InjurySeverity.MODERADA,
            random = Random(42)
        )
        assertTrue(injured.injuredRounds in InjurySeverity.MODERADA.minRounds..InjurySeverity.MODERADA.maxRounds)
        assertFalse(injured.isStarter)
        assertEquals(1, injured.injuryHistory.size)

        val treated = MedicalEngine.applyTreatment(
            player = injured,
            treatment = MedicalTreatmentType.TRATAMENTO_ESPECIALIZADO,
            currentRound = 6
        )
        assertTrue(treated.injuredRounds < injured.injuredRounds)

        var current = treated
        var round = 7
        while (current.injuredRounds > 0) {
            current = MedicalEngine.advanceRecoveryOneRound(current, round++)
        }
        assertEquals(0, current.injuredRounds)
        assertNull(current.currentInjuryName)
        assertNotNull(current.injuryHistory.first().recoveredRound)
    }

    @Test
    fun governanceAndFfpEngine_handlesElectionsAndFifaPunishments() {
        val club = LeagueCatalog.allClubs().first { it.division == Division.SERIE_A }
        val gov = GovernanceAndFfpEngine.buildInitialGovernance(club, seasonYear = 2026L)
        assertEquals(3, gov.electionCandidates.size)
        assertEquals(2030L, gov.nextElectionYear)
        assertFalse(gov.electionPending)

        // Cannot elect outside the 4-year cycle
        val (lockedGov, lockedGrant) = GovernanceAndFfpEngine.resolveElection(gov, "cand_invest", club, currentSeasonYear = 2027L)
        assertEquals(0L, lockedGrant)
        assertEquals(gov.presidentName, lockedGov.presidentName)

        // Reaching 2030 opens the election
        val year2030Outcome = GovernanceAndFfpEngine.evaluateRoundCompliance(
            currentFfp = FairPlayState(),
            currentGov = gov,
            club = club,
            userPosition = 5,
            monthlyLedger = emptyMap(),
            completedRound = 1,
            isSeasonEnd = false,
            seasonYear = 2030L
        )
        assertTrue(year2030Outcome.governance.electionPending)

        val (electedGov, grant) = GovernanceAndFfpEngine.resolveElection(
            year2030Outcome.governance,
            "cand_invest",
            club,
            currentSeasonYear = 2030L
        )
        assertTrue(grant > 0L)
        assertTrue(electedGov.presidentName.contains("Ricardo"))
        assertFalse(electedGov.electionPending)
        assertEquals(2034L, electedGov.nextElectionYear)
        assertEquals(2030L, electedGov.lastElectionYear)

        // Trigger periodic FFP breach on round 6 with severe negative balance
        val distressedClub = club.copy(budget = -12_000_000L)
        val outcome = GovernanceAndFfpEngine.evaluateRoundCompliance(
            currentFfp = FairPlayState(),
            currentGov = electedGov,
            club = distressedClub,
            userPosition = 18,
            monthlyLedger = mapOf(1 to MonthlyLedgerRecord(monthIndex = 1, ticketIncome = 500_000L, salaryExpense = 6_000_000L)),
            completedRound = 6,
            isSeasonEnd = false,
            seasonYear = 2031L
        )
        assertEquals(FfpComplianceLevel.BREACH, outcome.fairPlay.compliance)
        assertTrue(outcome.fairPlay.transferBanRounds > 0)
        assertTrue(outcome.pointsPenaltyApplied > 0)
        assertNotNull(outcome.punishmentHeadline)

        // End of season missing objective triggers penalty
        val seasonEndOutcome = GovernanceAndFfpEngine.evaluateRoundCompliance(
            currentFfp = FairPlayState(),
            currentGov = gov.copy(seasonObjectiveMaxPos = 4),
            club = club,
            userPosition = 15,
            monthlyLedger = emptyMap(),
            completedRound = 38,
            isSeasonEnd = true
        )
        assertTrue(seasonEndOutcome.governance.objectiveFailedPunished)
        assertTrue(seasonEndOutcome.fairPlay.transferBanRounds >= 3)
        assertTrue(seasonEndOutcome.fairPlay.totalFinesPaid > 0L)
    }
}
