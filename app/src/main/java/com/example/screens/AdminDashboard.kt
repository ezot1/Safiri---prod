package com.example.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.*
import com.example.models.AlertSeverity
import com.example.models.FleetBusItem
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel

@Composable
fun AdminDashboard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Fleet, 1: Live Map, 2: Drivers, 3: Alerts, 4: Settings

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
                    icon = { Icon(Icons.Filled.DirectionsBus, contentDescription = "Fleet") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleAccent,
                        unselectedIconColor = TextTertiaryColor,
                        indicatorColor = Surface3Color
                    ),
                    modifier = Modifier.testTag("admin_tab_fleet")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Filled.Map, contentDescription = "Live Map") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleAccent,
                        unselectedIconColor = TextTertiaryColor,
                        indicatorColor = Surface3Color
                    ),
                    modifier = Modifier.testTag("admin_tab_mapview")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Filled.Badge, contentDescription = "Drivers") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleAccent,
                        unselectedIconColor = TextTertiaryColor,
                        indicatorColor = Surface3Color
                    ),
                    modifier = Modifier.testTag("admin_tab_drivers")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Filled.Campaign, contentDescription = "Alerts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleAccent,
                        unselectedIconColor = TextTertiaryColor,
                        indicatorColor = Surface3Color
                    ),
                    modifier = Modifier.testTag("admin_tab_alerts")
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleAccent,
                        unselectedIconColor = TextTertiaryColor,
                        indicatorColor = Surface3Color
                    ),
                    modifier = Modifier.testTag("admin_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (activeTab) {
                0 -> AdminFleetTab(viewModel)
                1 -> AdminMapViewTab(viewModel)
                2 -> AdminDriversTab(viewModel)
                3 -> AdminAlertsTab(viewModel)
                4 -> AdminSettingsTab(viewModel)
            }
        }
    }
}

// ==========================================
// 1. FLEET STATUS TAB (AdminFleetTab)
// ==========================================
@Composable
fun AdminFleetTab(viewModel: AppViewModel) {
    val fleetBuses by viewModel.adminFleetBuses.collectAsState()
    val boardedCount by viewModel.boardedCount.collectAsState()

    ScrollView(
        modifier = Modifier.fillMaxSize(),
        contentContainerStyle = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FLEET DASHBOARD",
                color = TextPrimaryColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.signOut() }) {
                Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = RedAccent)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Four summary metric tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(label = "ACTIVE BUSES", value = "4 / 5", modifier = Modifier.weight(1f))
            // Updates dynamically as boarding happens!
            MetricTile(label = "STUDENTS ABOARD", value = "${59 + boardedCount}", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(label = "DELAYED BUSES", value = "1", modifier = Modifier.weight(1f))
            MetricTile(label = "SAFETY REPORTS", value = "0", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fleet list card
        SafiriCard(testTag = "fleet_bus_status_card") {
            Text(
                text = "SCHOOL SHUTTLE FLEET PROGRESS",
                color = TextTertiaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            fleetBuses.forEach { bus ->
                val isActive = bus.plate != "KDE 991Z"
                val plateColor = if (isActive) AccentBlue else TextTertiaryColor

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SafiriTag(text = bus.plate, color = plateColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = bus.driverName,
                                color = TextPrimaryColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isActive) {
                            OccupancyBar(count = bus.studentCount, capacity = bus.capacity)
                        } else {
                            Text(text = "Out of Service • Roster Pending", color = TextTertiaryColor, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    SafiriTag(text = bus.status, color = bus.statusColor)
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
// 2. LIVE MAP MULTI-BUS TRACKING TAB
// ==========================================
@Composable
fun AdminMapViewTab(viewModel: AppViewModel) {
    val fleetBuses by viewModel.adminFleetBuses.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "LIVE FLEET RADAR",
                color = TextPrimaryColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Simultaneous GPS signals tracked through Nairobi streets",
                color = TextSecondaryColor,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Multiple buses drawn on Map Canvas
        val busOffsets = fleetBuses.filter { it.plate != "KDE 991Z" }.map {
            Pair(it.latOffset, it.lngOffset)
        }

        MapHero(
            progressPct = 0f,
            isMultipleBuses = true,
            multipleBusOffsets = busOffsets,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Position logs and fleet stats (Scrollable Area)
        ScrollView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentContainerStyle = Modifier.padding(horizontal = 16.dp)
        ) {
            SafiriCard(testTag = "bus_positions_list_card") {
                Text(
                    text = "RADAR TRACKING LOGS",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                fleetBuses.filter { it.plate != "KDE 991Z" }.forEachIndexed { idx, bus ->
                    val colors = listOf(AccentBlue, AmberAccent, GreenAccent, PurpleAccent)
                    val dotColor = colors[idx % colors.size]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = bus.plate, color = TextPrimaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Driver: ${bus.driverName}", color = TextSecondaryColor, fontSize = 11.sp)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            SafiriTag(text = bus.status, color = bus.statusColor)
                            Text(text = "Speed: ${bus.speed}km/h", color = TextTertiaryColor, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    HorizontalDivider(color = BorderColor)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fleet stats card
            SafiriCard(testTag = "fleet_stats_card") {
                Text(
                    text = "DIAGNOSTIC TELEMETRY",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                SummaryLabelRow("Total Mileage Today", "182 km")
                SummaryLabelRow("Average Speed Across Fleet", "41.5 km/h")
                SummaryLabelRow("GPS Carrier Quality", "Excellent (100%)")
                SummaryLabelRow("Last Satellite Update", "Just Now")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// 3. DRIVERS ROSTER & RATINGS TAB
// ==========================================
@Composable
fun AdminDriversTab(viewModel: AppViewModel) {
    val drivers by viewModel.driversList.collectAsState()

    ScrollView(
        modifier = Modifier.fillMaxSize(),
        contentContainerStyle = Modifier.padding(16.dp)
    ) {
        Text(
            text = "DRIVER ROSTERS",
            color = TextPrimaryColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Monitor performance, ratings, and active licenses",
            color = TextSecondaryColor,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Driver Roster card
        SafiriCard(testTag = "drivers_roster_card") {
            Text(
                text = "Nairobi Shuttle Captains",
                color = TextTertiaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            drivers.forEach { driver ->
                val progressColor = if (driver.performanceProgress > 0.9f) GreenAccent else AmberAccent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(driver.avatarColor.copy(alpha = 0.15f))
                                .border(1.dp, driver.avatarColor.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = driver.initials,
                                color = driver.avatarColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = driver.name, color = TextPrimaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Lane: ${driver.busPlate} • Exp: ${driver.yearsExperience} yrs", color = TextSecondaryColor, fontSize = 11.sp)
                        }
                    }

                    // Progress bar representing driver score
                    Column(
                        modifier = Modifier.weight(0.8f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Score: ${(driver.performanceProgress * 100).toInt()}%",
                                color = progressColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { driver.performanceProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = progressColor,
                            trackColor = Surface3Color
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    SafiriTag(text = "${driver.onTimePct}% on-time", color = progressColor)
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekday statistics chart (Mon-Fri)
        SafiriCard(testTag = "admin_on_time_chart") {
            Text(
                text = "FLEET AVERAGE ON-TIME CHART",
                color = TextTertiaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                ChartBar(day = "Mon", rate = 75, highlighted = false)
                ChartBar(day = "Tue", rate = 88, highlighted = false)
                ChartBar(day = "Wed", rate = 92, highlighted = true)
                ChartBar(day = "Thu", rate = 80, highlighted = false)
                ChartBar(day = "Fri", rate = 87, highlighted = false)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
// 4. BROADCAST SERVICE ALERTS TAB (AdminAlertsTab)
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdminAlertsTab(viewModel: AppViewModel) {
    val alerts by viewModel.activeAlerts.collectAsState()

    var alertTitle by remember { mutableStateOf("") }
    var alertDetail by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf(AlertSeverity.INFO) }

    ScrollView(
        modifier = Modifier.fillMaxSize(),
        contentContainerStyle = Modifier.padding(16.dp)
    ) {
        Text(
            text = "BROADCAST CENTRE",
            color = TextPrimaryColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Post critical traffic, weather, or scheduling alerts to parents",
            color = TextSecondaryColor,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Post Alert Form
        SafiriCard(testTag = "broadcast_form_card") {
            Text(
                text = "POST EMERGENCY BROADCAST",
                color = TextTertiaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Title input
            TextField(
                value = alertTitle,
                onValueChange = { alertTitle = it },
                placeholder = { Text("Alert Title (e.g. Kawangware Floods)", color = TextTertiaryColor) },
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
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("admin_alert_title"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Detail input
            TextField(
                value = alertDetail,
                onValueChange = { alertDetail = it },
                placeholder = { Text("Details and detour routes...", color = TextTertiaryColor) },
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
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("admin_alert_detail")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Severity Level selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SeverityBtn(
                    label = "Danger",
                    severity = AlertSeverity.DANGER,
                    selected = selectedSeverity == AlertSeverity.DANGER,
                    color = RedAccent,
                    onClick = { selectedSeverity = AlertSeverity.DANGER }
                )
                SeverityBtn(
                    label = "Warning",
                    severity = AlertSeverity.WARNING,
                    selected = selectedSeverity == AlertSeverity.WARNING,
                    color = AmberAccent,
                    onClick = { selectedSeverity = AlertSeverity.WARNING }
                )
                SeverityBtn(
                    label = "Info",
                    severity = AlertSeverity.INFO,
                    selected = selectedSeverity == AlertSeverity.INFO,
                    color = AccentBlue,
                    onClick = { selectedSeverity = AlertSeverity.INFO }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.postAlert(alertTitle, alertDetail, selectedSeverity)
                    alertTitle = ""
                    alertDetail = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("post_alert_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Post to all parents", color = BackgroundColor, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active alerts
        SafiriCard(testTag = "admin_active_alerts_list") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.ListAlt, contentDescription = "Alerts", tint = PurpleAccent)
                Text(
                    text = "ACTIVE ALERTS (LONG PRESS TO DISMISS)",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (alerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active service alerts.", color = TextSecondaryColor, fontSize = 13.sp)
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
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onLongClick = { viewModel.dismissAlert(alert.id) },
                                onClick = {}
                            )
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

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun RowScope.SeverityBtn(
    label: String,
    severity: AlertSeverity,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val bg = if (selected) color.copy(alpha = 0.2f) else SurfaceColor
    val borderCol = if (selected) color else BorderColor

    Box(
        modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = if (selected) color else TextSecondaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// 5. SCHOOL SETTINGS & CONFIGS TAB
// ==========================================
@Composable
fun AdminSettingsTab(viewModel: AppViewModel) {
    val t1 by viewModel.toggleAdminEmergency.collectAsState()
    val t2 by viewModel.toggleAdminGeofence.collectAsState()
    val t3 by viewModel.toggleAdminDailySummary.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showBusesDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }

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
                        colors = listOf(PurpleAccent, AccentBlue)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "SK", color = BackgroundColor, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Sarah Kimani", color = TextPrimaryColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = "admin@safiri.co.ke", color = TextSecondaryColor, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        SafiriTag(text = "School Administrator", color = PurpleAccent)

        Spacer(modifier = Modifier.height(24.dp))

        // Settings rows card
        SafiriCard(testTag = "school_settings_card") {
            Text(
                text = "SCHOOL TRANSIT PREFERENCES",
                color = TextTertiaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            SettingsRow(
                icon = Icons.Filled.Business,
                label = "School profile",
                subLabel = "St. Mary's Academy, Nairobi",
                onClick = { showProfileDialog = true }
            )
            HorizontalDivider(color = BorderColor)
            SettingsRow(
                icon = Icons.Filled.DirectionsBus,
                label = "Manage buses",
                subLabel = "Configure fleet plates, capacities",
                onClick = { showBusesDialog = true }
            )
            HorizontalDivider(color = BorderColor)
            SettingsRow(
                icon = Icons.Filled.Payment,
                label = "Subscription",
                subLabel = "Plan active: Safiri Enterprise",
                onClick = { showSubscriptionDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Admin notifications
        SafiriCard(testTag = "admin_notifications_card") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = "Notification Configuration", tint = PurpleAccent)
                Text(
                    text = "ADMIN NOTIFICATION SETTINGS",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            ToggleRow(label = "Emergency Broadcast", subLabel = "Instant notification when driver pushes SOS", checked = t1, onCheckedChange = { viewModel.toggleAdminEmergency.value = it })
            HorizontalDivider(color = BorderColor)
            ToggleRow(label = "Geofence breach", subLabel = "Ping if a bus shifts 500m off-route", checked = t2, onCheckedChange = { viewModel.toggleAdminGeofence.value = it })
            HorizontalDivider(color = BorderColor)
            ToggleRow(label = "Daily Summary Report", subLabel = "Receive on-time records every 6 PM", checked = t3, onCheckedChange = { viewModel.toggleAdminDailySummary.value = it })
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { viewModel.signOut() },
            colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("admin_sign_out"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign Out", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // 1. School Profile Edit Dialog
    if (showProfileDialog) {
        var schoolName by remember { mutableStateOf("St. Mary's Academy") }
        var location by remember { mutableStateOf("Nairobi, Kenya") }
        var contactEmail by remember { mutableStateOf("admin@safiri.co.ke") }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            containerColor = SurfaceColor,
            title = {
                Text(
                    text = "EDIT SCHOOL PROFILE",
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
                    Text("School Name", color = TextSecondaryColor, fontSize = 12.sp)
                    TextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
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
                    Text("Location", color = TextSecondaryColor, fontSize = 12.sp)
                    TextField(
                        value = location,
                        onValueChange = { location = it },
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
                    Text("Contact Email", color = TextSecondaryColor, fontSize = 12.sp)
                    TextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it },
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
                    onClick = { showProfileDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    Text("Save Changes", color = BackgroundColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Cancel", color = TextSecondaryColor)
                }
            }
        )
    }

    // 2. Manage Buses Dialog
    if (showBusesDialog) {
        AlertDialog(
            onDismissRequest = { showBusesDialog = false },
            containerColor = SurfaceColor,
            title = {
                Text(
                    text = "MANAGE FLEET BUSES",
                    color = TextPrimaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Toggle Bus Operational Status:", color = TextSecondaryColor, fontSize = 12.sp)

                    val buses = listOf(
                        Triple("KDE 732X", "Erick Mwangi", true),
                        Triple("KDE 119A", "John Kiprop", true),
                        Triple("KDE 540R", "Grace Wambui", true),
                        Triple("KDE 902Y", "Peter Koech", false)
                    )

                    buses.forEach { (plate, driver, active) ->
                        var isChecked by remember { mutableStateOf(active) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Surface3Color)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(plate, color = TextPrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Driver: $driver", color = TextSecondaryColor, fontSize = 11.sp)
                            }
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { isChecked = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PurpleAccent,
                                    checkedTrackColor = PurpleAccent.copy(alpha = 0.4f),
                                    uncheckedThumbColor = TextTertiaryColor,
                                    uncheckedTrackColor = SurfaceColor
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBusesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    Text("Close", color = BackgroundColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 3. Subscription Dialog
    if (showSubscriptionDialog) {
        AlertDialog(
            onDismissRequest = { showSubscriptionDialog = false },
            containerColor = SurfaceColor,
            title = {
                Text(
                    text = "SAFIRI ENTERPRISE PLAN",
                    color = TextPrimaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PurpleAccent.copy(alpha = 0.15f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = PurpleAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Active Subscription", color = TextPrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Plan: Safiri Enterprise Tier", color = PurpleAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Text("Billing Period", color = TextTertiaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Monthly renewal of $250.00 USD on the 11th of every month.", color = TextPrimaryColor, fontSize = 13.sp)

                    HorizontalDivider(color = BorderColor)

                    Text("Features Included:", color = TextTertiaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    val features = listOf(
                        "Up to 25 Active Shuttles",
                        "Real-time SMS notification fail-safe",
                        "Full Admin Telemetry suite",
                        "Dedicated 24/7 Account Manager"
                    )
                    features.forEach { feat ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, contentDescription = "Check", tint = GreenAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(feat, color = TextSecondaryColor, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSubscriptionDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    Text("Done", color = BackgroundColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subLabel: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = TextSecondaryColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, color = TextPrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subLabel, color = TextSecondaryColor, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "Chevron", tint = TextTertiaryColor)
    }
}
