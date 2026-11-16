package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CareerFirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário deve autenticar com Google antes de acessar o Firestore.")
    }

    fun observeManagerProfile(userId: String): Flow<ManagerProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            ManagerProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureManagerProfile(defaultName: String): Result<ManagerProfile> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val snap = docRef.get().await()
        if (snap.exists()) {
            snap.toObject(
                ManagerProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: ManagerProfile(id = uid, userId = uid, managerName = defaultName)
        } else {
            val profile = ManagerProfile(
                userId = uid,
                managerName = defaultName.trim().ifBlank { "Técnico" }.take(60),
                autoSyncEnabled = true,
                preferredSimSpeed = 2L
            )
            docRef.set(profile.toCreateMap()).await()
            docRef.get().await().toObject(
                ManagerProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: profile.copy(id = uid)
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
    }

    suspend fun updateManagerSettings(
        managerName: String,
        autoSyncEnabled: Boolean,
        preferredSimSpeed: Long,
        totalTitlesDelta: Long = 0L,
        totalWinsDelta: Long = 0L,
        lastSquadSyncAt: Long? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val updates = buildMap<String, Any> {
            put("managerName", managerName.trim().ifBlank { "Técnico" }.take(60))
            put("autoSyncEnabled", autoSyncEnabled)
            put("preferredSimSpeed", preferredSimSpeed)
            if (totalTitlesDelta != 0L) put("totalTitles", FieldValue.increment(totalTitlesDelta))
            if (totalWinsDelta != 0L) put("totalWins", FieldValue.increment(totalWinsDelta))
            lastSquadSyncAt?.let { put("lastSquadSyncAt", it) }
            put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.update(updates).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
    }

    fun observeCareers(userId: String): Flow<List<CareerSave>> = flow {
        val colRef = db.collection("users").document(userId).collection("careers")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CareerSave::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun createCareer(career: CareerSave): Result<String> = runCatching {
        val uid = requireUserId()
        val careerId = career.id.ifBlank { "car_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
        val docRef = db.collection("users").document(uid).collection("careers").document(careerId)
        val payload = career.copy(userId = uid).toCreateMap()
        docRef.set(payload).await()
        careerId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/careers")
    }

    suspend fun updateCareer(career: CareerSave): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(career.id.isNotBlank()) { "Career ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("careers").document(career.id)
        docRef.update(career.toUpdateMap()).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/careers/${career.id}")
    }

    suspend fun retireCareer(careerId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("careers").document(careerId)
        docRef.update(
            mapOf(
                "status" to "retired",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/careers/$careerId")
    }

    suspend fun deleteCareer(careerId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("careers").document(careerId)
        docRef.delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/careers/$careerId")
    }

    fun observeTransfers(userId: String, careerId: String): Flow<List<TransferRecord>> = flow {
        val colRef = db.collection("users").document(userId)
            .collection("careers").document(careerId)
            .collection("transfers")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TransferRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun addTransfer(transfer: TransferRecord): Result<String> = runCatching {
        val uid = requireUserId()
        require(transfer.careerId.isNotBlank()) { "Career ID must not be blank" }
        val transferId = "tr_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("users").document(uid)
            .collection("careers").document(transfer.careerId)
            .collection("transfers").document(transferId)
        docRef.set(transfer.copy(userId = uid).toCreateMap()).await()
        transferId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/careers/${transfer.careerId}/transfers")
    }

    fun observeTrophies(userId: String, careerId: String): Flow<List<TrophyRecord>> = flow {
        val colRef = db.collection("users").document(userId)
            .collection("careers").document(careerId)
            .collection("trophies")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TrophyRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.seasonYear }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun addTrophy(trophy: TrophyRecord): Result<String> = runCatching {
        val uid = requireUserId()
        require(trophy.careerId.isNotBlank()) { "Career ID must not be blank" }
        val trophyId = "cup_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("users").document(uid)
            .collection("careers").document(trophy.careerId)
            .collection("trophies").document(trophyId)
        docRef.set(trophy.copy(userId = uid).toCreateMap()).await()
        trophyId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/careers/${trophy.careerId}/trophies")
    }
}
