@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.sentinel.ai.ui.guidance

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sentinel.ai.ui.i18n.LocalizedText as Text
import com.sentinel.ai.ui.i18n.localized
import kotlinx.coroutines.delay

object GuidancePreferences {
    private fun prefs(context: Context) = context.getSharedPreferences("safex_guidance", Context.MODE_PRIVATE)
    fun needsIntro(context: Context) = prefs(context).getInt("intro_version", 0) < 1
    fun finishIntro(context: Context, tour: Boolean) { prefs(context).edit().putInt("intro_version", 1).putBoolean("tour_pending", tour).apply() }
    fun consumePendingTour(context: Context): Boolean {
        val pending = prefs(context).getBoolean("tour_pending", false)
        if (pending) prefs(context).edit().putBoolean("tour_pending", false).apply()
        return pending
    }
    fun finishTour(context: Context) { prefs(context).edit().putInt("tour_version", 1).apply() }
}

data class GuideStep(val target: String, val route: String, val title: String, val body: String)
val AppGuideSteps = listOf(
    GuideStep("home.protection", "dashboard", "Your protection status", "This status shows whether notification checks are ready, paused or missing access. Manual scans work without enabling notification access."),
    GuideStep("scan.modes", "scanner", "Choose what to check", "Select Message, Link, Screenshot, QR code or File. Each mode checks a different input; a low-risk result is not a safety guarantee."),
    GuideStep("scan.message", "scanner", "Check an unexpected message", "Choose Message, paste the complete request and tap Analyze privately. Read the reasons, especially requests for passwords, OTPs or money."),
    GuideStep("scan.link", "scanner", "Check the real destination", "Choose Link and paste an address before opening it. Read the actual host and structural warning. Offline checks cannot follow a hidden short link or inspect live website content."),
    GuideStep("scan.screenshot", "scanner", "Read a screenshot privately", "Choose Screenshot and import a clear image. Recognition runs locally. Review spelling and missing words; the screenshot cannot reveal a hidden HTML link destination."),
    GuideStep("scan.qr", "scanner", "Decode a QR before acting", "Choose QR code to scan with the camera or import an image. Review the decoded link or payment details. Matching UPI details do not verify bank ownership; do not pay from a warning screen."),
    GuideStep("scan.file", "scanner", "Inspect a file without running it", "Choose File and select a document or archive. SafeX AI checks its name, type and bounded contents. It does not execute files or certify every file as malware-free."),
    GuideStep("scan.samples", "scanner", "Try a harmless example", "These synthetic examples use the real detector. Compare an OTP request with ordinary safety advice. You never need to open the example link."),
    GuideStep("story.summary", "story", "Connect the whole situation", "Add reviewed messages, screenshots, links or QR content to a private case. Arrange their order and tap a finding to see supporting evidence. Save explicitly for encrypted storage; use Help after a scam for next steps."),
    GuideStep("settings.floating", "settings", "Use the floating shield", "Set up the floating assistant and grant display-over-apps access only if you want it. Outside the app, tap the shield, choose a screen area, approve one capture, crop and review the content before scanning."),
    GuideStep("settings.protection", "settings", "Choose your protection", "Enable only the checks you want. Notification protection needs Android listener access and your protection switch. Selected-text and link handoff checks are separate choices."),
    GuideStep("settings.notifications", "settings", "Allow readable notification checks", "Use Manage notification access to grant Android access, then enable warning notifications. Hidden previews and protected OTP notifications may not expose text. No permission is required for manual text scanning."),
    GuideStep("settings.sound", "settings", "Test the warning tune", "Send a test warning here. Android notification volume, channel sound and Do Not Disturb control playback. A silent test does not prove protection is off."),
    GuideStep("settings.reading", "settings", "Make guidance easy to read", "Choose English, Hindi or Gujarati and adjust text size. Menus, warnings and this guide follow your choice immediately."),
    GuideStep("settings.history", "settings", "Control local history", "Choose the retention period and delete records when needed. A saved record is not a submitted police complaint. In the floating assistant, Save and Done are separate actions."),
    GuideStep("history.search", "history", "Find and review saved checks", "Search local history or filter warnings. Open a record to review reasons, destinations and context questions. Delete all asks for confirmation. Reviewing a record never opens its link automatically."),
    GuideStep("alerts.header", "alerts", "Review incoming-message alerts", "Supported notification checks appear here when protection and Android access are enabled. Open an alert to read the explanation and full details. An empty list does not prove that every message was checked."),
    GuideStep("help.situation", "incident_help", "Get help for what happened", "Choose whether you opened a link, shared a secret, installed an app or sent money. Follow the relevant checklist; SafeX AI does not verify recovery or replace your bank."),
    GuideStep("help.contact", "incident_help", "Contact official help in India", "For financial cyber fraud, open the dialer with 1930. For immediate danger, use 112. You confirm the call in your phone app. Use the official reporting website for a complaint; this app never calls or submits automatically.")
)

internal data class GuideAnchor(val bounds: Rect, val requester: BringIntoViewRequester)
class GuideController(private val context: Context, private val progress: MutableState<Int>) {
    internal val anchors = mutableStateMapOf<String, GuideAnchor>()
    val index get() = progress.value
    val active get() = index in AppGuideSteps.indices
    val current get() = AppGuideSteps.getOrNull(index)
    fun start(index: Int = 0) { progress.value = index.coerceIn(AppGuideSteps.indices) }
    fun next() { if (index == AppGuideSteps.lastIndex) stop() else progress.value++ }
    fun previous() { if (index > 0) progress.value-- }
    fun stop() { progress.value = -1; GuidancePreferences.finishTour(context) }
}
val LocalGuideController = staticCompositionLocalOf<GuideController?> { null }

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.guidanceTarget(key: String): Modifier = composed {
    val guide = LocalGuideController.current
    val requester = remember { BringIntoViewRequester() }
    DisposableEffect(guide, key) { onDispose { guide?.anchors?.remove(key) } }
    this.bringIntoViewRequester(requester).onGloballyPositioned { coordinates ->
        val anchor = GuideAnchor(coordinates.boundsInRoot(), requester)
        if (guide?.anchors?.get(key) != anchor) guide?.anchors?.set(key, anchor)
    }.testTag("guide_target_$key")
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GuideOverlay(controller: GuideController, onIncidentHelp: () -> Unit = {}) {
    val step = controller.current ?: return
    val anchor = controller.anchors[step.target]
    LaunchedEffect(step, anchor?.requester) { delay(220); anchor?.requester?.bringIntoView() }
    BackHandler { controller.stop() }
    val title = localized("Feature tour")
    BoxWithConstraints(Modifier.fillMaxSize().testTag("feature_tour").semantics { paneTitle = title }
        .pointerInput(Unit) { detectTapGestures {} }) {
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val rect = anchor?.bounds?.takeIf { it.width > 0 && it.height > 0 && it.bottom > 0 && it.top < heightPx }
        val density = LocalDensity.current
        val gutterPx = with(density) { 32.dp.toPx() }
        val above = ((rect?.top ?: 0f) - WindowInsets.safeDrawing.getTop(density) - gutterPx).coerceAtLeast(0f)
        val below = (heightPx - (rect?.bottom ?: heightPx) - WindowInsets.safeDrawing.getBottom(density) - gutterPx).coerceAtLeast(0f)
        val fitsBesideTarget = maxOf(above, below) >= with(density) { 320.dp.toPx() }
        val placeAtTop = if (fitsBesideTarget) above > below else (rect?.center?.y ?: 0f) > heightPx * .5f
        val cardLimit = if (fitsBesideTarget) minOf(maxHeight * .6f, with(density) { maxOf(above, below).toDp() }) else maxHeight * .6f
        val accent = MaterialTheme.colorScheme.primary
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color.Black.copy(alpha = .78f))
            rect?.let {
                val visible = it.intersect(Rect(0f, 0f, size.width, size.height))
                drawRoundRect(Color.Transparent, visible.topLeft, visible.size, CornerRadius(18.dp.toPx()), blendMode = BlendMode.Clear)
                drawRoundRect(accent, visible.topLeft, visible.size, CornerRadius(18.dp.toPx()), style = Stroke(3.dp.toPx()))
            }
        }
        Card(Modifier.align(if (placeAtTop) Alignment.TopCenter else Alignment.BottomCenter)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp).widthIn(max = 600.dp).fillMaxWidth()
            .heightIn(max = cardLimit), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Feature tour", color = accent, style = MaterialTheme.typography.labelLarge)
                Text("${controller.index + 1} / ${AppGuideSteps.size}", localize = false, style = MaterialTheme.typography.labelMedium)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(step.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    Text(step.body, style = MaterialTheme.typography.bodyMedium)
                    if (rect == null) Text("This control appears when its screen is ready. You can continue the guide or close it to try the feature.", style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = controller::stop, modifier = Modifier.weight(1f).testTag("tour_skip")) { Text("Close tour") }
                    if (controller.index > 0) OutlinedButton(onClick = controller::previous, modifier = Modifier.testTag("tour_back")) { Text("Back") }
                    Button(onClick = controller::next, modifier = Modifier.testTag("tour_next")) { Text(if (controller.index == AppGuideSteps.lastIndex) "Finish" else "Next") }
                }
                TextButton(onClick = { controller.stop(); onIncidentHelp() }, modifier = Modifier.testTag("tour_incident_help")) { Text("Need help after a scam?") }
            }
        }
    }
}
