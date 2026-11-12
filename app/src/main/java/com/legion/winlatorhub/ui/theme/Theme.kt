package com.legion.winlatorhub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SteamColorScheme = darkColorScheme(
    background = SteamBgGradEnd,
    surface = SteamCardBg,
    primary = SteamFocusBlue,
    secondary = SteamGreenPlay,
    onBackground = SteamTextPrimary,
    onSurface = SteamTextPrimary
)

@Composable
fun WinlatorHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SteamColorScheme, typography = Typography, content = content)
}
