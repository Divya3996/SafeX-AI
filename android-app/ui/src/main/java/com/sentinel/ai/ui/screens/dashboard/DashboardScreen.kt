package com.sentinel.ai.ui.screens.dashboard

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import com.sentinel.ai.ui.guidance.guidanceTarget
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.ai.core.model.ProtectionDecision
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.ui.components.PremiumListRow
import com.sentinel.ai.ui.components.PremiumPanel
import com.sentinel.ai.ui.components.PremiumSectionTitle
import com.sentinel.ai.ui.components.StatusDot
import com.sentinel.ai.ui.components.riskColor
import com.sentinel.ai.ui.components.RiskState
import com.sentinel.ai.ui.protection.ProtectionSnapshot
import com.sentinel.ai.ui.theme.SentinelSpacing
import com.sentinel.ai.ui.util.SenderPresentation
import com.sentinel.ai.ui.util.resolveSenderPresentation
import com.sentinel.ai.ui.util.toAppLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onThreatSelected: (String) -> Unit,
    onNavigateToScanner: () -> Unit = {},
    onIncidentHelp: () -> Unit = {},
    onFeatureGuide: () -> Unit = {},
    onStory: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(DashboardUiAction.RefreshStatus)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DashboardContent(
        onStory = onStory,
        onFeatureGuide = onFeatureGuide,
        onIncidentHelp = onIncidentHelp,
        uiState = uiState,
        onAction = viewModel::onAction,
        onThreatSelected = onThreatSelected,
        onNavigateToScanner = onNavigateToScanner,
        appLabelResolver = { source -> source.toAppLabel(context) },
        senderPresentationResolver = { name, identifier ->
            resolveSenderPresentation(
                context = context,
                senderDisplayName = name,
                senderIdentifier = identifier
            )
        }
    )
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun DashboardContent(
    uiState: DashboardUiState,
    onAction: (DashboardUiAction) -> Unit,
    onThreatSelected: (String) -> Unit,
    onNavigateToScanner: () -> Unit,
    appLabelResolver: (String) -> String,
    senderPresentationResolver: (String?, String?) -> SenderPresentation,
    modifier: Modifier = Modifier,
    onIncidentHelp: () -> Unit = {},
    onFeatureGuide: () -> Unit = {},
    onStory: () -> Unit = {}
) {
    val status = dashboardStatus(uiState.protection)
    val display by com.sentinel.ai.core.feature.DisplayPreferences.settings.collectAsState()
    val timeFormatter = remember(display.language) { SimpleDateFormat("h:mm a", Locale.forLanguageTag(display.language.tag)) }

    Column(
        modifier = modifier
            .fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = SentinelSpacing.MD, bottom = SentinelSpacing.XXL),
        verticalArrangement = Arrangement.spacedBy(SentinelSpacing.LG)

    ) {
        Column {
            DashboardHeader(status = status)
        }

        Column {
            ProtectionCard(
                status = status,
                enabled = uiState.protection.protectionEnabled,
                onToggle = { onAction(DashboardUiAction.ToggleGuard) }
            )
        }

        Column {
            PremiumSectionTitle(text = "Recent Activity")
        }

        Column {
            PremiumPanel {
                if (uiState.recentScans.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "No recent scans",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Completed link scans will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    uiState.recentScans.take(3).forEachIndexed { index, scan ->
                        RecentActivityRow(
                            scan = scan,
                            timeLabel = timeFormatter.format(Date(scan.timestamp)),
                            onClick = { onThreatSelected(scan.id) }
                        )
                        if (index < uiState.recentScans.take(3).lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }

        Column {
            PremiumSectionTitle(text = "Quick actions")
        }

        Column {
            PremiumPanel {
                DashboardActionRow(
                    title = "Review the whole situation", description = "Connect messages, screenshots, links and QR clues privately",
                    icon = Icons.Filled.Shield, onClick = onStory
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DashboardActionRow(
                    title = "Analyze content",
                    description = "Check a message, link, screenshot or QR code",
                    icon = Icons.Filled.Link,
                    onClick = onNavigateToScanner
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DashboardActionRow(
                    title = "Help after a scam", description = "Steps for clicks, shared secrets and payments",
                    icon = Icons.Filled.Shield, onClick = onIncidentHelp
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DashboardActionRow(title = "Learn SafeX AI", description = "Replay the feature tour or read step-by-step guidance anytime.",
                    icon = Icons.Filled.Shield, onClick = onFeatureGuide)
            }
        }

        uiState.error?.let { message ->
            Column {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun RecentActivityRow(
    scan: ScanResult,
    timeLabel: String,
    onClick: () -> Unit
) {
    val color = riskColor(scan.riskLevel)
    val status = when (scan.decision) {
        ProtectionDecision.ALLOW -> "Low risk"
        ProtectionDecision.WARN -> "Warning"
        ProtectionDecision.BLOCK -> "Dangerous"
    }

    PremiumListRow(
        title = scan.target?.takeIf { it.isNotBlank() } ?: scan.source,
        localizeTitle = false,
        description = timeLabel,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.XS)
            ) {
                StatusDot(color = color)
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
            }
        },
        onClick = onClick
    )
}

@Composable
private fun DashboardHeader(status: DashboardStatus) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Device protection",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.XS)
        ) {
            StatusDot(color = status.color)
            Text(
                text = status.topLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProtectionCard(
    status: DashboardStatus,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    PremiumPanel {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(status.color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                com.sentinel.ai.ui.components.SafeXLogo(Modifier.size(52.dp))
            }
            Spacer(modifier = Modifier.height(SentinelSpacing.MD))
            Column(Modifier.fillMaxWidth().guidanceTarget("home.protection"), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = status.mainLabel,
                style = MaterialTheme.typography.displaySmall,
                color = status.color,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(SentinelSpacing.XS))
            Text(
                text = status.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            }
            Spacer(modifier = Modifier.height(SentinelSpacing.LG))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Real-time protection",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (enabled) "On" else "Off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

@Composable
private fun DashboardActionRow(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    PremiumListRow(
        title = title,
        description = description,
        leading = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        },
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        onClick = onClick
    )
}

private data class DashboardStatus(
    val topLabel: String,
    val mainLabel: String,
    val message: String,
    val color: Color
)

private fun dashboardStatus(snapshot: ProtectionSnapshot): DashboardStatus = when {
    !snapshot.protectionEnabled -> DashboardStatus(
        topLabel = "Paused",
        mainLabel = "Paused",
        message = "Real-time protection is turned off.",
        color = riskColor(RiskState.Dangerous)
    )
    snapshot.missingPermissions.isNotEmpty() -> DashboardStatus(
        topLabel = "Setup needed",
        mainLabel = "Ready to scan",
        message = "Manual checks work offline. Enable notification access and warnings for incoming-message protection.",
        color = riskColor(RiskState.Suspicious)
    )
    !snapshot.guardServiceRunning -> DashboardStatus("Reconnecting", "Ready to scan", "Notification listener is disconnected. Manual analysis remains available.", riskColor(RiskState.Suspicious))
    else -> DashboardStatus(
        topLabel = "Enabled",
        mainLabel = "Listening",
        message = "Checking supported incoming notifications locally. Shared and pasted content can be scanned anytime.",
        color = riskColor(RiskState.Safe)
    )
}
