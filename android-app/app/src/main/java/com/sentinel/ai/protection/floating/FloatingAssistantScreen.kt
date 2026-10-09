package com.sentinel.ai.protection.floating

import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sentinel.ai.core.feature.FloatingAssistantControl
import com.sentinel.ai.core.feature.WarningChannels
import com.sentinel.ai.core.model.*
import com.sentinel.ai.ui.components.SafeXLogo
import com.sentinel.ai.ui.i18n.LocalizedText as Text
import com.sentinel.ai.ui.i18n.localized
import com.sentinel.ai.ui.screens.scanner.AnalysisResultContent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingAssistantScreen(state: FloatingSession, model: FloatingSessionViewModel, onClose: () -> Unit,
    onCapture: () -> Unit, onImport: () -> Unit, onEnable: () -> Unit, onNotificationPermission: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var permitted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var canNotify by remember { mutableStateOf(WarningChannels.canPost(context)) }
    val running by FloatingAssistantControl.running.collectAsState()
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { permitted = Settings.canDrawOverlays(context); canNotify = WarningChannels.canPost(context) } }
        owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val scroll = rememberScrollState()
    LaunchedEffect(state.stage) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SafeXLogo(Modifier.size(40.dp))
            Text("Floating assistant", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Close") }
        }
        if (state.stage == FloatingStage.RESULT && state.result != null) {
            Text(if (state.saved) "Saved to local history" else "Private review • not saved", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
            AnalysisResultContent(state.result, onClose, modifier = Modifier.weight(1f), persistReviews = false, onResultUpdated = model::updateResult)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp)) }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = model::save, enabled = !state.saved && !state.saving, modifier = Modifier.weight(1f)) { Text(if (state.saving) "Saving…" else if (state.saved) "Saved" else "Save result") }
                OutlinedButton(onClick = { model.reset() }, modifier = Modifier.weight(1f)) { Text("New scan") }
            }
        } else Column(Modifier.weight(1f).verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.error?.let { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(it, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
            when (state.stage) {
                FloatingStage.SETUP -> {
                    Text("Check suspicious content wherever you encounter it", style = MaterialTheme.typography.headlineMedium)
                    Text("A movable shield opens a private screen crop or pasted-content review. Capturing starts only when you choose it and approve Android's prompt.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Card {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Your choice. Your phone.", style = MaterialTheme.typography.titleMedium)
                            Text("One captured screen is held temporarily in memory so you can crop it. Only selected content is analyzed locally. Images are not saved or uploaded. Results stay private until you tap Save result.")
                            Text("Some apps hide floating buttons or protect their screenshots. Use the original message's Share action or Paste when capture is unavailable.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(if (running) "Floating assistant is running" else "Floating assistant is paused", style = MaterialTheme.typography.titleMedium)
                    Text(if (permitted) "Display permission is enabled" else "Display permission is needed for the shield button", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = if (running) { { FloatingAssistantControl.stop(context) } } else onEnable, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(if (running) "Pause assistant" else "Enable floating assistant") }
                    Text(if (canNotify) "Status and warning notifications are available" else "Notifications are disabled. Enable them for status and background warnings.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onNotificationPermission, modifier = Modifier.fillMaxWidth()) { Text("Manage warning notifications") }
                    HorizontalDivider()
                    Button(onClick = onCapture, modifier = Modifier.fillMaxWidth()) { Text("Scan screen area") }
                    OutlinedButton(onClick = { model.reset("paste") }, modifier = Modifier.fillMaxWidth()) { Text("Paste link or message") }
                    OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Choose screenshot") }
                }
                FloatingStage.PASTE -> {
                    Text("Review a link or message", style = MaterialTheme.typography.headlineMedium)
                    Text("Copy content in the other app, then tap Paste here. SafeX AI never monitors your clipboard.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = {
                        val value = runCatching {
                            val clip = context.getSystemService(android.content.ClipboardManager::class.java).primaryClip
                            if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(context)?.toString() else null
                        }.getOrNull()
                        if (value.isNullOrBlank()) model.error("Clipboard is empty or unavailable. Paste or type the original content.")
                        else model.edit(value)
                    }) { Text("Paste") }
                    ReviewTextField(state.text, model::edit)
                    Button(onClick = { model.analyze() }, enabled = state.text.isNotBlank() && state.text.length <= 12000, modifier = Modifier.fillMaxWidth()) { Text("Analyze privately") }
                    LinkChoices(state.text) { model.analyze(it, "link") }
                }
                FloatingStage.WAITING, FloatingStage.EXTRACTING, FloatingStage.ANALYZING -> {
                    CircularProgressIndicator()
                    Text(when (state.stage) { FloatingStage.WAITING -> "Capturing one frame…"; FloatingStage.EXTRACTING -> "Reading selected content offline…"; else -> "Analyzing selected content…" }, style = MaterialTheme.typography.titleMedium)
                    Text("No screen images are saved. You can cancel at any time.")
                    OutlinedButton(onClick = { model.reset() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }
                FloatingStage.CROP -> {
                    val bitmap = state.bitmap
                    if (bitmap != null) {
                        val selection = state.selection
                        var zoom by remember(bitmap) { mutableStateOf(false) }
                        var precise by remember(bitmap) { mutableStateOf(false) }
                        Text("Crop the captured screen", style = MaterialTheme.typography.headlineMedium)
                        Text("Drag the corners or move the selection. This is a frozen image; screen capture has already stopped.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AndroidView(factory = { ScreenCropView(it) }, update = {
                            it.bitmap = bitmap; it.selection = selection; it.reviewMode = false
                            if (it.zoomed != zoom) it.zoomed = zoom
                            it.onSelection = model::selectCrop
                            it.contentDescription = localizedLabel(context, "Captured image. Adjust the selection using the handles or precise crop controls.")
                        }, modifier = Modifier.fillMaxWidth().height(400.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { zoom = !zoom }) { Text(if (zoom) "Zoom out" else "Zoom in") }
                            TextButton(onClick = { model.selectCrop(ScreenSelection.FULL); zoom = false }) { Text("Use captured image") }
                            TextButton(onClick = { precise = !precise }) { Text("Adjust crop precisely") }
                        }
                        if (precise) {
                            CropEdge("Left edge", selection.left) { model.selectCrop(selection.copy(left = it.coerceAtMost(selection.right - .02f))) }
                            CropEdge("Top edge", selection.top) { model.selectCrop(selection.copy(top = it.coerceAtMost(selection.bottom - .02f))) }
                            CropEdge("Right edge", selection.right) { model.selectCrop(selection.copy(right = it.coerceAtLeast(selection.left + .02f))) }
                            CropEdge("Bottom edge", selection.bottom) { model.selectCrop(selection.copy(bottom = it.coerceAtLeast(selection.top + .02f))) }
                        }
                        Button(onClick = { model.crop(selection) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Read selected area") }
                        OutlinedButton(onClick = { model.reset() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel capture") }
                    }
                }
                FloatingStage.REVIEW -> {
                    Text("Review extracted content", style = MaterialTheme.typography.headlineMedium)
                    Text("Check the spelling before analysis. OCR can change a link; a screenshot does not reveal its hidden destination.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.extraction?.partial == true) Text("Extraction is incomplete. Choose a smaller area or review the selected text carefully.", color = MaterialTheme.colorScheme.error)
                    state.bitmap?.let { bitmap ->
                        AndroidView(factory = { ScreenCropView(it) }, update = {
                            it.bitmap = bitmap; it.reviewMode = true; it.lines = state.extraction?.lines.orEmpty()
                            it.selectedLines = state.selectedLines; it.onLine = model::selectLine
                            it.contentDescription = localizedLabel(context, "Selected image. Tap a recognized line or use the text selection list below.")
                            it.invalidate()
                        }, modifier = Modifier.fillMaxWidth().height(220.dp))
                    }
                    var chooseLines by remember { mutableStateOf(false) }
                    TextButton(onClick = { chooseLines = !chooseLines }) { Text("Select recognized lines") }
                    if (chooseLines) state.extraction?.lines.orEmpty().forEachIndexed { index, line ->
                        Row(Modifier.fillMaxWidth().toggleable(index in state.selectedLines, role = Role.Checkbox, onValueChange = { model.selectLine(index) }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(index in state.selectedLines, onCheckedChange = null)
                            Text(line.text, localize = false, modifier = Modifier.weight(1f))
                        }
                    }
                    if (state.selectedLines.isNotEmpty()) TextButton(onClick = model::useAllText) { Text("Use all extracted text") }
                    ReviewTextField(state.text, model::edit)
                    Button(onClick = { model.analyze() }, enabled = state.text.isNotBlank() && state.text.length <= 12000, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.selectedLines.isEmpty()) "Analyze reviewed message" else "Analyze selected text")
                    }
                    LinkChoices(state.text) { model.analyze(it, "link") }
                    state.extraction?.qrCodes.orEmpty().forEach { qr ->
                        Card {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("QR content found", style = MaterialTheme.typography.titleMedium)
                                Text(qr.take(500), localize = false)
                                Button(onClick = { model.analyze(qr, "qr") }, enabled = qr.length <= 12000) { Text("Analyze this QR content") }
                            }
                        }
                    }
                    Text("Only the content you choose is analyzed. Nothing opens automatically.", style = MaterialTheme.typography.bodySmall)
                }
                else -> Unit
            }
        }
    }
}
private fun localizedLabel(context: android.content.Context, text: String) = com.sentinel.ai.core.i18n.I18n.translate(context, text)
@Composable private fun ReviewTextField(value: String, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text("Text to analyze") }, modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp), isError = value.length > 12000)
    Text("${value.length} / 12,000", localize = false, style = MaterialTheme.typography.labelSmall)
}
@Composable private fun LinkChoices(text: String, analyze: (String) -> Unit) {
    val links = remember(text) { MessageSignals.urls(text, limit = 9) }
    if (links.isNotEmpty()) {
        Text("Choose a detected link", style = MaterialTheme.typography.titleMedium)
        Text("This checks the selected link. Surrounding message text is not included.", style = MaterialTheme.typography.bodySmall)
        links.take(8).forEach { link -> OutlinedButton(onClick = { analyze(link) }, modifier = Modifier.fillMaxWidth()) { Text(link.take(240), localize = false) } }
        if (links.size > 8) Text("More than eight links were found. Select or paste additional links separately.", color = MaterialTheme.colorScheme.error)
    }
}
@Composable private fun CropEdge(title: String, value: Float, change: (Float) -> Unit) {
    val label = localizedLabel(LocalContext.current, title)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title); Text("${(value * 100).toInt()}%", localize = false) }
    Slider(value = value.coerceIn(0f, 1f), onValueChange = change, valueRange = 0f..1f, modifier = Modifier.semantics { contentDescription = label })
}
