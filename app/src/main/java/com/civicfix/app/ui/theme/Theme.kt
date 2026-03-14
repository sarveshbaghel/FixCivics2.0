package com.civicfix.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// CivicFix Brand Colors (matching Stitch design)
val CivicFixBlue = Color(0xFF137FEC)
val CivicFixBlueDark = Color(0xFF0F6AD0)
val CivicFixBlueLight = Color(0xFFE8F2FD)

val PendingYellow = Color(0xFFF59E0B)
val ResolvedGreen = Color(0xFF10B981)
val RejectedRed = Color(0xFFEF4444)

private val LightColorScheme = lightColorScheme(
    primary = CivicFixBlue,
    onPrimary = Color.White,
    primaryContainer = CivicFixBlueLight,
    onPrimaryContainer = CivicFixBlueDark,
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1A202C),
    surface = Color.White,
    onSurface = Color(0xFF1A202C),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0),
    error = RejectedRed,
    onError = Color.White,
)

@Composable
fun CivicFixTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = CivicFixBlue.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
