package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =========================================================================
// SAFIRI & MOOVIT TRANSIT DESIGN SYSTEM COLOR PALETTES
// =========================================================================

enum class AppThemeMode(val title: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

// Static Base Palette Constants for ViewModels & Non-Composable data models
val DefaultAccentBlue = Color(0xFF0E7AF6)
val DefaultGreenAccent = Color(0xFF10B981)
val DefaultAmberAccent = Color(0xFFF59E0B)
val DefaultRedAccent = Color(0xFFEF4444)
val DefaultPurpleAccent = Color(0xFF8B5CF6)
val DefaultMoovitOrange = Color(0xFFFF5B37)
val DefaultBorderColor = Color(0x24FFFFFF)
val DefaultTextPrimaryColor = Color(0xFFF8FAFC)
val DefaultTextSecondaryColor = Color(0xFF94A3B8)
val DefaultTextTertiaryColor = Color(0xFF64748B)
val DefaultSurfaceColor = Color(0xFF181B28)
val DefaultSurface2Color = Color(0xFF212536)
val DefaultSurface3Color = Color(0xFF2C3246)
val DefaultBackgroundColor = Color(0xFF0F121E)

data class SafiriColorScheme(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentBlue: Color,
    val greenAccent: Color,
    val amberAccent: Color,
    val redAccent: Color,
    val purpleAccent: Color,
    val moovitOrange: Color,
    val moovitYellow: Color,
    val moovitHeaderBg: Color,
    val cardBackground: Color,
    val searchBarBackground: Color
)

val DarkSafiriColors = SafiriColorScheme(
    isDark = true,
    background = Color(0xFF0F121E),      // Deep Slate Midnight Canvas
    surface = Color(0xFF181B28),         // Clean Moovit Transit Surface
    surface2 = Color(0xFF212536),        // Elevated Card & Panel Surface
    surface3 = Color(0xFF2C3246),        // Search Input & Chip Surface
    border = Color(0x24FFFFFF),          // Refined 1dp Outlines
    textPrimary = Color(0xFFF8FAFC),     // Bright High-Contrast Text
    textSecondary = Color(0xFF94A3B8),   // Soft Slate Secondary Text
    textTertiary = Color(0xFF64748B),    // Muted Captions & Hints
    accentBlue = Color(0xFF38BDF8),      // Electric Transit Blue
    greenAccent = Color(0xFF10B981),     // Moovit Live Green
    amberAccent = Color(0xFFF59E0B),     // Alert Warning Amber
    redAccent = Color(0xFFEF4444),       // Critical Alert Red
    purpleAccent = Color(0xFFA78BFA),    // Shuttle Route Purple
    moovitOrange = Color(0xFFFF5B37),    // Signature Moovit Brand Coral Orange
    moovitYellow = Color(0xFFFFB800),    // Moovit Transit Gold
    moovitHeaderBg = Color(0xFF141724),  // Deep Dark Moovit App Bar
    cardBackground = Color(0xFF181B28),
    searchBarBackground = Color(0xFF212536)
)

val LightSafiriColors = SafiriColorScheme(
    isDark = false,
    background = Color(0xFFF5F7FA),      // Clean Crisp Moovit Light Background
    surface = Color(0xFFFFFFFF),         // Pure White Cards & Panels
    surface2 = Color(0xFFFFFFFF),        // Elevated White Container
    surface3 = Color(0xFFEDF2F7),        // Search Bar & Filter Chip Container
    border = Color(0xFFE2E8F0),          // Subtle Light Border
    textPrimary = Color(0xFF0F172A),     // Deep Slate Black for Maximum Contrast
    textSecondary = Color(0xFF475569),   // Readable Medium Gray
    textTertiary = Color(0xFF94A3B8),    // Subtle Caption Gray
    accentBlue = Color(0xFF0284C7),      // Deep Moovit Transit Blue
    greenAccent = Color(0xFF059669),     // Deep Emerald Live Indicator
    amberAccent = Color(0xFFD97706),     // High-Contrast Amber
    redAccent = Color(0xFFDC2626),       // Clear Alert Red
    purpleAccent = Color(0xFF7C3AED),    // Rich Shuttle Purple
    moovitOrange = Color(0xFFEA580C),    // Vibrant Moovit Coral Orange
    moovitYellow = Color(0xFFD97706),    // Moovit Amber Gold
    moovitHeaderBg = Color(0xFFFFFFFF),  // Crisp White Moovit Header
    cardBackground = Color(0xFFFFFFFF),
    searchBarBackground = Color(0xFFF1F5F9)
)

val LocalSafiriColors = staticCompositionLocalOf { DarkSafiriColors }

// Composable Dynamic Accessors - Resolve instantly to current theme mode
val BackgroundColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.background

val SurfaceColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.surface

val Surface2Color: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.surface2

val Surface3Color: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.surface3

val BorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.border

val TextPrimaryColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.textPrimary

val TextSecondaryColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.textSecondary

val TextTertiaryColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.textTertiary

val AccentBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.accentBlue

val GreenAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.greenAccent

val AmberAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.amberAccent

val RedAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.redAccent

val PurpleAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.purpleAccent

val MoovitOrange: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSafiriColors.current.moovitOrange

