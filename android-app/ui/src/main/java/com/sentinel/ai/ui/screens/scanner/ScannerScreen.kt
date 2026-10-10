package com.sentinel.ai.ui.screens.scanner

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.sentinel.ai.ui.guidance.guidanceTarget
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.ai.core.model.DecisionPolicy

@Composable
fun ScannerScreen(viewModel: ScannerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScannerContent(state, viewModel::onAction)
}

@Composable
fun ScannerContent(uiState: ScannerUiState, onAction: (ScannerUiAction) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { returned ->
        if (returned.resultCode == android.app.Activity.RESULT_OK) {
            returned.data?.getStringExtra("qr_content")?.let { onAction(ScannerUiAction.ScanLiveQr(it)) }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onAction(ScannerUiAction.UpdateInput(it.toString())) }
    }
    when {
        uiState.scanResult != null -> {
            androidx.activity.compose.BackHandler { onAction(ScannerUiAction.ClearResult) }
            val result = uiState.scanResult
            AnalysisResultContent(result, onClose = { onAction(ScannerUiAction.ClearResult) }, onOpen = {
                if (DecisionPolicy.canOpen(result)) {
                    if (launchBrowser(context, result.target!!)) onAction(ScannerUiAction.ClearResult)
                    else Toast.makeText(context, com.sentinel.ai.core.i18n.I18n.translate(context, "No external browser available."), Toast.LENGTH_LONG).show()
                }
            }, modifier)
        }
        uiState.isScanning -> Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
            Spacer(Modifier.height(24.dp))
            Text("Analyzing on your device", style = MaterialTheme.typography.titleLarge)
            Text("Your content stays private.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = { onAction(ScannerUiAction.CancelScan) }) { Text("Cancel") }
        }
        else -> ScanInputContent(uiState.scanType, uiState.scanInput,
            { onAction(ScannerUiAction.UpdateInput(it)) }, { onAction(ScannerUiAction.SetScanType(it)) },
            { onAction(ScannerUiAction.RunScan) }, uiState.error, modifier,
            onLiveQr = { camera.launch(Intent().setClassName(context.packageName, "com.sentinel.ai.protection.qr.LiveQrActivity")) },
            onChooseFile = { picker.launch(if (uiState.scanType in listOf(ScanType.IMAGE, ScanType.QR)) arrayOf("image/*") else arrayOf("*/*")) },
            onSample = { text, type -> onAction(ScannerUiAction.LoadSample(text, type)) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScanInputContent(scanType: ScanType, scanInput: String, onInputChange: (String) -> Unit,
    onTypeChange: (ScanType) -> Unit, onRunScan: () -> Unit, error: String?, modifier: Modifier = Modifier,
    onChooseFile: () -> Unit = {}, onLiveQr: () -> Unit = {}, onSample: (String, ScanType) -> Unit = { _, _ -> }) {
    val context = LocalContext.current
    var help by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    if (help) {
        androidx.activity.compose.BackHandler { help = false }
        com.sentinel.ai.ui.screens.help.IncidentHelpScreen { help = false }
        return
    }
    val language by com.sentinel.ai.core.feature.DisplayPreferences.settings.collectAsState()
    val scamSample = when (language.language) {
        com.sentinel.ai.core.feature.AppLanguage.HINDI -> "बैंक सहायता: तुरंत अपना ओटीपी और पासवर्ड भेजें। केवाईसी के बिना खाता बंद होगा।"
        com.sentinel.ai.core.feature.AppLanguage.GUJARATI -> "બેંક સહાય: હમણાં તમારો ઓટીપી અને પાસવર્ડ મોકલો. કેવાયસી વગર ખાતું બંધ થશે."
        else -> "Bank support: urgent! Share your OTP and password immediately or your account will be blocked."
    }
    val safetySample = when (language.language) {
        com.sentinel.ai.core.feature.AppLanguage.HINDI -> "आपका भुगतान सफल हुआ। अपना ओटीपी या पासवर्ड किसी को न बताएं।"
        com.sentinel.ai.core.feature.AppLanguage.GUJARATI -> "તમારી ચુકવણી સફળ થઈ. તમારો ઓટીપી કે પાસવર્ડ ક્યારેય કોઈને આપશો નહીં."
        else -> "Your payment was successful. Never share your OTP or PIN with anyone."
    }
    val isDocument = scanType in listOf(ScanType.FILE, ScanType.IMAGE, ScanType.QR)
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Check before you trust", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Text("Find phishing and scam signals in the content you receive.", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            AssistChip(onClick = {}, label = { Text("On-device • works offline") }, leadingIcon = { Icon(Icons.Default.Lock, null, Modifier.size(16.dp)) })
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlowRow(modifier = Modifier.guidanceTarget("scan.modes"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanType.entries.forEach { type ->
                        FilterChip(modifier = Modifier.guidanceTarget("scan." + when(type) { ScanType.TEXT -> "message"; ScanType.LINK -> "link"; ScanType.IMAGE -> "screenshot"; ScanType.QR -> "qr"; ScanType.FILE -> "file" }), selected = scanType == type, onClick = { onTypeChange(type) }, label = { Text(label(type)) })
                    }
                }
                if (isDocument) {
                    if (scanType == ScanType.QR) Button(onClick = onLiveQr, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Icon(Icons.Default.QrCodeScanner, null); Spacer(Modifier.width(8.dp)); Text("Scan with camera")
                    }
                    OutlinedButton(onClick = onChooseFile, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Icon(Icons.Default.UploadFile, null); Spacer(Modifier.width(8.dp))
                        Text(if (scanInput.isBlank()) "Choose ${label(scanType).lowercase()}" else "Choose a different file")
                    }
                    if (scanInput.isNotBlank()) Text("Item selected • ready to analyze", color = MaterialTheme.colorScheme.primary)
                    Text(if (scanType == ScanType.FILE) "Checks file type, suspicious names and bounded archive contents. Files are never executed." else
                        "Choose a clear image. Recognition runs locally and the image is not stored.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    OutlinedTextField(value = scanInput, onValueChange = onInputChange, modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (scanType == ScanType.TEXT) "Message to analyze" else "Web address") },
                        placeholder = { Text(if (scanType == ScanType.TEXT) "Paste an unexpected message…" else "https://example.com") },
                        minLines = if (scanType == ScanType.TEXT) 5 else 1, maxLines = if (scanType == ScanType.TEXT) 10 else 3,
                        isError = error != null, shape = MaterialTheme.shapes.medium)
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.let { onInputChange(it.toString()) }
                    }) { Icon(Icons.Default.ContentPaste, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Paste from clipboard") }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                Button(onClick = onRunScan, enabled = scanInput.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Icon(Icons.Default.Shield, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Analyze privately")
                }
            }
        }
        OutlinedButton(onClick = { help = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Default.HealthAndSafety, null); Spacer(Modifier.width(8.dp)); Text("Help after a scam")
        }
        Text("Try a sample", style = MaterialTheme.typography.titleMedium)
        Text("Synthetic examples run through the real detector. No link needs to be opened.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(modifier = Modifier.guidanceTarget("scan.samples"), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SuggestionChip(onClick = { onSample(scamSample, ScanType.TEXT) }, label = { Text("OTP scam") })
            SuggestionChip(onClick = { onSample(safetySample, ScanType.TEXT) }, label = { Text("Safety advice") })
            SuggestionChip(onClick = { onSample("https://paypal-secure.example/verify?redirect=https://example.com", ScanType.LINK) }, label = { Text("Suspicious link") })
            SuggestionChip(onClick = { onSample("https://www.google.com", ScanType.LINK) }, label = { Text("Ordinary link") })
        }
        Text("A low-risk result is not a guarantee of safety. Verify unexpected requests independently.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable internal fun LiveScanContent(scanType: ScanType, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text("Analyzing ${label(scanType).lowercase()} locally…")
    }
}
private fun label(type: ScanType) = when (type) {
    ScanType.TEXT -> "Message"; ScanType.LINK -> "Link"; ScanType.FILE -> "File"; ScanType.IMAGE -> "Screenshot"; ScanType.QR -> "QR code"
}

internal fun launchBrowser(context: Context, url: String): Boolean {
    val uri = Uri.parse(url)
    if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) return false
    val intent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.chrome")
    return runCatching { context.startActivity(intent); true }.getOrElse {
        intent.setPackage(null)
        val handlers = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).filter { it.activityInfo.packageName != context.packageName }
        if (handlers.isEmpty()) false else runCatching {
            // Explicit external intents prevent the selected browser role from routing back to Sentinel.
            val options = handlers.map { Intent(intent).setComponent(ComponentName(it.activityInfo.packageName, it.activityInfo.name)) }
            context.startActivity(Intent.createChooser(options.first(), com.sentinel.ai.core.i18n.I18n.translate(context, "Open with")).putExtra(Intent.EXTRA_INITIAL_INTENTS, options.drop(1).toTypedArray()))
            true
        }.getOrDefault(false)
    }
}
