package com.example.models

import androidx.compose.ui.graphics.Color

enum class UserRole {
    PARENT,
    DRIVER,
    ADMIN
}

enum class StudentStatus {
    NOT_BOARDED,
    BOARDED,
    DROPPED_OFF,
    ABSENT
}

enum class JourneyEventType {
    BOARDED,
    IN_TRANSIT,
    ARRIVED_SCHOOL,
    DEPARTED_SCHOOL,
    ALIGHTED
}

data class Student(
    val id: String,
    val name: String,
    val initials: String,
    val avatarColor: Color = Color(0xFF3B82F6),
    val parentName: String = "Sarah Ochieng",
    val parentEmail: String = "parent@safiri.co.ke",
    val parentPhone: String = "0712345678",
    val assignedBusPlate: String = "KDE 732X",
    val assignedDriverName: String = "Erick Mwangi",
    val pickupStop: String = "Kilimani Stop",
    val dropoffStop: String = "Upper Hill / Academy",
    val status: StudentStatus = StudentStatus.NOT_BOARDED,
    val boardedTime: String? = null,
    val absentReason: String? = null,
    val parentNote: String? = null,
    val boarded: Boolean = (status == StudentStatus.BOARDED),
    val classGrade: String = "Grade 3",
    val schoolName: String = "St. Austin's Academy",
    val photoUrl: String? = null,
    val routeId: String = "route_1"
)

data class ParentStudentLink(
    val id: String = java.util.UUID.randomUUID().toString(),
    val parentId: String,
    val studentId: String,
    val relationship: String = "parent",
    val isPrimary: Boolean = true,
    val createdAt: String = System.currentTimeMillis().toString()
)

data class JourneyEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val studentId: String,
    val tripId: String = "trip_today",
    val eventType: JourneyEventType,
    val stopName: String,
    val busPlate: String,
    val lat: Double = -1.2921,
    val lng: Double = 36.8219,
    val notes: String? = null,
    val timestamp: String,
    val dateStr: String = "Today"
)

data class RegistrationData(
    val step: Int = 1,
    val parentFullName: String = "",
    val parentEmail: String = "",
    val parentPassword: String = "",
    val parentPhone: String = "",
    val childFullName: String = "",
    val childGrade: String = "Grade 3",
    val schoolName: String = "St. Austin's Academy",
    val pickupStop: String = "Kilimani Stop",
    val photoUrl: String? = null,
    val completed: Boolean = false
)

data class InterportalMessage(
    val id: String,
    val senderName: String,
    val senderRole: String,
    val recipientRole: String, // DRIVER, PARENT, ADMIN, ALL
    val targetBusPlate: String,
    val studentName: String? = null,
    val content: String,
    val timestamp: String,
    val isRead: Boolean = false
)

data class RouteStop(
    val id: String,
    val name: String,
    val isCompleted: Boolean = false,
    val isCurrent: Boolean = false,
    val isPending: Boolean = true,
    val liveEtaMin: Int = 0,
    val distanceMeters: Int = 0,
    val isParentStop: Boolean = false,
    val platformBay: String = "Bay 1",
    val delayNote: String = "On time",
    val transferConnection: String = "Direct Shuttle"
)

data class IncidentReport(
    val id: String,
    val title: String,
    val location: String,
    val reportedTime: String,
    val votes: Int,
    val category: String,
    val isUpvoted: Boolean = false
)

data class TripHistoryItem(
    val date: String,
    val timeRange: String,
    val studentCount: Int,
    val statusText: String,
    val statusColor: Color
)

data class RouteOption(
    val id: String,
    val busPlate: String,
    val driverName: String,
    val departureTime: String,
    val arrivalTime: String,
    val duration: String,
    val stopSequence: List<String>,
    val isAlternative: Boolean = false
)

enum class AlertSeverity {
    DANGER,  // Red
    WARNING, // Amber
    INFO     // Blue
}

enum class SubscriptionStatus {
    ACTIVE,
    PAYMENT_DUE,
    INACTIVE
}

data class InvoiceItem(
    val id: String,
    val date: String,
    val amount: String,
    val status: String,
    val paymentMethod: String,
    val receiptNumber: String
)

data class BusAlert(
    val id: String,
    val title: String,
    val detail: String,
    val timestamp: String,
    val severity: AlertSeverity,
    val status: String
)

data class DriverRosterItem(
    val id: String,
    val name: String,
    val initials: String,
    val avatarColor: Color,
    val busPlate: String,
    val yearsExperience: Int,
    val performanceProgress: Float, // 0 to 1
    val onTimePct: Int
)

data class FleetBusItem(
    val id: String,
    val plate: String,
    val driverName: String,
    val studentCount: Int,
    val occupancyCount: Int,
    val capacity: Int,
    val status: String,
    val statusColor: Color,
    val latOffset: Float = 0f,
    val lngOffset: Float = 0f,
    val speed: Int = 42,
    val signal: String = "Excellent"
)
