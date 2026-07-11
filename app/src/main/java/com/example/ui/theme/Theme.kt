package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = AccentBlue,
    secondary = PurpleAccent,
    tertiary = GreenAccent,
    background = BackgroundColor,
    surface = SurfaceColor,
    onPrimary = BackgroundColor,
    onSecondary = BackgroundColor,
    onBackground = TextPrimaryColor,
    onSurface = TextPrimaryColor
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for Safiri
  dynamicColor: Boolean = false, // Disable dynamic colors to keep Transit App styling intact
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
