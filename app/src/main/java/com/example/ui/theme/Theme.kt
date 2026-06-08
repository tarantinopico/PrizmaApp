package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val PrizmaColorScheme =
  darkColorScheme(
    primary = PrizmaPrimary,
    secondary = PrizmaPrimaryVariant,
    tertiary = PrizmaPrimary,
    background = PrizmaDeepBlack,
    surface = PrizmaSurface,
    surfaceVariant = PrizmaSurfaceVariant,
    onPrimary = PrizmaOnPrimary,
    onSecondary = PrizmaOnPrimary,
    onTertiary = PrizmaOnPrimary,
    onBackground = PrizmaOnBackground,
    onSurface = PrizmaOnSurface,
    onSurfaceVariant = PrizmaOnSurface
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disabling dynamic color to enforce our premium dark theme
  content: @Composable () -> Unit,
) {
  val colorScheme = PrizmaColorScheme // Always use premium dark theme for Prizma

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = PrizmaShapes,
    content = content
  )
}
