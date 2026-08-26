package com.example.viewmodel

import android.app.Application
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.UserEntity
import com.example.models.*
import com.example.ui.theme.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.network.GeminiRepository
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.sin
import kotlin.math.cos

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao by lazy {
        try {
            AppDatabase.getDatabase(application).userDao()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(application).isEmpty()) {
                FirebaseApp.initializeApp(application)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val isFirebaseAuthEnabled: Boolean
        get() = firebaseAuth != null

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(application).isEmpty()) {
                FirebaseApp.initializeApp(application)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val isFirestoreEnabled: Boolean
        get() = firestore != null

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: RuntimeException("Firebase execution failed"))
            }
        }
    }

    // --- RATE LIMITING STATE ---
    private val lastSignInAttemptTime = ConcurrentHashMap<String, Long>()
    private val signInFailCount = ConcurrentHashMap<String, Int>()
    private val lockUntilTime = ConcurrentHashMap<String, Long>()
    private val lastSignUpAttemptTime = ConcurrentHashMap<String, Long>()

    private fun checkSignInRateLimit(email: String): Pair<Boolean, String> {
        val currentTime = System.currentTimeMillis()
        val trimmedEmail = email.trim().lowercase()

        // 1. Check lock
        val lockUntil = lockUntilTime[trimmedEmail] ?: 0L
        if (currentTime < lockUntil) {
            val remainingSec = ((lockUntil - currentTime) / 1000).coerceAtLeast(1)
            return Pair(false, "Too many failed attempts. Account temporarily locked. Try again in $remainingSec seconds.")
        }

        // 2. Check rapid sign-ins (max 1 every 2 seconds)
        val lastAttempt = lastSignInAttemptTime[trimmedEmail] ?: 0L
        if (currentTime - lastAttempt < 2000) {
            return Pair(false, "Please wait a moment before trying again.")
        }

        return Pair(true, "")
    }

    private fun recordSignInAttempt(email: String, success: Boolean) {
        val currentTime = System.currentTimeMillis()
        val trimmedEmail = email.trim().lowercase()
        lastSignInAttemptTime[trimmedEmail] = currentTime

        if (success) {
            signInFailCount.remove(trimmedEmail)
            lockUntilTime.remove(trimmedEmail)
        } else {
            val currentFails = (signInFailCount[trimmedEmail] ?: 0) + 1
            signInFailCount[trimmedEmail] = currentFails
            if (currentFails >= 5) {
                lockUntilTime[trimmedEmail] = currentTime + 30000 // Lock for 30 seconds
                signInFailCount.remove(trimmedEmail) // Reset fail count after locking
            }
        }
    }

    private fun checkSignUpRateLimit(email: String): Pair<Boolean, String> {
        val currentTime = System.currentTimeMillis()
        val trimmedEmail = email.trim().lowercase()

        val lastAttempt = lastSignUpAttemptTime[trimmedEmail] ?: 0L
        if (currentTime - lastAttempt < 5000) {
            return Pair(false, "Please wait 5 seconds between registration attempts.")
        }
        return Pair(true, "")
    }

    private fun recordSignUpAttempt(email: String) {
        lastSignUpAttemptTime[email.trim().lowercase()] = System.currentTimeMillis()
    }

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

    // --- MOOVIT TRANSIT INTEGRATION STATE (Lines, Timetables, Nearby Stations, Multi-Modal Plans & Live Guidance) ---
    private val _transitLines = MutableStateFlow<List<TransitLine>>(emptyList())
    val transitLines: StateFlow<List<TransitLine>> = _transitLines.asStateFlow()

    private val _selectedTransitLine = MutableStateFlow<TransitLine?>(null)
    val selectedTransitLine: StateFlow<TransitLine?> = _selectedTransitLine.asStateFlow()

    private val _savedFavoriteLines = MutableStateFlow<Set<String>>(setOf("line_732", "line_44"))
    val savedFavoriteLines: StateFlow<Set<String>> = _savedFavoriteLines.asStateFlow()

    private val _nearbyStations = MutableStateFlow<List<NearbyStation>>(emptyList())
    val nearbyStations: StateFlow<List<NearbyStation>> = _nearbyStations.asStateFlow()

    private val _savedStationIds = MutableStateFlow<Set<String>>(setOf("st_1", "st_3"))
    val savedStationIds: StateFlow<Set<String>> = _savedStationIds.asStateFlow()

    private val _multiModalPlans = MutableStateFlow<List<MultiModalPlan>>(emptyList())
    val multiModalPlans: StateFlow<List<MultiModalPlan>> = _multiModalPlans.asStateFlow()

    private val _activeLiveGuidance = MutableStateFlow<MultiModalPlan?>(null)
    val activeLiveGuidance: StateFlow<MultiModalPlan?> = _activeLiveGuidance.asStateFlow()

    private val _currentLiveStepIndex = MutableStateFlow(0)
    val currentLiveStepIndex: StateFlow<Int> = _currentLiveStepIndex.asStateFlow()

    private val _liveGuidanceAlightAlertFired = MutableStateFlow(false)
    val liveGuidanceAlightAlertFired: StateFlow<Boolean> = _liveGuidanceAlightAlertFired.asStateFlow()

    // --- GLOBAL TRANSIT APPS INTEGRATION STATE (Citymapper, Transit, Moovit, DB Navigator, Grab) ---
    private val _crowdednessLevel = MutableStateFlow("Seats Available")
    val crowdednessLevel: StateFlow<String> = _crowdednessLevel.asStateFlow()

    private val _getOffAlertActive = MutableStateFlow(true)
    val getOffAlertActive: StateFlow<Boolean> = _getOffAlertActive.asStateFlow()

    private val _co2SavedKg = MutableStateFlow(2.4f)
    val co2SavedKg: StateFlow<Float> = _co2SavedKg.asStateFlow()

    private val _caloriesBurned = MutableStateFlow(165)
    val caloriesBurned: StateFlow<Int> = _caloriesBurned.asStateFlow()

    private val _shareToastMessage = MutableStateFlow<String?>(null)
    val shareToastMessage: StateFlow<String?> = _shareToastMessage.asStateFlow()

    private val _selectedTransitAppFilter = MutableStateFlow("All-In-One")
    val selectedTransitAppFilter: StateFlow<String> = _selectedTransitAppFilter.asStateFlow()

    private val _communityIncidents = MutableStateFlow<List<IncidentReport>>(emptyList())
    val communityIncidents: StateFlow<List<IncidentReport>> = _communityIncidents.asStateFlow()

    // --- DRIVER EXPERIENCE STATE ---
    // --- INTERPORTAL MESSAGING & INTERCONNECTED STATE ---
    private val _interportalMessages = MutableStateFlow<List<InterportalMessage>>(emptyList())
    val interportalMessages: StateFlow<List<InterportalMessage>> = _interportalMessages.asStateFlow()

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _journeyEvents = MutableStateFlow<List<JourneyEvent>>(emptyList())
    val journeyEvents: StateFlow<List<JourneyEvent>> = _journeyEvents.asStateFlow()

    private val _parentStudentLinks = MutableStateFlow<List<ParentStudentLink>>(emptyList())
    val parentStudentLinks: StateFlow<List<ParentStudentLink>> = _parentStudentLinks.asStateFlow()

    private val _registrationData = MutableStateFlow(RegistrationData())
    val registrationData: StateFlow<RegistrationData> = _registrationData.asStateFlow()

    val currentChild: StateFlow<Student?> = combine(_currentUser, _students, _parentStudentLinks) { email, allStudents, links ->
        if (email == null) {
            allStudents.firstOrNull()
        } else {
            val link = links.find { it.parentId.equals(email, ignoreCase = true) && it.isPrimary }
            if (link != null) {
                allStudents.find { it.id == link.studentId }
            } else {
                allStudents.find { it.parentEmail.equals(email, ignoreCase = true) } 
                    ?: allStudents.find { it.parentName.contains(_currentUserName.value, ignoreCase = true) }
                    ?: if (email.equals("parent@safiri.co.ke", ignoreCase = true)) allStudents.firstOrNull() else null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    // --- ADMIN SUBSCRIPTION & FLEET LICENSING STATE ---
    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.ACTIVE)
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()

    private val _agreedMonthlyAmount = MutableStateFlow("KES 15,000 ($150 USD)")
    val agreedMonthlyAmount: StateFlow<String> = _agreedMonthlyAmount.asStateFlow()

    private val _subscriptionPlanName = MutableStateFlow("Safiri Fleet Enterprise Tier")
    val subscriptionPlanName: StateFlow<String> = _subscriptionPlanName.asStateFlow()

    private val _nextDueDate = MutableStateFlow("August 1, 2026")
    val nextDueDate: StateFlow<String> = _nextDueDate.asStateFlow()

    private val _paybillAccount = MutableStateFlow("M-Pesa Paybill 247247 (Acc: SAF-8832)")
    val paybillAccount: StateFlow<String> = _paybillAccount.asStateFlow()

    private val _paymentProcessing = MutableStateFlow(false)
    val paymentProcessing: StateFlow<Boolean> = _paymentProcessing.asStateFlow()

    private val _subscriptionToast = MutableStateFlow<String?>(null)
    val subscriptionToast: StateFlow<String?> = _subscriptionToast.asStateFlow()

    private val _invoiceHistory = MutableStateFlow<List<InvoiceItem>>(emptyList())
    val invoiceHistory: StateFlow<List<InvoiceItem>> = _invoiceHistory.asStateFlow()

    // --- THEME MODE (DARK / LIGHT / SYSTEM) ---
    private val _themeMode = MutableStateFlow<AppThemeMode>(AppThemeMode.DARK)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun toggleThemeMode() {
        _themeMode.value = if (_themeMode.value == AppThemeMode.DARK) AppThemeMode.LIGHT else AppThemeMode.DARK
    }

    // --- GOOGLE MAPS & GEMINI API KEY INTEGRATION ---
    private val _mapsApiKey = MutableStateFlow<String>(
        try {
            val key = com.example.BuildConfig.GOOGLE_MAPS_API_KEY
            if (key.isNullOrBlank() || key == "MY_GOOGLE_MAPS_API_KEY") "" else key
        } catch (e: Exception) {
            ""
        }
    )
    val mapsApiKey: StateFlow<String> = _mapsApiKey.asStateFlow()

    private val _geminiApiKey = MutableStateFlow<String>(
        try {
            val key = com.example.BuildConfig.GEMINI_API_KEY
            if (key.isNullOrBlank() || key == "MY_GEMINI_API_KEY") "" else key
        } catch (e: Exception) {
            ""
        }
    )
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _apiKeySavedToast = MutableStateFlow<String?>(null)
    val apiKeySavedToast: StateFlow<String?> = _apiKeySavedToast.asStateFlow()

    fun updateMapsApiKey(newKey: String) {
        _mapsApiKey.value = newKey.trim()
        _apiKeySavedToast.value = "Google Maps API Key configured successfully!"
    }

    fun updateGeminiApiKey(newKey: String) {
        _geminiApiKey.value = newKey.trim()
        com.example.network.GeminiRepository.setCustomApiKey(newKey.trim())
        _apiKeySavedToast.value = "Gemini API Key configured successfully!"
    }

    fun dismissApiKeyToast() {
        _apiKeySavedToast.value = null
    }

    // Job holding the simulation tick loop
    private var simulationJob: Job? = null
    private var adminDriftTicks = 0f

    init {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _currentUser.value = null
        _currentRole.value = null
        _currentUserName.value = "User"
        resetData()
        seedDefaultUsers()
        startSimulation()
        setupFirebaseAuthListener()
    }

    companion object {
        fun hashPassword(password: String): String {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
                hash.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                password
            }
        }
    }

    private fun seedDefaultUsers() {
        viewModelScope.launch {
            try {
                updateOrInsertDefaultUser("parent@safiri.co.ke", "Sarah Ochieng", "PARENT", "demo1234")
                updateOrInsertDefaultUser("driver@safiri.co.ke", "Erick Mwangi", "DRIVER", "demo1234")
                updateOrInsertDefaultUser("admin@safiri.co.ke", "Admin Chief", "ADMIN", "demo1234")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun updateOrInsertDefaultUser(email: String, name: String, role: String, plainPassword: String) {
        val existing = userDao?.getUserByEmail(email)
        val hashed = hashPassword(plainPassword)
        if (existing == null || existing.passwordHash == plainPassword) {
            userDao?.insertUser(UserEntity(email, name, role, "email", hashed))
        }
    }

    private fun resetData() {
        // Initial stops list with DB Navigator Platform/Bay and Transfer data
        _routeStops.value = listOf(
            RouteStop("1", "Kawangware Terminal", isCompleted = false, isCurrent = true, liveEtaMin = 2, distanceMeters = 300, platformBay = "Bay 4 (Naivasha Rd)", delayNote = "On time", transferConnection = "Matatu Route 46"),
            RouteStop("2", "Westlands Hub", isCompleted = false, liveEtaMin = 8, distanceMeters = 1800, platformBay = "Platform 2B", delayNote = "+2 min (Bypass Traffic)", transferConnection = "Express Line 105"),
            RouteStop("3", "Kilimani Station", isCompleted = false, isParentStop = true, liveEtaMin = 14, distanceMeters = 3400, platformBay = "Bay 1 (Valley Arcade)", delayNote = "On time", transferConnection = "School Shuttle Direct"),
            RouteStop("4", "Upper Hill / Academy", isCompleted = false, liveEtaMin = 22, distanceMeters = 5100, platformBay = "Platform A (School Gate)", delayNote = "On time", transferConnection = "End Station Drop-off")
        )

        // Community incidents feed (Moovit & Transit style)
        _communityIncidents.value = listOf(
            IncidentReport("inc_1", "Heavy Congestion at Ngong Rd Junction", "Kilimani - Ngong Rd", "4 min ago", 18, "Traffic", isUpvoted = false),
            IncidentReport("inc_2", "Rain Shower Slowing Down Bypass", "Valley Arcade Bypass", "12 min ago", 9, "Weather", isUpvoted = true),
            IncidentReport("inc_3", "Road Resurfacing at Bus Bay 2", "Naivasha Rd Terminal", "25 min ago", 14, "Roadwork", isUpvoted = false)
        )

        // Nearby stops detected
        _nearbyStops.value = listOf(
            "Kilimani Bus Bay" to "120m away",
            "Valley Arcade Gate" to "340m away",
            "Yaya Centre" to "510m away"
        )

        // Initial Driver boarding list with Parent and Bus linking
        _students.value = listOf(
            Student(
                id = "1",
                name = "Brian Kamau",
                initials = "BK",
                avatarColor = DefaultAccentBlue,
                parentName = "Sarah Ochieng",
                parentEmail = "parent@safiri.co.ke",
                parentPhone = "0712345678",
                assignedBusPlate = "KDE 732X",
                assignedDriverName = "Erick Mwangi",
                pickupStop = "Kilimani Stop",
                dropoffStop = "St. Mary's Academy",
                status = StudentStatus.NOT_BOARDED
            ),
            Student(
                id = "2",
                name = "Zara Mwangi",
                initials = "ZM",
                avatarColor = DefaultGreenAccent,
                parentName = "Sarah Ochieng",
                parentEmail = "parent@safiri.co.ke",
                parentPhone = "0712345678",
                assignedBusPlate = "KDE 732X",
                assignedDriverName = "Erick Mwangi",
                pickupStop = "Kilimani Stop",
                dropoffStop = "St. Mary's Academy",
                status = StudentStatus.NOT_BOARDED
            ),
            Student(
                id = "3",
                name = "Liam Ochieng",
                initials = "LO",
                avatarColor = DefaultAmberAccent,
                parentName = "David Ochieng",
                parentEmail = "david@safiri.co.ke",
                parentPhone = "0722114455",
                assignedBusPlate = "KDE 119A",
                assignedDriverName = "John Kiprop",
                pickupStop = "Westlands Hub",
                dropoffStop = "St. Mary's Academy",
                status = StudentStatus.NOT_BOARDED
            ),
            Student(
                id = "4",
                name = "Amina Njoroge",
                initials = "AN",
                avatarColor = DefaultPurpleAccent,
                parentName = "Fatuma Njoroge",
                parentEmail = "fatuma@safiri.co.ke",
                parentPhone = "0733889900",
                assignedBusPlate = "KDE 540R",
                assignedDriverName = "Otieno Onyango",
                pickupStop = "Kawangware Terminal",
                dropoffStop = "St. Mary's Academy",
                status = StudentStatus.NOT_BOARDED
            ),
            Student(
                id = "5",
                name = "David Kipchoge",
                initials = "DK",
                avatarColor = DefaultRedAccent,
                parentName = "Eliud Kipchoge",
                parentEmail = "eliud@safiri.co.ke",
                parentPhone = "0700112233",
                assignedBusPlate = "KDE 902Y",
                assignedDriverName = "James Kamau",
                pickupStop = "Upper Hill",
                dropoffStop = "St. Mary's Academy",
                status = StudentStatus.NOT_BOARDED
            ),
            Student(
                id = "6",
                name = "Faith Wanjiru",
                initials = "FW",
                avatarColor = DefaultGreenAccent,
                parentName = "Grace Wanjiru",
                parentEmail = "grace@safiri.co.ke",
                parentPhone = "0711998877",
                assignedBusPlate = "KDE 732X",
                assignedDriverName = "Erick Mwangi",
                pickupStop = "Kilimani Stop",
                dropoffStop = "St. Mary's Academy",
                status = StudentStatus.NOT_BOARDED
            )
        )

        _interportalMessages.value = listOf(
            InterportalMessage("msg1", "Sarah Ochieng (Parent)", "PARENT", "DRIVER", "KDE 732X", "Brian Kamau", "Hello Erick, Brian will be at Kilimani stop by 7:10 AM with his lunchbox.", "7:02 AM"),
            InterportalMessage("msg2", "Erick Mwangi (Driver)", "DRIVER", "PARENT", "KDE 732X", null, "Good morning parents on KDE 732X! Departing Kawangware, ETA Kilimani is 12 mins.", "7:05 AM"),
            InterportalMessage("msg3", "Admin Chief", "ADMIN", "ALL", "KDE 732X", null, "Scheduled morning route dispatch is active. All 4 buses are online.", "6:45 AM")
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
            FleetBusItem("1", "KDE 732X", "Erick Mwangi", 18, 18, 22, "On Time", DefaultGreenAccent),
            FleetBusItem("2", "KDE 119A", "John Kiprop", 12, 12, 22, "Delayed", DefaultAmberAccent, speed = 30),
            FleetBusItem("3", "KDE 540R", "Otieno Onyango", 21, 21, 22, "Active", DefaultAccentBlue),
            FleetBusItem("4", "KDE 902Y", "James Kamau", 8, 8, 22, "On Time", DefaultGreenAccent),
            FleetBusItem("5", "KDE 991Z", "Inactive", 0, 0, 22, "No Driver", DefaultTextTertiaryColor)
        )

        _driversList.value = listOf(
            DriverRosterItem("1", "Erick Mwangi", "EM", DefaultAccentBlue, "KDE 732X", 6, 0.95f, 96),
            DriverRosterItem("2", "John Kiprop", "JK", DefaultGreenAccent, "KDE 119A", 4, 0.78f, 82),
            DriverRosterItem("3", "Otieno Onyango", "OO", DefaultAmberAccent, "KDE 540R", 8, 0.92f, 91),
            DriverRosterItem("4", "James Kamau", "JK", DefaultPurpleAccent, "KDE 902Y", 3, 0.88f, 87)
        )

        _subscriptionStatus.value = SubscriptionStatus.ACTIVE
        _invoiceHistory.value = listOf(
            InvoiceItem("INV-2026-007", "01 Jul 2026", "KES 15,000", "PAID", "M-Pesa 247247", "REC-892341"),
            InvoiceItem("INV-2026-006", "01 Jun 2026", "KES 15,000", "PAID", "Visa ending in 8842", "REC-771239"),
            InvoiceItem("INV-2026-005", "01 May 2026", "KES 15,000", "PAID", "M-Pesa 247247", "REC-661002")
        )

        // Seed Moovit Transit Lines
        _transitLines.value = listOf(
            TransitLine(
                id = "line_732",
                lineNumber = "732",
                name = "Kilimani Express Shuttle",
                color = DefaultAccentBlue,
                operator = "Safiri Express Direct",
                frequencyMinutes = 15,
                serviceHours = "06:00 AM - 18:30 PM",
                direction1Name = "To Upper Hill / Academy",
                direction1Stops = listOf("Kawangware Terminal", "Westlands Hub", "Kilimani Station", "Upper Hill / Academy"),
                direction2Name = "To Kawangware Terminal",
                direction2Stops = listOf("Upper Hill / Academy", "Kilimani Station", "Westlands Hub", "Kawangware Terminal"),
                activeBusesCount = 3,
                timetableDepartures = listOf(
                    LineDeparture("06:45 AM", "Upper Hill / Academy", isLive = true, "Seats Available", 0),
                    LineDeparture("07:00 AM", "Upper Hill / Academy", isLive = true, "Few Seats", 2),
                    LineDeparture("07:15 AM", "Upper Hill / Academy", isLive = false, "Empty", 0),
                    LineDeparture("07:30 AM", "Upper Hill / Academy", isLive = false, "Empty", 0),
                    LineDeparture("15:30 PM", "Kawangware Terminal", isLive = false, "Empty", 0),
                    LineDeparture("16:00 PM", "Kawangware Terminal", isLive = false, "Empty", 0)
                ),
                statusType = LineStatusType.ON_TIME,
                statusDetail = "Normal service on all stops with live GPS tracking",
                isFavorite = true,
                wheelchairAccessible = true
            ),
            TransitLine(
                id = "line_119",
                lineNumber = "119",
                name = "Kawangware - Valley Shuttle",
                color = DefaultPurpleAccent,
                operator = "Safiri Community Lines",
                frequencyMinutes = 20,
                serviceHours = "06:15 AM - 19:00 PM",
                direction1Name = "To Upper Hill Campus",
                direction1Stops = listOf("Naivasha Terminal", "Kawangware Junction", "Valley Arcade", "Upper Hill Campus"),
                direction2Name = "To Naivasha Terminal",
                direction2Stops = listOf("Upper Hill Campus", "Valley Arcade", "Kawangware Junction", "Naivasha Terminal"),
                activeBusesCount = 2,
                timetableDepartures = listOf(
                    LineDeparture("06:30 AM", "Upper Hill Campus", isLive = true, "Seats Available", 4),
                    LineDeparture("06:50 AM", "Upper Hill Campus", isLive = true, "Standing Only", 5),
                    LineDeparture("07:10 AM", "Upper Hill Campus", isLive = false, "Empty", 0)
                ),
                statusType = LineStatusType.MODERATE_DELAY,
                statusDetail = "+5 min delay due to Naivasha Rd roadworks",
                isFavorite = false,
                wheelchairAccessible = true
            ),
            TransitLine(
                id = "line_44",
                lineNumber = "44",
                name = "Westlands - Lavington Direct",
                color = DefaultGreenAccent,
                operator = "Safiri Direct",
                frequencyMinutes = 12,
                serviceHours = "06:00 AM - 19:30 PM",
                direction1Name = "To Lavington School Zone",
                direction1Stops = listOf("Sarit Centre Hub", "Westlands Ring Rd", "Rhapta Road", "Lavington School Zone"),
                direction2Name = "To Sarit Centre Hub",
                direction2Stops = listOf("Lavington School Zone", "Rhapta Road", "Westlands Ring Rd", "Sarit Centre Hub"),
                activeBusesCount = 4,
                timetableDepartures = listOf(
                    LineDeparture("06:40 AM", "Lavington School Zone", isLive = true, "Seats Available", 0),
                    LineDeparture("06:52 AM", "Lavington School Zone", isLive = true, "Seats Available", 0),
                    LineDeparture("07:04 AM", "Lavington School Zone", isLive = false, "Empty", 0)
                ),
                statusType = LineStatusType.ON_TIME,
                statusDetail = "On schedule with smooth traffic flow",
                isFavorite = true,
                wheelchairAccessible = true
            ),
            TransitLine(
                id = "line_102",
                lineNumber = "102",
                name = "Karen Academy Express",
                color = DefaultAmberAccent,
                operator = "Safiri South County",
                frequencyMinutes = 25,
                serviceHours = "06:30 AM - 18:00 PM",
                direction1Name = "To St. Austin's Academy",
                direction1Stops = listOf("Karen Shopping Centre", "Bomas Junction", "Upper Hill Link", "St. Austin's Academy"),
                direction2Name = "To Karen Shopping Centre",
                direction2Stops = listOf("St. Austin's Academy", "Upper Hill Link", "Bomas Junction", "Karen Shopping Centre"),
                activeBusesCount = 2,
                timetableDepartures = listOf(
                    LineDeparture("06:40 AM", "St. Austin's Academy", isLive = true, "Seats Available", 0),
                    LineDeparture("07:05 AM", "St. Austin's Academy", isLive = false, "Empty", 0)
                ),
                statusType = LineStatusType.ON_TIME,
                statusDetail = "Normal operation across Langata / Karen corridor",
                isFavorite = false,
                wheelchairAccessible = true
            ),
            TransitLine(
                id = "line_14",
                lineNumber = "14",
                name = "Lavington Loop Connector",
                color = Color(0xFF06B6D4),
                operator = "Safiri Feeder Network",
                frequencyMinutes = 15,
                serviceHours = "06:00 AM - 18:00 PM",
                direction1Name = "To Valley Arcade Hub",
                direction1Stops = listOf("James Gichuru Gate", "Lavington Green", "Muthangari Rd", "Valley Arcade Hub"),
                direction2Name = "To James Gichuru Gate",
                direction2Stops = listOf("Valley Arcade Hub", "Muthangari Rd", "Lavington Green", "James Gichuru Gate"),
                activeBusesCount = 2,
                timetableDepartures = listOf(
                    LineDeparture("06:55 AM", "Valley Arcade Hub", isLive = true, "Seats Available", 0),
                    LineDeparture("07:10 AM", "Valley Arcade Hub", isLive = false, "Empty", 0)
                ),
                statusType = LineStatusType.ON_TIME,
                statusDetail = "Normal flow on James Gichuru road",
                isFavorite = false,
                wheelchairAccessible = true
            )
        )

        // Seed Moovit Nearby Stations
        _nearbyStations.value = listOf(
            NearbyStation(
                id = "st_1",
                name = "Kilimani Junction Stop",
                distanceMeters = 85,
                walkingTimeMinutes = 1,
                platformBay = "Bay 1 (Valley Arcade)",
                nextDepartures = listOf(
                    StationNextDeparture("732", "Kilimani Express", "Upper Hill / Academy", 2, "KDE 732X", DefaultAccentBlue),
                    StationNextDeparture("119", "Kawangware Shuttle", "Upper Hill Campus", 6, "KDE 119A", DefaultPurpleAccent)
                ),
                wheelchairAccessible = true,
                isSaved = true
            ),
            NearbyStation(
                id = "st_2",
                name = "Valley Arcade Transit Bay",
                distanceMeters = 240,
                walkingTimeMinutes = 3,
                platformBay = "Bay 3 (Main Road)",
                nextDepartures = listOf(
                    StationNextDeparture("14", "Lavington Loop", "Valley Arcade Hub", 4, "KDE 442P", Color(0xFF06B6D4)),
                    StationNextDeparture("732", "Kilimani Express", "Upper Hill / Academy", 11, "KDE 732X", DefaultAccentBlue)
                ),
                wheelchairAccessible = true,
                isSaved = false
            ),
            NearbyStation(
                id = "st_3",
                name = "Westlands Link Hub",
                distanceMeters = 480,
                walkingTimeMinutes = 6,
                platformBay = "Platform 2B",
                nextDepartures = listOf(
                    StationNextDeparture("44", "Westlands Direct", "Lavington School Zone", 3, "KDE 883L", DefaultGreenAccent),
                    StationNextDeparture("732", "Kilimani Express", "Upper Hill / Academy", 14, "KDE 732X", DefaultAccentBlue)
                ),
                wheelchairAccessible = true,
                isSaved = true
            ),
            NearbyStation(
                id = "st_4",
                name = "Upper Hill School Gate Station",
                distanceMeters = 650,
                walkingTimeMinutes = 8,
                platformBay = "Platform A",
                nextDepartures = listOf(
                    StationNextDeparture("102", "Karen Academy", "St. Austin's Academy", 5, "KDE 552K", DefaultAmberAccent),
                    StationNextDeparture("119", "Kawangware Shuttle", "Upper Hill Campus", 12, "KDE 119A", DefaultPurpleAccent)
                ),
                wheelchairAccessible = true,
                isSaved = false
            )
        )

        // Seed Moovit Multi-Modal Itinerary Plans
        _multiModalPlans.value = listOf(
            MultiModalPlan(
                id = "plan_best",
                title = "Fastest Shuttle Connection",
                routeTypeTag = "BEST_ROUTE",
                totalDurationMinutes = 24,
                walkMinutes = 3,
                transitMinutes = 21,
                departureTime = "06:45 AM",
                arrivalTime = "07:09 AM",
                transfersCount = 0,
                primaryLineNumber = "732",
                primaryLineColor = DefaultAccentBlue,
                primaryBusPlate = "KDE 732X",
                crowdLevel = "Seats Available",
                co2SavingsKg = 2.8f,
                caloriesBurned = 130,
                steps = listOf(
                    ItineraryStep(ItineraryStepType.WALK_TO_STOP, "Walk 120m to Kilimani Junction Stop", "Head east on Argwings Kodhek Rd (2 min)", 2, 120),
                    ItineraryStep(ItineraryStepType.BOARD_BUS, "Board Shuttle 732 (KDE 732X)", "Platform Bay 1 • Driver: Erick Mwangi", 1, 0, "732", "KDE 732X"),
                    ItineraryStep(ItineraryStepType.RIDE_BUS, "Ride 3 stops on Kilimani Express", "Passing Westlands Hub & Valley Arcade", 18, 4200, "732", "KDE 732X", listOf("Kilimani Junction", "Valley Arcade", "Upper Hill Link", "St. Austin's Academy Gate")),
                    ItineraryStep(ItineraryStepType.ALIGHT, "Alight at St. Austin's Academy Gate", "Get off at front exit door", 1, 0, isAlertTrigger = true),
                    ItineraryStep(ItineraryStepType.WALK_TO_DESTINATION, "Walk 50m into School Reception", "Safe designated pedestrian lane", 2, 50)
                )
            ),
            MultiModalPlan(
                id = "plan_alternative",
                title = "Kawangware Regular Route",
                routeTypeTag = "FEWEST_TRANSFERS",
                totalDurationMinutes = 32,
                walkMinutes = 5,
                transitMinutes = 27,
                departureTime = "06:35 AM",
                arrivalTime = "07:07 AM",
                transfersCount = 0,
                primaryLineNumber = "119",
                primaryLineColor = DefaultPurpleAccent,
                primaryBusPlate = "KDE 119A",
                crowdLevel = "Seats Available",
                co2SavingsKg = 2.2f,
                caloriesBurned = 175,
                steps = listOf(
                    ItineraryStep(ItineraryStepType.WALK_TO_STOP, "Walk 240m to Valley Arcade Bay 3", "Head north towards Naivasha Rd", 4, 240),
                    ItineraryStep(ItineraryStepType.BOARD_BUS, "Board Shuttle 119 (KDE 119A)", "Platform Bay 3 • Driver: John Kiprop", 1, 0, "119", "KDE 119A"),
                    ItineraryStep(ItineraryStepType.RIDE_BUS, "Ride 4 stops on Kawangware Route", "Passing Junction & Upper Hill", 24, 5100, "119", "KDE 119A", listOf("Valley Arcade", "Kawangware Junction", "Upper Hill", "St. Austin's Academy")),
                    ItineraryStep(ItineraryStepType.ALIGHT, "Alight at Upper Hill Station", "Alight at Platform A", 1, 0, isAlertTrigger = true),
                    ItineraryStep(ItineraryStepType.WALK_TO_DESTINATION, "Walk 150m to School Reception", "Pedestrian crosswalk", 2, 150)
                )
            ),
            MultiModalPlan(
                id = "plan_least_walk",
                title = "Direct Estate Pick-up Express",
                routeTypeTag = "LEAST_WALKING",
                totalDurationMinutes = 28,
                walkMinutes = 1,
                transitMinutes = 27,
                departureTime = "06:50 AM",
                arrivalTime = "07:18 AM",
                transfersCount = 0,
                primaryLineNumber = "732",
                primaryLineColor = DefaultGreenAccent,
                primaryBusPlate = "KDE 732X",
                crowdLevel = "Empty Seats",
                co2SavingsKg = 3.1f,
                caloriesBurned = 85,
                steps = listOf(
                    ItineraryStep(ItineraryStepType.WALK_TO_STOP, "Walk 40m to Estate Gate Pick-up", "Immediate front porch pickup", 1, 40),
                    ItineraryStep(ItineraryStepType.BOARD_BUS, "Board Express Bus 732 (KDE 732X)", "Direct Boarding with Driver Erick", 1, 0, "732", "KDE 732X"),
                    ItineraryStep(ItineraryStepType.RIDE_BUS, "Ride Express Direct to School", "Express non-stop route", 25, 4800, "732", "KDE 732X"),
                    ItineraryStep(ItineraryStepType.ALIGHT, "Alight directly inside School Gate", "Security guarded drop-off bay", 1, 0, isAlertTrigger = true)
                )
            )
        )
    }

    // --- MOOVIT TRANSIT INTERACTIVE METHODS ---

    fun selectTransitLine(line: TransitLine?) {
        _selectedTransitLine.value = line
    }

    fun toggleFavoriteLine(lineId: String) {
        val current = _savedFavoriteLines.value.toMutableSet()
        if (current.contains(lineId)) {
            current.remove(lineId)
        } else {
            current.add(lineId)
        }
        _savedFavoriteLines.value = current
    }

    fun toggleSavedStation(stationId: String) {
        val current = _savedStationIds.value.toMutableSet()
        if (current.contains(stationId)) {
            current.remove(stationId)
        } else {
            current.add(stationId)
        }
        _savedStationIds.value = current
    }

    fun startLiveGuidance(plan: MultiModalPlan) {
        _activeLiveGuidance.value = plan
        _currentLiveStepIndex.value = 0
        _liveGuidanceAlightAlertFired.value = false
    }

    fun stopLiveGuidance() {
        _activeLiveGuidance.value = null
        _currentLiveStepIndex.value = 0
        _liveGuidanceAlightAlertFired.value = false
    }

    fun advanceLiveStep() {
        val currentPlan = _activeLiveGuidance.value ?: return
        val nextIndex = _currentLiveStepIndex.value + 1
        if (nextIndex < currentPlan.steps.size) {
            _currentLiveStepIndex.value = nextIndex
            val step = currentPlan.steps[nextIndex]
            if (step.isAlertTrigger || step.type == ItineraryStepType.ALIGHT) {
                _liveGuidanceAlightAlertFired.value = true
            }
        } else {
            // Completed guidance
            stopLiveGuidance()
        }
    }

    fun setCustomPickupStation(stationName: String) {
        val child = currentChild.value
        if (child != null) {
            _students.value = _students.value.map { st ->
                if (st.id == child.id) {
                    st.copy(pickupStop = stationName)
                } else st
            }
            _shareToastMessage.value = "Updated pickup stop to $stationName for ${child.name}!"
        }
    }

    fun reportLineDelay(lineId: String, delayMins: Int, reason: String) {
        _transitLines.value = _transitLines.value.map { line ->
            if (line.id == lineId) {
                line.copy(
                    statusType = if (delayMins > 10) LineStatusType.HEAVY_DELAY else LineStatusType.MODERATE_DELAY,
                    statusDetail = "+$delayMins min delay: $reason"
                )
            } else {
                line
            }
        }
        _communityIncidents.value = listOf(
            IncidentReport(
                id = "inc_${System.currentTimeMillis()}",
                title = "Reported +$delayMins min on Line ${lineId.replace("line_", "")}",
                location = reason,
                reportedTime = "Just now",
                votes = 1,
                category = "Traffic"
            )
        ) + _communityIncidents.value
    }


    // --- SUBSCRIPTION MANAGEMENT METHODS ---
    fun setSubscriptionStatus(status: SubscriptionStatus) {
        _subscriptionStatus.value = status
        _subscriptionToast.value = when (status) {
            SubscriptionStatus.ACTIVE -> "Subscription set to ACTIVE. Fleet system operational."
            SubscriptionStatus.PAYMENT_DUE -> "Subscription set to PAYMENT DUE. Invoice warning enabled."
            SubscriptionStatus.INACTIVE -> "Subscription set to INACTIVE. App features locked."
        }
    }

    fun paySubscriptionInvoice(phoneNumber: String, amount: String) {
        viewModelScope.launch {
            _paymentProcessing.value = true
            delay(1500)
            val newReceipt = "REC-${(100000..999999).random()}"
            val newInvoice = InvoiceItem(
                id = "INV-2026-${(100..999).random()}",
                date = "23 Jul 2026",
                amount = if (amount.isBlank()) "KES 15,000" else amount,
                status = "PAID",
                paymentMethod = if (phoneNumber.isNotBlank()) "M-Pesa ($phoneNumber)" else "M-Pesa 247247",
                receiptNumber = newReceipt
            )
            _invoiceHistory.value = listOf(newInvoice) + _invoiceHistory.value
            _subscriptionStatus.value = SubscriptionStatus.ACTIVE
            _nextDueDate.value = "August 23, 2026"
            _paymentProcessing.value = false
            _subscriptionToast.value = "Payment Received! Receipt #$newReceipt. Fleet tracking reactivated!"
        }
    }

    fun clearSubscriptionToast() {
        _subscriptionToast.value = null
    }

    // Sign in logic with password verification, Firebase Auth integration, and portal-based role checks
    fun signIn(email: String, passwordRaw: String = "", role: UserRole, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val trimmedEmail = email.trim().lowercase()
                
                val limitCheck = checkSignInRateLimit(trimmedEmail)
                if (!limitCheck.first) {
                    onComplete(false, limitCheck.second)
                    return@launch
                }

                // Immediately set current time to block concurrent/rapid requests
                lastSignInAttemptTime[trimmedEmail] = System.currentTimeMillis()

                var firebaseSuccess = false
                if (firebaseAuth != null) {
                    try {
                        firebaseAuth!!.signInWithEmailAndPassword(trimmedEmail, passwordRaw).await()
                        firebaseSuccess = true
                    } catch (e: Exception) {
                        val msg = e.message ?: ""
                        if (msg.contains("no firebase app", ignoreCase = true) ||
                            msg.contains("not initialized", ignoreCase = true) ||
                            e is IllegalStateException) {
                            firebaseSuccess = false
                        } else {
                            recordSignInAttempt(trimmedEmail, success = false)
                            onComplete(false, "Firebase Auth: ${e.localizedMessage ?: e.message}")
                            return@launch
                        }
                    }
                }

                if (firebaseSuccess) {
                    // Firebase Auth succeeded. Retrieve role information from local Room database.
                    val user = userDao?.getUserByEmail(trimmedEmail)
                    if (user != null) {
                        val userRole = when (user.role) {
                            "PARENT" -> UserRole.PARENT
                            "DRIVER" -> UserRole.DRIVER
                            "ADMIN" -> UserRole.ADMIN
                            else -> role
                        }
                        
                        // Enforce distinct login portals
                        if (userRole != role) {
                            firebaseAuth?.signOut()
                            val expectedPortal = when (userRole) {
                                UserRole.PARENT -> "Parent"
                                UserRole.DRIVER -> "Driver"
                                UserRole.ADMIN -> "School Administrator"
                            }
                            onComplete(false, "This account is registered as a $expectedPortal. Please use the correct login portal.")
                            return@launch
                        }

                        _currentUser.value = user.email
                        _currentRole.value = userRole
                        _currentUserName.value = user.name
                        recordSignInAttempt(trimmedEmail, success = true)
                        onComplete(true, "Signed in successfully with Firebase!")
                    } else {
                        // User exists in Firebase but not locally. Map them to the selected portal role.
                        val stringRole = when (role) {
                            UserRole.PARENT -> "PARENT"
                            UserRole.DRIVER -> "DRIVER"
                            UserRole.ADMIN -> "ADMIN"
                        }
                        val defaultName = trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                        val newUser = UserEntity(trimmedEmail, defaultName, stringRole, "firebase_email", "")
                        userDao?.insertUser(newUser)

                        _currentUser.value = trimmedEmail
                        _currentRole.value = role
                        _currentUserName.value = defaultName
                        recordSignInAttempt(trimmedEmail, success = true)
                        onComplete(true, "Signed in successfully with Firebase!")
                    }
                } else {
                    // Fallback to local Room database authentication
                    val user = userDao?.getUserByEmail(trimmedEmail)
                    if (user != null) {
                        if (user.provider == "email") {
                            val hashedInput = hashPassword(passwordRaw)
                            if (user.passwordHash != hashedInput) {
                                recordSignInAttempt(trimmedEmail, success = false)
                                onComplete(false, "Invalid password.")
                                return@launch
                            }
                        }
                        val userRole = when (user.role) {
                            "PARENT" -> UserRole.PARENT
                            "DRIVER" -> UserRole.DRIVER
                            "ADMIN" -> UserRole.ADMIN
                            else -> role
                        }

                        // Enforce distinct login portals
                        if (userRole != role) {
                            val expectedPortal = when (userRole) {
                                UserRole.PARENT -> "Parent"
                                UserRole.DRIVER -> "Driver"
                                UserRole.ADMIN -> "School Administrator"
                            }
                            onComplete(false, "This account is registered as a $expectedPortal. Please use the correct login portal.")
                            return@launch
                        }

                        _currentUser.value = user.email
                        _currentRole.value = userRole
                        _currentUserName.value = user.name
                        recordSignInAttempt(trimmedEmail, success = true)
                        onComplete(true, "Signed in successfully!")
                    } else {
                        // Pre-registered fallback check for instant login of any typed email for simplicity
                        val stringRole = when (role) {
                            UserRole.PARENT -> "PARENT"
                            UserRole.DRIVER -> "DRIVER"
                            UserRole.ADMIN -> "ADMIN"
                        }
                        val defaultName = trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                        val newUser = UserEntity(trimmedEmail, defaultName, stringRole, "email", hashPassword(passwordRaw.ifEmpty { "demo1234" }))
                        userDao?.insertUser(newUser)
                        
                        _currentUser.value = trimmedEmail
                        _currentRole.value = role
                        _currentUserName.value = defaultName
                        recordSignInAttempt(trimmedEmail, success = true)
                        onComplete(true, "Signed in successfully!")
                    }
                }
            } catch (e: Exception) {
                onComplete(false, "Authentication error: ${e.message}")
            }
        }
    }

    // Sign up logic storing data locally in Room and also with Firebase Auth if available
    fun signUp(name: String, email: String, role: UserRole, provider: String, passwordRaw: String = "", onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val trimmedEmail = email.trim().lowercase()

                val limitCheck = checkSignUpRateLimit(trimmedEmail)
                if (!limitCheck.first) {
                    onComplete(false, limitCheck.second)
                    return@launch
                }
                recordSignUpAttempt(trimmedEmail)

                val existing = userDao?.getUserByEmail(trimmedEmail)
                if (existing != null) {
                    onComplete(false, "Account with this email already exists.")
                    return@launch
                }

                var firebaseSuccess = false
                if (firebaseAuth != null && provider == "email") {
                    try {
                        firebaseAuth!!.createUserWithEmailAndPassword(trimmedEmail, passwordRaw).await()
                        firebaseSuccess = true
                    } catch (e: Exception) {
                        val msg = e.message ?: ""
                        if (msg.contains("no firebase app", ignoreCase = true) ||
                            msg.contains("not initialized", ignoreCase = true) ||
                            e is IllegalStateException) {
                            firebaseSuccess = false
                        } else {
                            onComplete(false, "Firebase Registration: ${e.localizedMessage ?: e.message}")
                            return@launch
                        }
                    }
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
                    provider = if (firebaseSuccess) "firebase_email" else provider,
                    passwordHash = if (provider == "google") "" else hashPassword(passwordRaw)
                )
                userDao?.insertUser(newUser)
                
                // Automatically log in newly registered user
                _currentUser.value = trimmedEmail
                _currentRole.value = role
                _currentUserName.value = name.trim()
                
                val successMsg = if (firebaseSuccess) "Registration successful with Firebase!" else "Registration successful!"
                onComplete(true, successMsg)
            } catch (e: Exception) {
                onComplete(false, "Sign up error: ${e.message}")
            }
        }
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                _distanceKm.value = kotlin.math.round(4.8f * remaining * 10f) / 10f

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

    // --- INTERCONNECTED PORTAL CONTROLLERS ---
    fun updateStudentStatus(studentId: String, newStatus: StudentStatus, reason: String? = null) {
        val currentTimeStr = "7:${(10..45).random()} AM"
        var targetStudentName = ""
        var targetBusPlate = ""
        var parentName = ""

        _students.value = _students.value.map { st ->
            if (st.id == studentId) {
                targetStudentName = st.name
                targetBusPlate = st.assignedBusPlate
                parentName = st.parentName
                val isBoardedNow = (newStatus == StudentStatus.BOARDED)
                st.copy(
                    status = newStatus,
                    boarded = isBoardedNow,
                    boardedTime = if (isBoardedNow) currentTimeStr else st.boardedTime,
                    absentReason = if (newStatus == StudentStatus.ABSENT) (reason ?: "Absent request by parent") else st.absentReason
                )
            } else st
        }

        // Recalculate boarded count
        _boardedCount.value = _students.value.count { it.status == StudentStatus.BOARDED }

        // Update Fleet Bus occupancy dynamically
        _adminFleetBuses.value = _adminFleetBuses.value.map { bus ->
            if (bus.plate == targetBusPlate) {
                val busStudentCount = _students.value.count { it.assignedBusPlate == bus.plate }
                val busBoardedCount = _students.value.count { it.assignedBusPlate == bus.plate && it.status == StudentStatus.BOARDED }
                bus.copy(
                    studentCount = busStudentCount,
                    occupancyCount = busBoardedCount
                )
            } else bus
        }

        // Auto post message sync to all portals
        val actionText = when (newStatus) {
            StudentStatus.BOARDED -> "boarded Bus $targetBusPlate at $currentTimeStr"
            StudentStatus.DROPPED_OFF -> "safely arrived and dropped off at school"
            StudentStatus.ABSENT -> "marked ABSENT today ($reason)"
            StudentStatus.NOT_BOARDED -> "status reset to waiting at stop"
        }

        val eventType = when (newStatus) {
            StudentStatus.BOARDED -> JourneyEventType.BOARDED
            StudentStatus.DROPPED_OFF -> JourneyEventType.ARRIVED_SCHOOL
            StudentStatus.ABSENT -> JourneyEventType.ALIGHTED
            StudentStatus.NOT_BOARDED -> JourneyEventType.IN_TRANSIT
        }
        val currentStopName = _students.value.find { it.id == studentId }?.pickupStop ?: "Kilimani Stop"
        val newJourneyEvent = JourneyEvent(
            studentId = studentId,
            eventType = eventType,
            stopName = currentStopName,
            busPlate = targetBusPlate,
            timestamp = currentTimeStr,
            notes = "Student $targetStudentName $actionText."
        )
        _journeyEvents.value = listOf(newJourneyEvent) + _journeyEvents.value

        sendInterportalMessage(
            senderName = "System Sync",
            senderRole = "SYSTEM",
            recipientRole = "ALL",
            targetBusPlate = targetBusPlate,
            studentName = targetStudentName,
            content = "Student Update: $targetStudentName $actionText."
        )
    }

    fun registerParentWithChild(
        parentFullName: String,
        parentEmail: String,
        parentPassword: String,
        parentPhone: String,
        childFullName: String,
        childGrade: String,
        schoolName: String,
        pickupStop: String,
        photoUrl: String? = null,
        onComplete: (Boolean, String) -> Unit
    ) {
        val trimmedEmail = parentEmail.trim().lowercase()

        viewModelScope.launch {
            val rateLimitCheck = checkSignUpRateLimit(trimmedEmail)
            if (!rateLimitCheck.first) {
                onComplete(false, rateLimitCheck.second)
                return@launch
            }
            recordSignUpAttempt(trimmedEmail)

            val hashedPassword = hashPassword(parentPassword)
            val userEntity = UserEntity(
                email = trimmedEmail,
                name = parentFullName.trim(),
                role = "PARENT",
                provider = "email",
                passwordHash = hashedPassword
            )
            userDao?.insertUser(userEntity)

            val initials = childFullName.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
            val newStudentId = "student_${System.currentTimeMillis()}"
            val assignedBus = when {
                pickupStop.contains("Kawangware", ignoreCase = true) -> "KBZ 441G"
                pickupStop.contains("Westlands", ignoreCase = true) -> "KDE 119A"
                pickupStop.contains("Upper Hill", ignoreCase = true) -> "KDE 902Y"
                else -> "KDE 732X"
            }

            val newStudent = Student(
                id = newStudentId,
                name = childFullName.trim(),
                initials = if (initials.isNotBlank()) initials else "ST",
                avatarColor = DefaultAccentBlue,
                parentName = parentFullName.trim(),
                parentEmail = trimmedEmail,
                parentPhone = parentPhone.trim(),
                assignedBusPlate = assignedBus,
                assignedDriverName = "Erick Mwangi",
                pickupStop = pickupStop,
                dropoffStop = schoolName,
                status = StudentStatus.NOT_BOARDED,
                classGrade = childGrade,
                schoolName = schoolName,
                photoUrl = photoUrl,
                routeId = "route_1"
            )

            _students.value = _students.value + newStudent

            val link = ParentStudentLink(
                parentId = trimmedEmail,
                studentId = newStudentId,
                relationship = "parent",
                isPrimary = true
            )
            _parentStudentLinks.value = _parentStudentLinks.value + link

            val welcomeEvent = JourneyEvent(
                studentId = newStudentId,
                eventType = JourneyEventType.IN_TRANSIT,
                stopName = pickupStop,
                busPlate = assignedBus,
                timestamp = "07:00 AM",
                notes = "Registration active for $childFullName ($childGrade) at $schoolName"
            )
            _journeyEvents.value = listOf(welcomeEvent) + _journeyEvents.value

            _currentUser.value = trimmedEmail
            _currentRole.value = UserRole.PARENT
            _currentUserName.value = parentFullName.trim()

            onComplete(true, "Registration successful!")
        }
    }

    fun boardStudent(studentId: String) {
        updateStudentStatus(studentId, StudentStatus.BOARDED)
    }

    fun unboardStudent(studentId: String) {
        updateStudentStatus(studentId, StudentStatus.DROPPED_OFF)
    }

    fun setStudentAbsentByParent(studentId: String, reason: String) {
        updateStudentStatus(studentId, StudentStatus.ABSENT, reason)
    }

    fun addParentNoteForDriver(studentId: String, note: String) {
        var studentName = ""
        var busPlate = ""
        _students.value = _students.value.map { st ->
            if (st.id == studentId) {
                studentName = st.name
                busPlate = st.assignedBusPlate
                st.copy(parentNote = note)
            } else st
        }
        if (note.isNotBlank()) {
            sendInterportalMessage(
                senderName = _currentUserName.value,
                senderRole = "PARENT",
                recipientRole = "DRIVER",
                targetBusPlate = busPlate,
                studentName = studentName,
                content = "Parent Note for $studentName: $note"
            )
        }
    }

    fun sendInterportalMessage(
        senderName: String,
        senderRole: String,
        recipientRole: String,
        targetBusPlate: String,
        studentName: String? = null,
        content: String
    ) {
        if (content.isBlank()) return
        val newMsg = InterportalMessage(
            id = "msg_${System.currentTimeMillis()}",
            senderName = senderName,
            senderRole = senderRole,
            recipientRole = recipientRole,
            targetBusPlate = targetBusPlate,
            studentName = studentName,
            content = content,
            timestamp = "Just Now"
        )
        _interportalMessages.value = listOf(newMsg) + _interportalMessages.value
    }

    fun triggerSOS(driverName: String = "Erick Mwangi", busPlate: String = "KDE 732X") {
        _sosActive.value = true
        val alertDetail = "CRITICAL EMERGENCY: Driver $driverName on Bus $busPlate triggered SOS assistance button!"
        postAlert("EMERGENCY SOS TRIGGERED", alertDetail, AlertSeverity.DANGER)
        sendInterportalMessage(
            senderName = driverName,
            senderRole = "DRIVER",
            recipientRole = "ADMIN",
            targetBusPlate = busPlate,
            content = alertDetail
        )
    }

    fun dismissSOS() {
        _sosActive.value = false
        // Remove SOS alert from active alerts
        _activeAlerts.value = _activeAlerts.value.filter { !it.title.contains("SOS", ignoreCase = true) }
    }

    fun addOrUpdateStudent(
        id: String?,
        name: String,
        parentName: String,
        parentPhone: String,
        busPlate: String,
        driverName: String,
        pickupStop: String
    ) {
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString("").take(2).uppercase().ifEmpty { "ST" }
        val newId = id ?: "st_${System.currentTimeMillis()}"
        val existing = _students.value.find { it.id == newId }
        
        val updatedStudent = Student(
            id = newId,
            name = name,
            initials = initials,
            avatarColor = existing?.avatarColor ?: DefaultAccentBlue,
            parentName = parentName,
            parentEmail = "parent_${name.lowercase().replace(" ", "")}@safiri.co.ke",
            parentPhone = parentPhone,
            assignedBusPlate = busPlate,
            assignedDriverName = driverName,
            pickupStop = pickupStop,
            dropoffStop = "St. Mary's Academy",
            status = existing?.status ?: StudentStatus.NOT_BOARDED,
            boardedTime = existing?.boardedTime,
            absentReason = existing?.absentReason,
            parentNote = existing?.parentNote
        )

        _students.value = if (existing != null) {
            _students.value.map { if (it.id == newId) updatedStudent else it }
        } else {
            _students.value + updatedStudent
        }

        // Sync fleet bus counts
        _adminFleetBuses.value = _adminFleetBuses.value.map { bus ->
            if (bus.plate == busPlate) {
                val count = _students.value.count { it.assignedBusPlate == busPlate }
                bus.copy(studentCount = count)
            } else bus
        }
    }

    fun linkStudentToParent(
        studentId: String,
        parentName: String,
        parentEmail: String,
        parentPhone: String
    ) {
        var studentName = ""
        var busPlate = ""
        _students.value = _students.value.map { st ->
            if (st.id == studentId) {
                studentName = st.name
                busPlate = st.assignedBusPlate
                st.copy(
                    parentName = parentName,
                    parentEmail = parentEmail,
                    parentPhone = parentPhone
                )
            } else st
        }

        sendInterportalMessage(
            senderName = "School Administration",
            senderRole = "ADMIN",
            recipientRole = "ALL",
            targetBusPlate = busPlate,
            studentName = studentName,
            content = "Parent Linked: Student $studentName is now linked to parent $parentName ($parentEmail)."
        )
    }

    fun linkChildToCurrentParent(studentId: String) {
        val parentName = if (_currentUserName.value.isNotBlank() && _currentUserName.value != "User") _currentUserName.value else "Sarah Ochieng"
        val parentEmail = _currentUser.value ?: "parent@safiri.co.ke"
        linkStudentToParent(studentId, parentName, parentEmail, "0712345678")
    }

    fun addDriverToRoster(name: String, busPlate: String, yearsExp: Int) {
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString("").take(2).uppercase().ifEmpty { "DR" }
        val newDriver = DriverRosterItem(
            id = "dr_${System.currentTimeMillis()}",
            name = name,
            initials = initials,
            avatarColor = DefaultAccentBlue,
            busPlate = busPlate,
            yearsExperience = yearsExp,
            performanceProgress = 0.90f,
            onTimePct = 94
        )
        _driversList.value = _driversList.value + newDriver

        // Ensure bus exists in fleet
        if (_adminFleetBuses.value.none { it.plate == busPlate }) {
            val newBus = FleetBusItem(
                id = "bus_${System.currentTimeMillis()}",
                plate = busPlate,
                driverName = name,
                studentCount = 0,
                occupancyCount = 0,
                capacity = 22,
                status = "Active",
                statusColor = DefaultGreenAccent
            )
            _adminFleetBuses.value = _adminFleetBuses.value + newBus
        } else {
            _adminFleetBuses.value = _adminFleetBuses.value.map { bus ->
                if (bus.plate == busPlate) bus.copy(driverName = name) else bus
            }
        }
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

    // --- GLOBAL TRANSIT APPS INTEGRATION CONTROLLERS ---
    fun updateCrowdedness(level: String) {
        _crowdednessLevel.value = level
    }

    fun toggleGetOffAlert() {
        _getOffAlertActive.value = !_getOffAlertActive.value
    }

    fun upvoteIncident(id: String) {
        _communityIncidents.value = _communityIncidents.value.map {
            if (it.id == id) {
                val nextVotes = if (it.isUpvoted) it.votes - 1 else it.votes + 1
                it.copy(votes = nextVotes, isUpvoted = !it.isUpvoted)
            } else it
        }
    }

    fun postCommunityIncident(title: String, location: String, category: String) {
        if (title.isBlank()) return
        val newInc = IncidentReport(
            id = System.currentTimeMillis().toString(),
            title = title,
            location = location.ifBlank { "Nairobi Metro" },
            reportedTime = "Just now",
            votes = 1,
            category = category,
            isUpvoted = true
        )
        _communityIncidents.value = listOf(newInc) + _communityIncidents.value
    }

    fun shareLiveTripLink() {
        _shareToastMessage.value = "Live Trip Link copied: https://safiri.app/live/KDE-732X"
    }

    fun clearShareToast() {
        _shareToastMessage.value = null
    }

    fun selectTransitAppFilter(filter: String) {
        _selectedTransitAppFilter.value = filter
    }

    private fun setupFirebaseAuthListener() {
        try {
            firebaseAuth?.addAuthStateListener { auth ->
                val fbUser = auth.currentUser
                if (fbUser != null) {
                    val email = fbUser.email
                    if (email != null && _currentUser.value != email) {
                        viewModelScope.launch {
                            val user = userDao?.getUserByEmail(email)
                            if (user != null) {
                                val userRole = when (user.role) {
                                    "PARENT" -> UserRole.PARENT
                                    "DRIVER" -> UserRole.DRIVER
                                    "ADMIN" -> UserRole.ADMIN
                                    else -> UserRole.PARENT
                                }
                                _currentUser.value = user.email
                                _currentRole.value = userRole
                                _currentUserName.value = user.name
                            } else {
                                // Fallback for newly authenticated Firebase users not yet in Room
                                val defaultName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                                val newUser = UserEntity(email, defaultName, "PARENT", "firebase_email", "")
                                userDao?.insertUser(newUser)
                                _currentUser.value = email
                                _currentRole.value = UserRole.PARENT
                                _currentUserName.value = defaultName
                            }
                        }
                    }
                } else {
                    // Firebase session is null.
                    // If the current local session was authenticated via Firebase, clear it.
                    if (_currentUser.value != null) {
                        viewModelScope.launch {
                            val email = _currentUser.value ?: ""
                            val user = userDao?.getUserByEmail(email)
                            if (user != null && user.provider == "firebase_email") {
                                _currentUser.value = null
                                _currentRole.value = null
                                _currentUserName.value = "User"
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val _showAiAssistantDialog = MutableStateFlow(false)
    val showAiAssistantDialog: StateFlow<Boolean> = _showAiAssistantDialog.asStateFlow()

    fun openAiAssistantDialog() {
        _showAiAssistantDialog.value = true
    }

    fun dismissAiAssistantDialog() {
        _showAiAssistantDialog.value = false
    }

    // --- GEMINI AI INTEGRATION METHODS ---

    /**
     * Low-Latency fast response query (gemini-3.1-flash-lite-preview)
     */
    fun runFastAiQuery(prompt: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val response = GeminiRepository.generateFastResponse(prompt)
            onResult(response)
        }
    }

    /**
     * Google Maps Grounding query (gemini-3.5-flash with googleMaps tool)
     */
    fun runMapsGroundedQuery(prompt: String, location: String = "Nairobi, Kenya", onResult: (String) -> Unit) {
        viewModelScope.launch {
            val response = GeminiRepository.generateMapsGroundedResponse(prompt, location)
            onResult(response)
        }
    }

    /**
     * High Thinking Mode query (gemini-3.1-pro-preview with thinkingLevel = HIGH, no maxOutputTokens)
     */
    fun runHighThinkingQuery(prompt: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val response = GeminiRepository.generateHighThinkingResponse(prompt)
            onResult(response)
        }
    }

    /**
     * General tasks query (gemini-3.5-flash)
     */
    fun runGeneralAiQuery(prompt: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val response = GeminiRepository.generateGeneralResponse(prompt)
            onResult(response)
        }
    }

    /**
     * Complex analysis query (gemini-3.1-pro-preview)
     */
    fun runComplexAnalysis(prompt: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val response = GeminiRepository.generateComplexAnalysis(prompt)
            onResult(response)
        }
    }

    // --- FIRESTORE PERSISTENCE METHODS ---

    fun persistStudentToFirestore(student: Student) {
        try {
            firestore?.collection("students")
                ?.document(student.id)
                ?.set(mapOf(
                    "id" to student.id,
                    "name" to student.name,
                    "parentName" to student.parentName,
                    "parentEmail" to student.parentEmail,
                    "parentPhone" to student.parentPhone,
                    "status" to student.status.name,
                    "assignedBusPlate" to student.assignedBusPlate,
                    "pickupStop" to student.pickupStop,
                    "dropoffStop" to student.dropoffStop,
                    "updatedAt" to System.currentTimeMillis()
                ))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun persistMessageToFirestore(message: InterportalMessage) {
        try {
            firestore?.collection("interportal_messages")
                ?.document(message.id)
                ?.set(mapOf(
                    "id" to message.id,
                    "senderName" to message.senderName,
                    "senderRole" to message.senderRole,
                    "recipientRole" to message.recipientRole,
                    "targetBusPlate" to message.targetBusPlate,
                    "content" to message.content,
                    "timestamp" to message.timestamp
                ))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        simulationJob?.cancel()
        super.onCleared()
    }
}
