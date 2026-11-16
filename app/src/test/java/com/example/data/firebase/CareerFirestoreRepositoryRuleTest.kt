package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CareerFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureManagerProfile_authenticatedOwner_createsAndUpdatesProfile() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CareerFirestoreRepository(firestore)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureManagerProfile("Técnico Alice")
        }
        assertTrue("Expected profile creation success", profileResult.isSuccess)
        val profile = profileResult.getOrThrow()
        assertEquals(uid, profile.userId)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateManagerSettings(
                managerName = "Mister Alice",
                autoSyncEnabled = false,
                preferredSimSpeed = 4L,
                totalWinsDelta = 1L
            )
        }
        assertTrue("Expected profile update success", updateResult.isSuccess)
    }

    @Test
    fun createAndObserveCareer_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CareerFirestoreRepository(firestore)
        val careerId = "car_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val career = CareerSave(
            id = careerId,
            userId = uid,
            clubId = "flamengo",
            clubName = "Flamengo",
            division = "SERIE_A",
            seasonYear = 2026L,
            currentRound = 1L,
            budget = 90_000_000L,
            stadiumCapacity = 78838L,
            ticketPriceLevel = "NORMAL",
            trainingFocus = "EQUILIBRADO",
            formation = "4-3-3",
            mentality = "OFENSIVO",
            marking = "PRESSAO",
            boardConfidence = 80L,
            points = 0L,
            leaguePosition = 1L,
            activeStarterIds = listOf("fla_1", "fla_2", "fla_3"),
            status = "active"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createCareer(career) }
        assertTrue("Expected career creation success", createResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCareers(uid).first { list -> list.any { it.id == careerId } }
        }
        val saved = observed.first { it.id == careerId }
        assertEquals("Flamengo", saved.clubName)

        val transferResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addTransfer(
                TransferRecord(
                    userId = uid,
                    careerId = careerId,
                    playerName = "Pedro",
                    position = "ATA",
                    fromClubName = "Flamengo",
                    toClubName = "Palmeiras",
                    fee = 45_000_000L,
                    transferType = "SELL",
                    seasonYear = 2026L,
                    roundNumber = 2L
                )
            )
        }
        assertTrue("Expected transfer log success", transferResult.isSuccess)
    }

    @Test
    fun observeCareers_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CareerFirestoreRepository(firestore)
        val careerId = "car_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCareer(
                CareerSave(
                    id = careerId,
                    userId = aliceUid,
                    clubId = "palmeiras",
                    clubName = "Palmeiras",
                    activeStarterIds = listOf("pal_1")
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CareerFirestoreRepository(firestore)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeCareers(aliceUid).first()
            }
        } catch (e: Exception) {
            caught = e
        }

        assertNotNull("Expected PERMISSION_DENIED for cross-user query", caught)
        val fsError = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", fsError)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
    }

    @Test
    fun observeCareers_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = CareerFirestoreRepository(firestore)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeCareers("some_uid_123").first()
            }
        } catch (e: Exception) {
            caught = e
        }

        assertNotNull("Expected PERMISSION_DENIED for unauthenticated query", caught)
        val fsError = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", fsError)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice.manager@test.com"
        const val BOB_EMAIL = "bob.manager@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
