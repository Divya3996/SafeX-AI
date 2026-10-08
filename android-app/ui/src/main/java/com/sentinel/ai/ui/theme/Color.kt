package com.sentinel.ai.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------------------------
// Muted semantic palette. Security colors are reserved for safe, warning, and dangerous states.
// ---------------------------------------------------------------------------------------------

val SentinelCyan = Color(0xFF35DEBC)
val SentinelGreen = Color(0xFF66B486)
val SentinelYellow = Color(0xFFD2A451)
val SentinelRed = Color(0xFFD96B6B)
val SentinelCritical = SentinelRed

// ---------------------------------------------------------------------------------------------
// Neutral dark surfaces avoid pure black and keep elevation quiet.
// ---------------------------------------------------------------------------------------------

val SentinelBackground = Color(0xFF08151D)
val SentinelSurface = Color(0xFF122936)
val SentinelSurfaceVariant = Color(0xFF1B3544)
val SentinelOutline = Color(0xFF345360)
val SentinelTextPrimary = Color(0xFFEFF9F8)
val SentinelTextSecondary = Color(0xFFA5BFCA)

// ---------------------------------------------------------------------------------------------
// Light theme surface & semantic tokens
//
// Calm, low-saturation Pixel-style neutrals. These only take effect in light mode; the default
// app theme remains dark, so no existing screen changes.
// ---------------------------------------------------------------------------------------------

val SentinelLightBackground = Color(0xFFF3F9F8)
val SentinelLightSurface = Color(0xFFFFFFFF)
val SentinelLightSurfaceVariant = Color(0xFFE2EEED)
val SentinelLightOutline = Color(0xFFC4CEDA)
val SentinelLightTextPrimary = Color(0xFF0E2B34)
val SentinelLightTextSecondary = Color(0xFF4E5C6B)

// ---------------------------------------------------------------------------------------------
// Container tokens (used by both light and dark schemes for primary/secondary/tertiary roles)
// ---------------------------------------------------------------------------------------------

val SentinelPrimaryContainerDark = Color(0xFF164237)
val SentinelOnPrimaryContainerDark = SentinelTextPrimary
val SentinelSecondaryContainerDark = Color(0xFF173B44)
val SentinelOnSecondaryContainerDark = SentinelTextPrimary
val SentinelTertiaryContainerDark = Color(0xFF352224)
val SentinelOnTertiaryContainerDark = Color(0xFFF0C2C2)

val SentinelPrimaryContainerLight = Color(0xFFB5EEE0)
val SentinelOnPrimaryContainerLight = Color(0xFF00210F)
val SentinelSecondaryContainerLight = Color(0xFFCDEAF7)
val SentinelOnSecondaryContainerLight = Color(0xFF001F2B)
val SentinelTertiaryContainerLight = Color(0xFFFFD9EC)
val SentinelOnTertiaryContainerLight = Color(0xFF3A041F)

// Shared error tokens
val SentinelError = SentinelRed
val SentinelErrorContainerDark = Color(0xFF382123)
val SentinelOnErrorContainerDark = Color(0xFFF1C4C4)
val SentinelErrorContainerLight = Color(0xFFFFDAD6)
val SentinelOnErrorContainerLight = Color(0xFF410E0E)
