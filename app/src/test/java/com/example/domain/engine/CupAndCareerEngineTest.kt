package com.example.domain.engine

import com.example.data.seed.LeagueCatalog
import com.example.domain.model.CupType
import com.example.domain.model.Division
import com.example.domain.model.Formation
import com.example.domain.model.MarkingStyle
import com.example.domain.model.Mentality
import com.example.domain.model.TicketPriceLevel
import com.example.domain.model.TrainingFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CupAndCareerEngineTest {

    @Test
    fun buildInitialCups_seedsRelevantKnockoutCompetitionsWithUserClub() {
        val allClubs = LeagueCatalog.allClubs()
        val serieAClub = allClubs.first { it.division == Division.SERIE_A }
        val brCups = CupAndCareerEngine.buildInitialCups(serieAClub, allClubs)

        assertTrue(brCups.any { it.cupType == CupType.COPA_DO_BRASIL })
        assertTrue(brCups.any { it.cupType == CupType.LIBERTADORES })
        brCups.forEach { cup ->
            assertEquals(8, cup.ties.size)
            assertTrue(cup.ties.any { it.homeClubId == serieAClub.id || it.awayClubId == serieAClub.id })
        }

        val euroClub = allClubs.first { it.division == Division.PREMIER_LEAGUE }
        val euroCups = CupAndCareerEngine.buildInitialCups(euroClub, allClubs)
        assertEquals(1, euroCups.size)
        assertEquals(CupType.CHAMPIONS, euroCups.first().cupType)
    }

    @Test
    fun cupBracket_advancesFromRoundOf16ToChampionWithShootoutTiebreakers() {
        val allClubs = LeagueCatalog.allClubs()
        val clubMap = allClubs.associateBy { it.id }
        val userClub = allClubs.first { it.division == Division.SERIE_A }
        var cup = CupAndCareerEngine.buildInitialCups(userClub, allClubs).first()

        // Resolve all 4 stages (Oitavas -> Quartas -> Semifinal -> Final) forcing draws on user's ties to exercise shootouts
        repeat(4) { stage ->
            assertEquals(stage, cup.currentStageIndex)
            val userTie = cup.ties.firstOrNull {
                it.stageIndex == stage && (it.homeClubId == userClub.id || it.awayClubId == userClub.id)
            }
            val shootout = if (userTie != null) {
                CupAndCareerEngine.simulatePenaltyShootout(
                    homeClubId = userTie.homeClubId,
                    awayClubId = userTie.awayClubId,
                    homeShort = "CAS",
                    awayShort = "VIS",
                    homeStrength = 78.0,
                    awayStrength = 78.0,
                    random = Random(42 + stage)
                )
            } else null

            cup = CupAndCareerEngine.resolveCupStageWithResults(
                cup = cup,
                userTieId = userTie?.id,
                userHomeScore = if (userTie != null) 1 else null,
                userAwayScore = if (userTie != null) 1 else null,
                userShootout = shootout,
                clubMap = clubMap,
                random = Random(100 + stage)
            )
        }

        assertNotNull(cup.championClubId)
        assertTrue(cup.ties.any { it.shootout != null })
    }

    @Test
    fun youthAcademyContractsSponsorsAndSeasonAwards_workEndToEnd() {
        val allClubs = LeagueCatalog.allClubs()
        val userClub = allClubs.first { it.division == Division.SERIE_A }

        // Youth intake & promotion
        val prospects = CupAndCareerEngine.generateYouthIntake(userClub, TrainingFocus.BASE)
        assertEquals(4, prospects.size)
        val promoted = CupAndCareerEngine.promoteProspectToPlayer(
            prospects.first(),
            userClub.players.map { it.shirtNumber }.toSet()
        )
        assertEquals(76, promoted.contractRounds)
        assertTrue(promoted.potential >= promoted.overall)

        // Contract renewal
        val starter = userClub.players.first()
        val renewed = CupAndCareerEngine.renewPlayerContract(starter)
        assertEquals(starter.contractRounds + 38, renewed.contractRounds)
        assertTrue(renewed.salary > starter.salary)

        // Sponsors
        val sponsors = CupAndCareerEngine.availableSponsorsForClub(userClub)
        assertEquals(4, sponsors.size)
        assertTrue(sponsors.all { it.perRoundBase > 0L })

        // Season awards
        val clubWithGoals = userClub.copy(
            points = 80,
            players = userClub.players.mapIndexed { idx, p ->
                if (idx == 0) p.copy(goalsSeason = 24, assistsSeason = 11) else p
            }
        )
        val updatedAll = allClubs.map { if (it.id == clubWithGoals.id) clubWithGoals else it }
        val awards = CupAndCareerEngine.computeSeasonAwards(updatedAll, clubWithGoals, 2026L, "Abel")
        assertEquals(24, awards.goldenBootGoals)
        assertEquals(11, awards.playmakerAssists)
        assertTrue(awards.prizeMoney > 0L)

        // Live knockout simulation reaches fulltime cleanly with ballZone in pitch bounds
        val cup = CupAndCareerEngine.buildInitialCups(clubWithGoals, updatedAll).first()
        val tie = cup.ties.first { it.homeClubId == clubWithGoals.id || it.awayClubId == clubWithGoals.id }
        var live = CupAndCareerEngine.prepareCupLiveMatch(tie, clubWithGoals, updatedAll, cup, TicketPriceLevel.NORMAL)
        assertTrue(live.isKnockout)
        while (!live.isFinished) {
            live = live.copy(isPaused = false)
            live = SeasonEngine.simulateTick(
                state = live,
                userClub = clubWithGoals,
                allClubs = updatedAll,
                formation = Formation.F_4_3_3,
                mentality = Mentality.OFENSIVO,
                marking = MarkingStyle.ZONAL
            )
            assertTrue(live.ballZone in 0.0f..1.0f)
        }
        assertEquals(90, live.minute)
    }
}
