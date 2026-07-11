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

// 5. Hero Map Component with Beautiful Nairobi Transit Visualization
@Composable
fun MapHero(
    progressPct: Float, // 0.0 to 1.0 representing the position of the bus along the path
    busPlate: String = "KDE 732X",
    statusText: String = "On Time",
    etaMinutes: Int = 12,
    stops: List<RouteStop> = emptyList(),
    modifier: Modifier = Modifier,
    isMultipleBuses: Boolean = false,
    multipleBusOffsets: List<Pair<Float, Float>> = emptyList() // Custom drift for other buses
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoaded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableStateOf(0) }

    val apiKey = remember {
        try {
            com.example.BuildConfig.GOOGLE_MAPS_API_KEY
        } catch (e: Exception) {
            ""
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

        // Overlay Badges (Top-left & Top-right) - Only visible when map is loaded & no error
        if (isLoaded && !hasError) {
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
        html, body { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #0F1117; }
        #map { width: 100%; height: 100%; }
        
        /* Style InfoWindows for Google Maps dark mode */
        .gm-style .gm-style-iw-c {
            background-color: #1C1F2B !important;
            border: 1px solid #2F3347 !important;
            border-radius: 8px !important;
            padding: 8px !important;
        }
        .gm-style .gm-style-iw-t::after {
            background: #1C1F2B !important;
            box-shadow: none !important;
        }
        .gm-style .gm-style-iw-d {
            overflow: hidden !important;
        }

        /* Leaflet custom styling */
        .leaflet-container { background: #0F1117 !important; }
        .leaflet-bar a { background-color: #1C1F2B !important; color: #F0F2FF !important; border: none !important; }
        .leaflet-bar { border: 1px solid #2F3347 !important; box-shadow: none !important; }
        .custom-popup .leaflet-popup-content-wrapper {
            background: #1C1F2B;
            color: #F0F2FF;
            border: 1px solid #2F3347;
            border-radius: 8px;
            font-family: sans-serif;
            font-size: 12px;
        }
        .custom-popup .leaflet-popup-tip {
            background: #1C1F2B;
            border: 1px solid #2F3347;
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

        var darkMapStyle = [
            { "elementType": "geometry", "stylers": [{ "color": "#0F1117" }] },
            { "elementType": "labels.text.stroke", "stylers": [{ "color": "#1C1F2B" }] },
            { "elementType": "labels.text.fill", "stylers": [{ "color": "#F0F2FF" }] },
            { "featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{ "color": "#818CF8" }] },
            { "featureType": "poi", "elementType": "labels.text.fill", "stylers": [{ "color": "#94A3B8" }] },
            { "featureType": "poi.park", "elementType": "geometry", "stylers": [{ "color": "#15202B" }] },
            { "featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{ "color": "#10B981" }] },
            { "featureType": "road", "elementType": "geometry", "stylers": [{ "color": "#1E293B" }] },
            { "featureType": "road", "elementType": "geometry.stroke", "stylers": [{ "color": "#2F3347" }] },
            { "featureType": "road", "elementType": "labels.text.fill", "stylers": [{ "color": "#94A3B8" }] },
            { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#1C1F2B" }] },
            { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{ "color": "#2F3347" }] },
            { "featureType": "road.highway", "elementType": "labels.text.fill", "stylers": [{ "color": "#4F8EF7" }] },
            { "featureType": "transit", "elementType": "geometry", "stylers": [{ "color": "#1F2937" }] },
            { "featureType": "transit.station", "elementType": "labels.text.fill", "stylers": [{ "color": "#E0E7FF" }] },
            { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#0B132B" }] },
            { "featureType": "water", "elementType": "labels.text.fill", "stylers": [{ "color": "#3B82F6" }] }
        ];

        function initGoogleMap() {
            if (mapInitialized) return;
            isGoogleMap = true;
            mapInitialized = true;

            var mapOptions = {
                center: { lat: -1.2825, lng: 36.7820 },
                zoom: 13,
                styles: darkMapStyle,
                disableDefaultUI: true,
                zoomControl: false,
                gestureHandling: 'greedy'
            };
            map = new google.maps.Map(document.getElementById('map'), mapOptions);

            stopsData.forEach(function(stop) {
                var infoWindow = new google.maps.InfoWindow({
                    content: "<div style='color: #F0F2FF; background: #1C1F2B; padding: 4px; border-radius: 4px; font-family: sans-serif; font-size: 11px;'><b>" + stop.name + "</b></div>",
                    disableAutoPan: true
                });

                var marker = new google.maps.Marker({
                    position: { lat: stop.lat, lng: stop.lng },
                    map: map,
                    icon: {
                        path: google.maps.SymbolPath.CIRCLE,
                        scale: 7,
                        fillColor: '#0F1117',
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
                strokeColor: '#4F8EF7',
                strokeOpacity: 0.7,
                strokeWeight: 5,
                map: map
            });

            busMarker = new google.maps.Marker({
                position: routeCoords[0],
                map: map,
                icon: {
                    path: google.maps.SymbolPath.CIRCLE,
                    scale: 9,
                    fillColor: '#4F8EF7',
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

                L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                    attribution: '&copy; OpenStreetMap &copy; CARTO',
                    subdomains: 'abcd',
                    maxZoom: 20
                }).addTo(map);

                stopsData.forEach(function(stop) {
                    L.circleMarker([stop.lat, stop.lng], {
                        radius: 7,
                        fillColor: '#0F1117',
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

// 10. Reusable ScrollView with custom contentContainerStyle for React Native-like scrolling with bottom padding
@Composable
fun ScrollView(
    modifier: Modifier = Modifier,
    contentContainerStyle: Modifier = Modifier.padding(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = contentContainerStyle.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

