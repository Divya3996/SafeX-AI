package com.sentinel.ai.ui.theme

import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------------
// Spacing tokens
//
// Single source of truth for all padding, margin and gap values. Components should reference
// these instead of raw dp literals to keep rhythm and whitespace consistent (and to support
// large-font scaling cleanly).
// ---------------------------------------------------------------------------------------------

object SentinelSpacing {
    val None = 0.dp
    val XXXS = 2.dp
    val XXS = 4.dp
    val XS = 8.dp
    val SM = 12.dp
    val MD = 16.dp
    val LG = 24.dp
    val XL = 32.dp
    val XXL = 48.dp
    val XXXL = 64.dp

    // Semantic groupings
    val ScreenHorizontal = MD   // standard left/right screen padding
    val ScreenVertical = MD     // standard top/bottom screen padding
    val BetweenSections = LG    // gap between major content sections
    val BetweenItems = XS       // gap between related items
    val CardPadding = MD        // internal card padding
    val ListItemGap = XS        // gap between list rows
    val DividerAlpha = 0.2f     // standard divider alpha
}
