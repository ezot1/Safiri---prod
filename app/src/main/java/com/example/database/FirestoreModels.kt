package com.example.database

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "PARENT",
    val phone: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw: Map<String, Any?> = mapOf(
            "userId" to userId,
            "email" to email,
            "displayName" to displayName,
            "role" to role,
            "phone" to phone,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class FirestoreStudent(
    @DocumentId val id: String = "",
    val parentUid: String = "",
    val name: String = "",
    val initials: String = "",
    val parentName: String = "",
    val parentEmail: String = "",
    val parentPhone: String = "",
    val assignedBusPlate: String = "",
    val assignedDriverName: String = "",
    val pickupStop: String = "",
    val dropoffStop: String = "",
    val status: String = "NOT_BOARDED",
    val classGrade: String? = null,
    val schoolName: String? = null,
    val boardedTime: String? = null,
    val absentReason: String? = null,
    val parentNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw: Map<String, Any?> = mapOf(
            "parentUid" to parentUid,
            "name" to name,
            "initials" to initials,
            "parentName" to parentName,
            "parentEmail" to parentEmail,
            "parentPhone" to parentPhone,
            "assignedBusPlate" to assignedBusPlate,
            "assignedDriverName" to assignedDriverName,
            "pickupStop" to pickupStop,
            "dropoffStop" to dropoffStop,
            "status" to status,
            "classGrade" to classGrade,
            "schoolName" to schoolName,
            "boardedTime" to boardedTime,
            "absentReason" to absentReason,
            "parentNote" to parentNote,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class FirestoreInterportalMessage(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val recipientUid: String? = null,
    val senderName: String = "",
    val senderRole: String = "PARENT",
    val recipientRole: String = "ALL",
    val targetBusPlate: String = "",
    val studentName: String? = null,
    val content: String = "",
    val displayTime: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw: Map<String, Any?> = mapOf(
            "senderUid" to senderUid,
            "recipientUid" to recipientUid,
            "senderName" to senderName,
            "senderRole" to senderRole,
            "recipientRole" to recipientRole,
            "targetBusPlate" to targetBusPlate,
            "studentName" to studentName,
            "content" to content,
            "displayTime" to displayTime,
            "createdAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class FirestoreBusAlert(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val title: String = "",
    val detail: String = "",
    val severity: String = "INFO",
    val status: String = "Ongoing",
    val busPlate: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw: Map<String, Any?> = mapOf(
            "authorUid" to authorUid,
            "title" to title,
            "detail" to detail,
            "severity" to severity,
            "status" to status,
            "busPlate" to busPlate,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class FirestoreCommunityIncident(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val title: String = "",
    val location: String = "",
    val category: String = "Traffic",
    val votes: Long = 1L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "reporterUid" to reporterUid,
            "title" to title,
            "location" to location,
            "category" to category,
            "votes" to votes,
            "isActive" to isActive,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class FirestoreFleetBus(
    @DocumentId val id: String = "",
    val plate: String = "",
    val driverUid: String = "",
    val driverName: String = "",
    val studentCount: Long = 0L,
    val occupancyCount: Long = 0L,
    val capacity: Long = 22L,
    val status: String = "On Time",
    val speed: Long = 0L,
    val lat: Double = -1.2921,
    val lng: Double = 36.8219,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
