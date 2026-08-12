package com.example.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.IncidentReport
import com.example.models.RouteStop
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================================
// 1. LIVE STEP-BY-STEP ALIGHT NAVIGATION & ECO IMPACT
// ==========================================================
@Composable
fun LiveNavigationGuideCard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val getOffAlertActive by viewModel.getOffAlertActive.collectAsState()
    val co2SavedKg by viewModel.co2SavedKg.collectAsState()
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()
    val etaMinutes by viewModel.etaMinutes.collectAsState()
    val stopsLeft by viewModel.stopsLeft.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "goPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    SafiriCard(
        testTag = "live_nav_guide_card",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GreenAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .alpha(pulseAlpha)
                                .background(GreenAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE ALIGHT & STEP GUIDANCE",
                            color = GreenAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Get Off Alert Toggle Button
            OutlinedButton(
                onClick = { viewModel.toggleGetOffAlert() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (getOffAlertActive) GreenAccent else TextTertiaryColor
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(30.dp)
                    .testTag("get_off_alert_toggle")
            ) {
                Icon(
                    imageVector = if (getOffAlertActive) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                    contentDescription = "Get Off Alert",
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (getOffAlertActive) "Alight Alert ON" else "Alert OFF",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step Guidance Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsBus,
                contentDescription = "Bus Step",
                tint = AccentBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stay on KDE 732X for 2 stops",
                    color = TextPrimaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Get ready to alight at Kilimani Station in $etaMinutes mins",
                    color = TextSecondaryColor,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$stopsLeft Stops",
                    color = AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Eco & Health Impact Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Eco footprint
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface3Color)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Park,
                    contentDescription = "CO2 Saved",
                    tint = GreenAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "${co2SavedKg}kg CO₂",
                        color = TextPrimaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "Saved vs Car", color = TextTertiaryColor, fontSize = 9.sp)
                }
            }

            // Health Calories
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface3Color)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalActivity,
                    contentDescription = "Calories",
                    tint = AmberAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "$caloriesBurned kcal",
                        color = TextPrimaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "Active Walk", color = TextTertiaryColor, fontSize = 9.sp)
                }
            }
        }
    }
}

// ==========================================================
// 2. VEHICLE CROWD DENSITY & RADAR
// ==========================================================
@Composable
fun LiveCrowdDensityCard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val crowdednessLevel by viewModel.crowdednessLevel.collectAsState()
    val etaMinutes by viewModel.etaMinutes.collectAsState()

    SafiriCard(
        testTag = "live_crowd_density_card",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Radar,
                    contentDescription = "Transit Radar",
                    tint = AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "VEHICLE CROWD DENSITY & RADAR",
                    color = AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Route strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Surface2Color)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentBlue)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SHUTTLE 732",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kilimani Express",
                        color = TextPrimaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Next stop: Kilimani Station • 1.2 km away",
                    color = TextSecondaryColor,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${etaMinutes}m",
                    color = GreenAccent,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "LIVE GPS",
                    color = GreenAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Crowdedness Reporter
        Text(
            text = "LIVE CROWD DENSITY REPORT",
            color = TextTertiaryColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        val crowdLevels = listOf(
            "Empty" to Icons.Filled.AirlineSeatReclineNormal,
            "Seats Available" to Icons.Filled.AirlineSeatReclineExtra,
            "Standing Only" to Icons.Filled.DirectionsWalk,
            "Full" to Icons.Filled.Group
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            crowdLevels.forEach { (level, icon) ->
                val isSelected = crowdednessLevel == level
                OutlinedButton(
                    onClick = {
                        viewModel.updateCrowdedness(level)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) AccentBlue.copy(alpha = 0.15f) else SurfaceColor,
                        contentColor = if (isSelected) AccentBlue else TextSecondaryColor
                    ),
                    border = BorderStroke(1.dp, if (isSelected) AccentBlue else BorderColor),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("crowd_btn_${level.replace(" ", "_")}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = level,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = level.split(" ").first(),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// ==========================================================
// 3. STATION PLATFORMS, BAYS & TRANSFERS
// ==========================================================
@Composable
fun PlatformAndTransferCard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val stops by viewModel.routeStops.collectAsState()

    SafiriCard(
        testTag = "platform_and_transfer_card",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Train,
                    contentDescription = "Platforms and Transfers",
                    tint = RedAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "STATION PLATFORMS & TRANSFERS",
                    color = RedAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Platform / Track Timeline
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            stops.forEachIndexed { index, stop ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Timeline indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (stop.isParentStop) AmberAccent else if (stop.isCompleted) GreenAccent else AccentBlue)
                        )
                        if (index < stops.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(30.dp)
                                    .background(BorderColor)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Stop Detail
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stop.name,
                                color = TextPrimaryColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // Track / Platform Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Surface3Color)
                                    .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stop.platformBay,
                                    color = TextPrimaryColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Transfer: ${stop.transferConnection}",
                                color = TextSecondaryColor,
                                fontSize = 10.sp
                            )
                            Text(
                                text = stop.delayNote,
                                color = if (stop.delayNote.contains("On time")) GreenAccent else AmberAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Connection Guarantee Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(GreenAccent.copy(alpha = 0.12f))
                .border(1.dp, GreenAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = "Guaranteed Connection",
                tint = GreenAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Connection Guaranteed: Shuttle KDE 732X holds for transfers at Valley Arcade.",
                color = TextPrimaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==========================================================
// 4. COMMUNITY INCIDENTS & DELAY REPORTS
// ==========================================================
@Composable
fun CommunityIncidentFeedCard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val incidents by viewModel.communityIncidents.collectAsState()
    var showReportModal by remember { mutableStateOf(false) }
    var reportTitle by remember { mutableStateOf("") }
    var reportLoc by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Traffic") }

    SafiriCard(
        testTag = "community_incident_feed_card",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Traffic,
                    contentDescription = "Community Incidents",
                    tint = AmberAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "COMMUNITY INCIDENTS & DELAYS",
                    color = AmberAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }

            Button(
                onClick = { showReportModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(28.dp)
                    .testTag("post_incident_report_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.AddAlert,
                    contentDescription = "Report Incident",
                    tint = BackgroundColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+ Report",
                    color = BackgroundColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Incidents list
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            incidents.forEach { incident ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceColor)
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (incident.category) {
                                    "Traffic" -> Icons.Filled.DirectionsCar
                                    "Weather" -> Icons.Filled.WbCloudy
                                    else -> Icons.Filled.Engineering
                                },
                                contentDescription = incident.category,
                                tint = AmberAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = incident.title,
                                color = TextPrimaryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${incident.location} • ${incident.reportedTime}",
                            color = TextSecondaryColor,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Upvote button
                    IconButton(
                        onClick = { viewModel.upvoteIncident(incident.id) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (incident.isUpvoted) AmberAccent.copy(alpha = 0.2f) else Surface3Color)
                            .size(36.dp)
                            .testTag("upvote_btn_${incident.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.ThumbUp,
                                contentDescription = "Upvote",
                                tint = if (incident.isUpvoted) AmberAccent else TextTertiaryColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${incident.votes}",
                                color = if (incident.isUpvoted) AmberAccent else TextSecondaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Incident Submission Dialog
        if (showReportModal) {
            AlertDialog(
                onDismissRequest = { showReportModal = false },
                title = {
                    Text(
                        text = "Report Traffic Incident",
                        color = TextPrimaryColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Help other parents & commuters by reporting real-time delays.",
                            color = TextSecondaryColor,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = reportTitle,
                            onValueChange = { reportTitle = it },
                            label = { Text("Incident Description") },
                            placeholder = { Text("e.g. Heavy traffic at Junction 4") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = reportLoc,
                            onValueChange = { reportLoc = it },
                            label = { Text("Location") },
                            placeholder = { Text("e.g. Ring Rd Kilimani") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.postCommunityIncident(reportTitle, reportLoc, selectedCat)
                            showReportModal = false
                            reportTitle = ""
                            reportLoc = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                    ) {
                        Text("Post Report", color = BackgroundColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportModal = false }) {
                        Text("Cancel", color = TextSecondaryColor)
                    }
                },
                containerColor = SurfaceColor
            )
        }
    }
}

// ==========================================================
// 5. LIVE TRIP SHARING & DRIVER CONNECT
// ==========================================================
@Composable
fun LiveJourneyShareCard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shareToastMessage by viewModel.shareToastMessage.collectAsState()

    SafiriCard(
        testTag = "live_journey_share_card",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share Journey",
                    tint = GreenAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE TRIP SHARING & DRIVER CONNECT",
                    color = GreenAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Captain Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Captain Erick",
                        tint = AccentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Captain Erick Mwangi",
                        color = TextPrimaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = AmberAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "4.92 • 1,240 trips • Verified Driver",
                            color = TextSecondaryColor,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Contact Icon
            IconButton(
                onClick = { 
                    try {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:0712345678")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        viewModel.shareLiveTripLink()
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GreenAccent)
                    .size(36.dp)
                    .testTag("contact_captain_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = "Call Captain",
                    tint = BackgroundColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Share Link Button
        Button(
            onClick = {
                viewModel.shareLiveTripLink()
            },
            colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("share_trip_link_btn"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.IosShare,
                contentDescription = "Share",
                tint = BackgroundColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Share Live Journey Tracking Link",
                color = BackgroundColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        if (shareToastMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreenAccent.copy(alpha = 0.15f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = shareToastMessage ?: "",
                    color = TextPrimaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = { viewModel.clearShareToast() },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
