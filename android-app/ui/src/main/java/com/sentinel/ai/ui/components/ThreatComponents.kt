package com.sentinel.ai.ui.components

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.ui.theme.SentinelFull
import com.sentinel.ai.ui.theme.SentinelMotion
import com.sentinel.ai.ui.theme.SentinelSpacing
import com.sentinel.ai.ui.theme.SentinelSize

/**
 * Summary card for a single threat event.
 */
@Composable
fun ThreatCard(
    title: String,
    source: String,
    riskLevel: RiskLevel,
    timestampLabel: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    SentinelCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    localize = false,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                Text(
                    text = source,
                    localize = false,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(SentinelSpacing.XS))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(SentinelSpacing.SM))
                Text(
                    text = timestampLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RiskBadge(riskLevel = riskLevel)
        }
    }
}

/**
 * Vertical timeline indicator paired with a time label and title.
 */
@Composable
fun ThreatTimelineItem(
    time: String,
    title: String,
    modifier: Modifier = Modifier,
    isLast: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    val semanticsModifier = modifier.semantics(mergeDescendants = true) {
        contentDescription = "$title • $time"
        if (onClick != null) {
            role = Role.Button
        }
    }

    val itemModifier = if (onClick != null) {
        semanticsModifier.clickable(onClick = onClick)
    } else {
        semanticsModifier
    }

    Row(
        modifier = itemModifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(SentinelSize.IconMedium)) {
            Box(
                modifier = Modifier
                    .size(SentinelSize.IconSmall)
                    .clip(SentinelFull)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {}
            if (!isLast) {
                Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                Box(
                    modifier = Modifier
                        .width(SentinelSize.BorderThickness)
                        .height(SentinelSize.IconLarge)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                    localize = false,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * History card with expandable supporting information.
 */
@Composable
fun ThreatHistoryCard(
    summary: String,
    status: RiskState,
    risk: RiskLevel,
    date: String,
    supportingInfo: String,
    modifier: Modifier = Modifier
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    SentinelCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, "$summary. Status: ${status.displayLabel()}. Risk: ${risk.displayLabel()}. Date: $date")
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    StatusChip(state = status)
                    Spacer(modifier = Modifier.height(SentinelSpacing.XS))
                    RiskBadge(riskLevel = risk)
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(SentinelMotion.DurationMedium)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(SentinelMotion.DurationMedium)) + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(SentinelSpacing.SM))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = SentinelSpacing.DividerAlpha))
                    Spacer(modifier = Modifier.height(SentinelSpacing.SM))
                    Text(
                        text = supportingInfo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(SentinelSpacing.SM))
            Text(
                text = if (expanded) "Show less" else "Show more",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .semantics { 
                        role = Role.Button
                        stateDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, if (expanded) "Expanded" else "Collapsed")
                    }
                    .clickable { expanded = !expanded }
                    .padding(vertical = SentinelSpacing.XXS)
                    .fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Card presenting a threat explanation alongside a recommendation.
 */
@Composable
fun ThreatExplanationCard(
    explanation: String,
    recommendation: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    SentinelCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)) {
                if (icon != null) {
                    Box(modifier = Modifier.size(SentinelSize.IconMedium), contentAlignment = Alignment.Center) {
                        icon()
                    }
                }
                Text(
                    text = "Explanation",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(SentinelSpacing.SM))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SentinelSpacing.MD))
            Text(
                text = "Recommendation",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(SentinelSpacing.XS))
            Text(
                text = recommendation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Expandable threat card with hoisted expanded state and animated content size.
 */
@Composable
fun ExpandableThreatCard(
    title: String,
    summary: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    expandedContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    riskLevel: RiskLevel? = null
) {
    val translationContext = androidx.compose.ui.platform.LocalContext.current
    SentinelCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                    localize = false,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (riskLevel != null) {
                    RiskBadge(riskLevel = riskLevel)
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(SentinelMotion.DurationMedium)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(SentinelMotion.DurationMedium)) + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(SentinelSpacing.SM))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = SentinelSpacing.DividerAlpha))
                    Spacer(modifier = Modifier.height(SentinelSpacing.SM))
                    expandedContent()
                }
            }

            Spacer(modifier = Modifier.height(SentinelSpacing.SM))
            Text(
                text = if (isExpanded) "Show less" else "Show more",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .semantics { 
                        role = Role.Button
                        stateDescription = com.sentinel.ai.core.i18n.I18n.translate(translationContext, if (isExpanded) "Expanded" else "Collapsed")
                    }
                    .clickable { onExpandedChange(!isExpanded) }
                    .padding(vertical = SentinelSpacing.XXS)
                    .fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}
