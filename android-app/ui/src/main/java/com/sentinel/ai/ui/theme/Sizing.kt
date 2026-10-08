package com.sentinel.ai.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------------
// Component size tokens
//
// Centralized dimensions for reusable components (touch targets, icon sizes, control heights).
// All interactive controls honor the 48.dp minimum touch target from Material 3 accessibility.
// ---------------------------------------------------------------------------------------------

object SentinelSize {
    // Touch targets
    val MinTouchTarget = 48.dp
    val CompactTouchTarget = 40.dp

    // Icon sizes
    val IconXS = 16.dp
    val IconSmall = 20.dp
    val IconMedium = 24.dp
    val IconLarge = 32.dp
    val IconXL = 48.dp

    // Control heights
    val ButtonHeight = MinTouchTarget
    val SmallButtonHeight = CompactTouchTarget
    val TextFieldHeight = 56.dp
    val ChipHeight = 32.dp
    val TopAppBarHeight = 64.dp
    val BottomBarHeight = 80.dp

    // Component specific
    val AvatarSize = 40.dp
    val AvatarSizeLarge = 64.dp
    val FabSize = 56.dp
    val DividerThickness: Dp = 1.dp
    val BorderThickness: Dp = 1.dp
    val IndicatorDot = 10.dp
    val MaxContentWidth = 720.dp
}
