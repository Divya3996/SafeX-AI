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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.core.model.*
import com.sentinel.ai.ui.components.SafeXLogo
import com.sentinel.ai.ui.i18n.LocalizedText as Text
import com.sentinel.ai.ui.screens.scanner.AnalysisResultContent

/** The primary action has its own layout area, outside the scrolling content and above the IME. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingAssistantScreen(state: FloatingSession, model: FloatingSessionViewModel, onClose: () -> Unit,
    onCapture: () -> Unit, onImport: () -> Unit, onEnable: () -> Unit, onNotificationPermission: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var permitted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var canNotify by remember { mutableStateOf(WarningChannels.canPost(context)) }
    var channelsEnabled by remember { mutableStateOf(WarningChannels.warningEnabled(context)) }
    val running by FloatingAssistantControl.running.collectAsState()
    var privacyExpanded by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }
    var replaceAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun newInput(action: () -> Unit) { if (state.hasContent && !state.saved) replaceAction = action else action() }
    replaceAction?.let { action ->
        AlertDialog(onDismissRequest = { replaceAction = null }, title = { Text("Start a new input?") },
            text = { Text("Your current review will be replaced when new content is accepted. Cancel the picker or capture prompt to keep it.") },
            confirmButton = { TextButton(onClick = { replaceAction = null; action() }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { replaceAction = null }) { Text("Keep review") } })
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { permitted = Settings.canDrawOverlays(context); canNotify = WarningChannels.canPost(context); channelsEnabled = WarningChannels.warningEnabled(context) } }
        owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val scroll = rememberScrollState()
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(state.stage) { scroll.scrollTo(0) }
    val candidates = remember(state.text) { ContentCandidateExtractor.extract(state.text, 9) }
    var selectedLink by remember(state.id) { mutableStateOf<String?>(null) }
    var selectedQr by remember(state.id) { mutableStateOf<String?>(null) }
    var confirmLink by remember { mutableStateOf<ContentCandidate?>(null) }
    var includeContext by remember { mutableStateOf(false) }
    fun analyzeLink(candidate: ContentCandidate) {
        focus.clearFocus()
        if (candidate.needsConfirmation) confirmLink = candidate else model.analyze(candidate.inspectionValue, "link", includeContext)
    }
    confirmLink?.let { candidate ->
        AlertDialog(onDismissRequest = { confirmLink = null }, title = { Text("Confirm link spelling") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Original visible text"); Text(candidate.original, localize = false)
                Text("Destination to inspect"); Text(candidate.inspectionValue, localize = false)
                Text("SafeX AI normalized visible characters for inspection. Compare this with the original message. Nothing opens automatically.")
            } }, confirmButton = { TextButton(onClick = { confirmLink = null; model.analyze(candidate.inspectionValue, "link", includeContext) }) { Text("Inspect destination") } },
            dismissButton = { TextButton(onClick = { confirmLink = null }) { Text("Keep editing") } })
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SafeXLogo(Modifier.size(32.dp))
            Column(Modifier.weight(1f)) {
                Text("SafeX AI", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("Floating assistant", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.stage != FloatingStage.SETUP) TextButton(onClick = { model.back() }) { Text("Back") }
            TextButton(onClick = onClose) { Text("Close") }
        }
        if (state.stage in setOf(FloatingStage.CROP, FloatingStage.REVIEW, FloatingStage.RESULT)) {
            Text(when (state.stage) { FloatingStage.CROP -> "1 / 3 • Choose an area"; FloatingStage.REVIEW -> "2 / 3 • Review content"; else -> "3 / 3 • Private result" },
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }
        if (state.stage == FloatingStage.RESULT && state.result != null) {
            Text(if (state.saved) "Saved to local history" else "Private review • not saved", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
            AnalysisResultContent(state.result, onClose, modifier = Modifier.weight(1f), persistReviews = false, onResultUpdated = model::updateResult, showChrome = false, compactReasons = true)
        } else Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (state.problem == FloatingProblem.EXTRACTION || state.problem == FloatingProblem.IMAGE) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { newInput(onImport) }) { Text("Choose screenshot") }
                    OutlinedButton(onClick = { newInput { model.open("paste") } }) { Text("Paste link or message") }
                }
            }
            if (state.stage == FloatingStage.SETUP) {
                Text("Protection within reach", style = MaterialTheme.typography.headlineMedium)
                Text("Choose content. Review privately. Decide safely.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.hasContent) Button(onClick = model::resume, modifier = Modifier.fillMaxWidth()) { Text("Resume private review") }
                Text(if (running) "Floating assistant is running" else "Floating assistant is paused", style = MaterialTheme.typography.titleMedium)
                Text(if (permitted) "Display permission is enabled" else "Display permission is needed for the shield button", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { newInput(onCapture) }, modifier = Modifier.fillMaxWidth()) { Text("Scan screen area") }
                OutlinedButton(onClick = { newInput { model.open("paste") } }, modifier = Modifier.fillMaxWidth()) { Text("Paste link or message") }
                OutlinedButton(onClick = { newInput(onImport) }, modifier = Modifier.fillMaxWidth()) { Text("Choose screenshot") }
                TextButton(onClick = { privacyExpanded = !privacyExpanded }) { Text("Privacy and capture limits") }
                if (privacyExpanded) Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your choice. Your phone.", style = MaterialTheme.typography.titleMedium)
                    Text("One captured screen is held temporarily in memory so you can crop it. Only selected content is analyzed locally. Images are not saved or uploaded. Results stay private until you tap Save result.")
                    Text("Some apps hide floating buttons or protect their screenshots. Use the original message's Share action or Paste when capture is unavailable.", style = MaterialTheme.typography.bodySmall)
                } }
                TextButton(onClick = { settingsExpanded = !settingsExpanded }) { Text("Assistant preferences") }
                if (settingsExpanded) {
                    val display by DisplayPreferences.settings.collectAsState()
                    val preferences by FloatingPreferences.settings.collectAsState()
                    Text("App language", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { AppLanguage.entries.forEach { language ->
                        FilterChip(selected = display.language == language, onClick = { DisplayPreferences.setLanguage(context, language) }, label = { Text(language.nativeName, localize = false) })
                    } }
                    Text("Text size", style = MaterialTheme.typography.titleSmall)
                    Text("${(display.textScale * 100).toInt()}%", localize = false)
                    Slider(value = display.textScale, onValueChange = DisplayPreferences::setTextScale, valueRange = .85f..1.5f,
                        modifier = Modifier.semantics { contentDescription = localizedLabel(context, "Text size") })
                    Text("Shield size", style = MaterialTheme.typography.titleSmall)
                    Slider(value = preferences.sizeDp.toFloat(), onValueChange = { FloatingPreferences.setSize(context, it.toInt()) }, valueRange = 52f..72f,
                        modifier = Modifier.semantics { contentDescription = localizedLabel(context, "Shield size") })
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Reduce motion", modifier = Modifier.weight(1f)); Switch(preferences.reducedMotion, { FloatingPreferences.setReducedMotion(context, it) })
                    }
                    Text(if (!canNotify) "Notifications are disabled. Enable them for status and background warnings." else if (channelsEnabled) "Status and warning notifications are available" else "Some warning channels are disabled. Review Android notification settings.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onNotificationPermission, modifier = Modifier.fillMaxWidth()) { Text("Manage warning notifications") }
                    OutlinedButton(onClick = { if (!WarningChannels.test(context)) model.error("Enable warning notifications before testing the sound.") }, modifier = Modifier.fillMaxWidth()) { Text("Test warning sound") }
                    Text("Warning sound follows Android volume, Do Not Disturb and notification channel settings.", style = MaterialTheme.typography.bodySmall)
                }
            }
            when (state.stage) {
                FloatingStage.PASTE -> {
                    Text("Review a link or message", style = MaterialTheme.typography.headlineSmall)
                    Text("Copy content in the other app, then tap Paste here. SafeX AI never monitors your clipboard.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = {
                        val value = runCatching {
                            val clip = context.getSystemService(android.content.ClipboardManager::class.java).primaryClip
                            if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(context)?.toString() else null
                        }.getOrNull()
                        if (value.isNullOrBlank()) model.error("Clipboard is empty or unavailable. Paste or type the original content.") else model.paste(value)
                    }) { Text("Paste") }
                    ReviewTextField(state.text, model::edit)
                    if (candidates.isNotEmpty()) LinkChoices(candidates, selectedLink, { selectedLink = it.inspectionValue; analyzeLink(it) })
                }
                FloatingStage.WAITING, FloatingStage.EXTRACTING, FloatingStage.ANALYZING -> {
                    val preferences by FloatingPreferences.settings.collectAsState()
                    if (preferences.reducedMotion) CircularProgressIndicator(progress = { .5f }) else CircularProgressIndicator()
                    Text(when (state.stage) { FloatingStage.WAITING -> "Capturing one frame…"; FloatingStage.EXTRACTING -> "Reading selected content offline…"; else -> "Analyzing selected content…" }, style = MaterialTheme.typography.titleMedium)
                    Text("No screen images are saved. You can cancel at any time.")
                }
                FloatingStage.CROP -> state.bitmap?.let { bitmap ->
                    var zoom by remember(bitmap) { mutableStateOf(false) }
                    var pan by remember(bitmap) { mutableStateOf(false) }
                    var precise by remember(bitmap) { mutableStateOf(false) }
                    Text("Crop the captured screen", style = MaterialTheme.typography.headlineSmall)
                    Text("Drag the corners or move the selection. This is a frozen image; screen capture has already stopped.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AndroidView(factory = { ScreenCropView(it) }, update = {
                        it.bitmap = bitmap; it.selection = state.selection; it.reviewMode = false; it.panMode = pan
                        if (it.zoomed != zoom) it.zoomed = zoom
                        it.onSelection = model::selectCrop; it.contentDescription = localizedLabel(context, "Captured image. Adjust the selection using the handles or precise crop controls.")
                    }, modifier = Modifier.fillMaxWidth().height(300.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { zoom = !zoom; if (!zoom) pan = false }) { Text(if (zoom) "Zoom out" else "Zoom in") }
                        if (zoom) FilterChip(pan, { pan = !pan }, label = { Text("Pan image") })
                        TextButton(onClick = { model.selectCrop(ScreenSelection.FULL); zoom = false; pan = false }) { Text("Use captured image") }
                        TextButton(onClick = { precise = !precise }) { Text("Adjust crop precisely") }
                    }
                    if (precise) {
                        CropEdge("Left edge", state.selection.left) { model.selectCrop(state.selection.copy(left = it.coerceAtMost(state.selection.right - .02f))) }
                        CropEdge("Top edge", state.selection.top) { model.selectCrop(state.selection.copy(top = it.coerceAtMost(state.selection.bottom - .02f))) }
                        CropEdge("Right edge", state.selection.right) { model.selectCrop(state.selection.copy(right = it.coerceAtLeast(state.selection.left + .02f))) }
                        CropEdge("Bottom edge", state.selection.bottom) { model.selectCrop(state.selection.copy(bottom = it.coerceAtLeast(state.selection.top + .02f))) }
                    }
                    if (state.editedDraft != null) Text("Reading another area replaces the extracted text and edits. Back keeps your current review.", style = MaterialTheme.typography.bodySmall)
                }
                FloatingStage.REVIEW -> {
                    Text("Review extracted content", style = MaterialTheme.typography.headlineSmall)
                    Text("Check the spelling before analysis. OCR can change a link; a screenshot does not reveal its hidden destination.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.extraction?.limited == true) Text("Extraction is incomplete. Choose a smaller area or review the selected text carefully.", color = MaterialTheme.colorScheme.error)
                    if (state.imageDownsampled) Text("Image was resized to fit device memory. Check small text carefully.", style = MaterialTheme.typography.bodySmall)
                    ExtractionCoverage(state.extraction)
                    var previewZoom by remember { mutableStateOf(false) }
                    state.bitmap?.let { bitmap ->
                        AndroidView(factory = { ScreenCropView(it) }, update = {
                            it.bitmap = bitmap; it.reviewMode = true; it.panMode = previewZoom
                            if (it.zoomed != previewZoom) it.zoomed = previewZoom
                            it.lines = state.extraction?.lines.orEmpty(); it.selectedLines = if (state.textMode == TextSelectionMode.SELECTED) state.selectedLines else emptySet()
                            it.onLine = model::selectLine; it.contentDescription = localizedLabel(context, "Selected image. Tap a recognized line or use the text selection list below."); it.invalidate()
                        }, modifier = Modifier.fillMaxWidth().height(280.dp))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state.bitmap != null) OutlinedButton(onClick = { previewZoom = !previewZoom }) { Text(if (previewZoom) "Zoom out" else "Zoom in") }
                        OutlinedButton(onClick = { if (state.original != null) model.changeArea() else newInput(onCapture) }) { Text(if (state.original != null) "Change area" else "Scan screen area") }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(state.scope == ReviewScope.MESSAGE, model::useAllText, label = { Text("All text") })
                        FilterChip(state.scope == ReviewScope.SELECTED_LINES, model::useSelectedText, label = { Text("Selected lines") })
                        if (state.editedDraft != null) FilterChip(state.scope == ReviewScope.EDITED_TEXT, model::useEditedText, label = { Text("Edited text") })
                        if (candidates.isNotEmpty()) FilterChip(state.scope == ReviewScope.LINKS, { model.setScope(ReviewScope.LINKS) }, label = { Text("Links") })
                        if (!state.extraction?.qrCodes.isNullOrEmpty()) FilterChip(state.scope == ReviewScope.QR, { model.setScope(ReviewScope.QR) }, label = { Text("QR") })
                    }
                    if (state.scope == ReviewScope.SELECTED_LINES) {
                        Text("${state.selectedLines.size} lines selected", style = MaterialTheme.typography.labelMedium)
                        if (state.selectedLines.isEmpty()) Text("Select at least one line or choose All text.")
                        state.extraction?.lines.orEmpty().forEachIndexed { index, line ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(index in state.selectedLines, role = Role.Checkbox, onValueChange = { model.selectLine(index) }), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(index in state.selectedLines, null)
                            Column(Modifier.weight(1f)) {
                                Text(line.text, localize = false)
                                if ((line.confidence ?: 1f) < .6f) Text("Check this reading", style = MaterialTheme.typography.labelSmall)
                            }
                            }
                        }
                    }
                    if (state.scope in setOf(ReviewScope.MESSAGE, ReviewScope.SELECTED_LINES, ReviewScope.EDITED_TEXT)) ReviewTextField(state.text, model::edit)
                    if (state.scope == ReviewScope.LINKS) {
                        Row(Modifier.fillMaxWidth().toggleable(includeContext, role = Role.Checkbox, onValueChange = { includeContext = it }), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(includeContext, null); Text("Include surrounding message", modifier = Modifier.weight(1f))
                        }
                        LinkChoices(candidates, selectedLink) { selectedLink = it.inspectionValue }
                    }
                    if (state.scope == ReviewScope.QR) state.extraction?.qrCodes.orEmpty().forEach { qr ->
                        Card(onClick = { selectedQr = qr }, colors = CardDefaults.cardColors(containerColor = if (selectedQr == qr) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("QR content found", style = MaterialTheme.typography.titleMedium)
                                Text(qrLabel(QrPayloadClassifier.classify(qr)))
                                Text(qr.take(1200), localize = false)
                                if (QrPayloadClassifier.unsupported(qr)) Text("This payload type cannot be fully assessed. Nothing was opened or executed.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Text("Only the content you choose is analyzed. Nothing opens automatically.", style = MaterialTheme.typography.bodySmall)
                }
                else -> Unit
            }
        }
        state.error?.let { message ->
            // Keep recovery compact so the keyboard cannot push the primary action outside the window.
            Surface(color = MaterialTheme.colorScheme.errorContainer) { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(message, modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }.weight(1f).heightIn(max = 96.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                TextButton(onClick = model::dismissError) { Text("Dismiss") }
            } }
        }
        HorizontalDivider()
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (state.stage) {
                FloatingStage.SETUP -> Button(onClick = if (running) { { FloatingAssistantControl.stop(context) } } else onEnable,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(if (running) "Pause assistant" else "Enable floating assistant") }
                FloatingStage.PASTE -> Button(onClick = { focus.clearFocus(); model.analyze() }, enabled = state.text.isNotBlank() && state.text.length <= 12000,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Analyze privately") }
                FloatingStage.CROP -> Button(onClick = { model.crop(state.selection) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Read selected area") }
                FloatingStage.REVIEW -> {
                    val link = candidates.firstOrNull { it.inspectionValue == selectedLink }
                    val qr = state.extraction?.qrCodes?.firstOrNull { it == selectedQr } ?: state.extraction?.qrCodes?.singleOrNull()
                    val enabled = when (state.scope) { ReviewScope.LINKS -> link != null; ReviewScope.QR -> qr != null && qr.length <= 12000; else -> state.text.isNotBlank() && state.text.length <= 12000 }
                    Button(onClick = {
                        focus.clearFocus()
                        when (state.scope) { ReviewScope.LINKS -> link?.let(::analyzeLink); ReviewScope.QR -> qr?.let { model.analyze(it, "qr") }; else -> model.analyze() }
                    }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(when (state.scope) { ReviewScope.LINKS -> "Analyze selected link"; ReviewScope.QR -> "Analyze this QR content"; ReviewScope.SELECTED_LINES -> "Analyze selected text"; else -> "Analyze reviewed message" })
                    }
                }
                FloatingStage.RESULT -> {
                    Button(onClick = model::save, enabled = !state.saved && !state.saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (state.saving) "Saving…" else if (state.saved) "Saved" else "Save result") }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { model.back() }, modifier = Modifier.weight(1f)) { Text("Edit and rescan") }
                        TextButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Done") }
                    }
                }
                else -> OutlinedButton(onClick = model::cancel, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Cancel") }
            }
        }
    }
}
private fun localizedLabel(context: android.content.Context, text: String) = com.sentinel.ai.core.i18n.I18n.translate(context, text)
@Composable private fun ReviewTextField(value: String, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text("Text to analyze") }, modifier = Modifier.fillMaxWidth().heightIn(min = 144.dp, max = 280.dp), isError = value.length > 12000)
    Text("${value.length} / 12,000", localize = false, style = MaterialTheme.typography.labelSmall)
}
@Composable private fun LinkChoices(candidates: List<ContentCandidate>, selected: String?, choose: (ContentCandidate) -> Unit) {
    Text("Choose a detected link", style = MaterialTheme.typography.titleMedium)
    candidates.take(8).forEach { candidate ->
        OutlinedButton(onClick = { choose(candidate) }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth()) {
            Text(candidate.original.take(600), localize = false)
            if (candidate.needsConfirmation) {
                Text("Destination to inspect", style = MaterialTheme.typography.labelSmall)
                Text(candidate.inspectionValue.take(600), localize = false, style = MaterialTheme.typography.bodySmall)
            }
            if (candidate.needsConfirmation) Text("Confirm link spelling", style = MaterialTheme.typography.labelSmall)
            else if (CandidateChange.ASSUMED_HTTPS in candidate.changes) Text("HTTPS assumed for inspection", style = MaterialTheme.typography.labelSmall)
            if (selected == candidate.inspectionValue) Text("Selected", style = MaterialTheme.typography.labelSmall)
        } }
    }
    if (candidates.size > 8) Text("More than eight links were found. Select or paste additional links separately.", color = MaterialTheme.colorScheme.error)
}
@Composable private fun ExtractionCoverage(extraction: ExtractionResult?) {
    extraction?.outcomes.orEmpty().forEach { outcome ->
        val engine = when (outcome.engine) { ExtractionEngine.LATIN -> "English OCR"; ExtractionEngine.DEVANAGARI -> "Hindi OCR"; ExtractionEngine.GUJARATI -> "Gujarati OCR"; ExtractionEngine.QR -> "QR detection" }
        val status = if (outcome.status == EvidenceSourceStatus.COMPLETED) "Completed" else if (outcome.qualityLimited) "Low quality" else "Unavailable"
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(engine, style = MaterialTheme.typography.labelSmall); Text(status, style = MaterialTheme.typography.labelSmall) }
    }
}
private fun qrLabel(kind: QrPayloadKind) = when (kind) { QrPayloadKind.WEB_LINK -> "Web link"; QrPayloadKind.PAYMENT -> "Payment request"; QrPayloadKind.TEXT -> "Text message"; QrPayloadKind.CONTACT -> "Contact details"; QrPayloadKind.WIFI -> "Wi-Fi configuration"; QrPayloadKind.EMAIL -> "Email action"; QrPayloadKind.SMS -> "SMS action"; QrPayloadKind.PHONE -> "Phone action"; QrPayloadKind.APP_ACTION -> "App action" }
@Composable private fun CropEdge(title: String, value: Float, change: (Float) -> Unit) {
    val label = localizedLabel(LocalContext.current, title)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title); Text("${(value * 100).toInt()}%", localize = false) }
    Slider(value.coerceIn(0f, 1f), change, valueRange = 0f..1f, modifier = Modifier.semantics { contentDescription = label })
}
