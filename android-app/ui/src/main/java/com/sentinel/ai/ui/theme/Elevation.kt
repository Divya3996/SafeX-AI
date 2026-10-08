package com.sentinel.ai.ui.theme

import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------------
// Elevation tokens
//
// Subtle, calm elevation. Material 3 applies elevation as a tonal overlay rather than a hard
// shadow, which fits the "subtle elevation" goal. These values are consumed directly by
// components that opt into explicit elevation (e.g. cards, sheets, FABs).
// ---------------------------------------------------------------------------------------------

object SentinelElevation {
    val None = 0.dp
    val Level1 = 1.dp
    val Level2 = 3.dp
    val Level3 = 6.dp
    val Level4 = 8.dp
    val Level5 = 12.dp

    // Semantic aliases
    val SurfaceResting = Level1
    val SurfaceRaised = Level2
    val CardResting = None
    val CardRaised = Level1
    val CardPress = Level1
    val Dialog = Level4
    val Modal = Level5
}
