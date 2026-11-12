package com.legion.winlatorhub.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val SteamBgGradStart = Color(0xFF141F2B)
val SteamBgGradEnd = Color(0xFF0E141B)
val SteamCardBg = Color(0xFF1A2633)
val SteamCardBorder = Color(0xFF263242)
val SteamFocusBlue = Color(0xFF66C0F4)
val SteamGreenPlay = Color(0xFF5CBF26)
val SteamTextPrimary = Color(0xFFF3F3F3)
val SteamTextSecondary = Color(0xFF8B9EAE)
val SteamTopBarBg = Color(0xFF171A21)
val SteamButtonBg = Color(0xFF2B3A4A)
val SteamIdleButtonBg = Color(0xFF1E2833)
val SteamIconBoxBg = Color(0xFF1B2B3C)
val SteamBadgeBg = Color(0xB30F161E)

// Pennelli allocati una volta sola, non a ogni recomposition di ogni card.
val ScreenGradient = Brush.verticalGradient(listOf(SteamBgGradStart, SteamBgGradEnd))
val CardGradient = Brush.verticalGradient(listOf(Color(0xFF263345), Color(0xFF141C24)))
