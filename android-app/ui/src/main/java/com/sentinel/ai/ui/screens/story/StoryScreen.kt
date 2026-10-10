@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.sentinel.ai.ui.screens.story

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.ai.core.feature.DisplayPreferences
import com.sentinel.ai.core.i18n.I18n
import com.sentinel.ai.core.story.*
import com.sentinel.ai.ui.guidance.guidanceTarget
import com.sentinel.ai.ui.i18n.LocalizedText as Text
import com.sentinel.ai.ui.i18n.localized
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StoryScreen(onHelp: () -> Unit, onCapture: () -> Unit, viewModel: StoryViewModel = hiltViewModel()) {
    val controller = viewModel.controller
    val state by controller.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val settings by DisplayPreferences.settings.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val focus = LocalFocusManager.current
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var savedDialog by remember { mutableStateOf(false) }
    var export by remember { mutableStateOf<String?>(null) }
    var selectedFinding by remember { mutableStateOf<String?>(null) }
    var selectedItem by remember { mutableStateOf<String?>(null) }
    var rename by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf("") }
    var intro by remember { mutableStateOf(!context.getSharedPreferences("safex_story", 0).getBoolean("guide_seen", false)) }
    val scroll = rememberScrollState()
    fun replace(action: () -> Unit) {
        if (state.dirty || state.input.isNotBlank()) confirm = "Replace the open private case? Unsaved changes and reviewed input will be discarded. Saved cases remain on this device." to action
        else action()
    }
    DisposableEffect(lifecycle, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) controller.setForeground(true)
            if (event == Lifecycle.Event.ON_STOP) {
                controller.setForeground(false)
                export = null; selectedItem = null; selectedFinding = null; savedDialog = false
                rename = false; titleInput = ""; confirm = null
            }
        }
        lifecycle.addObserver(observer); controller.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { lifecycle.removeObserver(observer); controller.setForeground(false) }
    }
    LaunchedEffect(state.case.id) {
        export = null; selectedItem = null; selectedFinding = null; savedDialog = false
        rename = false; titleInput = ""; confirm = null
    }
    // Protect this destination and every dialog; do not change a pre-existing secure flag.
    val activity = context as? android.app.Activity
    DisposableEffect(activity) {
        val already = activity?.window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE) != 0
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!already) activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { uri -> controller.importImage(uri, false) } }
    val qrPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { uri -> controller.importImage(uri, true) } }
    val date = remember(settings.language) { SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag(settings.language.tag)) }
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountTree, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Text("Connect the clues", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 12.dp))
                IconButton(onClick = { replace { controller.clear(); controller.refreshSaved() } }, enabled = !state.busy, modifier = Modifier.testTag("story_clear")) {
                    Icon(Icons.Default.Add, localized("Start a new private case"))
                }
            }
            if (intro) StoryPanel {
                Text("Connect the clues", style = MaterialTheme.typography.titleLarge)
                Text("Add only items from the same situation. Review extracted content, arrange the order, then inspect the linked reasons. A low-concern result is not a safety guarantee.")
                Text("Your draft stays in memory. Leaving this screen for two minutes or locking the phone clears it. Save explicitly to keep an encrypted case.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { intro = false; context.getSharedPreferences("safex_story", 0).edit().putBoolean("guide_seen", true).apply() }) { Text("Got it") }
            }
            StoryPanel(Modifier.guidanceTarget("story.summary")) {
                val accent = when (state.analysis.concern) { StoryConcern.HIGH -> MaterialTheme.colorScheme.error; StoryConcern.REVIEW -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.primary }
                Text(state.analysis.concern.label, color = accent, style = MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(state.case.title, localize = state.case.title in setOf("Private situation", "Synthetic example"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { titleInput = state.case.title; rename = true }, enabled = !state.busy) { Icon(Icons.Default.Edit, localized("Name this case")) }
                }
                Text(if (state.case.savedAt != null && !state.dirty) "Saved with on-device encryption" else "On device • Private draft", style = MaterialTheme.typography.labelLarge)
                if (state.case.synthetic) Text("Synthetic example • Actual detector", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text("${state.case.items.size} / 8", localize = false, style = MaterialTheme.typography.labelLarge)
                Text(if (state.analysis.findings.isEmpty()) "No supported story pattern yet. Add relevant evidence and verify independently." else "Tap a finding to see the exact supporting evidence.")
                if (state.analysis.incomplete) Text("Some checks have limited coverage. Missing evidence remains unknown.", color = MaterialTheme.colorScheme.error)
            }
            state.error?.let { StoryPanel { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = controller::dismissMessage) { Text("Dismiss") } } }
            state.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("story_notice")) }
            StoryPanel {
                Text(if (state.editingId == null) "Add reviewed evidence" else "Edit and recheck evidence", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(StorySource.MESSAGE, StorySource.LINK, StorySource.QR).forEach { source ->
                        FilterChip(selected = state.source == source, onClick = { controller.selectSource(source) }, enabled = !state.busy,
                            label = { Text(source.label) }, modifier = Modifier.testTag("story_source_${source.name}"))
                    }
                }
                state.extractionNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                state.qrChoices.forEachIndexed { index, value ->
                    OutlinedButton(onClick = { controller.chooseQr(value) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("story_qr_$index")) {
                        Text("QR ${index + 1}", localize = false)
                    }
                }
                OutlinedTextField(value = state.input, onValueChange = controller::editInput, enabled = !state.busy,
                    label = { Text("Review text or decoded content") }, minLines = 3, maxLines = 7,
                    modifier = Modifier.fillMaxWidth().testTag("story_input"), isError = state.input.length > StoryLimits.ITEM_CHARS)
                Text("${state.input.length} / 12,000", localize = false, style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = {
                        fun unavailable() { android.widget.Toast.makeText(context, I18n.translate(context, "Clipboard is empty or unavailable. Paste or type the original content."), android.widget.Toast.LENGTH_SHORT).show() }
                        try {
                            val clip = context.getSystemService(ClipboardManager::class.java).primaryClip
                            val raw = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
                            if (!raw.isNullOrBlank()) controller.editInput(raw) else unavailable()
                        } catch (_: RuntimeException) { unavailable() }
                    }, enabled = !state.busy) { Text("Paste") }
                    TextButton(onClick = {
                        if (state.input.isNotBlank()) confirm = "Replace the reviewed input with a chosen image? Your added evidence stays in the case." to { imagePicker.launch(arrayOf("image/*")) }
                        else imagePicker.launch(arrayOf("image/*"))
                    }, enabled = !state.busy, modifier = Modifier.testTag("story_import_image")) { Text("Choose screenshot") }
                    TextButton(onClick = {
                        if (state.input.isNotBlank()) confirm = "Replace the reviewed input with a chosen image? Your added evidence stays in the case." to { qrPicker.launch(arrayOf("image/*")) }
                        else qrPicker.launch(arrayOf("image/*"))
                    }, enabled = !state.busy, modifier = Modifier.testTag("story_import_qr")) { Text("Choose QR image") }
                    TextButton(onClick = onCapture, enabled = !state.busy, modifier = Modifier.testTag("story_capture")) { Text("Use floating capture") }
                }
                if (state.source in setOf(StorySource.IMAGE, StorySource.FLOATING)) Text("Screenshot text cannot reveal a hidden HTML destination. OCR edits are marked in the timeline.", style = MaterialTheme.typography.bodySmall)
            }
            if (state.demoSteps.isNotEmpty()) {
                Text("Demo step ${state.demoIndex + 1} of ${state.demoSteps.size}", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = controller::nextDemo, enabled = !state.busy && state.input.isBlank() && state.demoIndex + 1 < state.demoSteps.size,
                    modifier = Modifier.fillMaxWidth().testTag("story_demo_next")) { Text("Next example message") }
            } else if (state.case.items.isEmpty()) StoryPanel {
                Text("Try a harmless story", style = MaterialTheme.typography.titleMedium)
                Text("Each example uses the actual private detector. No link or payment needs to be opened.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { replace { controller.loadDemo(StoryExamples.task(settings.language.tag)) } }, modifier = Modifier.fillMaxWidth().testTag("story_demo_task")) { Text("Task-scam example") }
                OutlinedButton(onClick = { replace { controller.loadDemo(StoryExamples.legitimate(settings.language.tag)) } }, modifier = Modifier.fillMaxWidth().testTag("story_demo_legitimate")) { Text("Legitimate comparison") }
            }
            Column(Modifier.testTag("story_findings"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.analysis.findings.isNotEmpty()) Text("Connected findings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                state.analysis.findings.forEach { finding ->
                    OutlinedCard(onClick = { selectedFinding = finding.id }, modifier = Modifier.fillMaxWidth().testTag("story_finding_${finding.id}")) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(finding.title, style = MaterialTheme.typography.titleMedium)
                            Text(finding.explanation)
                            Text("View supporting evidence", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            Column(Modifier.testTag("story_timeline"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Evidence timeline", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Text("Order is chosen by you. Times below show when items were added, not when a sender sent them.", style = MaterialTheme.typography.bodySmall)
                state.case.items.forEachIndexed { index, item -> StoryPanel(Modifier.testTag("story_item_$index")) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", localize = false, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
                        Text(item.source.label, style = MaterialTheme.typography.titleMedium)
                        Text(date.format(Date(item.addedAt)), localize = false, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    }
                    if (item.edited) Text("Reviewed text was edited", style = MaterialTheme.typography.labelSmall)
                    item.extractionNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Text(item.text.take(320), localize = false)
                    item.result?.let { Text(it.headline, color = if (it.decision == com.sentinel.ai.core.model.ProtectionDecision.ALLOW) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { selectedItem = item.id }) { Text("View full evidence") }
                        TextButton(onClick = {
                            if (state.input.isNotBlank()) confirm = "Replace the current reviewed input? Added evidence stays in the case." to { controller.editItem(item.id) }
                            else controller.editItem(item.id)
                        }, enabled = !state.busy, modifier = Modifier.testTag("story_edit_$index")) { Text("Edit") }
                        TextButton(onClick = { confirm = "Remove this evidence and recompute the case? A previously saved snapshot is unchanged until you save again." to { controller.remove(item.id) } }, enabled = !state.saving, modifier = Modifier.testTag("story_remove_$index")) { Text("Remove") }
                        IconButton(onClick = { controller.move(item.id, -1) }, enabled = !state.busy && index > 0, modifier = Modifier.testTag("story_up_$index")) { Icon(Icons.Default.ArrowUpward, localized("Move earlier")) }
                        IconButton(onClick = { controller.move(item.id, 1) }, enabled = !state.busy && index < state.case.items.lastIndex, modifier = Modifier.testTag("story_down_$index")) { Icon(Icons.Default.ArrowDownward, localized("Move later")) }
                    }
                } }
            }
            if (state.case.items.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = controller::save, enabled = !state.busy && state.input.isBlank() && (state.dirty || state.case.savedAt == null), modifier = Modifier.testTag("story_save")) { Text("Save encrypted case") }
                OutlinedButton(onClick = {
                    export = StoryExport.lines(state.case, state.analysis).joinToString("\n\n") { I18n.translate(context, it, settings.language) }
                }, enabled = !state.busy, modifier = Modifier.testTag("story_export")) { Text("Preview redacted summary") }
            }
            Text("Offline checks cannot verify identity, follow hidden redirects or guarantee recovery. Use an independent trusted channel before acting.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { intro = true }) { Text("Show story guide") }
        }
        Surface(tonalElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (state.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(if (state.saving) "Saving securely…" else "Checking on this device…", style = MaterialTheme.typography.labelLarge)
                    if (!state.saving) OutlinedButton(onClick = controller::cancel, modifier = Modifier.fillMaxWidth().testTag("story_cancel")) { Text("Cancel") }
                } else Button(onClick = { focus.clearFocus(); controller.addReviewed() }, enabled = state.input.isNotBlank() && state.input.length <= StoryLimits.ITEM_CHARS,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("story_add")) { Text(if (state.editingId == null) "Add reviewed evidence" else "Update evidence") }
                FlowRow(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { controller.refreshSaved(); savedDialog = true }, enabled = !state.busy, modifier = Modifier.testTag("story_saved")) { Text("Saved cases") }
                    TextButton(onClick = onHelp, modifier = Modifier.testTag("story_help")) { Text("Help after a scam") }
                }
            }
        }
    }
    confirm?.let { (message, action) ->
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text("Confirm your choice") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { confirm = null; action() }, modifier = Modifier.testTag("story_confirm")) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } })
    }
    if (rename) AlertDialog(onDismissRequest = { rename = false }, title = { Text("Name this case") },
        text = { OutlinedTextField(titleInput, { titleInput = it.take(80) }, label = { Text("Case name") }) },
        confirmButton = { TextButton(onClick = { controller.rename(titleInput.ifBlank { "Private situation" }); rename = false }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { rename = false }) { Text("Cancel") } })
    if (savedDialog) AlertDialog(onDismissRequest = { savedDialog = false }, title = { Text("Saved cases") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Cases are encrypted on this device and follow your history-retention setting.")
                if (state.listing.cases.isEmpty()) Text("No saved cases")
                if (state.listing.unreadable > 0) Text("Some saved cases cannot be opened. You can delete saved cases to remove damaged or inaccessible data.", color = MaterialTheme.colorScheme.error)
                state.listing.cases.forEach { saved ->
                    Column {
                        Text(saved.title, localize = saved.title in setOf("Private situation", "Synthetic example"), style = MaterialTheme.typography.titleMedium)
                        Text(date.format(Date(saved.savedAt)), localize = false, style = MaterialTheme.typography.labelSmall)
                        Row {
                            TextButton(onClick = { savedDialog = false; replace { controller.open(saved.id) } }, modifier = Modifier.testTag("story_open_${saved.id}")) { Text("Open case") }
                            TextButton(onClick = { savedDialog = false; confirm = "Delete this encrypted saved case? The open private draft is separate." to { controller.deleteSaved(saved.id) } }) { Text("Delete") }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { savedDialog = false }) { Text("Done") } },
        dismissButton = { TextButton(onClick = { savedDialog = false; confirm = "Delete all encrypted saved cases? This cannot be undone." to { controller.deleteSaved() } }, modifier = Modifier.testTag("story_delete_saved_all")) { Text("Delete all") } })
    export?.let { preview -> AlertDialog(onDismissRequest = { export = null }, title = { Text("Preview redacted summary") },
        text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) { Text(preview, localize = false, modifier = Modifier.testTag("story_export_preview")) } },
        confirmButton = { TextButton(onClick = {
            try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, preview), I18n.translate(context, "Share redacted summary"))) }
            catch (_: android.content.ActivityNotFoundException) { android.widget.Toast.makeText(context, I18n.translate(context, "No sharing app is available."), android.widget.Toast.LENGTH_SHORT).show() }
            export = null
        }, modifier = Modifier.testTag("story_share")) { Text("Share redacted summary") } }, dismissButton = { TextButton(onClick = { export = null }) { Text("Cancel") } }) }
    val finding = state.analysis.findings.firstOrNull { it.id == selectedFinding }
    if (finding != null) AlertDialog(onDismissRequest = { selectedFinding = null }, title = { Text(finding.title) },
        text = { Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(finding.explanation)
            finding.evidence.distinctBy { it.itemId to (it.start to it.end) }.forEach { signal ->
                val item = state.case.items.firstOrNull { it.id == signal.itemId }
                if (item != null) {
                    Text("Evidence ${state.case.items.indexOf(item) + 1}", style = MaterialTheme.typography.labelLarge)
                    Text(item.text.substring(signal.start.coerceIn(0, item.text.length), signal.end.coerceIn(signal.start.coerceIn(0, item.text.length), item.text.length)), localize = false)
                }
            }
            Text(finding.action, style = MaterialTheme.typography.titleSmall)
            finding.watchFor?.let { Text("Common in this pattern", style = MaterialTheme.typography.labelLarge); Text(it) }
        } }, confirmButton = { TextButton(onClick = { selectedFinding = null }) { Text("Done") } })
    val item = state.case.items.firstOrNull { it.id == selectedItem }
    if (item != null) AlertDialog(onDismissRequest = { selectedItem = null }, title = { Text("Reviewed evidence") },
        text = { Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(item.text, localize = false)
            item.result?.let { result ->
                Text("Individual check", style = MaterialTheme.typography.titleMedium)
                Text(result.summary); result.reasons.take(12).forEach { Text(it.message) }
                Text(result.coverage, style = MaterialTheme.typography.bodySmall)
                result.paymentReviews.orEmpty().forEach { review ->
                    Text("Unverified payment details", style = MaterialTheme.typography.titleSmall)
                    Text(review.address.orEmpty(), localize = false)
                    Text(review.amount.orEmpty(), localize = false)
                    review.issues.forEach { Text(it) }
                }
            }
        } }, confirmButton = { TextButton(onClick = { selectedItem = null }) { Text("Done") } })
}

@Composable
private fun StoryPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
