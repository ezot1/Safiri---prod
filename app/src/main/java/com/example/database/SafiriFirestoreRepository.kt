package com.example.database

import android.content.Context
import com.example.R
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SafiriFirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: RuntimeException("Firestore task failed"))
            }
        }
    }

    // --- USER PROFILES ---

    suspend fun getUserProfile(uid: String): Result<FirestoreUserProfile?> {
        val path = "$COLLECTION_USERS/$uid"
        return try {
            val snap = db.collection(COLLECTION_USERS).document(uid).get().awaitResult()
            val profile = snap.toObject(
                FirestoreUserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun createUserProfile(profile: FirestoreUserProfile): Result<Unit> {
        val path = "$COLLECTION_USERS/${profile.userId}"
        return try {
            db.collection(COLLECTION_USERS).document(profile.userId)
                .set(profile.toCreateMap())
                .awaitResult()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveDeviceMessagingToken(uid: String, token: String): Result<Unit> {
        val tokenId = tokenDocumentId(token)
        val path = "$COLLECTION_USERS/$uid/$COLLECTION_DEVICE_TOKENS/$tokenId"
        return try {
            db.collection(COLLECTION_USERS).document(uid)
                .collection(COLLECTION_DEVICE_TOKENS).document(tokenId)
                .set(
                    mapOf(
                        "token" to token,
                        "platform" to "android",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .awaitResult()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeDeviceMessagingToken(uid: String, token: String): Result<Unit> {
        val tokenId = tokenDocumentId(token)
        val path = "$COLLECTION_USERS/$uid/$COLLECTION_DEVICE_TOKENS/$tokenId"
        return try {
            db.collection(COLLECTION_USERS).document(uid)
                .collection(COLLECTION_DEVICE_TOKENS).document(tokenId)
                .delete()
                .awaitResult()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeSubscription(): Flow<FirestoreSubscription?> =
        db.collection(COLLECTION_SUBSCRIPTIONS).document(SUBSCRIPTION_DOCUMENT_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    FirestoreSubscription::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeSubscriptionInvoices(): Flow<List<FirestoreSubscriptionInvoice>> =
        db.collection(COLLECTION_SUBSCRIPTIONS)
            .document(SUBSCRIPTION_DOCUMENT_ID)
            .collection(COLLECTION_INVOICES)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreSubscriptionInvoice::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun createParentRegistration(
        profile: FirestoreUserProfile,
        student: FirestoreStudent
    ): Result<Unit> {
        val path = "$COLLECTION_USERS/${profile.userId}, $COLLECTION_STUDENTS/${student.id}"
        return try {
            db.runBatch { batch ->
                batch.set(
                    db.collection(COLLECTION_USERS).document(profile.userId),
                    profile.toCreateMap()
                )
                batch.set(
                    db.collection(COLLECTION_STUDENTS).document(student.id),
                    student.toCreateMap()
                )
            }.awaitResult()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- STUDENTS ---

    fun observeStudentsForParent(parentUid: String): Flow<List<FirestoreStudent>> =
        db.collection(COLLECTION_STUDENTS)
            .whereEqualTo("parentUid", parentUid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreStudent::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeStudentsForDriver(driverUid: String): Flow<List<FirestoreStudent>> =
        observeDriverBusPlate(driverUid).flatMapLatest { busPlate ->
            if (busPlate.isBlank()) {
                flowOf(emptyList<FirestoreStudent>())
            } else {
                db.collection(COLLECTION_STUDENTS)
                    .whereEqualTo("assignedBusPlate", busPlate)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            FirestoreStudent::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
            }
        }

    fun observeAllStudents(): Flow<List<FirestoreStudent>> =
        db.collection(COLLECTION_STUDENTS)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreStudent::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun updateStudentBusAssignmentByAdmin(
        studentId: String,
        busPlate: String,
        driverName: String,
        pickupStop: String,
        dropoffStop: String
    ): Result<Unit> {
        val path = "$COLLECTION_STUDENTS/$studentId"
        return try {
            db.collection(COLLECTION_STUDENTS).document(studentId)
                .update(
                    mapOf(
                        "assignedBusPlate" to busPlate,
                        "assignedDriverName" to driverName,
                        "pickupStop" to pickupStop,
                        "dropoffStop" to dropoffStop,
                        "schoolName" to dropoffStop,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .awaitResult()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun getStudentById(studentId: String): Result<FirestoreStudent?> {
        val path = "$COLLECTION_STUDENTS/$studentId"
        return try {
            val snap = db.collection(COLLECTION_STUDENTS).document(studentId).get().awaitResult()
            Result.success(
                snap.toObject(
                    FirestoreStudent::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun createStudent(studentId: String, student: FirestoreStudent): Result<String> {
        val path = "$COLLECTION_STUDENTS/$studentId"
        return try {
            db.collection(COLLECTION_STUDENTS).document(studentId)
                .set(student.toCreateMap())
                .awaitResult()
            Result.success(studentId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStudentByParent(
        studentId: String,
        status: String,
        pickupStop: String? = null,
        absentReason: String? = null,
        parentNote: String? = null
    ): Result<Unit> {
        val path = "$COLLECTION_STUDENTS/$studentId"
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            pickupStop?.let { updates["pickupStop"] = it }
            absentReason?.let { updates["absentReason"] = it }
            parentNote?.let { updates["parentNote"] = it }
            db.collection(COLLECTION_STUDENTS).document(studentId)
                .update(updates)
                .awaitResult()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStudentStatusByDriver(
        studentId: String,
        status: String,
        boardedTime: String? = null
    ): Result<Unit> {
        val path = "$COLLECTION_STUDENTS/$studentId"
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            boardedTime?.let { updates["boardedTime"] = it }
            db.collection(COLLECTION_STUDENTS).document(studentId)
                .update(updates)
                .awaitResult()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- INTERPORTAL MESSAGES ---

    fun observeMessagesForSender(senderUid: String): Flow<List<FirestoreInterportalMessage>> =
        db.collection(COLLECTION_MESSAGES)
            .whereEqualTo("senderUid", senderUid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreInterportalMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeMessagesForDriver(driverUid: String): Flow<List<FirestoreInterportalMessage>> =
        observeDriverBusPlate(driverUid).flatMapLatest { busPlate ->
            if (busPlate.isBlank()) {
                flowOf(emptyList<FirestoreInterportalMessage>())
            } else {
                db.collection(COLLECTION_MESSAGES)
                    .whereEqualTo("targetBusPlate", busPlate)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            FirestoreInterportalMessage::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
            }
        }

    fun observeAllMessagesForStaff(): Flow<List<FirestoreInterportalMessage>> =
        db.collection(COLLECTION_MESSAGES)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreInterportalMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    private fun observeDriverBusPlate(driverUid: String): Flow<String> =
        db.collection(COLLECTION_DRIVERS)
            .document(driverUid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.getBoolean("active") == true) {
                    snapshot.getString("busPlate").orEmpty()
                } else {
                    ""
                }
            }
            .distinctUntilChanged()

    suspend fun sendInterportalMessage(
        messageId: String,
        message: FirestoreInterportalMessage
    ): Result<String> {
        val path = "$COLLECTION_MESSAGES/$messageId"
        return try {
            db.collection(COLLECTION_MESSAGES).document(messageId)
                .set(message.toCreateMap())
                .awaitResult()
            Result.success(messageId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- ALERTS ---

    fun observeActiveAlerts(): Flow<List<FirestoreBusAlert>> =
        db.collection(COLLECTION_ALERTS)
            .whereIn("status", listOf("Ongoing", "Upcoming", "Scheduled"))
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreBusAlert::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun createAlert(alertId: String, alert: FirestoreBusAlert): Result<String> {
        val path = "$COLLECTION_ALERTS/$alertId"
        return try {
            db.collection(COLLECTION_ALERTS).document(alertId)
                .set(alert.toCreateMap())
                .awaitResult()
            Result.success(alertId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveAlert(alertId: String): Result<Unit> {
        val path = "$COLLECTION_ALERTS/$alertId"
        return try {
            db.collection(COLLECTION_ALERTS).document(alertId)
                .update(
                    mapOf(
                        "status" to "Resolved",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .awaitResult()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- COMMUNITY INCIDENTS ---

    fun observeActiveIncidents(): Flow<List<FirestoreCommunityIncident>> =
        db.collection(COLLECTION_INCIDENTS)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreCommunityIncident::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun createIncident(
        incidentId: String,
        incident: FirestoreCommunityIncident
    ): Result<String> {
        val path = "$COLLECTION_INCIDENTS/$incidentId"
        return try {
            db.collection(COLLECTION_INCIDENTS).document(incidentId)
                .set(incident.toCreateMap())
                .awaitResult()
            Result.success(incidentId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementIncidentVotes(incidentId: String, delta: Long): Result<Unit> {
        val path = "$COLLECTION_INCIDENTS/$incidentId"
        return try {
            db.collection(COLLECTION_INCIDENTS).document(incidentId)
                .update(
                    mapOf(
                        "votes" to FieldValue.increment(delta),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .awaitResult()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- FLEET BUSES ---

    fun observeActiveFleetBuses(): Flow<List<FirestoreFleetBus>> =
        db.collection(COLLECTION_FLEET_BUSES)
            .whereIn("status", listOf("On Time", "Delayed", "Active"))
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreFleetBus::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun getFleetBusForDriver(driverUid: String): Result<FirestoreFleetBus?> {
        val path = COLLECTION_FLEET_BUSES
        return try {
            val snapshot = db.collection(COLLECTION_FLEET_BUSES)
                .whereEqualTo("driverUid", driverUid)
                .limit(2)
                .get()
                .awaitResult()
            if (snapshot.size() > 1) {
                Result.failure(IllegalStateException("Driver is assigned to more than one fleet bus."))
            } else {
                val document = snapshot.documents.firstOrNull()
                Result.success(
                    document?.toObject(FirestoreFleetBus::class.java)?.copy(id = document.id)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun updateBusLocationByDriver(
        busId: String,
        lat: Double,
        lng: Double,
        speed: Long
    ): Result<Unit> {
        val path = "$COLLECTION_FLEET_BUSES/$busId"
        return try {
            db.collection(COLLECTION_FLEET_BUSES).document(busId)
                .update(
                    mapOf(
                        "lat" to lat,
                        "lng" to lng,
                        "speed" to speed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .awaitResult()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBusTelemetryByDriver(
        busId: String,
        lat: Double,
        lng: Double,
        speed: Long,
        occupancyCount: Long,
        status: String
    ): Result<Unit> {
        val path = "$COLLECTION_FLEET_BUSES/$busId"
        return try {
            db.collection(COLLECTION_FLEET_BUSES).document(busId)
                .update(
                    mapOf(
                        "lat" to lat,
                        "lng" to lng,
                        "speed" to speed,
                        "occupancyCount" to occupancyCount,
                        "status" to status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .awaitResult()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    companion object {
        private const val COLLECTION_DEVICE_TOKENS = "device_tokens"
        private const val COLLECTION_SUBSCRIPTIONS = "subscriptions"
        private const val COLLECTION_INVOICES = "invoices"
        private const val SUBSCRIPTION_DOCUMENT_ID = "main"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_DRIVERS = "drivers"
        const val COLLECTION_STUDENTS = "students"
        const val COLLECTION_MESSAGES = "interportal_messages"
        const val COLLECTION_ALERTS = "alerts"
        const val COLLECTION_INCIDENTS = "community_incidents"
        const val COLLECTION_FLEET_BUSES = "fleet_buses"

        private fun tokenDocumentId(token: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(token.toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte) }
    }
}
