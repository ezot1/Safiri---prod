package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.UserEntity
import com.example.models.*
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.cos

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.getDatabase(application).userDao()

    // --- AUTHENTICATION STATE ---
    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    private val _currentRole = MutableStateFlow<UserRole?>(null)
    val currentRole: StateFlow<UserRole?> = _currentRole.asStateFlow()

    private val _currentUserName = MutableStateFlow<String>("User")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    // --- SIMULATED BUS TRACKING STATE (PARENT & SHARED) ---
    private val _progress = MutableStateFlow(0f) // 0.0 to 1.0 along the route path
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _etaMinutes = MutableStateFlow(15)
    val etaMinutes: StateFlow<Int> = _etaMinutes.asStateFlow()

    private val _distanceKm = MutableStateFlow(4.8f)
    val distanceKm: StateFlow<Float> = _distanceKm.asStateFlow()

    private val _stopsLeft = MutableStateFlow(3)
    val stopsLeft: StateFlow<Int> = _stopsLeft.asStateFlow()

    private val _progressPct = MutableStateFlow(0)
    val progressPct: StateFlow<Int> = _progressPct.asStateFlow()

    private val _routeStops = MutableStateFlow<List<RouteStop>>(emptyList())
    val routeStops: StateFlow<List<RouteStop>> = _routeStops.asStateFlow()

    private val _savedStops = MutableStateFlow<List<RouteStop>>(emptyList())
    val savedStops: StateFlow<List<RouteStop>> = _savedStops.asStateFlow()

    private val _nearbyStops = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val nearbyStops: StateFlow<List<Pair<String, String>>> = _nearbyStops.asStateFlow()

    private val _selectedRouteOption = MutableStateFlow<String>("fastest")
    val selectedRouteOption: StateFlow<String> = _selectedRouteOption.asStateFlow()

    private val _starredStops = MutableStateFlow<Set<String>>(setOf("Kilimani", "St. Mary's Academy"))
    val starredStops: StateFlow<Set<String>> = _starredStops.asStateFlow()

    // --- DRIVER EXPERIENCE STATE ---
    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _boardedCount = MutableStateFlow(0)
    val boardedCount: StateFlow<Int> = _boardedCount.asStateFlow()

    private val _kilimaniComplete = MutableStateFlow(false)
    val kilimaniComplete: StateFlow<Boolean> = _kilimaniComplete.asStateFlow()

    private val _driverNextStopName = MutableStateFlow("Kilimani Stop")
    val driverNextStopName: StateFlow<String> = _driverNextStopName.asStateFlow()

    private val _sosActive = MutableStateFlow(false)
    val sosActive: StateFlow<Boolean> = _sosActive.asStateFlow()

    // --- ADMIN EXPERIENCE STATE ---
    private val _activeAlerts = MutableStateFlow<List<BusAlert>>(emptyList())
    val activeAlerts: StateFlow<List<BusAlert>> = _activeAlerts.asStateFlow()

    private val _adminFleetBuses = MutableStateFlow<List<FleetBusItem>>(emptyList())
    val adminFleetBuses: StateFlow<List<FleetBusItem>> = _adminFleetBuses.asStateFlow()

    private val _driversList = MutableStateFlow<List<DriverRosterItem>>(emptyList())
    val driversList: StateFlow<List<DriverRosterItem>> = _driversList.asStateFlow()

    // Notification Toggles (Me / Profile screen)
    val toggleTenMin = MutableStateFlow(true)
    val toggleChildBoarded = MutableStateFlow(true)
    val toggleDelayWarning = MutableStateFlow(true)
    val toggleTripCompleted = MutableStateFlow(false)

    // Admin Settings Notifications
    val toggleAdminEmergency = MutableStateFlow(true)
    val toggleAdminGeofence = MutableStateFlow(false)
    val toggleAdminDailySummary = MutableStateFlow(true)

    // Job holding the simulation tick loop
    private var simulationJob: Job? = null
    private var adminDriftTicks = 0f

    init {
        resetData()
        seedDefaultUsers()
        startSimulation()
    }

    private fun seedDefaultUsers() {
        viewModelScope.launch {
            try {
                if (userDao.getUserByEmail("parent@safiri.co.ke") == null) {
                    userDao.insertUser(UserEntity("parent@safiri.co.ke", "Sarah Ochieng", "PARENT", "email", "demo1234"))
                }
                if (userDao.getUserByEmail("driver@safiri.co.ke") == null) {
                    userDao.insertUser(UserEntity("driver@safiri.co.ke", "Erick Mwangi", "DRIVER", "email", "demo1234"))
                }
                if (userDao.getUserByEmail("admin@safiri.co.ke") == null) {
                    userDao.insertUser(UserEntity("admin@safiri.co.ke", "Admin Chief", "ADMIN", "email", "demo1234"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun resetData() {
        // Initial stops list
        _routeStops.value = listOf(
            RouteStop("1", "Kawangware", isCompleted = false, isCurrent = true, liveEtaMin = 2, distanceMeters = 300),
            RouteStop("2", "Westlands", isCompleted = false, liveEtaMin = 8, distanceMeters = 1800),
            RouteStop("3", "Kilimani", isCompleted = false, isParentStop = true, liveEtaMin = 14, distanceMeters = 3400),
            RouteStop("4", "Upper Hill", isCompleted = false, liveEtaMin = 22, distanceMeters = 5100)
        )

        // Nearby stops detected
        _nearbyStops.value = listOf(
            "Kilimani Bus Bay" to "120m away",
            "Valley Arcade Gate" to "340m away",
            "Yaya Centre" to "510m away"
        )

        // Initial Driver boarding list
        _students.value = listOf(
            Student("1", "Brian Kamau", "BK", AccentBlue),
            Student("2", "Zara Mwangi", "ZM", GreenAccent),
            Student("3", "Liam Ochieng", "LO", AmberAccent),
            Student("4", "Amina Njoroge", "AN", PurpleAccent),
            Student("5", "David Kipchoge", "DK", RedAccent),
            Student("6", "Faith Wanjiru", "FW", GreenAccent)
        )
        _boardedCount.value = 0
        _kilimaniComplete.value = false
        _driverNextStopName.value = "Kilimani Stop"
        _sosActive.value = false

        // Preloaded Alerts
        _activeAlerts.value = listOf(
            BusAlert("1", "Ngong Road Delay", "Heavy congestion near City Mortuary traffic circle. Route diverted through Chania Avenue.", "Ongoing • 5m ago", AlertSeverity.DANGER, "Ongoing"),
            BusAlert("2", "Thursday Route Change", "Express Route B will pick up 15 mins earlier due to scheduled school sports tournament.", "Tomorrow • 1h ago", AlertSeverity.WARNING, "Upcoming"),
            BusAlert("3", "School Holiday Friday", "No morning or afternoon shuttle services will operate this Friday due to Madaraka Day.", "Friday • 4h ago", AlertSeverity.INFO, "Scheduled")
        )

        // Preloaded fleet info
        _adminFleetBuses.value = listOf(
            FleetBusItem("1", "KDE 732X", "Erick Mwangi", 18, 18, 22, "On Time", GreenAccent),
            FleetBusItem("2", "KDE 119A", "John Kiprop", 12, 12, 22, "Delayed", AmberAccent, speed = 30),
            FleetBusItem("3", "KDE 540R", "Otieno Onyango", 21, 21, 22, "Active", AccentBlue),
            FleetBusItem("4", "KDE 902Y", "James Kamau", 8, 8, 22, "On Time", GreenAccent),
            FleetBusItem("5", "KDE 991Z", "Inactive", 0, 0, 22, "No Driver", TextTertiaryColor)
        )

        _driversList.value = listOf(
            DriverRosterItem("1", "Erick Mwangi", "EM", AccentBlue, "KDE 732X", 6, 0.95f, 96),
            DriverRosterItem("2", "John Kiprop", "JK", GreenAccent, "KDE 119A", 4, 0.78f, 82),
            DriverRosterItem("3", "Otieno Onyango", "OO", AmberAccent, "KDE 540R", 8, 0.92f, 91),
            DriverRosterItem("4", "James Kamau", "JK", PurpleAccent, "KDE 902Y", 3, 0.88f, 87)
        )
    }

    // Sign in logic prefilling based on credentials
    fun signIn(email: String, role: UserRole, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val trimmedEmail = email.trim().lowercase()
                val user = userDao.getUserByEmail(trimmedEmail)
                if (user != null) {
                    val userRole = when (user.role) {
                        "PARENT" -> UserRole.PARENT
                        "DRIVER" -> UserRole.DRIVER
                        "ADMIN" -> UserRole.ADMIN
                        else -> role
                    }
                    _currentUser.value = user.email
                    _currentRole.value = userRole
                    _currentUserName.value = user.name
                    onComplete(true, "Signed in successfully!")
                } else {
                    // Pre-registered fallback check for instant login of any typed email for simplicity
                    val stringRole = when (role) {
                        UserRole.PARENT -> "PARENT"
                        UserRole.DRIVER -> "DRIVER"
                        UserRole.ADMIN -> "ADMIN"
                    }
                    val defaultName = trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                    val newUser = UserEntity(trimmedEmail, defaultName, stringRole, "email", "demo1234")
                    userDao.insertUser(newUser)
                    
                    _currentUser.value = trimmedEmail
                    _currentRole.value = role
                    _currentUserName.value = defaultName
                    onComplete(true, "Signed in successfully!")
                }
            } catch (e: Exception) {
                onComplete(false, "Authentication error: ${e.message}")
            }
        }
    }

    // Sign up logic storing data locally in Room
    fun signUp(name: String, email: String, role: UserRole, provider: String, passwordHash: String = "", onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val trimmedEmail = email.trim().lowercase()
                val existing = userDao.getUserByEmail(trimmedEmail)
                if (existing != null) {
                    onComplete(false, "Account with this email already exists.")
                    return@launch
                }
                val stringRole = when (role) {
                    UserRole.PARENT -> "PARENT"
                    UserRole.DRIVER -> "DRIVER"
                    UserRole.ADMIN -> "ADMIN"
                }
                val newUser = UserEntity(
                    email = trimmedEmail,
                    name = name.trim(),
                    role = stringRole,
                    provider = provider,
                    passwordHash = passwordHash
                )
                userDao.insertUser(newUser)
                
                // Automatically log in newly registered user
                _currentUser.value = trimmedEmail
                _currentRole.value = role
                _currentUserName.value = name.trim()
                onComplete(true, "Registration successful!")
            } catch (e: Exception) {
                onComplete(false, "Sign up error: ${e.message}")
            }
        }
    }

    fun signOut() {
        _currentUser.value = null
        _currentRole.value = null
        _currentUserName.value = "User"
        resetData()
    }

    // --- LIVE SIMULATION TIMER (Runs every 2.5 seconds) ---
    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (true) {
                delay(2500)

                // 1. Advance Route progress for parent view
                var nextProgress = _progress.value + 0.04f
                if (nextProgress > 1f) {
                    nextProgress = 0f // Loop back
                }
                _progress.value = nextProgress

                // Calculations
                val remaining = 1f - nextProgress
                val eta = (15 * remaining).toInt().coerceAtLeast(1)
                _etaMinutes.value = eta
                _distanceKm.value = String.format("%.1f", 4.8f * remaining).toFloat()

                val countLeft = when {
                    nextProgress < 0.25f -> 3
                    nextProgress < 0.55f -> 2
                    nextProgress < 0.85f -> 1
                    else -> 0
                }
                _stopsLeft.value = countLeft
                _progressPct.value = (nextProgress * 100).toInt()

                // Update isCompleted status of stops based on progress
                _routeStops.value = _routeStops.value.mapIndexed { idx, stop ->
                    val isComp = when (idx) {
                        0 -> nextProgress > 0.15f
                        1 -> nextProgress > 0.45f
                        2 -> nextProgress > 0.75f
                        3 -> nextProgress > 0.95f
                        else -> false
                    }
                    val isCurr = when (idx) {
                        0 -> nextProgress <= 0.15f
                        1 -> nextProgress > 0.15f && nextProgress <= 0.45f
                        2 -> nextProgress > 0.45f && nextProgress <= 0.75f
                        3 -> nextProgress > 0.75f
                        else -> false
                    }
                    stop.copy(
                        isCompleted = isComp,
                        isCurrent = isCurr,
                        isPending = !isComp && !isCurr,
                        liveEtaMin = (stop.liveEtaMin - 1).coerceAtLeast(0)
                    )
                }

                // 2. Increment Admin buses simulated movement using sine/cosine phase shift
                adminDriftTicks += 0.05f
                _adminFleetBuses.value = _adminFleetBuses.value.mapIndexed { idx, bus ->
                    if (bus.plate == "KDE 991Z") bus // Keep inactive
                    else {
                        // Create slightly offset speeds and frequencies for map realism
                        val speedPhase = adminDriftTicks * (1f + (idx * 0.15f))
                        val progressOffset = (0.2f * idx + (sin(speedPhase) * 0.15f + 0.5f)).coerceIn(0f, 1f).toFloat()

                        // Calculate student count dynamically mirroring simulated boarding
                        val newCount = if (idx == 0) {
                            12 + _boardedCount.value // Match the driver boarding screen!
                        } else {
                            bus.studentCount
                        }

                        bus.copy(
                            latOffset = progressOffset,
                            studentCount = newCount,
                            occupancyCount = newCount
                        )
                    }
                }
            }
        }
    }

    // Star stops toggling
    fun toggleStarStop(stopName: String) {
        val current = _starredStops.value.toMutableSet()
        if (current.contains(stopName)) {
            current.remove(stopName)
        } else {
            current.add(stopName)
        }
        _starredStops.value = current
    }

    fun selectRouteOption(option: String) {
        _selectedRouteOption.value = option
    }

    // --- BOARDING CONTROLLER ---
    fun boardStudent(studentId: String) {
        _students.value = _students.value.map {
            if (it.id == studentId && !it.boarded) {
                _boardedCount.value = _boardedCount.value + 1
                it.copy(boarded = true)
            } else {
                it
            }
        }
    }

    fun triggerSOS() {
        _sosActive.value = true
    }

    fun dismissSOS() {
        _sosActive.value = false
    }

    fun setKilimaniComplete() {
        _kilimaniComplete.value = true
        _driverNextStopName.value = "Upper Hill Stop"
    }

    // --- ADMIN ALERTS ---
    fun postAlert(title: String, detail: String, severity: AlertSeverity) {
        if (title.isEmpty() || detail.isEmpty()) return
        val newAlert = BusAlert(
            id = (System.currentTimeMillis()).toString(),
            title = title,
            detail = detail,
            timestamp = "Just Now • Active",
            severity = severity,
            status = "Ongoing"
        )
        _activeAlerts.value = listOf(newAlert) + _activeAlerts.value
    }

    fun dismissAlert(alertId: String) {
        _activeAlerts.value = _activeAlerts.value.filter { it.id != alertId }
    }

    override fun onCleared() {
        simulationJob?.cancel()
        super.onCleared()
    }
}
