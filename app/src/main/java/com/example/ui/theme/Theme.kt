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
    primary = CleanPrimaryDark,
    onPrimary = CleanOnPrimaryDark,
    secondaryContainer = CleanSecondaryContainerDark,
    onSecondaryContainer = CleanOnSecondaryContainerDark,
    background = CleanBackgroundDark,
    onBackground = CleanOnBackgroundDark,
    surface = CleanSurfaceDark,
    onSurface = CleanOnSurfaceDark,
    surfaceVariant = CleanSurfaceVariantDark,
    onSurfaceVariant = CleanOnSurfaceVariantDark,
    outline = CleanOutlineDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CleanPrimaryLight,
    onPrimary = CleanOnPrimaryLight,
    secondaryContainer = CleanSecondaryContainerLight,
    onSecondaryContainer = CleanOnSecondaryContainerLight,
    background = CleanBackgroundLight,
    onBackground = CleanOnBackgroundLight,
    surface = CleanSurfaceLight,
    onSurface = CleanOnSurfaceLight,
    surfaceVariant = CleanSurfaceVariantLight,
    onSurfaceVariant = CleanOnSurfaceVariantLight,
    outline = CleanOutlineLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to strictly enforce the "Clean Minimalism" palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
