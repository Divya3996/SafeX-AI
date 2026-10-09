package com.sentinel.ai.ui.screens.scanner

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sentinel.ai.core.model.*
import com.sentinel.ai.ui.components.riskColor

@Composable
fun AnalysisResultContent(result: ScanResult, onClose: () -> Unit, onOpen: (() -> Unit)? = null, modifier: Modifier = Modifier,
    persistReviews: Boolean = true, onResultUpdated: (ScanResult) -> Unit = {}) {
    val journal by com.sentinel.ai.core.event.ThreatJournal.scanResults.collectAsState()
    val latest = if (persistReviews) journal.firstOrNull { it.id == result.id && it.contextReview != null } ?: result else result
    var displayed by remember(result.id) { mutableStateOf(result) }
    LaunchedEffect(latest) { displayed = ReviewDisplayState.reconcile(displayed, latest) }
    var help by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    if (help) {
        BackHandler { help = false }
        com.sentinel.ai.ui.screens.help.IncidentHelpScreen { help = false }
        return
    }
    val open = onOpen ?: {
        if (DecisionPolicy.canOpen(displayed) && !launchBrowser(context, displayed.target!!))
            android.widget.Toast.makeText(context, com.sentinel.ai.core.i18n.I18n.translate(context, "No external browser available."), android.widget.Toast.LENGTH_LONG).show()
    }
    ResultBody(displayed, onClose, open, modifier, saving, onHelp = { help = true }, onContext = { answers ->
        if (!saving) {
            focus.clearFocus()
            val prior = displayed.contextReview ?: ContextAnswers()
            val merged = ContextAnswers(
                unexpected = prior.unexpected || answers.unexpected,
                asksForSecret = prior.asksForSecret || answers.asksForSecret,
                asksForPayment = prior.asksForPayment || answers.asksForPayment,
                asksToInstall = prior.asksToInstall || answers.asksToInstall,
                pressuresYou = prior.pressuresYou || answers.pressuresYou,
                recipientMismatch = prior.recipientMismatch || answers.recipientMismatch,
                amountMismatch = prior.amountMismatch || answers.amountMismatch)
            val updated = ContextReview.apply(displayed, merged)
            saving = true
            scope.launch {
                try {
                    if (persistReviews) com.sentinel.ai.core.event.ThreatJournal.recordDurably(com.sentinel.ai.core.event.ThreatEvent.LinkThreatDetected(updated))
                    displayed = updated
                    onResultUpdated(updated)
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) {
                    android.widget.Toast.makeText(context, com.sentinel.ai.core.i18n.I18n.translate(context, "Could not save this review. Try again."), android.widget.Toast.LENGTH_LONG).show()
                } finally { saving = false }
            }
        }
    })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultBody(result: ScanResult, onClose: () -> Unit, onOpen: () -> Unit, modifier: Modifier,
    saving: Boolean, onContext: (ContextAnswers) -> Unit, onHelp: () -> Unit) {
    val color = riskColor(result.riskLevel)
    val scroll = rememberScrollState()
    LaunchedEffect(result.contextReview) { if (result.contextReview != null) scroll.scrollTo(0) }
    var confirm by remember(result.id) { mutableStateOf(false) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Open a suspicious link?") },
        text = { Text("This destination has risk signals. Verify it independently before entering any personal or payment information.") },
        confirmButton = { TextButton(onClick = { confirm = false; if (DecisionPolicy.canOpen(result)) onOpen() }) { Text("Open anyway") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Stay here") } })
    Column(modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Analysis result", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, com.sentinel.ai.ui.i18n.localized("Close result")) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .10f))) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(if (result.decision == ProtectionDecision.ALLOW) Icons.Default.VerifiedUser else Icons.Default.Warning, null, tint = color, modifier = Modifier.size(36.dp))
                Text(when (result.decision) { ProtectionDecision.ALLOW -> "No strong risk signals"; ProtectionDecision.WARN -> "Review before acting"; ProtectionDecision.BLOCK -> "High-risk content" },
                    style = MaterialTheme.typography.headlineMedium, color = color)
                Text(result.summary, style = MaterialTheme.typography.bodyLarge)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Risk index ${result.riskScore.toInt()} / 100", style = MaterialTheme.typography.labelLarge)
                    Text("${result.durationMs} ms", style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(progress = { result.riskScore / 100f }, modifier = Modifier.fillMaxWidth(), color = color)
                Text(result.category + if (result.isDemo) " • synthetic sample" else "", style = MaterialTheme.typography.labelMedium)
            }
        }
        Text("Why this result", style = MaterialTheme.typography.titleMedium)
        val reasons = result.reasons.filter { it.source != ScanReasonSource.PROVIDER_STATUS }.map { it.message }.distinct()
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (reasons.isEmpty()) Text("No strong signals were found by the available checks.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                reasons.take(8).forEach { reason ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Info, null, tint = color, modifier = Modifier.size(18.dp))
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        result.linkInspections.orEmpty().take(8).forEach { LinkIdentityCard(it) }
        result.paymentReviews.orEmpty().take(8).forEach { PaymentReviewCard(it, onContext, saving) }
        if (result.contentType != "file") ContextReviewCard(result, onContext, saving)
        OutlinedButton(onClick = onHelp, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Icon(Icons.Default.HealthAndSafety, null); Spacer(Modifier.width(8.dp)); Text("I already clicked or shared information")
        }
        Text("What to do next", style = MaterialTheme.typography.titleMedium)
        Text(result.guidance, style = MaterialTheme.typography.bodyLarge)
        result.target?.let {
            Text(if (result.contentType == "link") "Analyzed destination" else "Analyzed content", style = MaterialTheme.typography.titleSmall)
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(it.take(1800), localize = false, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider()
        Text(result.modelStatus, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(result.coverage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Done") }
        if (DecisionPolicy.canOpen(result)) OutlinedButton(onClick = {
            if (result.decision == ProtectionDecision.WARN) confirm = true else onOpen()
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (result.decision == ProtectionDecision.WARN) "Review opening this link" else "Open in browser")
        }
    }
}

@Composable fun UrlScanResultContent(result: ScanResult, onOpen: () -> Unit, onGoBack: () -> Unit,
    onBypass: () -> Unit, onScanAgain: () -> Unit, modifier: Modifier = Modifier, displaySource: String = result.source) {
    AnalysisResultContent(result, onGoBack, { if (DecisionPolicy.canOpen(result)) onOpen() }, modifier)
}
@Composable fun FileScanResultContent(result: ScanResult, onOpen: () -> Unit, onGoBack: () -> Unit,
    onBypass: () -> Unit, onScanAgain: () -> Unit, modifier: Modifier = Modifier, displaySource: String = result.source) {
    AnalysisResultContent(result.copy(contentType = "file"), onGoBack, modifier = modifier)
}
