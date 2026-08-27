package com.example.components

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.models.*
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================================
// 1. MOOVIT LINES & TIMETABLES EXPLORER
// ==========================================================
@Composable
fun MoovitLinesExplorerCard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val lines by viewModel.transitLines.collectAsState()
    val favoriteLines by viewModel.savedFavoriteLines.collectAsState()
    val selectedLine by viewModel.selectedTransitLine.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var filterFavoritesOnly by remember { mutableStateOf(false) }

    val filteredLines = remember(lines, searchQuery, favoriteLines, filterFavoritesOnly) {
        lines.filter { line ->
            val matchesQuery = line.lineNumber.contains(searchQuery, ignoreCase = true) ||
                    line.name.contains(searchQuery, ignoreCase = true) ||
                    line.operator.contains(searchQuery, ignoreCase = true) ||
                    line.direction1Stops.any { it.contains(searchQuery, ignoreCase = true) }
            val matchesFav = !filterFavoritesOnly || favoriteLines.contains(line.id)
            matchesQuery && matchesFav
        }
    }

    SafiriCard(
        testTag = "moovit_lines_explorer_card",
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
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsBus,
                        contentDescription = "Transit Lines",
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "TRANSIT LINES & TIMETABLES",
                        color = AccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Explore routes, frequencies & stop schedules",
                        color = TextSecondaryColor,
                        fontSize = 10.sp
                    )
                }
            }

            FilterChip(
                selected = filterFavoritesOnly,
                onClick = { filterFavoritesOnly = !filterFavoritesOnly },
                label = { Text("Starred (${favoriteLines.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        imageVector = if (filterFavoritesOnly) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Filter Stars",
                        modifier = Modifier.size(12.dp),
                        tint = if (filterFavoritesOnly) AmberAccent else TextSecondaryColor
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AmberAccent.copy(alpha = 0.2f),
                    selectedLabelColor = TextPrimaryColor
                ),
                border = BorderStroke(1.dp, if (filterFavoritesOnly) AmberAccent else BorderColor),
                modifier = Modifier.height(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar for Lines
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search bus line or stop (e.g. 732, Westlands)", fontSize = 12.sp, color = TextTertiaryColor) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = TextSecondaryColor, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextSecondaryColor, modifier = Modifier.size(14.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceColor,
                unfocusedContainerColor = SurfaceColor,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = BorderColor
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("moovit_line_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // List of lines
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filteredLines.forEach { line ->
                val isFav = favoriteLines.contains(line.id)
                MoovitLineItemRow(
                    line = line,
                    isFavorite = isFav,
                    onToggleFavorite = { viewModel.toggleFavoriteLine(line.id) },
                    onSelect = { viewModel.selectTransitLine(line) }
                )
            }
        }
    }

    // Modal Sheet for Line Timetable & Station Map
    if (selectedLine != null) {
        MoovitLineDetailDialog(
            line = selectedLine!!,
            isFavorite = favoriteLines.contains(selectedLine!!.id),
            onToggleFavorite = { viewModel.toggleFavoriteLine(selectedLine!!.id) },
            onDismiss = { viewModel.selectTransitLine(null) },
            viewModel = viewModel
        )
    }
}

@Composable
fun MoovitLineItemRow(
    line: TransitLine,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect)
            .padding(10.dp)
            .testTag("line_item_${line.lineNumber}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Line Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(line.color)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = line.lineNumber,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = line.name,
                        color = TextPrimaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Every ${line.frequencyMinutes}m • ${line.direction1Name}",
                        color = TextSecondaryColor,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Status pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (line.statusType) {
                            LineStatusType.ON_TIME -> GreenAccent.copy(alpha = 0.15f)
                            LineStatusType.MODERATE_DELAY -> AmberAccent.copy(alpha = 0.15f)
                            else -> RedAccent.copy(alpha = 0.15f)
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = when (line.statusType) {
                        LineStatusType.ON_TIME -> "On Time"
                        LineStatusType.MODERATE_DELAY -> "Delay"
                        else -> "Detour"
                    },
                    color = when (line.statusType) {
                        LineStatusType.ON_TIME -> GreenAccent
                        LineStatusType.MODERATE_DELAY -> AmberAccent
                        else -> RedAccent
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp).testTag("fav_btn_${line.lineNumber}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Favorite Line",
                    tint = if (isFavorite) AmberAccent else TextTertiaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun MoovitLineDetailDialog(
    line: TransitLine,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: AppViewModel
) {
    var selectedDirection by remember { mutableStateOf(1) }
    var showDelayReportDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor.copy(alpha = 0.96f))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(line.color)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = line.lineNumber,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = line.name,
                                color = TextPrimaryColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${line.operator} • ${line.serviceHours}",
                                color = TextSecondaryColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) AmberAccent else TextTertiaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextPrimaryColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Direction switcher tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface3Color)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedDirection == 1) AccentBlue else Color.Transparent)
                            .clickable { selectedDirection = 1 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = line.direction1Name,
                            color = if (selectedDirection == 1) Color.White else TextSecondaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedDirection == 2) AccentBlue else Color.Transparent)
                            .clickable { selectedDirection = 2 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = line.direction2Name,
                            color = if (selectedDirection == 2) Color.White else TextSecondaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (line.statusType) {
                                LineStatusType.ON_TIME -> GreenAccent.copy(alpha = 0.12f)
                                LineStatusType.MODERATE_DELAY -> AmberAccent.copy(alpha = 0.12f)
                                else -> RedAccent.copy(alpha = 0.12f)
                            }
                        )
                        .border(
                            1.dp,
                            when (line.statusType) {
                                LineStatusType.ON_TIME -> GreenAccent.copy(alpha = 0.3f)
                                LineStatusType.MODERATE_DELAY -> AmberAccent.copy(alpha = 0.3f)
                                else -> RedAccent.copy(alpha = 0.3f)
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = when (line.statusType) {
                                LineStatusType.ON_TIME -> Icons.Filled.CheckCircle
                                LineStatusType.MODERATE_DELAY -> Icons.Filled.Warning
                                else -> Icons.Filled.Error
                            },
                            contentDescription = "Status",
                            tint = when (line.statusType) {
                                LineStatusType.ON_TIME -> GreenAccent
                                LineStatusType.MODERATE_DELAY -> AmberAccent
                                else -> RedAccent
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = line.statusDetail,
                            color = TextPrimaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = { showDelayReportDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Report Delay", fontSize = 10.sp, color = AmberAccent, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Stops & Timetable
                val currentStops = if (selectedDirection == 1) line.direction1Stops else line.direction2Stops

                Text(
                    text = "STATIONS ALONG ROUTE (${currentStops.size})",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentStops.forEachIndexed { index, stopName ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (index == 0) GreenAccent else if (index == currentStops.size - 1) RedAccent else line.color)
                                )
                                if (index < currentStops.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(24.dp)
                                            .background(BorderColor)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Surface2Color)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stopName,
                                    color = TextPrimaryColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Bay ${(index % 3) + 1}",
                                    color = TextTertiaryColor,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Timetable departures strip
                Text(
                    text = "NEXT DEPARTURES TODAY",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    line.timetableDepartures.forEach { dep ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Surface3Color)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = dep.time, color = TextPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                if (dep.isLive) {
                                    Text(text = "LIVE", color = GreenAccent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                } else {
                                    Text(text = "Scheduled", color = TextTertiaryColor, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDelayReportDialog) {
            var delayMinutesText by remember { mutableStateOf("5") }
            var reasonText by remember { mutableStateOf("Heavy jam at Junction") }

            AlertDialog(
                onDismissRequest = { showDelayReportDialog = false },
                title = { Text("Report Delay on Line ${line.lineNumber}", color = TextPrimaryColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Share real-time disruption with fellow riders.", color = TextSecondaryColor, fontSize = 12.sp)
                        OutlinedTextField(
                            value = delayMinutesText,
                            onValueChange = { delayMinutesText = it },
                            label = { Text("Delay (Minutes)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = reasonText,
                            onValueChange = { reasonText = it },
                            label = { Text("Reason / Location") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val mins = delayMinutesText.toIntOrNull() ?: 5
                            viewModel.reportLineDelay(line.id, mins, reasonText)
                            showDelayReportDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                    ) {
                        Text("Post Delay", color = BackgroundColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDelayReportDialog = false }) {
                        Text("Cancel", color = TextSecondaryColor)
                    }
                },
                containerColor = SurfaceColor
            )
        }
    }
}

// ==========================================================
// 2. MOOVIT NEARBY STATIONS & LIVE RADAR
// ==========================================================
@Composable
fun MoovitNearbyStationsRadarCard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val stations by viewModel.nearbyStations.collectAsState()
    val savedStationIds by viewModel.savedStationIds.collectAsState()
    var selectedStationForPickup by remember { mutableStateOf<NearbyStation?>(null) }

    SafiriCard(
        testTag = "moovit_nearby_stations_card",
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
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GreenAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.NearMe,
                        contentDescription = "Nearby Stations",
                        tint = GreenAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "NEARBY STATIONS & LIVE DEPARTURES",
                        color = GreenAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Real-time arriving buses within walking distance",
                        color = TextSecondaryColor,
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = "${stations.size} Stations",
                color = GreenAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            stations.forEach { station ->
                val isSaved = savedStationIds.contains(station.id)
                MoovitStationRowItem(
                    station = station,
                    isSaved = isSaved,
                    onToggleSaved = { viewModel.toggleSavedStation(station.id) },
                    onSetPickup = { selectedStationForPickup = station }
                )
            }
        }
    }

    if (selectedStationForPickup != null) {
        AlertDialog(
            onDismissRequest = { selectedStationForPickup = null },
            title = { Text("Set Pickup Station", color = TextPrimaryColor, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Do you want to set \"${selectedStationForPickup!!.name}\" as the active pickup station for your child?",
                    color = TextSecondaryColor,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCustomPickupStation(selectedStationForPickup!!.name)
                        selectedStationForPickup = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent)
                ) {
                    Text("Confirm Pickup", color = BackgroundColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedStationForPickup = null }) {
                    Text("Cancel", color = TextSecondaryColor)
                }
            },
            containerColor = SurfaceColor
        )
    }
}

@Composable
fun MoovitStationRowItem(
    station: NearbyStation,
    isSaved: Boolean,
    onToggleSaved: () -> Unit,
    onSetPickup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("station_item_${station.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.name,
                        color = TextPrimaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (station.wheelchairAccessible) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Accessible,
                            contentDescription = "Accessible",
                            tint = AccentBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${station.distanceMeters}m away • ~${station.walkingTimeMinutes} min walk • ${station.platformBay}",
                    color = TextSecondaryColor,
                    fontSize = 10.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleSaved, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Save Station",
                        tint = if (isSaved) AccentBlue else TextTertiaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                OutlinedButton(
                    onClick = onSetPickup,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("Set Pickup", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Next arriving lines at this station
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            station.nextDepartures.forEach { dep ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Surface3Color)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(dep.lineColor)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(text = dep.lineNumber, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "in ${dep.etaMinutes}m",
                        color = GreenAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================================
// 3. MOOVIT MULTI-MODAL TRIP PLANNER & ROUTE FILTER
// ==========================================================
@Composable
fun MoovitMultiModalPlannerCard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val plans by viewModel.multiModalPlans.collectAsState()
    val activeLiveGuidance by viewModel.activeLiveGuidance.collectAsState()
    var selectedFilter by remember { mutableStateOf("BEST_ROUTE") } // BEST_ROUTE, FEWEST_TRANSFERS, LEAST_WALKING
    var expandedPlanId by remember { mutableStateOf<String?>("plan_best") }

    val filteredPlans = remember(plans, selectedFilter) {
        when (selectedFilter) {
            "FEWEST_TRANSFERS" -> plans.sortedBy { it.transfersCount }
            "LEAST_WALKING" -> plans.sortedBy { it.walkMinutes }
            else -> plans.sortedBy { it.totalDurationMinutes }
        }
    }

    SafiriCard(
        testTag = "moovit_multimodal_planner_card",
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
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AmberAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AltRoute,
                        contentDescription = "Trip Planner",
                        tint = AmberAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SCHOOL COMMUTE ROUTE PLANNER",
                        color = AmberAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Compare optimal school routes, pickup timings & bus drop-offs",
                        color = TextSecondaryColor,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips (Fastest, Direct Route, Closest Pickup)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "BEST_ROUTE" to "Fastest",
                "FEWEST_TRANSFERS" to "Direct Route",
                "LEAST_WALKING" to "Closest Pickup"
            ).forEach { (filterKey, label) ->
                val isSelected = selectedFilter == filterKey
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filterKey },
                    label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberAccent.copy(alpha = 0.2f),
                        selectedLabelColor = TextPrimaryColor
                    ),
                    border = BorderStroke(1.dp, if (isSelected) AmberAccent else BorderColor),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Itinerary Plans
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filteredPlans.forEach { plan ->
                val isExpanded = expandedPlanId == plan.id
                MoovitPlanCard(
                    plan = plan,
                    isExpanded = isExpanded,
                    isActiveLiveGuidance = activeLiveGuidance?.id == plan.id,
                    onToggleExpand = {
                        expandedPlanId = if (isExpanded) null else plan.id
                    },
                    onStartLiveGuidance = {
                        viewModel.startLiveGuidance(plan)
                    }
                )
            }
        }
    }
}

@Composable
fun MoovitPlanCard(
    plan: MultiModalPlan,
    isExpanded: Boolean,
    isActiveLiveGuidance: Boolean,
    onToggleExpand: () -> Unit,
    onStartLiveGuidance: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .border(
                1.dp,
                if (isActiveLiveGuidance) GreenAccent else if (isExpanded) AmberAccent.copy(alpha = 0.5f) else BorderColor,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
            .testTag("plan_card_${plan.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(plan.primaryLineColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "LINE ${plan.primaryLineNumber}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = plan.title,
                        color = TextPrimaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${plan.departureTime} → ${plan.arrivalTime} • ${plan.walkMinutes}m safe walk • Shuttle ${plan.primaryBusPlate}",
                        color = TextSecondaryColor,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${plan.totalDurationMinutes} min",
                    color = AccentBlue,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = TextSecondaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expanded Step-by-Step Breakdown
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Divider(color = BorderColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "SCHOOL ROUTE PICK-UP & DROP-OFF STOPS",
                    color = TextTertiaryColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                plan.steps.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when (step.type) {
                                        ItineraryStepType.WALK_TO_STOP, ItineraryStepType.WALK_TO_DESTINATION -> Surface3Color
                                        ItineraryStepType.BOARD_BUS, ItineraryStepType.RIDE_BUS -> plan.primaryLineColor.copy(alpha = 0.2f)
                                        ItineraryStepType.ALIGHT -> GreenAccent.copy(alpha = 0.2f)
                                        ItineraryStepType.TRANSFER -> AmberAccent.copy(alpha = 0.2f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (step.type) {
                                    ItineraryStepType.WALK_TO_STOP, ItineraryStepType.WALK_TO_DESTINATION -> Icons.Filled.DirectionsWalk
                                    ItineraryStepType.BOARD_BUS, ItineraryStepType.RIDE_BUS -> Icons.Filled.DirectionsBus
                                    ItineraryStepType.ALIGHT -> Icons.Filled.School
                                    ItineraryStepType.TRANSFER -> Icons.Filled.DirectionsBus
                                },
                                contentDescription = null,
                                tint = when (step.type) {
                                    ItineraryStepType.WALK_TO_STOP, ItineraryStepType.WALK_TO_DESTINATION -> TextPrimaryColor
                                    ItineraryStepType.BOARD_BUS, ItineraryStepType.RIDE_BUS -> plan.primaryLineColor
                                    ItineraryStepType.ALIGHT -> GreenAccent
                                    ItineraryStepType.TRANSFER -> AmberAccent
                                },
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = step.instruction, color = TextPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = step.subDetail, color = TextSecondaryColor, fontSize = 10.sp)
                        }

                        Text(
                            text = "${step.durationMinutes} min",
                            color = TextTertiaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ==========================================================
// 4. MOOVIT LIVE GUIDANCE & GET-OFF ALERTS BANNER
// ==========================================================
@Composable
fun MoovitLiveGuidanceBanner(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val activePlan by viewModel.activeLiveGuidance.collectAsState()
    val currentStepIndex by viewModel.currentLiveStepIndex.collectAsState()
    val alightAlertFired by viewModel.liveGuidanceAlightAlertFired.collectAsState()

    if (activePlan == null) return

    val currentStep = activePlan!!.steps.getOrNull(currentStepIndex) ?: return

    val infiniteTransition = rememberInfiniteTransition(label = "navPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "navPulse"
    )

    SafiriCard(
        testTag = "moovit_live_guidance_banner",
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
                        .size(8.dp)
                        .clip(CircleShape)
                        .alpha(pulseAlpha)
                        .background(if (alightAlertFired) RedAccent else GreenAccent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (alightAlertFired) "ALIGHT ALERT • GET OFF NEXT STOP" else "LIVE TRIP GUIDANCE ACTIVE",
                    color = if (alightAlertFired) RedAccent else GreenAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            IconButton(
                onClick = { viewModel.stopLiveGuidance() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Exit Navigation", tint = TextSecondaryColor, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Step Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (alightAlertFired) RedAccent.copy(alpha = 0.15f) else SurfaceColor)
                .border(1.dp, if (alightAlertFired) RedAccent else BorderColor, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (currentStep.type) {
                    ItineraryStepType.WALK_TO_STOP, ItineraryStepType.WALK_TO_DESTINATION -> Icons.Filled.DirectionsWalk
                    ItineraryStepType.BOARD_BUS -> Icons.Filled.DirectionsBus
                    ItineraryStepType.RIDE_BUS -> Icons.Filled.DirectionsBus
                    ItineraryStepType.ALIGHT -> Icons.Filled.ExitToApp
                    ItineraryStepType.TRANSFER -> Icons.Filled.TransferWithinAStation
                },
                contentDescription = "Step",
                tint = if (alightAlertFired) RedAccent else AccentBlue,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentStep.instruction,
                    color = TextPrimaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentStep.subDetail,
                    color = TextSecondaryColor,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Step ${currentStepIndex + 1}/${activePlan!!.steps.size}",
                    color = AccentBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.stopLiveGuidance() },
                modifier = Modifier.weight(1f).height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryColor)
            ) {
                Text("End Trip", fontSize = 11.sp)
            }

            Button(
                onClick = { viewModel.advanceLiveStep() },
                modifier = Modifier.weight(2f).height(36.dp).testTag("advance_live_step_btn"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent)
            ) {
                Text("Next Step →", color = BackgroundColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
