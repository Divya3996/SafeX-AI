package com.sentinel.ai.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.sentinel.ai.ui.theme.SentinelMotion
import com.sentinel.ai.ui.theme.SentinelRed
import com.sentinel.ai.ui.theme.SentinelYellow

enum class ShieldState {
    Idle,
    Scanning,
    Safe,
    Warning,
    Dangerous
}

@Composable
fun SentinelShield(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    val semanticsModifier = if (contentDescription != null) {
        modifier.semantics(mergeDescendants = true) {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        modifier.clearAndSetSemantics { }
    }

    Canvas(modifier = semanticsModifier.fillMaxSize()) {
        val scaleFactor = size.minDimension / 100f
        withTransform({
            translate(
                left = center.x - 50f * scaleFactor,
                top = center.y - 50f * scaleFactor
            )
            scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
        }) {
            drawPath(path = ShieldPathBase, color = tint)
        }
    }
}

@Composable
fun AnimatedSentinelShield(
    state: ShieldState,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    contentDescription: String? = null
) {
    val animatedTint by animateColorAsState(
        targetValue = tint ?: shieldTintFor(state),
        animationSpec = tween(SentinelMotion.DurationMedium, easing = SentinelMotion.StandardEasing),
        label = "shield-tint"
    )

    val infinite = rememberInfiniteTransition(label = "shield-pulse")
    val safeSettle = remember { androidx.compose.animation.core.Animatable(1f) }

    LaunchedEffect(state) {
        if (state == ShieldState.Safe) {
            safeSettle.snapTo(0.86f)
            safeSettle.animateTo(1f, SentinelMotion.StandardSpring)
        }
    }

    val scale: Float
    val glow: Float
    when (state) {
        ShieldState.Safe -> {
            scale = safeSettle.value
            glow = 0f
        }
        else -> {
            val config = pulseConfig(state)
            val s = infinite.animateFloat(
                initialValue = config.minScale,
                targetValue = config.maxScale,
                animationSpec = infiniteRepeatable(
                    animation = tween(config.duration, easing = SentinelMotion.StandardEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shield-scale"
            )
            val g = infinite.animateFloat(
                initialValue = config.minGlow,
                targetValue = config.maxGlow,
                animationSpec = infiniteRepeatable(
                    animation = tween(config.duration, easing = SentinelMotion.StandardEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shield-glow"
            )
            scale = s.value
            glow = g.value
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (glow > 0.001f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val haloScale = size.minDimension / 100f
                repeat(3) { layer ->
                val layerFactor = 1f + (layer + 1) * 0.07f
                val layerAlpha = glow * (1f - layer / 4f) * 0.5f
                val pathScale = haloScale * scale * layerFactor
                withTransform({
                    translate(
                        left = center.x - 50f * pathScale,
                        top = center.y - 50f * pathScale
                    )
                    scale(pathScale, pathScale, pivot = Offset.Zero)
                }) {
                    drawPath(path = ShieldPathBase, color = animatedTint, alpha = layerAlpha)
                }
                }
            }
        }

        SentinelShield(
            modifier = Modifier.fillMaxSize().scale(scale),
            tint = animatedTint,
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun shieldTintFor(state: ShieldState): Color {
    val scheme = MaterialTheme.colorScheme
    return when (state) {
        ShieldState.Idle -> scheme.primary
        ShieldState.Scanning -> scheme.secondary
        ShieldState.Safe -> scheme.primary
        ShieldState.Warning -> SentinelYellow
        ShieldState.Dangerous -> SentinelRed
    }
}

private data class PulseConfig(
    val minScale: Float,
    val maxScale: Float,
    val minGlow: Float,
    val maxGlow: Float,
    val duration: Int
)

private fun pulseConfig(state: ShieldState): PulseConfig = when (state) {
    ShieldState.Idle -> PulseConfig(
        minScale = 0.985f,
        maxScale = 1.02f,
        minGlow = 0.05f,
        maxGlow = 0.18f,
        duration = SentinelMotion.DurationExtraLong
    )
    ShieldState.Scanning -> PulseConfig(
        minScale = 0.97f,
        maxScale = 1.04f,
        minGlow = 0.15f,
        maxGlow = 0.5f,
        duration = SentinelMotion.DurationLong
    )
    ShieldState.Warning -> PulseConfig(
        minScale = 0.96f,
        maxScale = 1.05f,
        minGlow = 0.2f,
        maxGlow = 0.55f,
        duration = SentinelMotion.DurationMedium
    )
    ShieldState.Dangerous -> PulseConfig(
        minScale = 0.95f,
        maxScale = 1.04f,
        minGlow = 0.3f,
        maxGlow = 0.6f,
        duration = SentinelMotion.DurationMedium
    )
    ShieldState.Safe -> PulseConfig(1f, 1f, 0f, 0f, SentinelMotion.DurationMedium)
}

private val ShieldPathBase: Path = createShieldPath(100f)

private fun createShieldPath(size: Float = 100f): Path {
    val path = Path()
    val u = size / 100f
    val x: (Float) -> Float = { it * u }
    val y: (Float) -> Float = { it * u }

    path.moveTo(x(50f), y(4f))
    path.cubicTo(x(78f), y(6f), x(95f), y(18f), x(95f), y(36f))
    path.cubicTo(x(95f), y(56f), x(76f), y(80f), x(50f), y(98f))
    path.cubicTo(x(24f), y(80f), x(5f), y(56f), x(5f), y(36f))
    path.cubicTo(x(5f), y(18f), x(22f), y(6f), x(50f), y(4f))
    path.close()
    return path
}
