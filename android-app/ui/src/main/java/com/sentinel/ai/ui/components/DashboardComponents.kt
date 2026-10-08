package com.sentinel.ai.ui.components

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sentinel.ai.ui.theme.SentinelElevation
import com.sentinel.ai.ui.theme.SentinelMotion
import com.sentinel.ai.ui.theme.SentinelSpacing
import com.sentinel.ai.ui.theme.SentinelSize

/**
 * Displays a large animated protection score inside a circular progress ring.
 *
 * @param score current score on a 0–100 scale
 * @param trend optional trend text rendered above the supporting text
 * @param supportingText optional secondary description below the trend
 * @param contentDescription when null a generic score label is generated
 */
@Composable
fun ScoreCard(
    score: Int,
    modifier: Modifier = Modifier,
    trend: String? = null,
    supportingText: String? = null,
    contentDescription: String? = null
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    val clampedScore = score.coerceIn(0, 100)
    val animatedScore by animateFloatAsState(
        targetValue = clampedScore.toFloat(),
        animationSpec = SentinelMotion.MediumTween,
        label = "score-animation"
    )

    val scoreColor = when {
        clampedScore >= 80 -> riskColor(RiskState.Safe)
        clampedScore >= 50 -> riskColor(RiskState.Suspicious)
        else -> riskColor(RiskState.Dangerous)
    }

    val description = contentDescription ?: "Protection score: $clampedScore out of 100"

    ElevatedSentinelCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            this.contentDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, description)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.LG)
        ) {
            Box(
                modifier = Modifier.size(SentinelSize.IconXL),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedScore / 100f },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = SentinelSize.BorderThickness * 2,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    color = scoreColor,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${animatedScore.toInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                if (trend != null) {
                    Text(
                        text = trend,
                        style = MaterialTheme.typography.titleMedium,
                        color = scoreColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (supportingText != null) {
                    Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * A compact statistic tile with an optional leading icon and subtitle.
 */
@Composable
fun StatisticCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    subtitle: String? = null
) {
    SentinelCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.size(SentinelSize.IconMedium),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Premium Material 3 metric card with a [RiskState]-driven theme-aware accent.
 */
@Composable
fun MetricCard(
    label: String,
    value: String,
    state: RiskState,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    val accent = riskColor(state)

    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.large),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.large,
        tonalElevation = SentinelElevation.None
    ) {
        Column(modifier = Modifier.padding(SentinelSpacing.MD)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SentinelSpacing.SM))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (supportingText != null) {
                Spacer(modifier = Modifier.height(SentinelSpacing.XS))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Protection status card combining an animated shield and a status chip.
 *
 * Supports all [RiskState] values: Safe, Suspicious, Dangerous, Neutral, and Scanning.
 */
@Composable
fun ProtectionStatusCard(
    state: RiskState,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    val shieldState = when (state) {
        RiskState.Safe -> ShieldState.Safe
        RiskState.Suspicious -> ShieldState.Warning
        RiskState.Dangerous -> ShieldState.Dangerous
        RiskState.Neutral -> ShieldState.Idle
        RiskState.Scanning -> ShieldState.Scanning
    }

    ElevatedSentinelCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, "${state.displayLabel()} protection status. $title. $description")
            if (onClick != null) {
                role = Role.Button
            }
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)
        ) {
            Box(modifier = Modifier.size(SentinelSize.IconLarge)) {
                AnimatedSentinelShield(
                    state = shieldState,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = null
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(state = state)
        }
    }
}

/**
 * Large-touch-target action card with an icon, title, and subtitle.
 */
@Composable
fun QuickActionCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    val description = contentDescription ?: "$title. $subtitle"

    SentinelCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            this.contentDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, description)
            role = Role.Button
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SentinelSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)
        ) {
            Box(
                modifier = Modifier.size(SentinelSize.IconLarge),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
