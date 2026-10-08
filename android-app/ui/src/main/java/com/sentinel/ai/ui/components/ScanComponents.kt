package com.sentinel.ai.ui.components

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sentinel.ai.ui.theme.SentinelElevation
import com.sentinel.ai.ui.theme.SentinelFull
import com.sentinel.ai.ui.theme.SentinelMotion
import com.sentinel.ai.ui.theme.SentinelShapes
import com.sentinel.ai.ui.theme.SentinelSpacing
import com.sentinel.ai.ui.theme.SentinelSize

enum class ScanStepState {
    Pending,
    Active,
    Completed,
    Failed
}

@Composable
fun LoadingShield(
    modifier: Modifier = Modifier,
    loadingText: String? = null,
    progress: Float? = null
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    val description = loadingText ?: "Loading"

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, description)
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)
    ) {
        Box(modifier = Modifier.size(SentinelSize.IconXL), contentAlignment = Alignment.Center) {
            if (progress != null) {
                val clampedProgress = progress.coerceIn(0f, 1f)
                CircularProgressIndicator(
                    progress = { clampedProgress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = SentinelSize.BorderThickness,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = SentinelSize.BorderThickness,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedSentinelShield(
                state = ShieldState.Scanning,
                modifier = Modifier.fillMaxSize(0.6f),
                contentDescription = null
            )
        }

        if (loadingText != null) {
            Text(
                text = loadingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ScanProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    showCircular: Boolean = true,
    title: String? = null,
    subtitle: String? = null
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    val clampedProgress = progress.coerceIn(0f, 1f)
    val percentage = (clampedProgress * 100).toInt()

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, "$title $percentage% complete. $subtitle")
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(modifier = Modifier.size(SentinelSize.IconLarge), contentAlignment = Alignment.Center) {
            if (showCircular) {
                CircularProgressIndicator(
                    progress = { clampedProgress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = SentinelSize.BorderThickness * 2,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LinearProgressIndicator(
            progress = { clampedProgress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ScanChecklistItem(
    title: String,
    state: ScanStepState,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    val indicatorColor = when (state) {
        ScanStepState.Pending -> MaterialTheme.colorScheme.outlineVariant
        ScanStepState.Active -> MaterialTheme.colorScheme.primary
        ScanStepState.Completed -> riskColor(RiskState.Safe)
        ScanStepState.Failed -> riskColor(RiskState.Dangerous)
    }

    val descriptionString = description ?: when (state) {
        ScanStepState.Pending -> "Waiting"
        ScanStepState.Active -> "In progress"
        ScanStepState.Completed -> "Complete"
        ScanStepState.Failed -> "Failed"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SentinelSpacing.XS)
            .semantics(mergeDescendants = true) {
                contentDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, "$title. $descriptionString")
            },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
    ) {
        Box(
            modifier = Modifier.size(SentinelSize.IconMedium),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                ScanStepState.Pending -> SentinelIndicatorDot(color = indicatorColor)
                ScanStepState.Active -> CircularProgressIndicator(
                    modifier = Modifier.size(SentinelSize.IconSmall),
                    strokeWidth = SentinelSize.BorderThickness,
                    color = indicatorColor
                )
                ScanStepState.Completed -> SentinelIndicatorDot(color = indicatorColor)
                ScanStepState.Failed -> SentinelIndicatorDot(color = indicatorColor)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (state == ScanStepState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScanStep(
    stepNumber: Int,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    state: ScanStepState = ScanStepState.Pending
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    val isCompleted = state == ScanStepState.Completed
    val isActive = state == ScanStepState.Active
    val isFailed = state == ScanStepState.Failed

    val circleColor = when {
        isCompleted -> MaterialTheme.colorScheme.primary
        isActive -> MaterialTheme.colorScheme.primary
        isFailed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val textColor = when {
        isFailed -> MaterialTheme.colorScheme.error
        isCompleted || isActive -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SentinelSpacing.XS)
            .semantics(mergeDescendants = true) {
                contentDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, "Step $stepNumber. $title. ${subtitle ?: state.name}")
            },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
    ) {
        Box(
            modifier = Modifier.size(SentinelSize.IconLarge),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = SentinelFull,
                color = if (isActive) circleColor.copy(alpha = 0.16f) else Color.Transparent,
                tonalElevation = SentinelElevation.None
            ) {}

            Text(
                text = stepNumber.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = circleColor,
                textAlign = TextAlign.Center
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = textColor
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

@Composable
fun ScanStatusRow(
    icon: @Composable () -> Unit,
    statusText: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SentinelSpacing.XS)
            .semantics(mergeDescendants = true) {
                val desc = "$statusText"
                if (supportingText != null) {
                    contentDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, "$desc. $supportingText")
                } else {
                    contentDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, desc)
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
    ) {
        Box(
            modifier = Modifier.size(SentinelSize.IconMedium),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (supportingText != null) {
                Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailing != null) {
            Box(modifier = Modifier.size(SentinelSize.IconMedium), contentAlignment = Alignment.Center) {
                trailing()
            }
        }
    }
}
