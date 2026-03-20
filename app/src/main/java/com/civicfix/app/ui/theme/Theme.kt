package com.civicfix.app.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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

private val DarkColorScheme = darkColorScheme(
    primary = CivicFixBlue,
    onPrimary = Color.White,
    primaryContainer = CivicFixBlueDark,
    onPrimaryContainer = CivicFixBlueLight,
    secondary = Color(0xFF94A3B8),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E293B),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = RejectedRed,
    onError = Color.White,
)

// ── SharedPreferences key for dark mode ──
const val CIVICFIX_THEME_PREFS = "civicfix_theme_prefs"
const val PREF_DARK_MODE = "pref_dark_mode"  // "system", "light", "dark"

/** Observable state holder for the current dark-mode preference across the app. */
object ThemeState {
    var darkModePreference by mutableStateOf("system")
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(CIVICFIX_THEME_PREFS, Context.MODE_PRIVATE)
        darkModePreference = prefs.getString(PREF_DARK_MODE, "system") ?: "system"
    }

    fun setDarkMode(context: Context, value: String) {
        darkModePreference = value
        context.getSharedPreferences(CIVICFIX_THEME_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_DARK_MODE, value)
            .apply()
    }
}

@Composable
fun CivicFixTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Initialize once
    LaunchedEffect(Unit) {
        ThemeState.init(context)
    }

    val systemDark = isSystemInDarkTheme()
    val isDark = when (ThemeState.darkModePreference) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = (if (isDark) Color(0xFF0F172A) else CivicFixBlue).toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
