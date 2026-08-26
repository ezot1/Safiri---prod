package com.example.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.example.models.RouteStop
import com.example.ui.theme.*

// 1. Custom Dark Surface Card with Subtle Border and Fade-in Animation
@Composable
fun SafiriCard(
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
    }

    var baseModifier = modifier
        .alpha(alpha.value)
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(Surface2Color)
        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        .padding(16.dp)

    if (testTag.isNotEmpty()) {
        baseModifier = baseModifier.testTag(testTag)
    }

    if (onClick != null) {
        baseModifier = baseModifier.clickable(onClick = onClick)
    }

    Column(modifier = baseModifier) {
        content()
    }
}

// 2. Color-Coded Status Tags / Pills
@Composable
fun SafiriTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AccentBlue,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

// 3. Animated Occupancy Bar
@Composable
fun OccupancyBar(
    count: Int,
    capacity: Int,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val ratio = (count.toFloat() / capacity.toFloat()).coerceIn(0f, 1f)
    val color = when {
        ratio < 0.5f -> GreenAccent
        ratio < 0.8f -> AmberAccent
        else -> RedAccent
    }

    val animatedWidth = remember { Animatable(0f) }
    LaunchedEffect(ratio) {
        animatedWidth.animateTo(
            targetValue = ratio,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Column(modifier = modifier.testTag(testTag)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Occupancy: $count / $capacity seats",
                color = TextSecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            SafiriTag(
                text = when {
                    ratio < 0.5f -> "Low"
                    ratio < 0.8f -> "Moderate"
                    else -> "Crowded"
                },
                color = color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Surface3Color)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedWidth.value)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

// 4. Custom Animated Toggle Switch / Row
@Composable
fun ToggleRow(
    label: String,
    subLabel: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val transition = updateTransition(targetState = checked, label = "ToggleState")
    val thumbOffset by transition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) },
        label = "ThumbOffset"
    ) { state ->
        if (state) 20.dp else 0.dp
    }
    val trackColor by transition.animateColor(label = "TrackColor") { state ->
        if (state) AccentBlue else Surface3Color
    }

    Row(
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = TextPrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (subLabel.isNotEmpty()) {
                Text(text = subLabel, color = TextSecondaryColor, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(trackColor)
                .clickable { onCheckedChange(!checked) }
                .padding(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .shadow(1.dp, CircleShape)
            )
        }
    }
}

// 5. High-Performance Instant Vector Transit Map (0ms Load Time, Premium Aesthetics)
@Composable
fun VectorTransitMap(
    progressPct: Float,
    isMultipleBuses: Boolean,
    multipleBusOffsets: List<Pair<Float, Float>>,
    stops: List<RouteStop>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceColor)
    ) {
        val widthDp = maxWidth
        val heightDp = maxHeight
        
        val infiniteTransition = rememberInfiniteTransition(label = "MapPulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulseScale"
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulseAlpha"
        )
        
        val routeCoords = listOf(
            -1.2825f to 36.7450f, // Kawangware Stop
            -1.2800f to 36.7600f,
            -1.2915f to 36.7845f, // Kilimani (Parent Stop)
            -1.2880f to 36.7980f,
            -1.2635f to 36.8040f, // Westlands Stop
            -1.2720f to 36.7820f, // School (St. Mary's)
            -1.2985f to 36.8125f  // Upper Hill Stop
        )

        val accentBlue = AccentBlue
        val greenAccent = GreenAccent
        val redAccent = RedAccent
        val amberAccent = AmberAccent
        val purpleAccent = PurpleAccent
        val surface3 = Surface3Color
        val bgCol = BackgroundColor
        
        val stopsData = listOf(
            Triple("Kawangware", -1.2825f to 36.7450f, greenAccent),
            Triple("Westlands", -1.2635f to 36.8040f, accentBlue),
            Triple("Kilimani", -1.2915f to 36.7845f, redAccent),
            Triple("Upper Hill", -1.2985f to 36.8125f, amberAccent),
            Triple("School Academy", -1.2720f to 36.7820f, purpleAccent)
        )
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val widthPx = size.width
            val heightPx = size.height
            
            fun mapToOffset(lat: Float, lng: Float): Offset {
                val minLat = -1.3030f
                val maxLat = -1.2580f
                val minLng = 36.7380f
                val maxLng = 36.8180f
                
                val x = ((lng - minLng) / (maxLng - minLng)) * widthPx
                val y = ((maxLat - lat) / (maxLat - minLat)) * heightPx
                return Offset(x, y)
            }
            
            // Draw visual tech-grid system
            val gridSpacing = 40.dp.toPx()
            val gridColor = Color(0x0F000000)
            if (gridSpacing > 1f && widthPx > 0f) {
                var currentX = 0f
                while (currentX < widthPx) {
                    drawLine(gridColor, Offset(currentX, 0f), Offset(currentX, heightPx), strokeWidth = 1.dp.toPx())
                    currentX += gridSpacing
                }
            }
            if (gridSpacing > 1f && heightPx > 0f) {
                var currentY = 0f
                while (currentY < heightPx) {
                    drawLine(gridColor, Offset(0f, currentY), Offset(widthPx, currentY), strokeWidth = 1.dp.toPx())
                    currentY += gridSpacing
                }
            }
            
            // Draw Route Paths
            val pathPoints = routeCoords.map { mapToOffset(it.first, it.second) }
            
            // Neon Glow Layer
            for (i in 0 until pathPoints.size - 1) {
                drawLine(
                    color = accentBlue.copy(alpha = 0.12f),
                    start = pathPoints[i],
                    end = pathPoints[i + 1],
                    strokeWidth = 12.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            // Standard Path Track
            for (i in 0 until pathPoints.size - 1) {
                drawLine(
                    color = surface3,
                    start = pathPoints[i],
                    end = pathPoints[i + 1],
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            // Completed Active Progress path
            val activeSegmentLimit = (pathPoints.size - 1) * progressPct
            for (i in 0 until pathPoints.size - 1) {
                if (i < activeSegmentLimit.toInt()) {
                    drawLine(
                        color = accentBlue,
                        start = pathPoints[i],
                        end = pathPoints[i + 1],
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                } else if (i == activeSegmentLimit.toInt()) {
                    val segmentProgress = activeSegmentLimit - i
                    val interpolatedEnd = Offset(
                        pathPoints[i].x + (pathPoints[i+1].x - pathPoints[i].x) * segmentProgress,
                        pathPoints[i].y + (pathPoints[i+1].y - pathPoints[i].y) * segmentProgress
                    )
                    drawLine(
                        color = accentBlue,
                        start = pathPoints[i],
                        end = interpolatedEnd,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            
            // Draw stops circle highlights and names
            stopsData.forEach { stop ->
                val pos = mapToOffset(stop.second.first, stop.second.second)
                
                // Pulsing radiant halo
                drawCircle(
                    color = stop.third.copy(alpha = pulseAlpha * 0.35f),
                    radius = 16.dp.toPx() * pulseScale,
                    center = pos
                )
                
                // Backing
                drawCircle(
                    color = bgCol,
                    radius = 6.dp.toPx(),
                    center = pos
                )
                
                // Ring border
                drawCircle(
                    color = stop.third,
                    radius = 6.dp.toPx(),
                    center = pos,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            
            // Draw live bus markers
            if (!isMultipleBuses) {
                val busLatLng = getInterpolatedLatLngInKotlin(progressPct)
                val busPos = mapToOffset(busLatLng.first, busLatLng.second)
                
                // Pulse
                drawCircle(
                    color = accentBlue.copy(alpha = pulseAlpha * 0.45f),
                    radius = 20.dp.toPx() * pulseScale,
                    center = busPos
                )
                
                // White backing
                drawCircle(
                    color = Color.White,
                    radius = 9.dp.toPx(),
                    center = busPos
                )
                
                // Blue core
                drawCircle(
                    color = accentBlue,
                    radius = 7.dp.toPx(),
                    center = busPos
                )
            } else {
                val adminColors = listOf(accentBlue, amberAccent, greenAccent, purpleAccent)
                val activeBusesCount = multipleBusOffsets.size.coerceAtMost(4)
                for (i in 0 until activeBusesCount) {
                    val busPct = multipleBusOffsets[i].first
                    val busColor = adminColors.getOrElse(i) { accentBlue }
                    val busLatLng = getInterpolatedLatLngInKotlin(busPct)
                    val busPos = mapToOffset(busLatLng.first, busLatLng.second)
                    
                    drawCircle(
                        color = busColor.copy(alpha = pulseAlpha * 0.4f),
                        radius = 16.dp.toPx() * pulseScale,
                        center = busPos
                    )
                    
                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = busPos
                    )
                    
                    drawCircle(
                        color = busColor,
                        radius = 6.dp.toPx(),
                        center = busPos
                    )
                }
            }
        }
        
        // Render precise text label overlays that scale cleanly with screen font densities
        stopsData.forEach { stop ->
            val minLat = -1.3030f
            val maxLat = -1.2580f
            val minLng = 36.7380f
            val maxLng = 36.8180f
            
            val stopLat = stop.second.first
            val stopLng = stop.second.second
            
            val pctX = (stopLng - minLng) / (maxLng - minLng)
            val pctY = (maxLat - stopLat) / (maxLat - minLat)
            
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val labelX = widthDp * pctX
                val labelY = heightDp * pctY
                
                Box(
                    modifier = Modifier
                        .offset(
                            x = labelX - 45.dp,
                            y = labelY - 26.dp
                        )
                        .width(90.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Surface2Color.copy(alpha = 0.85f))
                        .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stop.first,
                        color = TextPrimaryColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

fun getInterpolatedLatLngInKotlin(pct: Float): Pair<Float, Float> {
    val coords = listOf(
        -1.2825f to 36.7450f, // Kawangware Stop
        -1.2800f to 36.7600f,
        -1.2915f to 36.7845f, // Kilimani (Parent Stop)
        -1.2880f to 36.7980f,
        -1.2635f to 36.8040f, // Westlands Stop
        -1.2720f to 36.7820f, // School Academy (St. Mary's)
        -1.2985f to 36.8125f  // Upper Hill Stop
    )
    if (pct <= 0f) return coords.first()
    if (pct >= 1f) return coords.last()
    
    val totalSegments = coords.size - 1
    val rawSegment = pct * totalSegments
    val index = rawSegment.toInt().coerceAtMost(totalSegments - 1)
    val segmentPct = rawSegment - index
    
    val p1 = coords[index]
    val p2 = coords[index + 1]
    
    val lat = p1.first + (p2.first - p1.first) * segmentPct
    val lng = p1.second + (p2.second - p1.second) * segmentPct
    return Pair(lat, lng)
}

// 6. Hero Map Component with Beautiful Nairobi Transit Visualization and Instant Vector Toggle
@Composable
fun MapHero(
    progressPct: Float, // 0.0 to 1.0 representing the position of the bus along the path
    busPlate: String = "KDE 732X",
    statusText: String = "On Time",
    etaMinutes: Int = 12,
    stops: List<RouteStop> = emptyList(),
    modifier: Modifier = Modifier,
    isMultipleBuses: Boolean = false,
    multipleBusOffsets: List<Pair<Float, Float>> = emptyList(), // Custom drift for other buses
    customApiKey: String = ""
) {
    var isVectorMode by remember { mutableStateOf(true) } // Fast vector transit map by default!
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoaded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableStateOf(0) }

    val apiKey = remember(customApiKey) {
        if (customApiKey.isNotBlank()) {
            customApiKey
        } else {
            try {
                com.example.BuildConfig.GOOGLE_MAPS_API_KEY
            } catch (e: Exception) {
                ""
            }
        }
    }

    // Auto-timeout fallback: if page hasn't loaded in 12 seconds, show offline error fallback
    LaunchedEffect(reloadTrigger) {
        kotlinx.coroutines.delay(12000)
        if (!isLoaded) {
            hasError = true
        }
    }

    LaunchedEffect(progressPct, isLoaded) {
        if (isLoaded && !isMultipleBuses) {
            webViewRef?.evaluateJavascript("if (typeof updateProgress === 'function') updateProgress($progressPct);", null)
        }
    }

    LaunchedEffect(multipleBusOffsets, isLoaded) {
        if (isLoaded && isMultipleBuses && multipleBusOffsets.isNotEmpty()) {
            val progressListJson = multipleBusOffsets.map { it.first }.toString()
            webViewRef?.evaluateJavascript("if (typeof updateAdminProgress === 'function') updateAdminProgress($progressListJson);", null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
    ) {
        if (isVectorMode) {
            VectorTransitMap(
                progressPct = progressPct,
                isMultipleBuses = isMultipleBuses,
                multipleBusOffsets = multipleBusOffsets,
                stops = stops,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            key(reloadTrigger) {
                AndroidView(
                    factory = { context ->
                        try {
                            val codeCacheDir = java.io.File(context.cacheDir, "WebView/Default/HTTP Cache/Code Cache")
                            java.io.File(codeCacheDir, "js").mkdirs()
                            java.io.File(codeCacheDir, "wasm").mkdirs()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                            
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                    android.util.Log.d("MapWebView", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                                    return true
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoaded = true
                                    if (isMultipleBuses) {
                                        evaluateJavascript("initAdminBuses();", null)
                                    } else {
                                        evaluateJavascript("updateProgress($progressPct);", null)
                                    }
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    hasError = true
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: android.webkit.WebResourceRequest?,
                                    error: android.webkit.WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        hasError = true
                                    }
                                }
                            }
                            loadDataWithBaseURL("https://appassets.androidplatform.net", getMapHtml(apiKey), "text/html", "UTF-8", null)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 1. Loading Activity Indicator matching transit theme
            if (!isLoaded && !hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = AccentBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading Transit Map...",
                            color = TextPrimaryColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connecting to live GPS tracker...",
                            color = TextSecondaryColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 2. Beautiful Offline Fallback Layout
            if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceColor)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Warning",
                            tint = AmberAccent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Connection Offline",
                            color = TextPrimaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Map couldn't load correctly.",
                            color = TextSecondaryColor,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Surface2Color)
                                .clickable {
                                    hasError = false
                                    isLoaded = false
                                    reloadTrigger++
                                }
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Retry",
                                tint = AccentBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retry Loading",
                                color = AccentBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Overlay Badges (Top-left & Top-right) - Always visible on both map styles
        if (!isMultipleBuses) {
            // Left overlay (Plate + Status)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface2Color.copy(alpha = 0.9f))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GreenAccent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$busPlate • $statusText",
                    color = TextPrimaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Right overlay (ETA Countdown)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentBlue)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${etaMinutes}m",
                    color = BackgroundColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "ETA",
                    color = BackgroundColor.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Admin Map Overlay info
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface2Color.copy(alpha = 0.9f))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "4 ACTIVE FLEET BUSES",
                    color = PurpleAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Map Mode Switcher overlay at the bottom right
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Surface2Color.copy(alpha = 0.9f))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isVectorMode) AccentBlue else Color.Transparent)
                    .clickable { isVectorMode = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "VECTOR",
                    color = if (isVectorMode) BackgroundColor else TextSecondaryColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (!isVectorMode) AccentBlue else Color.Transparent)
                    .clickable { isVectorMode = false }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "GPS LIVE",
                    color = if (!isVectorMode) BackgroundColor else TextSecondaryColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getMapHtml(apiKey: String): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" />
    <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js"></script>
    <style>
        html, body { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #F8FAFC; }
        #map { width: 100%; height: 100%; }
        
        /* Style InfoWindows for Google Maps light mode */
        .gm-style .gm-style-iw-c {
            background-color: #FFFFFF !important;
            border: 1px solid #E2E8F0 !important;
            border-radius: 8px !important;
            padding: 8px !important;
            color: #0F172A !important;
        }
        .gm-style .gm-style-iw-t::after {
            background: #FFFFFF !important;
            box-shadow: none !important;
        }
        .gm-style .gm-style-iw-d {
            overflow: hidden !important;
        }

        /* Leaflet custom light styling */
        .leaflet-container { background: #F8FAFC !important; }
        .leaflet-bar a { background-color: #FFFFFF !important; color: #0F172A !important; border: none !important; }
        .leaflet-bar { border: 1px solid #CBD5E1 !important; box-shadow: 0 1px 3px rgba(0,0,0,0.08) !important; }
        .custom-popup .leaflet-popup-content-wrapper {
            background: #FFFFFF;
            color: #0F172A;
            border: 1px solid #E2E8F0;
            border-radius: 8px;
            font-family: sans-serif;
            font-size: 12px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
        }
        .custom-popup .leaflet-popup-tip {
            background: #FFFFFF;
            border: 1px solid #E2E8F0;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        var map;
        var isGoogleMap = false;
        var mapInitialized = false;
        var busMarker;
        var adminBuses = [];
        var adminColors = ['#4F8EF7', '#F59E0B', '#10B981', '#9333EA'];
        var adminPlates = ['KDE 732X', 'KDE 119A', 'KDE 540R', 'KDE 902Y'];

        var stopsData = [
            { id: "1", name: "Kawangware Stop", lat: -1.2825, lng: 36.7450, color: "#10B981" },
            { id: "2", name: "Westlands Stop", lat: -1.2635, lng: 36.8040, color: "#4F8EF7" },
            { id: "3", name: "Kilimani (Parent Stop)", lat: -1.2915, lng: 36.7845, color: "#EF4444" },
            { id: "4", name: "Upper Hill Stop", lat: -1.2985, lng: 36.8125, color: "#F59E0B" },
            { id: "5", name: "St. Mary's Academy (School)", lat: -1.2720, lng: 36.7820, color: "#9333EA" }
        ];

        var routeCoords = [
            { lat: -1.2825, lng: 36.7450 },
            { lat: -1.2800, lng: 36.7600 },
            { lat: -1.2915, lng: 36.7845 },
            { lat: -1.2880, lng: 36.7980 },
            { lat: -1.2635, lng: 36.8040 },
            { lat: -1.2720, lng: 36.7820 },
            { lat: -1.2985, lng: 36.8125 }
        ];

        var leafletRouteCoords = routeCoords.map(function(c) {
            return [c.lat, c.lng];
        });

        var lightMapStyle = [
            { "elementType": "geometry", "stylers": [{ "color": "#F8FAFC" }] },
            { "elementType": "labels.text.stroke", "stylers": [{ "color": "#FFFFFF" }] },
            { "elementType": "labels.text.fill", "stylers": [{ "color": "#0F172A" }] },
            { "featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{ "color": "#2563EB" }] },
            { "featureType": "poi", "elementType": "labels.text.fill", "stylers": [{ "color": "#64748B" }] },
            { "featureType": "poi.park", "elementType": "geometry", "stylers": [{ "color": "#E2F5EA" }] },
            { "featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{ "color": "#10B981" }] },
            { "featureType": "road", "elementType": "geometry", "stylers": [{ "color": "#FFFFFF" }] },
            { "featureType": "road", "elementType": "geometry.stroke", "stylers": [{ "color": "#E2E8F0" }] },
            { "featureType": "road", "elementType": "labels.text.fill", "stylers": [{ "color": "#475569" }] },
            { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#EFF6FF" }] },
            { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{ "color": "#BFDBFE" }] },
            { "featureType": "road.highway", "elementType": "labels.text.fill", "stylers": [{ "color": "#1D4ED8" }] },
            { "featureType": "transit", "elementType": "geometry", "stylers": [{ "color": "#F1F5F9" }] },
            { "featureType": "transit.station", "elementType": "labels.text.fill", "stylers": [{ "color": "#1E293B" }] },
            { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#E0F2FE" }] },
            { "featureType": "water", "elementType": "labels.text.fill", "stylers": [{ "color": "#0284C7" }] }
        ];

        function initGoogleMap() {
            if (mapInitialized) return;
            isGoogleMap = true;
            mapInitialized = true;

            var mapOptions = {
                center: { lat: -1.2825, lng: 36.7820 },
                zoom: 13,
                styles: lightMapStyle,
                disableDefaultUI: true,
                zoomControl: false,
                gestureHandling: 'greedy'
            };
            map = new google.maps.Map(document.getElementById('map'), mapOptions);

            stopsData.forEach(function(stop) {
                var infoWindow = new google.maps.InfoWindow({
                    content: "<div style='color: #0F172A; background: #FFFFFF; padding: 4px; border-radius: 4px; font-family: sans-serif; font-size: 11px;'><b>" + stop.name + "</b></div>",
                    disableAutoPan: true
                });

                var marker = new google.maps.Marker({
                    position: { lat: stop.lat, lng: stop.lng },
                    map: map,
                    icon: {
                        path: google.maps.SymbolPath.CIRCLE,
                        scale: 7,
                        fillColor: '#FFFFFF',
                        fillOpacity: 1,
                        strokeColor: stop.color,
                        strokeWeight: 3
                    },
                    title: stop.name
                });

                marker.addListener('click', function() {
                    infoWindow.open(map, marker);
                });
            });

            var routePath = new google.maps.Polyline({
                path: routeCoords,
                geodesic: true,
                strokeColor: '#2563EB',
                strokeOpacity: 0.8,
                strokeWeight: 5,
                map: map
            });

            busMarker = new google.maps.Marker({
                position: routeCoords[0],
                map: map,
                icon: {
                    path: google.maps.SymbolPath.CIRCLE,
                    scale: 9,
                    fillColor: '#2563EB',
                    fillOpacity: 1,
                    strokeColor: '#FFFFFF',
                    strokeWeight: 2
                },
                title: "Bus KDE 732X"
            });
        }

        function initLeafletMap() {
            if (mapInitialized && !isGoogleMap) return;
            isGoogleMap = false;
            mapInitialized = true;

            // Clean any residual content
            var mapDiv = document.getElementById('map');
            mapDiv.innerHTML = '';

            try {
                map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([-1.2825, 36.7820], 13);

                L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                    attribution: '&copy; OpenStreetMap &copy; CARTO',
                    subdomains: 'abcd',
                    maxZoom: 20
                }).addTo(map);

                stopsData.forEach(function(stop) {
                    L.circleMarker([stop.lat, stop.lng], {
                        radius: 7,
                        fillColor: '#FFFFFF',
                        color: stop.color,
                        weight: 3,
                        opacity: 1,
                        fillOpacity: 1
                    }).addTo(map).bindPopup("<b>" + stop.name + "</b>", { className: 'custom-popup' });
                });

                L.polyline(leafletRouteCoords, {
                    color: '#4F8EF7',
                    weight: 5,
                    opacity: 0.65
                }).addTo(map);

                busMarker = L.circleMarker(leafletRouteCoords[0], {
                    radius: 9,
                    fillColor: '#4F8EF7',
                    color: '#FFFFFF',
                    weight: 2,
                    opacity: 1,
                    fillOpacity: 1
                }).addTo(map).bindPopup("<b>Bus KDE 732X</b><br>Live Tracking", { className: 'custom-popup' });

                setTimeout(function() {
                    map.invalidateSize();
                }, 250);
            } catch (e) {
                console.error("Leaflet initialization failed: ", e);
            }
        }

        function interpolate(p1, p2, pct) {
            return {
                lat: p1.lat + (p2.lat - p1.lat) * pct,
                lng: p1.lng + (p2.lng - p1.lng) * pct
            };
        }

        function getInterpolatedPoint(coords, pct) {
            if (pct <= 0) return coords[0];
            if (pct >= 1) return coords[coords.length - 1];
            var totalSegments = coords.length - 1;
            var rawSegment = pct * totalSegments;
            var index = Math.floor(rawSegment);
            var segmentPct = rawSegment - index;
            return interpolate(coords[index], coords[index + 1], segmentPct);
        }

        function updateProgress(pct) {
            if (!mapInitialized) return;
            if (isGoogleMap) {
                if (busMarker) {
                    var newPos = getInterpolatedPoint(routeCoords, pct);
                    busMarker.setPosition(newPos);
                    map.panTo(newPos);
                }
            } else {
                if (busMarker) {
                    var newPos = getInterpolatedPoint(routeCoords, pct);
                    busMarker.setLatLng([newPos.lat, newPos.lng]);
                    if (typeof map !== 'undefined' && map) {
                        map.panTo([newPos.lat, newPos.lng], { animate: true });
                    }
                }
                if (typeof map !== 'undefined' && map) {
                    map.invalidateSize();
                }
            }
        }

        function initAdminBuses() {
            if (!mapInitialized) return;

            // Clear single bus marker
            if (busMarker) {
                if (isGoogleMap) {
                    busMarker.setMap(null);
                } else {
                    busMarker.remove();
                }
            }

            // Clear previous admin buses
            if (adminBuses && adminBuses.length > 0) {
                for (var j = 0; j < adminBuses.length; j++) {
                    if (isGoogleMap) {
                        adminBuses[j].setMap(null);
                    } else {
                        adminBuses[j].remove();
                    }
                }
            }
            adminBuses = [];

            for (var i = 0; i < 4; i++) {
                var initialPos = getInterpolatedPoint(routeCoords, (i * 0.25) % 1.0);
                if (isGoogleMap) {
                    var m = new google.maps.Marker({
                        position: initialPos,
                        map: map,
                        icon: {
                            path: google.maps.SymbolPath.CIRCLE,
                            scale: 9,
                            fillColor: adminColors[i],
                            fillOpacity: 1,
                            strokeColor: '#FFFFFF',
                            strokeWeight: 2
                        },
                        title: adminPlates[i]
                    });
                    adminBuses.push(m);
                } else {
                    var m = L.circleMarker([initialPos.lat, initialPos.lng], {
                        radius: 9,
                        fillColor: adminColors[i],
                        color: '#FFFFFF',
                        weight: 2,
                        opacity: 1,
                        fillOpacity: 1
                    }).addTo(map).bindPopup("<b>" + adminPlates[i] + "</b><br>On live route", { className: 'custom-popup' });
                    adminBuses.push(m);
                }
            }

            if (isGoogleMap) {
                map.setView(new google.maps.LatLng(-1.2825, 36.7820));
                map.setZoom(12);
            } else {
                map.setView([-1.2825, 36.7820], 12);
                map.invalidateSize();
            }
        }

        function updateAdminProgress(pctList) {
            if (!mapInitialized || !adminBuses || adminBuses.length === 0) return;

            for (var i = 0; i < pctList.length; i++) {
                if (i < adminBuses.length) {
                    var pct = pctList[i];
                    var newPos = getInterpolatedPoint(routeCoords, pct);
                    if (isGoogleMap) {
                        adminBuses[i].setPosition(newPos);
                    } else {
                        adminBuses[i].setLatLng([newPos.lat, newPos.lng]);
                    }
                }
            }
            if (!isGoogleMap) {
                map.invalidateSize();
            }
        }

        // Global Google Auth Failure callback
        window.gm_authFailure = function() {
            console.warn("Google Maps authentication failed. Falling back to Leaflet map...");
            initLeafletMap();
        };

        // Determine loading strategy
        var apiKey = "$apiKey";
        var hasKey = apiKey && apiKey.length > 0 && !apiKey.includes("YOUR_") && !apiKey.includes("placeholder") && !apiKey.includes("API_KEY");

        if (hasKey) {
            var script = document.createElement('script');
            script.src = "https://maps.googleapis.com/maps/api/js?key=" + apiKey + "&callback=initGoogleMap";
            script.async = true;
            script.defer = true;
            script.onerror = function() {
                console.warn("Google Maps script load error. Falling back to Leaflet...");
                initLeafletMap();
            };
            document.head.appendChild(script);

            // Timeout fallback
            setTimeout(function() {
                if (!mapInitialized) {
                    console.warn("Google Maps initialization timed out. Falling back to Leaflet...");
                    initLeafletMap();
                }
            }, 4000);
        } else {
            initLeafletMap();
        }
    </script>
</body>
</html>
    """.trimIndent()
}

// 10. Reusable ScrollView with custom contentContainerStyle for smooth vertical scrolling
@Composable
fun ScrollView(
    modifier: Modifier = Modifier,
    contentContainerStyle: Modifier = Modifier.padding(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = contentContainerStyle.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

