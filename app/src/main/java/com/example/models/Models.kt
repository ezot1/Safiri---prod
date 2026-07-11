package com.example.models

import androidx.compose.ui.graphics.Color

enum class UserRole {
    PARENT,
    DRIVER,
    ADMIN
}

data class Student(
    val id: String,
    val name: String,
    val initials: String,
    val avatarColor: Color,
    val boarded: Boolean = false
)

data class RouteStop(
    val id: String,
    val name: String,
    val isCompleted: Boolean = false,
    val isCurrent: Boolean = false,
    val isPending: Boolean = true,
    val liveEtaMin: Int = 0,
    val distanceMeters: Int = 0,
    val isParentStop: Boolean = false
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
