package com.sentinel.ai.ui.screens.history

import com.sentinel.ai.ui.i18n.LocalizedText as Text
import com.sentinel.ai.ui.guidance.guidanceTarget

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.ai.core.model.ProtectionDecision
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.ui.components.PremiumPanel
import com.sentinel.ai.ui.components.RiskState
import com.sentinel.ai.ui.components.riskColor
import com.sentinel.ai.ui.theme.SentinelSpacing
import com.sentinel.ai.ui.util.SenderPresentation
import com.sentinel.ai.ui.util.resolveSenderPresentation
import com.sentinel.ai.ui.util.toAppLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val formatter = rememberHistoryFormatter()
    val sortedItems = remember(uiState.history) {
        uiState.history.sortedByDescending { it.timestamp }
    }
    var selectedResult by remember { mutableStateOf<ScanResult?>(null) }
    var query by remember { mutableStateOf("") }
    var warningsOnly by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val visibleItems = sortedItems.filter { (!warningsOnly || it.decision != ProtectionDecision.ALLOW) &&
        (query.isBlank() || listOf(it.target, it.source, it.category, it.senderDisplayName, it.explanation, com.sentinel.ai.core.i18n.I18n.translate(context, it.category), com.sentinel.ai.core.i18n.I18n.translate(context, it.explanation)).any { value -> value?.contains(query, true) == true }) }
    if (confirmDelete) androidx.compose.material3.AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text("Delete all history?") }, text = { Text("This removes the saved analyses from this device. This cannot be undone.") },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { viewModel.delete(); selectedResult = null; confirmDelete = false }) { Text("Delete all") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })

    selectedResult?.let { result ->
        ModalBottomSheet(
            onDismissRequest = { selectedResult = null },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            com.sentinel.ai.ui.screens.scanner.AnalysisResultContent(result = result,
                onClose = { selectedResult = null }, modifier = Modifier.fillMaxWidth())

        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = SentinelSpacing.MD)
    ) {
        Text(
            text = "Your checks, saved only on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(SentinelSpacing.MD))
        androidx.compose.material3.OutlinedTextField(value = query, onValueChange = { query = it },
            label = { Text("Search local history") }, singleLine = true, modifier = Modifier.fillMaxWidth().guidanceTarget("history.search"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            androidx.compose.material3.FilterChip(selected = warningsOnly, onClick = { warningsOnly = !warningsOnly }, label = { Text("Warnings only") })
            androidx.compose.material3.TextButton(onClick = { confirmDelete = true }, enabled = sortedItems.isNotEmpty()) { Text("Delete all") }
        }
        if (visibleItems.isEmpty()) {
            EmptyHistory(modifier = Modifier.weight(1f))
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                tonalElevation = 1.dp
            ) {
                LazyColumn {
                    itemsIndexed(
                        items = visibleItems,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val sender = resolveSenderPresentation(
                            context = context,
                            senderDisplayName = item.senderDisplayName,
                            senderIdentifier = item.senderIdentifier
                        )
                        HistoryItemRow(
                            item = item,
                            appLabel = item.source.toAppLabel(context),
                            senderPresentation = sender,
                            timestampLabel = formatter.format(Date(item.timestamp)),
                            onClick = { selectedResult = item },
                            modifier = Modifier.padding(horizontal = SentinelSpacing.MD)
                        )
                        if (index < visibleItems.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 40.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }

        uiState.error?.let { message ->
            Spacer(modifier = Modifier.height(SentinelSpacing.MD))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
internal fun HistoryItemRow(
    item: ScanResult,
    appLabel: String,
    senderPresentation: SenderPresentation,
    timestampLabel: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = riskColor(item.riskLevel)
    val target = historyTarget(item, senderPresentation, appLabel)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = SentinelSpacing.MD),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(44.dp)
                .background(accent, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(SentinelSpacing.SM))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = target,
                localize = false,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.XS)) {
                Text(
                    text = historyStatus(item.decision),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
                if (timestampLabel.isNotBlank()) {
                    Text(
                        text = timestampLabel,
                localize = false,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(SentinelSpacing.SM))
        Text(
            text = "No scan history",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Completed scans will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryResultSheet(
    result: ScanResult,
    onClose: () -> Unit
) {
    val accent = riskColor(result.riskLevel)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = historyStatus(result.decision),
            style = MaterialTheme.typography.headlineSmall,
            color = accent
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Risk score ${result.riskScore.toInt().coerceIn(0, 100)} / 100",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(SentinelSpacing.LG))
        PremiumPanel {
            Text(
                text = result.senderIdentifier ?: result.source,
                localize = false,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(SentinelSpacing.SM))
            Text(
                text = result.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(SentinelSpacing.LG))
        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Close")
        }
    }
}

private fun historyTarget(
    item: ScanResult,
    senderPresentation: SenderPresentation,
    appLabel: String
): String = when {
    !item.target.isNullOrBlank() -> item.target.orEmpty()
    !item.senderIdentifier.isNullOrBlank() -> item.senderIdentifier.orEmpty()
    item.source.startsWith("http", ignoreCase = true) -> item.source
    senderPresentation.primaryText.isNotBlank() && senderPresentation.primaryText != "Unknown sender" ->
        senderPresentation.primaryText
    else -> appLabel
}

private fun historyStatus(decision: ProtectionDecision): String = when (decision) {
    ProtectionDecision.ALLOW -> "LOW RISK"
    ProtectionDecision.WARN -> "WARNING"
    ProtectionDecision.BLOCK -> "DANGEROUS"
}

@Composable
internal fun rememberHistoryFormatter(): SimpleDateFormat {
    val display by com.sentinel.ai.core.feature.DisplayPreferences.settings.collectAsState()
    return remember(display.language) { SimpleDateFormat("MMM d, h:mm a", Locale.forLanguageTag(display.language.tag)) }
}
