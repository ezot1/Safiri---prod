package com.example.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.*
import com.example.models.AlertSeverity
import com.example.models.InterportalMessage
import com.example.models.JourneyEvent
import com.example.models.JourneyEventType
import com.example.models.Student
import com.example.models.StudentStatus
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ParentDashboard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Directions, 1: Lines & Stops, 2: Plan, 3: Alerts, 4: Me
    val themeMode by viewModel.themeMode.collectAsState()
    val activeAlerts by viewModel.activeAlerts.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundColor,
        topBar = {
            // Moovit-style Brand Top Header
            Surface(
                color = SurfaceColor,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MoovitOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsBus,
                                contentDescription = "Safiri Transit",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SAFIRI",
                                    color = TextPrimaryColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AccentBlue.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "TRANSIT",
                                        color = AccentBlue,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(GreenAccent)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Nairobi Metro • Live GPS",
                                    color = TextSecondaryColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Theme Toggle Button (Light / Dark Mode)
                    IconButton(
                        onClick = { viewModel.toggleThemeMode() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Surface3Color)
                            .testTag("theme_mode_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (themeMode == AppThemeMode.LIGHT) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            contentDescription = "Toggle Dark/Light Mode",
                            tint = if (themeMode == AppThemeMode.LIGHT) TextPrimaryColor else AmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceColor,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Filled.DirectionsBus, contentDescription = "Directions") },
                    label = { Text("Directions", fontSize = 11.sp, fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        unselectedIconColor = TextTertiaryColor,
                        selectedTextColor = AccentBlue,
                        unselectedTextColor = TextTertiaryColor,
                        indicatorColor = AccentBlue.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("parent_tab_home")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Filled.Star, contentDescription = "Saved") },
                    label = { Text("Saved", fontSize = 11.sp, fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        unselectedIconColor = TextTertiaryColor,
                        selectedTextColor = AccentBlue,
                        unselectedTextColor = TextTertiaryColor,
                        indicatorColor = AccentBlue.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("parent_tab_saved")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Filled.AltRoute, contentDescription = "Plan") },
                    label = { Text("Plan", fontSize = 11.sp, fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        unselectedIconColor = TextTertiaryColor,
                        selectedTextColor = AccentBlue,
                        unselectedTextColor = TextTertiaryColor,
                        indicatorColor = AccentBlue.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("parent_tab_planner")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeAlerts.isNotEmpty()) {
                                    Badge(
                                        containerColor = RedAccent,
                                        contentColor = Color.White
                                    ) {
                                        Text("${activeAlerts.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Alerts")
                        }
                    },
                    label = { Text("Alerts", fontSize = 11.sp, fontWeight = if (activeTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        unselectedIconColor = TextTertiaryColor,
                        selectedTextColor = AccentBlue,
                        unselectedTextColor = TextTertiaryColor,
                        indicatorColor = AccentBlue.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("parent_tab_alerts")
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Me") },
                    label = { Text("Me", fontSize = 11.sp, fontWeight = if (activeTab == 4) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        unselectedIconColor = TextTertiaryColor,
                        selectedTextColor = AccentBlue,
                        unselectedTextColor = TextTertiaryColor,
                        indicatorColor = AccentBlue.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("parent_tab_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (activeTab) {
                0 -> ParentHomeTab(viewModel)
                1 -> ParentSavedTab(viewModel)
                2 -> ParentPlanTab(viewModel)
                3 -> ParentAlertsTab(viewModel)
                4 -> ParentMeTab(viewModel)
            }
        }
    }
}

// ==========================================
// 1. HOME TAB (ParentHomeTab)
// ==========================================
@Composable
fun ParentHomeTab(viewModel: AppViewModel) {
    val progress by viewModel.progress.collectAsState()
    val etaMinutes by viewModel.etaMinutes.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()
    val stopsLeft by viewModel.stopsLeft.collectAsState()
    val progressPct by viewModel.progressPct.collectAsState()
    val stops by viewModel.routeStops.collectAsState()

    val currentChild by viewModel.currentChild.collectAsState()
    val journeyEvents by viewModel.journeyEvents.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()

    val assignedBusPlate = currentChild?.assignedBusPlate ?: "KDE 732X"

    val mapsApiKey by viewModel.mapsApiKey.collectAsState()

    // Smooth scrolling layout for Parent Home Tab
    val parentScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(parentScrollState)
    ) {
        // Map Hero at the top showing the specific assigned bus
        MapHero(
            progressPct = progress,
            busPlate = assignedBusPlate,
            statusText = "On Route",
            etaMinutes = etaMinutes,
            stops = stops,
            customApiKey = mapsApiKey,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Surface3Color)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Child-Specific Greeting Header & Live Boarding Status Card
            ChildSpecificGreetingCard(
                child = currentChild,
                parentName = currentUserName,
                etaMinutes = etaMinutes,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Child Journey Timeline Card (Boarded -> En route -> Arrived at school)
            ChildJourneyTimelineCard(
                child = currentChild,
                events = journeyEvents,
                etaMinutes = etaMinutes
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Child's Activity Feed Today Card
            ChildActivityFeedCard(
                child = currentChild,
                events = journeyEvents
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Big ETA card
            SafiriCard(testTag = "eta_card") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$etaMinutes",
                            color = AccentBlue,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = " min to school",
                            color = TextSecondaryColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    SafiriTag(text = "On Time", color = GreenAccent)
                }
                Text(
                    text = "Bus $assignedBusPlate is en route to ${currentChild?.schoolName ?: "St. Austin's Academy"}",
                    color = TextPrimaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricTile(
                        label = "DISTANCE REM.",
                        value = "${distanceKm} km",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    MetricTile(
                        label = "STOPS LEFT",
                        value = "$stopsLeft",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    MetricTile(
                        label = "PROGRESS",
                        value = "$progressPct%",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AccentBlue,
                    trackColor = Surface3Color,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interconnected Real-time Cards
            ParentSOSAlertCard(viewModel)
            ParentChildrenSyncCard(viewModel)
            Spacer(modifier = Modifier.height(14.dp))
            ParentDriverMessageCard(viewModel)
            Spacer(modifier = Modifier.height(14.dp))

            // Occupancy Section card
            SafiriCard(testTag = "occupancy_card") {
                OccupancyBar(count = 16, capacity = 22)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Moovit Live Guidance Active Banner (if user started trip navigation)
            MoovitLiveGuidanceBanner(viewModel)
            Spacer(modifier = Modifier.height(12.dp))

            // Integrated Real-Time Transit Features
            LiveNavigationGuideCard(viewModel)
            Spacer(modifier = Modifier.height(12.dp))
            PlatformAndTransferCard(viewModel)
            Spacer(modifier = Modifier.height(12.dp))
            LiveJourneyShareCard(viewModel)

            Spacer(modifier = Modifier.height(14.dp))

            // Today's schedule
            SafiriCard(testTag = "schedule_card") {
                Text(
                    text = "TODAY'S SCHEDULE",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                ScheduleRow(time = "06:45 AM", label = "Morning pickup • Kilimani", status = "Done", color = GreenAccent, active = false)
                ScheduleRow(time = "07:30 AM", label = "Arrive at school • Academy", status = "Arrived", color = GreenAccent, active = false)
                ScheduleRow(time = "03:45 PM", label = "Afternoon pickup • Academy", status = "En Route", color = AccentBlue, active = true)
                ScheduleRow(time = "04:30 PM", label = "Return home • Kilimani", status = "Scheduled", color = TextTertiaryColor, active = false)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// 2. SAVED TAB (ParentSavedTab)
// ==========================================
@Composable
fun ParentSavedTab(viewModel: AppViewModel) {
    val starredStops by viewModel.starredStops.collectAsState()
    val nearbyStops by viewModel.nearbyStops.collectAsState()

    ScrollView(
        modifier = Modifier.fillMaxSize(),
        contentContainerStyle = Modifier.padding(16.dp)
    ) {
        Text(
            text = "SAVED & FAVORITE STOPS",
            color = TextPrimaryColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Quick access to your regular pick-ups and nearbys",
            color = TextSecondaryColor,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Starred Stops Card
        SafiriCard(testTag = "starred_stops_card") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Star, contentDescription = "Favorites", tint = AmberAccent)
                Text(
                    text = "FAVORITE STOPS",
                    color = TextTertiaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            val favoriteStopList = listOf(
                Triple("Kilimani Stop", "Near Valley Arcade, Nairobi", "ETA 8m"),
                Triple("St. Mary's Academy", "School Campus Drop-off", "Arrived"),
                Triple("Kawangware Terminal", "Bus Bay 4, Naivasha Rd", "Next Day")
            )

            favoriteStopList.forEach { (name, desc, eta) ->
                val isStarred = starredStops.contains(name)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.toggleStarStop(name) }) {
                            Icon(
                                imageVector = if (isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Toggle Star",
                                tint = if (isStarred) AmberAccent else TextTertiaryColor
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(text = name, color = TextPrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = desc, color = TextSecondaryColor, fontSize = 11.sp)
                        }
                    }
                    SafiriTag(text = eta, color = if (eta == "Arrived") GreenAccent else AccentBlue)
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nearby Stops Card
        SafiriCard(testTag = "nearby_stops_card") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Location", tint = AccentBlue)
                Text(
                    text = "NEARBY STOPS DETECTED",
                    color = TextTertiaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            nearbyStops.forEach { (name, distance) ->
                val isSaved = starredStops.contains(name)
                val btnColor by animateColorAsState(if (isSaved) GreenAccent else Surface3Color, label = "SaveBtnColor")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = name, color = TextPrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = distance, color = TextSecondaryColor, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.toggleStarStop(name) },
                        colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isSaved) "Saved ✓" else "Save",
                            color = if (isSaved) BackgroundColor else TextPrimaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Moovit Transit Lines & Timetables Explorer
        MoovitLinesExplorerCard(viewModel)
    }
}

// ==========================================
// 3. PLAN TAB (ParentPlanTab)
// ==========================================
@Composable
fun ParentPlanTab(viewModel: AppViewModel) {
    var fromText by remember { mutableStateOf("Kilimani, Nairobi") }
    var toText by remember { mutableStateOf("St. Mary's Academy") }
    val selectedOption by viewModel.selectedRouteOption.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isSearching by remember { mutableStateOf(false) }
    var searchCompleted by remember { mutableStateOf(false) }

    ScrollView(
        modifier = Modifier.fillMaxSize(),
        contentContainerStyle = Modifier.padding(16.dp)
    ) {
        Text(
            text = "PLAN YOUR TRIP",
            color = TextPrimaryColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Compare schools transit lanes, delays & timings",
            color = TextSecondaryColor,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Moovit Multi-Modal Trip Planner with Live Turn-by-Turn Guidance
        MoovitMultiModalPlannerCard(viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Search inputs
        SafiriCard(testTag = "planner_form") {
            Text(text = "FROM", color = TextTertiaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            TextField(
                value = fromText,
                onValueChange = { fromText = it },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceColor,
                    unfocusedContainerColor = SurfaceColor,
                    focusedTextColor = TextPrimaryColor,
                    unfocusedTextColor = TextPrimaryColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "TO", color = TextTertiaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            TextField(
                value = toText,
                onValueChange = { toText = it },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceColor,
                    unfocusedContainerColor = SurfaceColor,
                    focusedTextColor = TextPrimaryColor,
                    unfocusedTextColor = TextPrimaryColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isSearching = true
                        searchCompleted = false
                        delay(1200)
                        isSearching = false
                        searchCompleted = true
                    }
                },
                enabled = !isSearching,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier.fillMaxWidth().testTag("search_routes_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = BackgroundColor,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Optimizing route...")
                } else {
                    Text("Search routes", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (searchCompleted) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreenAccent.copy(alpha = 0.15f))
                    .border(1.dp, GreenAccent, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Success",
                    tint = GreenAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Optimal lanes updated! Erick Mwangi's shuttle (KDE 732X) is verified as the fastest path from $fromText to $toText.",
                    color = TextPrimaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Compare Options
        Text(
            text = "AVAILABLE ROUTE SHUTTLES",
            color = TextTertiaryColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Option 1: Fastest (Accent blue)
        RouteOptionCard(
            plate = "KDE 732X",
            driver = "Erick Mwangi",
            duration = "34 min",
            schedule = "06:45 AM - 07:19 AM",
            stops = "Kilimani • Westlands • Academy",
            isAlternative = false,
            isSelected = selectedOption == "fastest",
            onClick = { viewModel.selectRouteOption("fastest") },
            testTag = "route_option_1"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option 2: Alternative (Purple)
        RouteOptionCard(
            plate = "KDE 119A",
            driver = "John Kiprop",
            duration = "45 min",
            schedule = "06:30 AM - 07:15 AM",
            stops = "Kawangware • Kilimani • Upper Hill • Academy",
            isAlternative = true,
            isSelected = selectedOption == "alternative",
            onClick = { viewModel.selectRouteOption("alternative") },
            testTag = "route_option_2"
        )
    }
}

@Composable
fun RouteOptionCard(
    plate: String,
    driver: String,
    duration: String,
    schedule: String,
    stops: String,
    isAlternative: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor = if (isSelected) {
        if (isAlternative) PurpleAccent else AccentBlue
    } else {
        BorderColor
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2Color)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SafiriTag(text = plate, color = if (isAlternative) PurpleAccent else AccentBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Driver: $driver", color = TextPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = duration,
                    color = if (isAlternative) PurpleAccent else AccentBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = schedule, color = TextPrimaryColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (isAlternative) {
                    Text(text = "Alternative", color = PurpleAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(text = "Fastest Route", color = GreenAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "STOPS: $stops", color = TextSecondaryColor, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

// ==========================================
// 4. ALERTS TAB (ParentAlertsTab)
// ==========================================
@Composable
fun ParentAlertsTab(viewModel: AppViewModel) {
    val alerts by viewModel.activeAlerts.collectAsState()

    ScrollView(
        modifier = Modifier.fillMaxSize(),
        contentContainerStyle = Modifier.padding(16.dp)
    ) {
        Text(
            text = "SERVICE ALERTS",
            color = TextPrimaryColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Active transit announcements & road warnings",
            color = TextSecondaryColor,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Alerts List Card
        SafiriCard(testTag = "alerts_list_card") {
            if (alerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active service alerts.", color = TextSecondaryColor, fontSize = 14.sp)
                }
            } else {
                alerts.forEach { alert ->
                    val color = when (alert.severity) {
                        AlertSeverity.DANGER -> RedAccent
                        AlertSeverity.WARNING -> AmberAccent
                        AlertSeverity.INFO -> AccentBlue
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = alert.title, color = TextPrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                SafiriTag(text = alert.status, color = color)
                            }
                            Text(text = alert.detail, color = TextSecondaryColor, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
                            Text(text = alert.timestamp, color = TextTertiaryColor, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    HorizontalDivider(color = BorderColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active Fleet Occupancy Mini-Bars
        Text(
            text = "ACTIVE FLEET OCCUPANCY RATES",
            color = TextTertiaryColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SafiriCard(testTag = "fleet_occupancy_card") {
            FleetMiniBar(plate = "KDE 732X", occupancy = "18/22", progress = 0.81f, color = AmberAccent)
            FleetMiniBar(plate = "KDE 119A", occupancy = "12/22", progress = 0.54f, color = AmberAccent)
            FleetMiniBar(plate = "KDE 540R", occupancy = "21/22", progress = 0.95f, color = RedAccent)
            FleetMiniBar(plate = "KDE 902Y", occupancy = "8/22", progress = 0.36f, color = GreenAccent)
        }
    }
}

@Composable
fun FleetMiniBar(plate: String, occupancy: String, progress: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = plate, color = TextPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = occupancy, color = TextSecondaryColor, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Surface3Color)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

// ==========================================
// 5. ME TAB (ParentMeTab)
// ==========================================
@Composable
fun ParentMeTab(viewModel: AppViewModel) {
    val context = LocalContext.current
    val t1 by viewModel.toggleTenMin.collectAsState()
    val t2 by viewModel.toggleChildBoarded.collectAsState()
    val t3 by viewModel.toggleDelayWarning.collectAsState()
    val t4 by viewModel.toggleTripCompleted.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserEmail by viewModel.currentUser.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var parentName by remember(currentUserName) { mutableStateOf(currentUserName) }
    var parentPhone by remember { mutableStateOf("+254 712 345 678") }

    // Dynamically calculate initials
    val initials = remember(parentName) {
        parentName.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it[0].uppercaseChar() }
            .joinToString("")
            .ifEmpty { "P" }
    }

    ScrollView(
        modifier = Modifier.fillMaxSize(),
        contentContainerStyle = Modifier.padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(AccentBlue, PurpleAccent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, color = BackgroundColor, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showEditProfileDialog = true }
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = parentName, color = TextPrimaryColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit Profile",
                    tint = AccentBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    val clean = parentPhone.replace(" ", "").replace("-", "")
                    if (clean.isNotBlank()) {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$clean")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }
            ) {
                Text(text = parentPhone, color = TextSecondaryColor, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Filled.Phone, contentDescription = "Call", tint = GreenAccent, modifier = Modifier.size(12.dp))
            }
            if (currentUserEmail != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = currentUserEmail ?: "", color = TextTertiaryColor, fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        SafiriTag(text = "Parent Account", color = AccentBlue)


        Spacer(modifier = Modifier.height(24.dp))

        // Notification toggles card
        SafiriCard(testTag = "notification_toggles_card") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = "Alerts", tint = AccentBlue)
                Text(
                    text = "NOTIFICATION SETTINGS",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            ToggleRow(label = "Bus 10 min away", subLabel = "Ping when shuttle is approaching your block", checked = t1, onCheckedChange = { viewModel.toggleTenMin.value = it })
            HorizontalDivider(color = BorderColor)
            ToggleRow(label = "Child boarded", subLabel = "Instant notification when student logs onto bus", checked = t2, onCheckedChange = { viewModel.toggleChildBoarded.value = it })
            HorizontalDivider(color = BorderColor)
            ToggleRow(label = "Delay warnings", subLabel = "Alert if severe gridlock affects ETA by 5+ mins", checked = t3, onCheckedChange = { viewModel.toggleDelayWarning.value = it })
            HorizontalDivider(color = BorderColor)
            ToggleRow(label = "Trip completed", subLabel = "SMS report when bus reaches safe destination", checked = t4, onCheckedChange = { viewModel.toggleTripCompleted.value = it })
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Appearance & Theme Mode Card (Dark, Light, System)
        val currentThemeMode by viewModel.themeMode.collectAsState()
        SafiriCard(testTag = "appearance_theme_card") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Palette, contentDescription = "Appearance", tint = MoovitOrange)
                Text(
                    text = "APPEARANCE & THEME",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Choose your preferred color theme for maps, lines & timetables",
                color = TextSecondaryColor,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Light Mode Option
                val isLightSelected = currentThemeMode == AppThemeMode.LIGHT
                Surface(
                    onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLightSelected) AccentBlue.copy(alpha = 0.15f) else Surface3Color,
                    border = BorderStroke(1.5.dp, if (isLightSelected) AccentBlue else BorderColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .testTag("theme_option_light")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LightMode,
                            contentDescription = "Light Theme",
                            tint = if (isLightSelected) AccentBlue else TextSecondaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Light",
                            color = if (isLightSelected) AccentBlue else TextPrimaryColor,
                            fontSize = 12.sp,
                            fontWeight = if (isLightSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // Dark Mode Option
                val isDarkSelected = currentThemeMode == AppThemeMode.DARK
                Surface(
                    onClick = { viewModel.setThemeMode(AppThemeMode.DARK) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkSelected) AccentBlue.copy(alpha = 0.15f) else Surface3Color,
                    border = BorderStroke(1.5.dp, if (isDarkSelected) AccentBlue else BorderColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .testTag("theme_option_dark")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = "Dark Theme",
                            tint = if (isDarkSelected) AccentBlue else TextSecondaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dark",
                            color = if (isDarkSelected) AccentBlue else TextPrimaryColor,
                            fontSize = 12.sp,
                            fontWeight = if (isDarkSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // System Default Option
                val isSystemSelected = currentThemeMode == AppThemeMode.SYSTEM
                Surface(
                    onClick = { viewModel.setThemeMode(AppThemeMode.SYSTEM) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSystemSelected) AccentBlue.copy(alpha = 0.15f) else Surface3Color,
                    border = BorderStroke(1.5.dp, if (isSystemSelected) AccentBlue else BorderColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .testTag("theme_option_system")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrightnessAuto,
                            contentDescription = "System Auto Theme",
                            tint = if (isSystemSelected) AccentBlue else TextSecondaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "System",
                            color = if (isSystemSelected) AccentBlue else TextPrimaryColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSystemSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // API Key Configuration Card (Google Maps & Gemini AI)
        val currentMapsApiKey by viewModel.mapsApiKey.collectAsState()
        val currentGeminiApiKey by viewModel.geminiApiKey.collectAsState()
        val apiKeySavedToast by viewModel.apiKeySavedToast.collectAsState()
        var editMapsKey by remember(currentMapsApiKey) { mutableStateOf(currentMapsApiKey) }
        var editGeminiKey by remember(currentGeminiApiKey) { mutableStateOf(currentGeminiApiKey) }
        var isApiCardExpanded by remember { mutableStateOf(false) }

        SafiriCard(testTag = "api_key_integration_card") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isApiCardExpanded = !isApiCardExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Key, contentDescription = "API Keys", tint = AccentBlue)
                    Column {
                        Text(
                            text = "API KEYS & MAPS INTEGRATION",
                            color = TextTertiaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = if (currentMapsApiKey.isNotBlank()) "Google Maps: Connected" else "Google Maps: Ready to configure",
                            color = if (currentMapsApiKey.isNotBlank()) GreenAccent else TextSecondaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Icon(
                    imageVector = if (isApiCardExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "Expand API Settings",
                    tint = TextSecondaryColor
                )
            }

            if (isApiCardExpanded) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Configure your Google Maps and Gemini AI keys to activate live turn-by-turn map tiles and intelligent transit queries.",
                    color = TextSecondaryColor,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Google Maps API Key Input
                Text(
                    text = "Google Maps API Key",
                    color = TextPrimaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = editMapsKey,
                    onValueChange = { editMapsKey = it },
                    placeholder = { Text("Enter Google Maps API Key (AIza...)", fontSize = 12.sp, color = TextTertiaryColor) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Surface3Color,
                        unfocusedContainerColor = Surface3Color,
                        focusedTextColor = TextPrimaryColor,
                        unfocusedTextColor = TextPrimaryColor,
                        focusedIndicatorColor = AccentBlue,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .testTag("maps_api_key_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Gemini API Key Input
                Text(
                    text = "Gemini AI API Key",
                    color = TextPrimaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = editGeminiKey,
                    onValueChange = { editGeminiKey = it },
                    placeholder = { Text("Enter Gemini API Key (AIza...)", fontSize = 12.sp, color = TextTertiaryColor) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Surface3Color,
                        unfocusedContainerColor = Surface3Color,
                        focusedTextColor = TextPrimaryColor,
                        unfocusedTextColor = TextPrimaryColor,
                        focusedIndicatorColor = AccentBlue,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .testTag("gemini_api_key_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.updateMapsApiKey(editMapsKey)
                        viewModel.updateGeminiApiKey(editGeminiKey)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_api_keys_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Apply to Maps", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (apiKeySavedToast != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = apiKeySavedToast ?: "",
                        color = GreenAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Safiri App Identity & Brand Showcase Card
        SafiriCard(testTag = "app_brand_card") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.img_app_logo),
                    contentDescription = "Safiri Brand Logo",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Safiri Transit",
                            color = TextPrimaryColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        SafiriTag(text = "v2.4", color = GreenAccent)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Smart School Transport & Multi-Modal Transit • Nairobi, KE",
                        color = TextSecondaryColor,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.signOut() },
            colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("sign_out_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign Out", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(parentName) }
        var tempPhone by remember { mutableStateOf(parentPhone) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = SurfaceColor,
            title = {
                Text(
                    text = "EDIT PROFILE INFORMATION",
                    color = TextPrimaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Parent Full Name", color = TextSecondaryColor, fontSize = 12.sp)
                    TextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Surface3Color,
                            unfocusedContainerColor = Surface3Color,
                            focusedTextColor = TextPrimaryColor,
                            unfocusedTextColor = TextPrimaryColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    )
                    Text("Contact Phone", color = TextSecondaryColor, fontSize = 12.sp)
                    TextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Surface3Color,
                            unfocusedContainerColor = Surface3Color,
                            focusedTextColor = TextPrimaryColor,
                            unfocusedTextColor = TextPrimaryColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        parentName = tempName
                        parentPhone = tempPhone
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Save Changes", color = BackgroundColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = TextSecondaryColor)
                }
            }
        )
    }
}

// ==========================================
// ADDITIONAL REUSABLE VIEWS
// ==========================================
@Composable
fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Surface3Color)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = TextTertiaryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = TextPrimaryColor, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ScheduleRow(time: String, label: String, status: String, color: Color, active: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dot indicator
        Box(
            modifier = Modifier
                .size(if (active) 12.dp else 8.dp)
                .clip(CircleShape)
                .background(if (active) color else color.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            if (active) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = time, color = TextPrimaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = label, color = TextSecondaryColor, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }

        SafiriTag(text = status, color = color)
    }
}

@Composable
fun TripHistoryRow(date: String, duration: String, delay: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = date, color = TextPrimaryColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = duration, color = TextSecondaryColor, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        SafiriTag(text = delay, color = color)
    }
}

@Composable
fun ParentSOSAlertCard(viewModel: AppViewModel) {
    val sosActive by viewModel.sosActive.collectAsState()
    if (sosActive) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RedAccent.copy(alpha = 0.2f))
                .border(2.dp, RedAccent, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = "Emergency", tint = RedAccent, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CRITICAL EMERGENCY SOS ACTIVE",
                    color = RedAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Bus KDE 732X driver triggered emergency assistance. School admin dispatch notified and tracking live.",
                color = TextPrimaryColor,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
fun ParentChildrenSyncCard(viewModel: AppViewModel) {
    val students by viewModel.students.collectAsState()
    val currentUserEmail by viewModel.currentUser.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()

    val parentStudentLinks by viewModel.parentStudentLinks.collectAsState()
    val currentChild by viewModel.currentChild.collectAsState()

    // Filter children strictly for this parent - NEVER show other parents' kids!
    val parentChildren = remember(students, currentUserEmail, currentUserName, parentStudentLinks, currentChild) {
        val list = students.filter { st ->
            (currentUserEmail != null && st.parentEmail.equals(currentUserEmail, ignoreCase = true)) ||
            (currentUserName.isNotBlank() && st.parentName.contains(currentUserName, ignoreCase = true)) ||
            parentStudentLinks.any { it.parentId.equals(currentUserEmail, ignoreCase = true) && it.studentId == st.id }
        }
        if (list.isNotEmpty()) {
            list
        } else if (currentChild != null && (currentUserEmail == null || currentChild?.parentEmail.equals(currentUserEmail, ignoreCase = true) || currentUserEmail.equals("parent@safiri.co.ke", ignoreCase = true))) {
            listOf(currentChild!!)
        } else {
            emptyList()
        }
    }

    var showAbsentDialogForStudent by remember { mutableStateOf<Student?>(null) }
    var absentReasonInput by remember { mutableStateOf("Medical appointment") }

    var showNoteDialogForStudent by remember { mutableStateOf<Student?>(null) }
    var noteInput by remember { mutableStateOf("") }

    var showLinkChildDialog by remember { mutableStateOf(false) }

    SafiriCard(testTag = "parent_children_card") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MY CHILDREN & LIVE BOARDING SYNC",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Real-time sync with Driver & Admin",
                    color = TextSecondaryColor,
                    fontSize = 11.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SafiriTag(text = "Connected", color = GreenAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = { showLinkChildDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Link", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Link Child", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (parentChildren.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No child currently linked to your profile",
                    color = TextSecondaryColor,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Click 'Link Child' above to connect your student",
                    color = TextTertiaryColor,
                    fontSize = 11.sp
                )
            }
        } else {
            parentChildren.forEach { child ->
            val statusColor = when (child.status) {
                StudentStatus.BOARDED -> GreenAccent
                StudentStatus.DROPPED_OFF -> AccentBlue
                StudentStatus.ABSENT -> RedAccent
                StudentStatus.NOT_BOARDED -> AmberAccent
            }
            val statusLabel = when (child.status) {
                StudentStatus.BOARDED -> "ON BUS (${child.boardedTime ?: "7:14 AM"})"
                StudentStatus.DROPPED_OFF -> "ARRIVED AT SCHOOL"
                StudentStatus.ABSENT -> "MARKED ABSENT TODAY"
                StudentStatus.NOT_BOARDED -> "WAITING AT STOP"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface3Color)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(child.avatarColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = child.initials, color = child.avatarColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = child.name, color = TextPrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Bus ${child.assignedBusPlate} • ${child.assignedDriverName}", color = TextSecondaryColor, fontSize = 11.sp)
                            Text(text = "Linked Parent: ${child.parentName} (${child.parentEmail})", color = GreenAccent, fontSize = 10.sp)
                        }
                    }
                    SafiriTag(text = statusLabel, color = statusColor)
                }

                if (!child.parentNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Note to driver: \"${child.parentNote}\"",
                        color = AmberAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!child.absentReason.isNullOrBlank() && child.status == StudentStatus.ABSENT) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Absent Reason: ${child.absentReason}",
                        color = RedAccent,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            showNoteDialogForStudent = child
                            noteInput = child.parentNote ?: ""
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.EditNote, contentDescription = "Note", modifier = Modifier.size(14.dp), tint = AccentBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Note to Driver", fontSize = 10.sp, color = AccentBlue)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            showAbsentDialogForStudent = child
                            absentReasonInput = child.absentReason ?: "Medical appointment"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (child.status == StudentStatus.ABSENT) RedAccent.copy(alpha = 0.3f) else RedAccent),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (child.status == StudentStatus.ABSENT) "Edit Absence" else "Mark Absent", fontSize = 10.sp, color = BackgroundColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    }

    // Dialog for Marking Absent
    showAbsentDialogForStudent?.let { child ->
        AlertDialog(
            onDismissRequest = { showAbsentDialogForStudent = null },
            title = { Text("Mark ${child.name} Absent Today", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimaryColor) },
            text = {
                Column {
                    Text("This will immediately notify Driver ${child.assignedDriverName} and School Admin.", fontSize = 12.sp, color = TextSecondaryColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = absentReasonInput,
                        onValueChange = { absentReasonInput = it },
                        label = { Text("Reason for absence") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setStudentAbsentByParent(child.id, absentReasonInput)
                        showAbsentDialogForStudent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) {
                    Text("Confirm Absence", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbsentDialogForStudent = null }) {
                    Text("Cancel", color = TextSecondaryColor)
                }
            },
            containerColor = SurfaceColor
        )
    }

    // Dialog for Parent Note
    showNoteDialogForStudent?.let { child ->
        AlertDialog(
            onDismissRequest = { showNoteDialogForStudent = null },
            title = { Text("Note for Driver ${child.assignedDriverName}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimaryColor) },
            text = {
                Column {
                    Text("Send a quick note regarding ${child.name} for Bus ${child.assignedBusPlate}.", fontSize = 12.sp, color = TextSecondaryColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("e.g. Will arrive 3 mins late at Kilimani Stop") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addParentNoteForDriver(child.id, noteInput)
                        showNoteDialogForStudent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Send Note", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialogForStudent = null }) {
                    Text("Cancel", color = TextSecondaryColor)
                }
            },
            containerColor = SurfaceColor
        )
    }

    // Dialog for Linking a Child to Parent Profile
    if (showLinkChildDialog) {
        AlertDialog(
            onDismissRequest = { showLinkChildDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Link Child", tint = AccentBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link Child to Your Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimaryColor)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Select any student registered in the school system to link to your parent account ($currentUserName):",
                        fontSize = 12.sp,
                        color = TextSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    students.forEach { st ->
                        val isAlreadyLinked = st.parentEmail.equals(currentUserEmail ?: "", ignoreCase = true) ||
                                st.parentName.contains(currentUserName, ignoreCase = true) ||
                                parentChildren.any { it.id == st.id }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Surface3Color)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(st.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimaryColor)
                                Text("Bus: ${st.assignedBusPlate} • Stop: ${st.pickupStop}", fontSize = 10.sp, color = TextSecondaryColor)
                                Text("Current Parent: ${st.parentName}", fontSize = 10.sp, color = TextTertiaryColor)
                            }
                            if (isAlreadyLinked) {
                                SafiriTag(text = "Linked ✓", color = GreenAccent)
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.linkChildToCurrentParent(st.id)
                                        showLinkChildDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Link Child", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLinkChildDialog = false }) {
                    Text("Close", color = TextSecondaryColor)
                }
            },
            containerColor = SurfaceColor
        )
    }
}

@Composable
fun ParentDriverMessageCard(viewModel: AppViewModel) {
    val messages by viewModel.interportalMessages.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    var messageText by remember { mutableStateOf("") }

    SafiriCard(testTag = "parent_driver_messages") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Forum, contentDescription = "Messages", tint = AccentBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DRIVER & ADMIN MESSAGES",
                    color = TextPrimaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            SafiriTag(text = "${messages.size} Messages", color = AccentBlue)
        }

        Spacer(modifier = Modifier.height(10.dp))

        messages.take(4).forEach { msg ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface3Color)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = msg.senderName, color = TextPrimaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = msg.timestamp, color = TextTertiaryColor, fontSize = 9.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = msg.content, color = TextSecondaryColor, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Message driver or admin...", fontSize = 11.sp) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendInterportalMessage(
                            senderName = "$currentUserName (Parent)",
                            senderRole = "PARENT",
                            recipientRole = "DRIVER",
                            targetBusPlate = "KDE 732X",
                            content = messageText
                        )
                        messageText = ""
                    }
                },
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ChildSpecificGreetingCard(
    child: Student?,
    parentName: String,
    etaMinutes: Int,
    viewModel: AppViewModel
) {
    val childName = child?.name ?: "your child"
    val parentFirstName = parentName.split(" ").firstOrNull() ?: parentName
    val busPlate = child?.assignedBusPlate ?: "KDE 732X"
    val grade = child?.classGrade ?: "Grade 3"
    val school = child?.schoolName ?: "St. Austin's Academy"
    val pickupStop = child?.pickupStop ?: "Kawangware Stop"

    SafiriCard(testTag = "child_greeting_card") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background((child?.avatarColor ?: AccentBlue).copy(alpha = 0.2f))
                        .border(2.dp, child?.avatarColor ?: AccentBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = child?.initials ?: "LO",
                        color = child?.avatarColor ?: AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Good morning, $parentFirstName",
                        color = TextSecondaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$childName is on the way",
                        color = TextPrimaryColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = { viewModel.signOut() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Surface3Color)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ExitToApp,
                    contentDescription = "Sign Out",
                    tint = RedAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Student & Bus Info Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface3Color)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Bus $busPlate",
                    color = AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface3Color)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$grade • $school",
                    color = TextSecondaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Boarding Status Badge
        val (statusText, statusColor) = when (child?.status) {
            StudentStatus.BOARDED -> Pair("Boarded at $pickupStop • ${child.boardedTime ?: "07:12 AM"}", GreenAccent)
            StudentStatus.DROPPED_OFF -> Pair("Arrived safely at $school", AccentBlue)
            StudentStatus.ABSENT -> Pair("Marked ABSENT today", RedAccent)
            else -> Pair("Waiting for Bus at $pickupStop • Pickup Soon", AmberAccent)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(statusColor.copy(alpha = 0.15f))
                .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (child?.status == StudentStatus.BOARDED || child?.status == StudentStatus.DROPPED_OFF) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
                contentDescription = "Status",
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LIVE BOARDING STATUS",
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = statusText,
                    color = TextPrimaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ChildJourneyTimelineCard(
    child: Student?,
    events: List<JourneyEvent>,
    etaMinutes: Int
) {
    val childName = child?.name ?: "Child"
    val school = child?.schoolName ?: "St. Austin's Academy"
    val stop = child?.pickupStop ?: "Kawangware Stop"
    val isBoarded = child?.status == StudentStatus.BOARDED
    val isArrived = child?.status == StudentStatus.DROPPED_OFF
    val boardedTime = child?.boardedTime ?: "07:12 AM"

    SafiriCard(testTag = "journey_timeline_card") {
        Text(
            text = "${childName.uppercase()}'S JOURNEY TIMELINE",
            color = TextTertiaryColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Step 1: Boarded
        TimelineRow(
            title = "1. Boarded Bus",
            subtitle = if (isBoarded || isArrived) "Boarded at $stop • $boardedTime" else "Waiting at $stop",
            isCompleted = isBoarded || isArrived,
            isActive = child?.status == StudentStatus.NOT_BOARDED,
            timeStr = if (isBoarded || isArrived) boardedTime else "Scheduled 07:10 AM"
        )

        // Step 2: En Route
        TimelineRow(
            title = "2. En Route to School",
            subtitle = if (isArrived) "Completed route on Ngong Road" else if (isBoarded) "In transit • $etaMinutes mins to $school" else "Pending boarding",
            isCompleted = isArrived,
            isActive = isBoarded,
            timeStr = if (isBoarded) "In Transit" else "Pending"
        )

        // Step 3: Arrived at School
        TimelineRow(
            title = "3. Arrived at $school",
            subtitle = if (isArrived) "Safely checked in at school gate" else "ETA ~$etaMinutes mins",
            isCompleted = isArrived,
            isActive = false,
            timeStr = if (isArrived) "07:35 AM" else "Est. 07:35 AM",
            isLast = true
        )
    }
}

@Composable
fun TimelineRow(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isActive: Boolean,
    timeStr: String,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> GreenAccent
                            isActive -> AccentBlue
                            else -> Surface3Color
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Done",
                        tint = BackgroundColor,
                        modifier = Modifier.size(14.dp)
                    )
                } else if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BackgroundColor)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(if (isCompleted) GreenAccent else Surface3Color)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = if (isCompleted || isActive) TextPrimaryColor else TextSecondaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeStr,
                    color = if (isCompleted) GreenAccent else if (isActive) AccentBlue else TextTertiaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = subtitle,
                color = TextSecondaryColor,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun ChildActivityFeedCard(
    child: Student?,
    events: List<JourneyEvent>
) {
    val childName = child?.name ?: "Child"
    val studentEvents = events.filter { child == null || it.studentId == child.id }

    SafiriCard(testTag = "child_activity_feed_card") {
        Text(
            text = "${childName.uppercase()}'S ACTIVITY TODAY",
            color = TextTertiaryColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (studentEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No activity yet today",
                    color = TextTertiaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            studentEvents.take(5).forEach { ev ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Surface3Color)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (icon, tint) = when (ev.eventType) {
                        JourneyEventType.BOARDED -> Pair(Icons.Filled.DirectionsBus, GreenAccent)
                        JourneyEventType.ARRIVED_SCHOOL -> Pair(Icons.Filled.School, AccentBlue)
                        JourneyEventType.ALIGHTED -> Pair(Icons.Filled.Home, PurpleAccent)
                        else -> Pair(Icons.Filled.AltRoute, AmberAccent)
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = "Event", tint = tint, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ev.notes ?: "Activity recorded at ${ev.stopName}",
                            color = TextPrimaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${ev.stopName} • Bus ${ev.busPlate}",
                            color = TextSecondaryColor,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = ev.timestamp,
                            color = TextSecondaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        SafiriTag(
                            text = when (ev.eventType) {
                                JourneyEventType.BOARDED -> "Boarded"
                                JourneyEventType.ARRIVED_SCHOOL -> "Arrived"
                                JourneyEventType.ALIGHTED -> "Alighted"
                                else -> "En Route"
                            },
                            color = tint
                        )
                    }
                }
            }
        }
    }
}
