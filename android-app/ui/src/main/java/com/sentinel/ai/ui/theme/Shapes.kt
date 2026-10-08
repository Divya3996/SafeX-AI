package com.sentinel.ai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------------
// Shape tokens
//
// A calm, Pixel-style rounded scale. Values intentionally match Material 3 defaults for the
// standard roles so existing screens do not change appearance. Larger radii (large/extraLarge)
// align with the app's existing 24.dp card corners.
//
//   none        0.dp
//   extraSmall  4.dp
//   small       8.dp
//   medium      12.dp
//   large       16.dp
//   extraLarge  28.dp
// ---------------------------------------------------------------------------------------------

val SentinelNone = RoundedCornerShape(0.dp)
val SentinelExtraSmall = RoundedCornerShape(4.dp)
val SentinelSmall = RoundedCornerShape(8.dp)
val SentinelMedium = RoundedCornerShape(12.dp)
val SentinelLarge = RoundedCornerShape(16.dp)
val SentinelExtraLarge = RoundedCornerShape(28.dp)
val SentinelFull = RoundedCornerShape(50)

val SentinelShapes = Shapes(
    extraSmall = SentinelExtraSmall,
    small = SentinelSmall,
    medium = SentinelMedium,
    large = SentinelLarge,
    extraLarge = SentinelExtraLarge
)
