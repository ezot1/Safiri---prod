package com.example.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.*
import com.example.models.InterportalMessage
import com.example.models.StudentStatus
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel

@Composable
fun DriverDashboard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Route, 1: Boarding, 2: History

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
                    icon = { Icon(Icons.Filled.Map, contentDescription = "Route") },
                    label = { Text("Route") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GreenAccent,
                        unselectedIconColor = TextTertiaryColor,
                        indicatorColor = Surface3Color,
                        selectedTextColor = GreenAccent
                    ),
                    modifier = Modifier.testTag("driver_tab_route")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Filled.HowToReg, contentDescription = "Boarding") },
                    label = { Text("Boarding") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GreenAccent,
                        unselectedIconColor = TextTertiaryColor,
                        indicatorColor = Surface3Color,
                        selectedTextColor = GreenAccent
                    ),
                    modifier = Modifier.testTag("driver_tab_boarding")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Filled.History, contentDescription = "History") },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GreenAccent,
                        unselectedIconColor = TextTertiaryColor,
                        indicatorColor = Surface3Color,
                        selectedTextColor = GreenAccent
                    ),
                    modifier = Modifier.testTag("driver_tab_history")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (activeTab) {
                0 -> DriverRouteTab(viewModel)
                1 -> DriverBoardingTab(viewModel)
                2 -> DriverHistoryTab(viewModel)
            }
        }
    }
}

// ==========================================
// 1. DRIVER ROUTE TAB (DriverRouteTab)
// ==========================================
@Composable
fun DriverRouteTab(viewModel: AppViewModel) {
    val progress by viewModel.progress.collectAsState()
    val etaMinutes by viewModel.etaMinutes.collectAsState()
    val stops by viewModel.routeStops.collectAsState()
    val sosActive by viewModel.sosActive.collectAsState()
    val boardedCount by viewModel.boardedCount.collectAsState()
    val kilimaniComplete by viewModel.kilimaniComplete.collectAsState()
    val driverNextStopName by viewModel.driverNextStopName.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()

    ScrollView(
        modifier = Modifier.fillMaxSize(),
        contentContainerStyle = Modifier.padding(16.dp)
    ) {
        // Map Hero Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            // Reusable Map Component showing Nairobi streets
            MapHero(
                progressPct = progress,
                busPlate = "KDE 732X",
                statusText = "Active Route",
                etaMinutes = etaMinutes,
                stops = stops,
                modifier = Modifier.fillMaxWidth()
            )

            // Customized Driver Badges (Top-Left and Top-Right overlaid)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left Driver Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface2Color.copy(alpha = 0.95f))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "KDE 732X • $currentUserName",
                        color = TextPrimaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Top-Right Controls (SOS and Sign-out)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.triggerSOS() },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("sos_button")
                    ) {
                        Text("SOS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }

                    IconButton(
                        onClick = { viewModel.signOut() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Surface2Color.copy(alpha = 0.95f))
                            .size(30.dp)
                    ) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Exit", tint = RedAccent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SOS alert banner inside the sheet
        AnimatedVisibility(visible = sosActive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = RedAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Emergency alert sent",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "HQ has received your GPS coordinates. SMS Fallback activated via Africa's Talking.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                    Button(
                        onClick = { viewModel.dismissSOS() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Dismiss", color = RedAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom sheet contents
        SafiriCard(testTag = "driver_metrics_card") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricTile(label = "ABOARD", value = "${12 + boardedCount} / 22", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(6.dp))
                MetricTile(label = "NEXT STOP", value = if (kilimaniComplete) "Upper Hill" else "Kilimani", modifier = Modifier.weight(1.2f))
                Spacer(modifier = Modifier.width(6.dp))
                MetricTile(label = "ETA", value = "${etaMinutes}m", modifier = Modifier.weight(0.8f))
                Spacer(modifier = Modifier.width(6.dp))
                MetricTile(label = "DONE %", value = "${(progress * 100).toInt()}%", modifier = Modifier.weight(0.9f))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Route stops card
        SafiriCard(testTag = "driver_stops_checklist_card") {
            Text(
                text = "ROUTE STOPS CHECKLIST",
                color = TextTertiaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Done stops show green checkmark dot + green pill. Current stop shows pulsing blue dot + Next pill. Pending shows grey dot.
            RouteChecklistRow(name = "Kawangware Stop", done = true, active = false, label = "Completed")
            RouteChecklistRow(name = "Westlands Mall Bay", done = true, active = false, label = "Completed")
            RouteChecklistRow(
                name = "Kilimani Bus Bay",
                done = kilimaniComplete,
                active = !kilimaniComplete,
                label = if (kilimaniComplete) "Completed" else "Next stop"
            )
            RouteChecklistRow(
                name = "Upper Hill Junction",
                done = false,
                active = kilimaniComplete,
                label = if (kilimaniComplete) "Next stop" else "Pending"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Trip summary card
        SafiriCard(testTag = "trip_summary_card") {
            Text(
                text = "TRIP PERFORMANCE SUMMARY",
                color = TextTertiaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            SummaryLabelRow(label = "Departure Time", value = "03:45 PM")
            SummaryLabelRow(label = "Distance Traveled So Far", value = "3.2 km")
            SummaryLabelRow(label = "Stops Completed", value = if (kilimaniComplete) "3 / 4" else "2 / 4")
            SummaryLabelRow(label = "Students Confirmed Aboard", value = "${boardedCount} / 6 checklist boarded")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
// 2. DRIVER BOARDING CHECKLIST TAB
// ==========================================
@Composable
fun DriverBoardingTab(viewModel: AppViewModel) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val boardedCount by viewModel.boardedCount.collectAsState()
    val kilimaniComplete by viewModel.kilimaniComplete.collectAsState()
    val driverNextStopName by viewModel.driverNextStopName.collectAsState()

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
                text = "STUDENT BOARDING",
                color = TextPrimaryColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.signOut() }) {
                Icon(Icons.Filled.Logout, contentDescription = "Log out", tint = RedAccent)
            }
        }

        Text(
            text = "Verify Kilimani students as they climb aboard the bus",
            color = TextSecondaryColor,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Progress bar card
        SafiriCard(testTag = "boarding_progress_card") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Checklist Progress", color = TextPrimaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("$boardedCount / 6 boarded", color = GreenAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { boardedCount.toFloat() / 6f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = GreenAccent,
                trackColor = Surface3Color
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Student checklist card
        SafiriCard(testTag = "students_checklist_card") {
            Text(
                text = "KILIMANI CHECKLIST",
                color = TextTertiaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            students.forEach { student ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface3Color)
                        .padding(12.dp)
                ) {
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(student.avatarColor.copy(alpha = 0.15f))
                                    .border(1.dp, student.avatarColor.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = student.initials,
                                    color = student.avatarColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = student.name,
                                    color = TextPrimaryColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                                     Text(
                                         text = "Parent: ${student.parentName} (${student.parentPhone}) • ${student.pickupStop}",
                                         color = TextSecondaryColor,
                                         fontSize = 11.sp
                                     )
                                     Spacer(modifier = Modifier.width(4.dp))
                                     Icon(
                                         imageVector = Icons.Filled.Call,
                                         contentDescription = "Call Parent",
                                         tint = GreenAccent,
                                         modifier = Modifier.size(12.dp)
                                     )
                                 }
                            }
                        }

                        val tagColor = when (student.status) {
                            StudentStatus.BOARDED -> GreenAccent
                            StudentStatus.DROPPED_OFF -> AccentBlue
                            StudentStatus.ABSENT -> RedAccent
                            StudentStatus.NOT_BOARDED -> AmberAccent
                        }
                        val tagText = when (student.status) {
                            StudentStatus.BOARDED -> "Boarded ✓"
                            StudentStatus.DROPPED_OFF -> "Dropped Off"
                            StudentStatus.ABSENT -> "ABSENT"
                            StudentStatus.NOT_BOARDED -> "Waiting"
                        }
                        SafiriTag(text = tagText, color = tagColor)
                    }

                    if (!student.parentNote.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberAccent.copy(alpha = 0.15f))
                                .border(1.dp, AmberAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Note from Parent: \"${student.parentNote}\"",
                                color = AmberAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!student.absentReason.isNullOrBlank() && student.status == StudentStatus.ABSENT) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(RedAccent.copy(alpha = 0.15f))
                                .border(1.dp, RedAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Parent marked ABSENT: ${student.absentReason}",
                                color = RedAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (student.status != StudentStatus.BOARDED) {
                            Button(
                                onClick = { viewModel.updateStudentStatus(student.id, StudentStatus.BOARDED) },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Board Student", color = BackgroundColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (student.status == StudentStatus.BOARDED) {
                            Button(
                                onClick = { viewModel.updateStudentStatus(student.id, StudentStatus.DROPPED_OFF) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Arrived / Drop Off", color = BackgroundColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (student.status != StudentStatus.NOT_BOARDED) {
                            OutlinedButton(
                                onClick = { viewModel.updateStudentStatus(student.id, StudentStatus.NOT_BOARDED) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reset", fontSize = 11.sp, color = TextSecondaryColor)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mark Kilimani complete button
            Button(
                onClick = { viewModel.setKilimaniComplete() },
                enabled = !kilimaniComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (kilimaniComplete) Surface3Color else GreenAccent,
                    disabledContainerColor = Surface3Color
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("mark_complete_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (kilimaniComplete) "Kilimani complete ✓" else "Mark Kilimani complete",
                    color = if (kilimaniComplete) TextSecondaryColor else BackgroundColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reveal Next Stop card when complete
        AnimatedVisibility(visible = kilimaniComplete) {
            SafiriCard(testTag = "next_stop_card") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.DirectionsTransit, contentDescription = "Transit", tint = AccentBlue)
                    Text(
                        text = "UPCOMING STATION INSTRUCTIONS",
                        color = TextTertiaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Upper Hill Junction drop-off",
                    color = TextPrimaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Expected arrivals: 04:25 PM • 14 kids scheduled to alight. Verify all baggage handles before releasing emergency pneumatic doors.",
                    color = TextSecondaryColor,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ==========================================
// 3. TRIP HISTORY & ANALYTICS TAB
// ==========================================
@Composable
fun DriverHistoryTab(viewModel: AppViewModel) {
    ScrollView(
        modifier = Modifier.fillMaxSize(),
        contentContainerStyle = Modifier.padding(16.dp)
    ) {
        Text(
            text = "TRIP PERFORMANCE STATS",
            color = TextPrimaryColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Analyze weekly logs & schedule ratings",
            color = TextSecondaryColor,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Metrics Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(label = "THIS WEEK RATE", value = "87%", modifier = Modifier.weight(1f))
            MetricTile(label = "LAST WEEK RATE", value = "82%", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekdays on-time bar chart (Mon-Fri)
        SafiriCard(testTag = "on_time_chart_card") {
            Text(
                text = "ON-TIME RATE BY WEEKDAY",
                color = TextTertiaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Mon: 75, Tue: 88, Wed: 92 (Highlighted), Thu: 80, Fri: 87
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                ChartBar(day = "Mon", rate = 75, highlighted = false)
                ChartBar(day = "Tue", rate = 88, highlighted = false)
                ChartBar(day = "Wed", rate = 92, highlighted = true) // Wednesday highlighted
                ChartBar(day = "Thu", rate = 80, highlighted = false)
                ChartBar(day = "Fri", rate = 87, highlighted = false)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Trips list
        SafiriCard(testTag = "recent_trips_history_card") {
            Text(
                text = "RECENT SHUTTLES LOGS",
                color = TextTertiaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            TripHistoryRow(date = "Today Morning", duration = "06:45 AM - 07:22 AM", delay = "On time", color = GreenAccent)
            TripHistoryRow(date = "Yesterday Evening", duration = "03:48 PM - 04:30 PM", delay = "+8 min", color = AmberAccent)
            TripHistoryRow(date = "Yesterday Morning", duration = "06:42 AM - 07:18 AM", delay = "On time", color = GreenAccent)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Monthly stats card
        SafiriCard(testTag = "monthly_stats_card") {
            Text(
                text = "MONTHLY TOTAL RECORD",
                color = TextTertiaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            SummaryLabelRow("Total Trips Dispatched", "22")
            SummaryLabelRow("On-Time Trips Rate", "19 / 22 (86%)")
            SummaryLabelRow("Students Transported", "484 kids")
            SummaryLabelRow("Odometer Distance covered", "312 km")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RouteChecklistRow(name: String, done: Boolean, active: Boolean, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (done) GreenAccent.copy(alpha = 0.15f)
                        else if (active) AccentBlue.copy(alpha = 0.15f)
                        else Surface3Color
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (done) Icons.Filled.Check else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "Status",
                    tint = if (done) GreenAccent else if (active) AccentBlue else TextTertiaryColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                color = if (done) TextSecondaryColor else TextPrimaryColor,
                fontSize = 14.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            )
        }

        SafiriTag(
            text = label,
            color = if (done) GreenAccent else if (active) AccentBlue else TextTertiaryColor
        )
    }
}

@Composable
fun ChartBar(day: String, rate: Int, highlighted: Boolean) {
    val barColor = if (highlighted) AccentBlue else Surface3Color
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxHeight()
    ) {
        Text(
            text = "$rate%",
            color = if (highlighted) AccentBlue else TextSecondaryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight(rate.toFloat() / 110f) // Scale safely
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(barColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = day,
            color = if (highlighted) TextPrimaryColor else TextTertiaryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SummaryLabelRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondaryColor, fontSize = 13.sp)
        Text(text = value, color = TextPrimaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
