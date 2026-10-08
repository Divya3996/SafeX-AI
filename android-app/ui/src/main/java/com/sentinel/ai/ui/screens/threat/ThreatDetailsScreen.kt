package com.sentinel.ai.ui.screens.threat

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.Threat
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.ui.components.ErrorState
import com.sentinel.ai.ui.components.InfoRow
import com.sentinel.ai.ui.components.RiskBadge
import com.sentinel.ai.ui.components.SentinelCard
import com.sentinel.ai.ui.components.SentinelSectionHeader
import com.sentinel.ai.ui.components.ThreatExplanationCard
import com.sentinel.ai.ui.components.ThreatLevelChip
import com.sentinel.ai.ui.components.riskColor
import com.sentinel.ai.ui.theme.SentinelSize
import com.sentinel.ai.ui.theme.SentinelShapes
import com.sentinel.ai.ui.theme.SentinelSpacing
import com.sentinel.ai.ui.util.SenderPresentation
import com.sentinel.ai.ui.util.resolveSenderPresentation
import com.sentinel.ai.ui.util.toAppLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatDetailsScreen(
    threatId: String,
    onBack: () -> Unit
) {
    val threat by ThreatJournal.observeThreat(threatId)
        .collectAsStateWithLifecycle(initialValue = ThreatJournal.threatFor(threatId))
    val context = LocalContext.current
    val scans by ThreatJournal.scanResults.collectAsStateWithLifecycle()
    val scan = scans.firstOrNull { it.id == threatId }
    if (scan != null) {
        com.sentinel.ai.ui.screens.scanner.AnalysisResultContent(scan, onBack)
        return
    }

    ThreatDetailsContent(
        threat = threat,
        sourceLabelResolver = { source -> source.toAppLabel(context) },
        senderPresentationResolver = { name, identifier ->
            resolveSenderPresentation(
                context = context,
                senderDisplayName = name,
                senderIdentifier = identifier
            )
        },
        onBack = onBack
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun ThreatDetailsContent(
    threat: Threat?,
    sourceLabelResolver: (String) -> String,
    senderPresentationResolver: (String?, String?) -> SenderPresentation,
    onBack: () -> Unit
) {
    val formatter = rememberTimestampFormatter()
    val accent = threat?.riskLevel?.let { riskColor(it) } ?: Color.Unspecified

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = SentinelSpacing.ScreenHorizontal,
                vertical = SentinelSpacing.ScreenVertical
            ),
        verticalArrangement = Arrangement.spacedBy(SentinelSpacing.BetweenSections)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(SentinelSize.MinTouchTarget)
                    .padding(SentinelSpacing.None),
                content = {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = com.sentinel.ai.ui.i18n.localized("Back")
                    )
                }
            )
            val currentRisk = threat?.riskLevel ?: RiskLevel.GREEN
            RiskBadge(riskLevel = currentRisk)
        }

        Column(verticalArrangement = Arrangement.spacedBy(SentinelSpacing.XS)) {
            Text(
                text = "Threat details",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "Deep-dive view of the selected detection with guidance for response.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (threat == null) {
            ErrorState(
                title = "Threat details unavailable",
                description = "No threat details are available for this item yet.",
                onRetry = onBack
            )
            return
        }

        val selectedThreat = threat
        val suspiciousIndicators = suspiciousIndicatorsFor(selectedThreat)
        val senderPresentation = senderPresentationResolver(
            selectedThreat.senderDisplayName,
            selectedThreat.senderIdentifier
        )

        SentinelCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ThreatLevelChip(state = riskStateOf(selectedThreat.riskLevel))
                        Spacer(modifier = Modifier.height(SentinelSpacing.MD))
                        Text(
                            text = senderPresentation.primaryText,
                localize = false,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        senderPresentation.secondaryText?.let { identifier ->
                            Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                            Text(
                                text = identifier,
                localize = false,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                        Text(
                            text = sourceLabelResolver(selectedThreat.source),
                localize = false,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier.size(SentinelSize.IconXL),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { (selectedThreat.riskScore / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                             strokeWidth = SentinelSize.BorderThickness * 2,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            color = accent,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text(
                            text = selectedThreat.riskScore.toInt().toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(SentinelSpacing.LG))
                InfoRow(
                    label = "Message preview",
                    value = selectedThreat.content,
                    localizeValue = false,
                    showDivider = false
                )
                Spacer(modifier = Modifier.height(SentinelSpacing.SM))
                Text(
                    text = formatter.format(Date(selectedThreat.timestamp)),
                localize = false,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SentinelSectionHeader(
            title = "Threat explanation",
            subtitle = "Why this message was flagged by the security pipeline"
        )
        SentinelCard {
            Text(
                text = selectedThreat.explanation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SentinelSectionHeader(
            title = "Suspicious indicators",
            subtitle = "Signals that contributed to the risk score"
        )
        SentinelCard {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM),
                verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
            ) {
                suspiciousIndicators.forEach { indicator ->
                    Text(indicator, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = SentinelSpacing.XXS))
                }
            }
        }

        SentinelSectionHeader(
            title = "Recommended action",
            subtitle = "A practical next step for the user"
        )
        SentinelCard {
            Text(
                text = selectedThreat.recommendation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SentinelSpacing.MD))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                content = { Text(text = "Return to dashboard") }
            )
        }
    }
}

private fun suspiciousIndicatorsFor(threat: Threat): List<String> {
    val derivedIndicators = buildList {
        val explanation = threat.explanation.lowercase(Locale.getDefault())
        val content = threat.content.lowercase(Locale.getDefault())

        if ("urgent" in explanation || "urgent" in content || "pressure" in explanation) add("Urgency pressure")
        if ("otp" in explanation || "otp" in content || "code" in content) add("Code request")
        if ("payment" in explanation || "payment" in content || "transfer" in content) add("Payment request")
        if ("link" in explanation || "url" in explanation || "http" in content) add("Suspicious link")
        if ("authority" in explanation || "imperson" in explanation) add("Authority impersonation")
    }

    return derivedIndicators
        .distinct()
        .ifEmpty { listOf("Risk scoring triggered", "Message review recommended", "User verification needed") }
}

private fun riskStateOf(level: RiskLevel): com.sentinel.ai.ui.components.RiskState = when (level) {
    RiskLevel.GREEN -> com.sentinel.ai.ui.components.RiskState.Safe
    RiskLevel.YELLOW -> com.sentinel.ai.ui.components.RiskState.Suspicious
    RiskLevel.RED -> com.sentinel.ai.ui.components.RiskState.Dangerous
    RiskLevel.CRITICAL -> com.sentinel.ai.ui.components.RiskState.Dangerous
}

private fun rememberTimestampFormatter(): SimpleDateFormat {
    return SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault())
}
