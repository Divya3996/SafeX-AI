package com.sentinel.ai.ui.components

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.ui.theme.SentinelCyan
import com.sentinel.ai.ui.theme.SentinelCritical
import com.sentinel.ai.ui.theme.SentinelGreen
import com.sentinel.ai.ui.theme.SentinelOutline
import com.sentinel.ai.ui.theme.SentinelRed
import com.sentinel.ai.ui.theme.SentinelFull
import com.sentinel.ai.ui.theme.SentinelShapes
import com.sentinel.ai.ui.theme.SentinelSize
import com.sentinel.ai.ui.theme.SentinelSpacing
import com.sentinel.ai.ui.theme.SentinelSurface
import com.sentinel.ai.ui.theme.SentinelSurfaceVariant
import com.sentinel.ai.ui.theme.SentinelYellow

internal fun riskColor(riskLevel: RiskLevel): Color {
    return when (riskLevel) {
        RiskLevel.GREEN -> SentinelGreen
        RiskLevel.YELLOW -> SentinelYellow
        RiskLevel.RED -> SentinelRed
        RiskLevel.CRITICAL -> SentinelCritical
    }
}

internal fun RiskLevel.displayLabel(): String {
    return when (this) {
        RiskLevel.GREEN -> "Low"
        RiskLevel.YELLOW -> "Moderate"
        RiskLevel.RED -> "High"
        RiskLevel.CRITICAL -> "Critical"
    }
}

@Composable
fun SentinelSectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = SentinelCyan
                )
            }
        }
    }
}

@Composable
fun SentinelMetricCard(
    label: String,
    value: String,
    accent: Color,
    supportingText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .border(SentinelSize.BorderThickness, accent.copy(alpha = 0.4f), MaterialTheme.shapes.large),
        color = SentinelSurfaceVariant.copy(alpha = 0.8f)
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
            Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SentinelPill(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        modifier = modifier
            .clip(SentinelShapes.small)
            .background(accent.copy(alpha = 0.15f))
            .border(SentinelSize.BorderThickness, accent.copy(alpha = 0.4f), SentinelShapes.small)
            .padding(horizontal = SentinelSpacing.SM, vertical = SentinelSpacing.XXS),
        color = accent,
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
fun SentinelIndicatorDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(SentinelSize.IndicatorDot)
            .clip(SentinelFull)
            .background(color)
    )
}
