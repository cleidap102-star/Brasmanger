package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.firebase.CareerSave
import com.example.data.firebase.TransferRecord
import com.example.data.firebase.TrophyRecord
import com.example.data.seed.LeagueCatalog
import com.example.domain.engine.CupAndCareerEngine
import com.example.domain.engine.SeasonEngine
import com.example.domain.model.MonthlyLedgerRecord
import com.example.domain.model.SeasonSnapshot
import com.example.domain.model.TrainingFocus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalCareerRepositoryTest {

    private lateinit var db: BrasManagerDatabase
    private lateinit var repo: LocalCareerRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BrasManagerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = LocalCareerRepository(db.careerLocalDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun careerLifecycle_persistsProfileCareersTransfersAndTrophiesLocally() = runBlocking {
        val profile = repo.ensureManagerProfile("Técnico Local").getOrThrow()
        assertEquals("Técnico Local", profile.managerName)

        val careerId = repo.createCareer(
            CareerSave(
                clubId = "flamengo",
                clubName = "Flamengo",
                division = "SERIE_A",
                seasonYear = 2026L,
                currentRound = 1L,
                budget = 50_000_000L,
                activeStarterIds = listOf("p1", "p2", "p3")
            )
        ).getOrThrow()

        val careers = repo.observeCareers().first()
        assertEquals(1, careers.size)
        assertEquals("Flamengo", careers.first().clubName)
        assertEquals(listOf("p1", "p2", "p3"), careers.first().activeStarterIds)

        repo.addTransfer(
            TransferRecord(
                careerId = careerId,
                playerName = "Craque Teste",
                position = "ATA",
                fromClubName = "Palmeiras",
                toClubName = "Flamengo",
                fee = 10_000_000L,
                transferType = "BUY"
            )
        ).getOrThrow()

        repo.addTrophy(
            TrophyRecord(
                careerId = careerId,
                competitionName = "Brasileirão Série A",
                clubName = "Flamengo",
                seasonYear = 2026L,
                finalPosition = 1L
            )
        ).getOrThrow()

        assertEquals(1, repo.observeTransfers(careerId).first().size)
        assertEquals(1, repo.observeTrophies(careerId).first().size)

        repo.deleteCareer(careerId).getOrThrow()
        assertTrue(repo.observeCareers().first().isEmpty())
    }

    @Test
    fun seasonSnapshot_roundtripsFullCareerStateThroughRoomAndMoshi() = runBlocking {
        repo.ensureManagerProfile("Técnico Inicial").getOrThrow()
        repo.updateManagerName("Abel Ferreira").getOrThrow()
        assertEquals("Abel Ferreira", repo.observeManagerProfile().first()?.managerName)

        val allClubs = LeagueCatalog.allClubs()
        val userClub = allClubs.first()
        val fixtures = SeasonEngine.generateAllFixtures(allClubs)
        val cups = CupAndCareerEngine.buildInitialCups(userClub, allClubs)
        val ucl = com.example.domain.engine.ChampionsLeagueEngine.buildInitialChampionsLeague(userClub, allClubs)
        val continental = com.example.domain.engine.ContinentalGroupEngine.buildInitialContinentalTournaments(userClub, allClubs)
        val awardsRecord = com.example.domain.engine.BallonDorEngine.computeAnnualAwards(
            clubs = allClubs,
            championsLeague = ucl,
            seasonYear = 2026L,
            userClub = userClub,
            managerName = "Abel Ferreira"
        )
        val sponsor = CupAndCareerEngine.availableSponsorsForClub(userClub).first()
        val youth = CupAndCareerEngine.generateYouthIntake(userClub, TrainingFocus.BASE)
        val ffp = com.example.domain.model.FairPlayState(
            compliance = com.example.domain.model.FfpComplianceLevel.WARNING,
            transferBanRounds = 2,
            pointsDeducted = 3
        )
        val gov = com.example.domain.engine.GovernanceAndFfpEngine.buildInitialGovernance(userClub)

        val careerId = repo.createCareer(
            CareerSave(
                clubId = userClub.id,
                clubName = userClub.name,
                division = userClub.division.code
            )
        ).getOrThrow()

        val snapshot = SeasonSnapshot(
            clubs = allClubs,
            fixtures = fixtures,
            monthlyLedger = mapOf(
                0 to MonthlyLedgerRecord(
                    monthIndex = 0,
                    ticketIncome = 2_500_000L,
                    salaryExpense = 1_100_000L,
                    otherDelta = 300_000L,
                    roundsPlayed = 3,
                    homeMatchesPlayed = 2,
                    endingBudget = userClub.budget + 1_700_000L
                )
            ),
            seasonTicketRevenue = 2_500_000L,
            cups = cups,
            activeSponsor = sponsor,
            youthProspects = youth,
            pendingJobOffers = emptyList(),
            isSacked = false,
            championsLeague = ucl,
            annualAwardsHistory = listOf(awardsRecord),
            continentalTournaments = continental,
            fairPlay = ffp,
            governance = gov,
            userBallonDorPlayerId = userClub.players.first().id,
            userBallonDorVoteSeason = 2026L
        )

        repo.saveSeasonSnapshot(careerId, snapshot).getOrThrow()
        val loaded = repo.loadSeasonSnapshot(careerId)
        assertNotNull(loaded)
        assertEquals(allClubs.size, loaded!!.clubs.size)
        assertEquals(fixtures.size, loaded.fixtures.size)
        assertEquals(2_500_000L, loaded.seasonTicketRevenue)
        assertEquals(2_500_000L, loaded.monthlyLedger[0]?.ticketIncome)
        assertEquals(cups.size, loaded.cups.size)
        assertEquals(sponsor.id, loaded.activeSponsor?.id)
        assertEquals(youth.size, loaded.youthProspects.size)
        assertEquals(36, loaded.championsLeague?.standings?.size)
        assertEquals(continental.size, loaded.continentalTournaments.size)
        assertEquals(1, loaded.annualAwardsHistory.size)
        assertEquals(2, loaded.fairPlay.transferBanRounds)
        assertEquals(gov.presidentName, loaded.governance.presidentName)
        assertEquals(2026L, loaded.userBallonDorVoteSeason)

        repo.deleteCareer(careerId).getOrThrow()
        assertNull(repo.loadSeasonSnapshot(careerId))
    }
}

