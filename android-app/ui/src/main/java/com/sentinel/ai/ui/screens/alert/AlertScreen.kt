package com.sentinel.ai.ui.screens.alert

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.ai.core.model.Alert
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.ui.components.ActionButton
import com.sentinel.ai.ui.components.AnimatedSentinelShield
import com.sentinel.ai.ui.components.RiskState
import com.sentinel.ai.ui.components.SecondaryButton
import com.sentinel.ai.ui.components.EmptyState
import com.sentinel.ai.ui.components.SentinelSectionHeader
import com.sentinel.ai.ui.components.ShieldState
import com.sentinel.ai.ui.components.ThreatCard
import com.sentinel.ai.ui.components.ThreatExplanationCard
import com.sentinel.ai.ui.components.ThreatLevelChip
import com.sentinel.ai.ui.components.riskColor
import com.sentinel.ai.ui.theme.SentinelFull
import com.sentinel.ai.ui.theme.SentinelSize
import com.sentinel.ai.ui.theme.SentinelSpacing
import com.sentinel.ai.ui.util.SenderPresentation
import com.sentinel.ai.ui.util.resolveSenderPresentation
import com.sentinel.ai.ui.util.toAppLabel

@Composable
fun AlertScreen(
    onNavigateToDetails: (String) -> Unit = {},
    viewModel: AlertViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AlertContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateToDetails = onNavigateToDetails,
        appLabelResolver = { source -> source.toAppLabel(context) },
        senderPresentationResolver = { name, identifier ->
            resolveSenderPresentation(context, name, identifier)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertContent(
    uiState: AlertUiState,
    onAction: (AlertUiAction) -> Unit,
    onNavigateToDetails: (String) -> Unit,
    appLabelResolver: (String) -> String,
    senderPresentationResolver: (String?, String?) -> SenderPresentation,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf<Alert?>(uiState.selectedAlert) }
    val alerts = uiState.alerts

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SentinelSpacing.ScreenHorizontal, vertical = SentinelSpacing.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(SentinelSpacing.BetweenSections),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = SentinelSpacing.XXL)
    ) {
        item {
            SentinelSectionHeader(
                title = "Notifications",
                subtitle = "Scanned messages and recent alerts"
            )
        }

        if (alerts.isEmpty()) {
            item {
                EmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    title = "No notifications scanned yet",
                    description = "SafeX AI will list scanned messages here as they arrive."
                )
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                AlertListItem(
                    alert = alert,
                    appLabel = appLabelResolver(alert.title),
                    senderPresentation = senderPresentationResolver(alert.senderDisplayName, alert.senderIdentifier),
                    onSelected = {
                        onAction(AlertUiAction.SelectAlert(alert.id))
                        selected = alert
                    },
                    modifier = Modifier
                )
            }
        }
    }

    if (selected != null) {
        ModalBottomSheet(
            onDismissRequest = {
                selected = null
            },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            NotificationScanSheetContent(
                alert = selected!!,
                appLabel = appLabelResolver(selected!!.title),
                senderPresentation = senderPresentationResolver(selected!!.senderDisplayName, selected!!.senderIdentifier),
                onClose = {
                    selected = null
                },
                onViewDetails = {
                    val id = selected!!.threatId
                    selected = null
                    onNavigateToDetails(id)
                }
            )
        }
    }
}

@Composable
fun NotificationScanSheetContent(
    alert: Alert,
    appLabel: String,
    senderPresentation: SenderPresentation,
    onClose: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = riskStateOfAlert(alert.riskLevel)
    val shieldState = when (state) {
        RiskState.Safe -> ShieldState.Safe
        RiskState.Suspicious -> ShieldState.Warning
        RiskState.Dangerous -> ShieldState.Dangerous
        RiskState.Neutral -> ShieldState.Idle
        RiskState.Scanning -> ShieldState.Scanning
    }
    val accent = riskColor(state)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SentinelSpacing.LG, vertical = SentinelSpacing.MD)
            .padding(bottom = SentinelSpacing.XL),
        verticalArrangement = Arrangement.spacedBy(SentinelSpacing.LG)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)
        ) {
            Box(
                modifier = Modifier
                    .size(SentinelSize.IconXL * 2)
                     .clip(SentinelFull)
                    .background(accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedSentinelShield(
                    state = shieldState,
                    modifier = Modifier.size(SentinelSize.IconXL),
                    contentDescription = when (state) {
                        RiskState.Safe -> "Safe status"
                        RiskState.Suspicious -> "Suspicious status"
                        RiskState.Dangerous -> "Dangerous status"
                        RiskState.Neutral -> "Neutral status"
                        RiskState.Scanning -> "Scanning status"
                    }
                )
            }

            ThreatLevelChip(state = state)

            Text(
                text = senderPresentation.primaryText,
                localize = false,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = appLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        ThreatExplanationCard(
            explanation = alert.summary,
            recommendation = recommendationFor(state)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
        ) {
            ActionButton(
                text = "Close",
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Filled.Notifications
            )
            SecondaryButton(
                text = "View details",
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Filled.Visibility
            )
        }
    }
}

@Composable
private fun AlertListItem(
    alert: Alert,
    appLabel: String,
    senderPresentation: SenderPresentation,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = riskStateOfAlert(alert.riskLevel)
    val accent = riskColor(state)
    val isHighRisk = alert.riskLevel == RiskLevel.CRITICAL || alert.riskLevel == RiskLevel.RED

    Column(modifier = modifier.fillMaxWidth()) {
        if (isHighRisk) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(com.sentinel.ai.ui.theme.SentinelSize.BorderThickness)
                    .background(accent, MaterialTheme.shapes.extraLarge)
            )
        }
        ThreatCard(
            title = senderPresentation.primaryText,
            source = appLabel,
            riskLevel = alert.riskLevel,
            timestampLabel = "",
            description = alert.summary,
            onClick = onSelected
        )
    }
}

private fun riskStateOfAlert(level: RiskLevel): RiskState = when (level) {
    RiskLevel.GREEN -> RiskState.Safe
    RiskLevel.YELLOW -> RiskState.Suspicious
    RiskLevel.RED -> RiskState.Dangerous
    RiskLevel.CRITICAL -> RiskState.Dangerous
}

private fun recommendationFor(state: RiskState): String = when (state) {
    RiskState.Safe -> "This notification looks legitimate. No action needed."
    RiskState.Suspicious -> "Review the sender and avoid tapping any links inside."
    RiskState.Dangerous -> "Do not engage. Block the sender and report the message."
    RiskState.Neutral -> "Monitor this conversation for further signals."
    RiskState.Scanning -> "Scan still in progress."
}
