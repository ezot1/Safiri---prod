package com.example.database

import android.content.Context
import com.example.R
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
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

    fun observeAllStudents(): Flow<List<FirestoreStudent>> =
        db.collection(COLLECTION_STUDENTS)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreStudent::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
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

    fun observeAllMessagesForStaff(): Flow<List<FirestoreInterportalMessage>> =
        db.collection(COLLECTION_MESSAGES)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreInterportalMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

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
        const val COLLECTION_USERS = "users"
        const val COLLECTION_STUDENTS = "students"
        const val COLLECTION_MESSAGES = "interportal_messages"
        const val COLLECTION_ALERTS = "alerts"
        const val COLLECTION_INCIDENTS = "community_incidents"
        const val COLLECTION_FLEET_BUSES = "fleet_buses"
    }
}
