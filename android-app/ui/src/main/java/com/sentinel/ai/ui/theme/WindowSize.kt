package com.sentinel.ai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

// ---------------------------------------------------------------------------------------------
// Adaptive window-size tokens
//
// Lightweight, dependency-free screen-size classification used to drive the adaptive navigation
// shell (bottom bar on phones, navigation rail on tablets). Mirrors the Material 3 window-size
// breakpoints without pulling in an extra artifact.
// ---------------------------------------------------------------------------------------------

/**
 * Coarse classification of the current window width, following Material 3 breakpoints.
 *
 * - [Compact]: phones (< 600.dp) — use a bottom navigation bar.
 * - [Medium]: small tablets / unfoldables (600–839.dp).
 * - [Expanded]: tablets / large screens (>= 840.dp) — use a navigation rail.
 */
enum class SentinelWindowWidthClass {
    Compact,
    Medium,
    Expanded;

    /** True when the layout should use a bottom navigation bar instead of a rail. */
    val isCompact: Boolean get() = this == Compact
}

/**
 * Remembers the current [SentinelWindowWidthClass] based on the window's width in dp.
 *
 * Stateless and recomputed automatically when configuration changes (e.g. rotation, windowing
 * mode on large screens).
 */
@Composable
fun rememberWindowWidthClass(): SentinelWindowWidthClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 600 -> SentinelWindowWidthClass.Compact
        widthDp < 840 -> SentinelWindowWidthClass.Medium
        else -> SentinelWindowWidthClass.Expanded
    }
}
