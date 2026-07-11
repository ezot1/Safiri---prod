package com.example.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.*
import com.example.models.AlertSeverity
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ParentDashboard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Home, 1: Saved, 2: Plan, 3: Alerts, 4: Me

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundColor,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceColor,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        unselectedIconColor = TextTertiaryColor,
                        indicatorColor = Surface3Color
                    ),
                    modifier = Modifier.testTag("parent_tab_home")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Filled.Star, contentDescription = "Saved") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        unselectedIconColor = TextTertiaryColor,
                        indicatorColor = Surface3Color
                    ),
                    modifier = Modifier.testTag("parent_tab_saved")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Filled.AltRoute, contentDescription = "Plan") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        unselectedIconColor = TextTertiaryColor,
                        indicatorColor = Surface3Color
                    ),
                    modifier = Modifier.testTag("parent_tab_planner")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Filled.Notifications, contentDescription = "Alerts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        unselectedIconColor = TextTertiaryColor,
                        indicatorColor = Surface3Color
                    ),
                    modifier = Modifier.testTag("parent_tab_alerts")
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Me") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        unselectedIconColor = TextTertiaryColor,
                        indicatorColor = Surface3Color
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Sticky Map Hero at the top
        MapHero(
            progressPct = progress,
            busPlate = "KDE 732X",
            statusText = "On Time",
            etaMinutes = etaMinutes,
            stops = stops,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        // The rest of the content scrolls independently
        ScrollView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentContainerStyle = Modifier.padding(16.dp)
        ) {
            // Drag handle bar style representation
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Surface3Color)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = " min",
                            color = TextSecondaryColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SafiriTag(text = "On Time", color = GreenAccent)
                        Spacer(modifier = Modifier.width(8.dp))
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
                }
                Text(
                    text = "Bus arriving at Kilimani stop • Head there now",
                    color = TextPrimaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Three metric tiles in a row
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

                // Route completion progress bar
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

            // Occupancy Section card
            SafiriCard(testTag = "occupancy_card") {
                OccupancyBar(count = 16, capacity = 22)
            }

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

            Spacer(modifier = Modifier.height(14.dp))

            // Recent trips card
            SafiriCard(testTag = "recent_trips_card") {
                Text(
                    text = "RECENT TRIPS HISTORY",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                TripHistoryRow(date = "Jul 10, 2026", duration = "03:48 PM - 04:22 PM", delay = "On time", color = GreenAccent)
                TripHistoryRow(date = "Jul 09, 2026", duration = "03:46 PM - 04:29 PM", delay = "+8 min", color = AmberAccent)
                TripHistoryRow(date = "Jul 08, 2026", duration = "03:45 PM - 04:21 PM", delay = "On time", color = GreenAccent)
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
            Text(text = parentPhone, color = TextSecondaryColor, fontSize = 12.sp)
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

        Spacer(modifier = Modifier.height(28.dp))

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
