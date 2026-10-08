package com.sentinel.ai.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

// ---------------------------------------------------------------------------------------------
// Motion tokens
//
// Calm, predictable Motion based on Material 3 guidance. Durations are intentionally restrained
// to keep the UI feeling responsive and premium.
// ---------------------------------------------------------------------------------------------

object SentinelMotion {
    // Durations (milliseconds)
    val DurationShort = 150
    val DurationMedium = 250
    val DurationLong = 350
    val DurationExtraLong = 450

    // Standard easings
    val StandardEasing: Easing = LinearOutSlowInEasing
    val EmphasizedEasing: Easing = FastOutLinearInEasing

    // Tween specs
    val ShortTween = tween<Float>(DurationShort, easing = StandardEasing)
    val MediumTween = tween<Float>(DurationMedium, easing = StandardEasing)
    val LongTween = tween<Float>(DurationLong, easing = StandardEasing)

    val StaggeredFadeIn = tween<Float>(DurationShort, easing = StandardEasing)

    // Spring specs for expressive, natural motion
    val StandardSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val SoftSpring = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
