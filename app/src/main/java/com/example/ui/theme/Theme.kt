package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkSafiriColors.accentBlue,
    secondary = DarkSafiriColors.moovitOrange,
    tertiary = DarkSafiriColors.greenAccent,
    background = DarkSafiriColors.background,
    surface = DarkSafiriColors.surface,
    surfaceVariant = DarkSafiriColors.surface2,
    onPrimary = DarkSafiriColors.background,
    onSecondary = DarkSafiriColors.background,
    onBackground = DarkSafiriColors.textPrimary,
    onSurface = DarkSafiriColors.textPrimary
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LightSafiriColors.accentBlue,
    secondary = LightSafiriColors.moovitOrange,
    tertiary = LightSafiriColors.greenAccent,
    background = LightSafiriColors.background,
    surface = LightSafiriColors.surface,
    surfaceVariant = LightSafiriColors.surface3,
    onPrimary = LightSafiriColors.surface,
    onSecondary = LightSafiriColors.surface,
    onBackground = LightSafiriColors.textPrimary,
    onSurface = LightSafiriColors.textPrimary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val safiriColors = if (darkTheme) DarkSafiriColors else LightSafiriColors
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  CompositionLocalProvider(LocalSafiriColors provides safiriColors) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}

