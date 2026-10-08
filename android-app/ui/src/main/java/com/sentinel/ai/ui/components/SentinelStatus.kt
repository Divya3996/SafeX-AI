package com.sentinel.ai.ui.components

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sentinel.ai.ui.theme.SentinelFull
import com.sentinel.ai.ui.theme.SentinelShapes
import com.sentinel.ai.ui.theme.SentinelSize
import com.sentinel.ai.ui.theme.SentinelSpacing

@Composable
fun RiskBadge(
    riskLevel: com.sentinel.ai.core.model.RiskLevel,
    modifier: Modifier = Modifier
) {
    val baseColor = riskColor(riskLevel)

    BadgeContent(
        label = riskLevel.displayLabel(),
        state = when (riskLevel) {
            com.sentinel.ai.core.model.RiskLevel.GREEN -> RiskState.Safe
            com.sentinel.ai.core.model.RiskLevel.YELLOW -> RiskState.Suspicious
            com.sentinel.ai.core.model.RiskLevel.RED -> RiskState.Dangerous
            com.sentinel.ai.core.model.RiskLevel.CRITICAL -> RiskState.Dangerous
        },
        baseColor = baseColor,
        modifier = modifier
    )
}

@Composable
fun StatusChip(
    state: RiskState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    val baseColor = riskColor(state)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val semanticsModifier = modifier.semantics(mergeDescendants = true) {
        contentDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, state.semanticLabel())
        if (onClick != null) {
            role = Role.Button
        }
    }

    val chipModifier = if (onClick != null) {
        semanticsModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        semanticsModifier
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = baseColor.copy(alpha = if (isPressed) 0.24f else 0.16f),
        label = "status-chip-container"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = baseColor,
        label = "status-chip-content"
    )

    Box(
        modifier = chipModifier
            .clip(MaterialTheme.shapes.small)
            .background(animatedContainerColor)
            .padding(
                horizontal = SentinelSpacing.SM,
                vertical = if (isPressed) SentinelSpacing.XXS + SentinelSpacing.XXS else SentinelSpacing.XXS
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = state.displayLabel(),
            style = MaterialTheme.typography.labelLarge,
            color = animatedContentColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SecurityIndicator(
    state: RiskState,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    val baseColor = riskColor(state)
    val description = contentDescription ?: state.semanticLabel()

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                this.contentDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, description)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.XS)
    ) {
        SentinelIndicatorDot(color = baseColor)
        Text(
            text = state.displayLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ThreatLevelChip(
    state: RiskState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    val baseColor = riskColor(state)
    val icon = state.threatIcon()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val semanticsModifier = modifier.semantics(mergeDescendants = true) {
        contentDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, state.semanticLabel())
        if (onClick != null) {
            role = Role.Button
        }
    }

    val chipModifier = if (onClick != null) {
        semanticsModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        semanticsModifier
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = baseColor.copy(alpha = if (isPressed) 0.24f else 0.16f),
        label = "threat-chip-container"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = baseColor,
        label = "threat-chip-content"
    )

    Box(
        modifier = chipModifier
            .clip(MaterialTheme.shapes.small)
            .background(animatedContainerColor)
            .padding(
                horizontal = SentinelSpacing.SM,
                vertical = if (isPressed) SentinelSpacing.XXS + SentinelSpacing.XXS else SentinelSpacing.XXS
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.XS)
        ) {
            Surface(
                color = animatedContentColor,
                modifier = Modifier.size(SentinelSize.IndicatorDot),
                shape = SentinelFull
            ) {}
            Text(
                text = icon,
                style = MaterialTheme.typography.labelLarge,
                color = animatedContentColor
            )
        }
    }
}

@Composable
private fun BadgeContent(
    label: String,
    state: RiskState,
    baseColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedContainerColor by animateColorAsState(
        targetValue = baseColor.copy(alpha = 0.16f),
        label = "badge-container"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = baseColor,
        label = "badge-content"
    )

    Text(
        text = label,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(animatedContainerColor)
            .padding(horizontal = SentinelSpacing.SM, vertical = SentinelSpacing.XXS),
        style = MaterialTheme.typography.labelLarge,
        color = animatedContentColor
    )
}

internal fun RiskState.displayLabel(): String = when (this) {
    RiskState.Safe -> "Safe"
    RiskState.Suspicious -> "Suspicious"
    RiskState.Dangerous -> "Dangerous"
    RiskState.Neutral -> "Neutral"
    RiskState.Scanning -> "Scanning"
}

internal fun RiskState.semanticLabel(): String = when (this) {
    RiskState.Safe -> "Safe status"
    RiskState.Suspicious -> "Suspicious status"
    RiskState.Dangerous -> "Dangerous status"
    RiskState.Neutral -> "Neutral status"
    RiskState.Scanning -> "Scanning status"
}

internal fun RiskState.threatIcon(): String = when (this) {
    RiskState.Safe -> "\u2713"
    RiskState.Suspicious -> "\u26A0"
    RiskState.Dangerous -> "\u2716"
    RiskState.Neutral -> "\u2014"
    RiskState.Scanning -> "\u231B"
}
