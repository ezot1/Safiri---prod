package com.example.screens

import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.*
import com.example.models.AlertSeverity
import com.example.models.FleetBusItem
import com.example.models.InterportalMessage
import com.example.models.Student
import com.example.models.StudentStatus
import com.example.models.SubscriptionStatus
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel

@Composable
fun AdminDashboard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Fleet, 1: Live Map, 2: Drivers, 3: Alerts, 4: Settings
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminSidebarContent(
                viewModel = viewModel,
                onClose = { scope.launch { drawerState.close() } }
            )
        },
        modifier = modifier
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = BackgroundColor,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceColor)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("admin_sidebar_toggle_btn")
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open Sidebar", tint = PurpleAccent)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SAFIRI ADM PORTAL",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryColor
                        )
                    }
                    IconButton(onClick = { viewModel.signOut() }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = RedAccent)
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
                    0 -> AdminFleetTab(viewModel, onOpenSidebar = { scope.launch { drawerState.open() } })
                    1 -> AdminMapViewTab(viewModel, onOpenSidebar = { scope.launch { drawerState.open() } })
                    2 -> AdminDriversTab(viewModel, onOpenSidebar = { scope.launch { drawerState.open() } })
                    3 -> AdminAlertsTab(viewModel, onOpenSidebar = { scope.launch { drawerState.open() } })
                    4 -> AdminSettingsTab(viewModel, onOpenSidebar = { scope.launch { drawerState.open() } })
                }
            }
        }
    }
}

// ==========================================
// 1. FLEET STATUS TAB (AdminFleetTab)
// ==========================================
@Composable
fun AdminFleetTab(
    viewModel: AppViewModel,
    onOpenSidebar: () -> Unit = {}
) {
    val fleetBuses by viewModel.adminFleetBuses.collectAsState()
    val boardedCount by viewModel.boardedCount.collectAsState()
    val subStatus by viewModel.subscriptionStatus.collectAsState()
    val subAmount by viewModel.agreedMonthlyAmount.collectAsState()

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
            IconButton(
                onClick = onOpenSidebar,
                modifier = Modifier.testTag("admin_fleet_sidebar_toggle")
            ) {
                Icon(Icons.Filled.DirectionsBus, contentDescription = "Active Routes Status", tint = PurpleAccent)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live SOS Alert Resolution Banner
        AdminSOSResolutionBanner(viewModel)

        if (subStatus == SubscriptionStatus.PAYMENT_DUE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmberAccent.copy(alpha = 0.15f))
                    .border(1.dp, AmberAccent, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = "Due", tint = AmberAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Subscription Fee Due Soon", color = TextPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Agreed Fee: $subAmount", color = TextSecondaryColor, fontSize = 10.sp)
                    }
                }
                Button(
                    onClick = { viewModel.paySubscriptionInvoice("", subAmount) },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Pay Now", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BackgroundColor)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

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

        Spacer(modifier = Modifier.height(16.dp))

        // Interconnected Students Roster
        AdminStudentInterconnectionCard(viewModel)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
// 2. LIVE MAP MULTI-BUS TRACKING TAB
// ==========================================
@Composable
fun AdminMapViewTab(
    viewModel: AppViewModel,
    onOpenSidebar: () -> Unit = {}
) {
    val fleetBuses by viewModel.adminFleetBuses.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
            IconButton(
                onClick = onOpenSidebar,
                modifier = Modifier.testTag("admin_mapview_sidebar_toggle")
            ) {
                Icon(Icons.Filled.DirectionsBus, contentDescription = "Active Routes Status", tint = PurpleAccent)
            }
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
fun AdminDriversTab(
    viewModel: AppViewModel,
    onOpenSidebar: () -> Unit = {}
) {
    val drivers by viewModel.driversList.collectAsState()

    ScrollView(
        modifier = Modifier.fillMaxSize(),
        contentContainerStyle = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
            }
            IconButton(
                onClick = onOpenSidebar,
                modifier = Modifier.testTag("admin_drivers_sidebar_toggle")
            ) {
                Icon(Icons.Filled.DirectionsBus, contentDescription = "Active Routes Status", tint = PurpleAccent)
            }
        }

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
fun AdminAlertsTab(
    viewModel: AppViewModel,
    onOpenSidebar: () -> Unit = {}
) {
    val alerts by viewModel.activeAlerts.collectAsState()

    var alertTitle by remember { mutableStateOf("") }
    var alertDetail by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf(AlertSeverity.INFO) }

    ScrollView(
        modifier = Modifier.fillMaxSize(),
        contentContainerStyle = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
            }
            IconButton(
                onClick = onOpenSidebar,
                modifier = Modifier.testTag("admin_alerts_sidebar_toggle")
            ) {
                Icon(Icons.Filled.DirectionsBus, contentDescription = "Active Routes Status", tint = PurpleAccent)
            }
        }

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
                    if (alertTitle.isNotBlank()) {
                        viewModel.postAlert(alertTitle, alertDetail, selectedSeverity)
                        viewModel.sendInterportalMessage(
                            senderName = "School Administration",
                            senderRole = "ADMIN",
                            recipientRole = "ALL",
                            targetBusPlate = "ALL",
                            content = "ADMIN BROADCAST: $alertTitle - $alertDetail"
                        )
                        alertTitle = ""
                        alertDetail = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("post_alert_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Post to all parents & drivers", color = BackgroundColor, fontWeight = FontWeight.Bold)
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
fun AdminSettingsTab(
    viewModel: AppViewModel,
    onOpenSidebar: () -> Unit = {}
) {
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
                label = "Subscription details",
                subLabel = "Manage billing, paybill & invoices",
                onClick = { showSubscriptionDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Full Interactive Admin Subscription Card
        AdminSubscriptionManagementCard(viewModel = viewModel)

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

// ==========================================
// SIDEBAR COMPONENT (AdminSidebarContent)
// ==========================================
@Composable
fun AdminSidebarContent(
    viewModel: AppViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fleetBuses by viewModel.adminFleetBuses.collectAsState()
    
    // Filter active buses (non-inactive, i.e., those assigned to a driver)
    val activeBuses = fleetBuses.filter { it.plate != "KDE 991Z" }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(SurfaceColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Right-aligned side border line
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(1.dp)
                .background(BorderColor)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SAFIRI RADAR",
                        color = PurpleAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Active Routes Monitor",
                        color = TextSecondaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("admin_sidebar_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Sidebar",
                        tint = TextSecondaryColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(16.dp))

            // Active Routes Title with live pulsing indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .alpha(pulseAlpha)
                        .background(GreenAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LIVE ACTIVE SHUTTLES",
                    color = TextTertiaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${activeBuses.size})",
                    color = TextSecondaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeBuses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsBus,
                            contentDescription = "No Active Buses",
                            tint = TextTertiaryColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Shuttles Running",
                            color = TextSecondaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Awaiting driver roster dispatch",
                            color = TextTertiaryColor,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    activeBuses.forEach { bus ->
                        ActiveRouteSidebarItem(bus = bus, pulseAlpha = pulseAlpha)
                    }
                }
            }
            
            // Footer Info
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "System Status",
                    color = TextTertiaryColor,
                    fontSize = 11.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(GreenAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "All Services Nominal",
                        color = GreenAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveRouteSidebarItem(
    bus: FleetBusItem,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    // Map status strictly to "On-time" or "Delayed"
    val displayStatus = when (bus.status.lowercase()) {
        "on time", "on-time" -> "On-time"
        "delayed" -> "Delayed"
        else -> "On-time" // default active states like "Active" map to "On-time"
    }
    
    val statusColor = if (displayStatus == "On-time") GreenAccent else RedAccent
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2Color)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("sidebar_route_item_${bus.plate.replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.DirectionsBus,
                    contentDescription = "Bus Icon",
                    tint = AccentBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = bus.plate,
                    color = TextPrimaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Status badge with pulsing dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .alpha(pulseAlpha)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = displayStatus,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Captain: ${bus.driverName}",
            color = TextSecondaryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${bus.studentCount}/${bus.capacity} Students Aboard",
                color = TextTertiaryColor,
                fontSize = 10.sp
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = "Speed",
                    tint = TextTertiaryColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${bus.speed} km/h",
                    color = TextSecondaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AdminSOSResolutionBanner(viewModel: AppViewModel) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Warning, contentDescription = "SOS", tint = RedAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("CRITICAL DRIVER SOS TRIGGERED", color = RedAccent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("Driver Erick Mwangi (KDE 732X) dispatched emergency signal", color = TextPrimaryColor, fontSize = 11.sp)
                    }
                }
                Button(
                    onClick = { viewModel.dismissSOS() },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Resolve SOS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
fun AdminStudentInterconnectionCard(viewModel: AppViewModel) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    var showAddStudentDialog by remember { mutableStateOf(false) }

    var editingParentStudent by remember { mutableStateOf<Student?>(null) }
    var editParentNameInput by remember { mutableStateOf("") }
    var editParentEmailInput by remember { mutableStateOf("") }
    var editParentPhoneInput by remember { mutableStateOf("") }

    var newStudentName by remember { mutableStateOf("") }
    var newParentName by remember { mutableStateOf("") }
    var newParentPhone by remember { mutableStateOf("") }
    var newBusPlate by remember { mutableStateOf("KDE 732X") }
    var newStop by remember { mutableStateOf("Kilimani Bus Bay") }

    SafiriCard(testTag = "admin_student_roster_card") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "INTERCONNECTED STUDENT & PARENT DIRECTORY",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Synced across Parent, Driver, & Admin portals",
                    color = TextSecondaryColor,
                    fontSize = 11.sp
                )
            }
            Button(
                onClick = { showAddStudentDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Student", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        students.forEach { student ->
            val statusColor = when (student.status) {
                StudentStatus.BOARDED -> GreenAccent
                StudentStatus.DROPPED_OFF -> AccentBlue
                StudentStatus.ABSENT -> RedAccent
                StudentStatus.NOT_BOARDED -> AmberAccent
            }
            val statusLabel = when (student.status) {
                StudentStatus.BOARDED -> "BOARDED"
                StudentStatus.DROPPED_OFF -> "ARRIVED"
                StudentStatus.ABSENT -> "ABSENT"
                StudentStatus.NOT_BOARDED -> "NOT BOARDED"
            }

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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(student.avatarColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = student.initials, color = student.avatarColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = student.name, color = TextPrimaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    val cleanPhone = student.parentPhone.replace(" ", "").replace("-", "")
                                    if (cleanPhone.isNotBlank()) {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:$cleanPhone")
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                }
                            ) {
                                Text(text = "Parent: ${student.parentName} (${student.parentPhone})", color = TextSecondaryColor, fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.Call, contentDescription = "Call Parent", tint = GreenAccent, modifier = Modifier.size(11.sp.value.dp))
                            }
                            Text(text = "Email: ${student.parentEmail}", color = AccentBlue, fontSize = 10.sp)
                        }
                    }
                    SafiriTag(text = statusLabel, color = statusColor)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bus: ${student.assignedBusPlate} • Driver: ${student.assignedDriverName} • Stop: ${student.pickupStop}",
                    color = TextTertiaryColor,
                    fontSize = 10.sp
                )

                if (!student.parentNote.isNullOrBlank()) {
                    Text(text = "Parent Note: \"${student.parentNote}\"", color = AmberAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                if (!student.absentReason.isNullOrBlank() && student.status == StudentStatus.ABSENT) {
                    Text(text = "Absence Reason: ${student.absentReason}", color = RedAccent, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = {
                            editingParentStudent = student
                            editParentNameInput = student.parentName
                            editParentEmailInput = student.parentEmail
                            editParentPhoneInput = student.parentPhone
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Link Parent", modifier = Modifier.size(12.dp), tint = PurpleAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Link / Change Parent", fontSize = 10.sp, color = PurpleAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal to Link Parent
    editingParentStudent?.let { student ->
        AlertDialog(
            onDismissRequest = { editingParentStudent = null },
            title = { Text("Link Parent to ${student.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimaryColor) },
            text = {
                Column {
                    Text("Link or update parent contact details for student ID: ${student.id}", fontSize = 11.sp, color = TextSecondaryColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editParentNameInput,
                        onValueChange = { editParentNameInput = it },
                        label = { Text("Parent Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editParentEmailInput,
                        onValueChange = { editParentEmailInput = it },
                        label = { Text("Parent Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editParentPhoneInput,
                        onValueChange = { editParentPhoneInput = it },
                        label = { Text("Parent Phone (+254)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editParentNameInput.isNotBlank() && editParentEmailInput.isNotBlank()) {
                            viewModel.linkStudentToParent(
                                studentId = student.id,
                                parentName = editParentNameInput,
                                parentEmail = editParentEmailInput,
                                parentPhone = editParentPhoneInput
                            )
                            editingParentStudent = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    Text("Save & Link Parent", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingParentStudent = null }) {
                    Text("Cancel", color = TextSecondaryColor)
                }
            },
            containerColor = SurfaceColor
        )
    }

    if (showAddStudentDialog) {
        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = { Text("Register New Student", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimaryColor) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newStudentName,
                        onValueChange = { newStudentName = it },
                        label = { Text("Student Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newParentName,
                        onValueChange = { newParentName = it },
                        label = { Text("Parent Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newParentPhone,
                        onValueChange = { newParentPhone = it },
                        label = { Text("Parent Phone (+254)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newBusPlate,
                        onValueChange = { newBusPlate = it },
                        label = { Text("Assigned Bus Plate") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newStop,
                        onValueChange = { newStop = it },
                        label = { Text("Pickup Stop") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newStudentName.isNotBlank() && newParentName.isNotBlank()) {
                            viewModel.addOrUpdateStudent(
                                id = null,
                                name = newStudentName,
                                parentName = newParentName,
                                parentPhone = if (newParentPhone.isNotBlank()) newParentPhone else "+254 700 000 000",
                                busPlate = newBusPlate,
                                driverName = "Erick Mwangi",
                                pickupStop = newStop
                            )
                            showAddStudentDialog = false
                            newStudentName = ""
                            newParentName = ""
                            newParentPhone = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    Text("Register & Sync", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text("Cancel", color = TextSecondaryColor)
                }
            },
            containerColor = SurfaceColor
        )
    }
}
