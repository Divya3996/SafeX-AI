package com.sentinel.ai.protection.floating

import android.Manifest
import android.app.*
import android.content.*
import android.media.projection.MediaProjectionManager
import android.os.*
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.sentinel.ai.core.feature.FloatingAssistantControl
import com.sentinel.ai.ui.theme.SentinelTheme
import com.sentinel.ai.ui.theme.ThemePreferences
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FloatingAssistantActivity : ComponentActivity() {
    private val model: FloatingSessionViewModel by viewModels()
    private var command by mutableStateOf("setup" to 0)
    private var consentPending = false
    private var pendingInput by mutableStateOf<String?>(null)
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { if (intent?.action == Intent.ACTION_SCREEN_OFF) model.reset() }
    }
    private val capturePermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        consentPending = false
        if (result.resultCode != RESULT_OK || result.data == null) model.captureDenied()
        else {
            val id = model.beginCapture()
            try {
                startForegroundService(Intent(this, OneShotScreenCaptureService::class.java)
                    .putExtra("request_id", id).putExtra("result_code", result.resultCode).putExtra("authorization", result.data))
                moveTaskToBack(true)
            } catch (_: RuntimeException) { model.captureDenied() }
        }
    }
    private val overlayPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(this)) startAssistant()
        else model.error("Display permission was not granted. Manual scanning still works.")
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* actual status is read from Android */ }
    private val imagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(model::importImage) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.content.ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        consentPending = savedInstanceState?.getBoolean("consent_pending") ?: false
        // Crop/text previews are sensitive; never expose them through recents or third-party screenshots.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        command = (if (savedInstanceState != null) "resume" else intent.getStringExtra("mode").orEmpty().ifBlank { "setup" }) to 0
        setContent {
            SentinelTheme(mode = ThemePreferences.get(this)) {
                Surface(Modifier.fillMaxSize()) {
                    val state by model.state.collectAsState()
                    BackHandler { if (!model.back()) finish() }
                    pendingInput?.let { mode ->
                        AlertDialog(onDismissRequest = { pendingInput = null },
                            title = { com.sentinel.ai.ui.i18n.LocalizedText("Start a new input?") },
                            text = { com.sentinel.ai.ui.i18n.LocalizedText("Your current review will be replaced when new content is accepted. Cancel the picker or capture prompt to keep it.") },
                            confirmButton = { TextButton(onClick = { pendingInput = null; executeInput(mode) }) { com.sentinel.ai.ui.i18n.LocalizedText("Continue") } },
                            dismissButton = { TextButton(onClick = { pendingInput = null }) { com.sentinel.ai.ui.i18n.LocalizedText("Keep review") } })
                    }
                    LaunchedEffect(command) {
                        if (!model.controller.matches(intent.getStringExtra("session_id"))) {
                            model.open("setup")
                            model.error("This private result is no longer available. Start a new scan.")
                            return@LaunchedEffect
                        }
                        if (command.first in setOf("capture", "import", "paste")) {
                            if (state.hasContent && !state.saved && !state.busy) pendingInput = command.first
                            else executeInput(command.first)
                        } else if (command.first == "resume") model.resume() else model.open(command.first)
                        if (command.first == "result" && state.result == null) model.error("This private result is no longer available. Start a new scan.")
                    }
                    FloatingAssistantScreen(state, model, onClose = ::close, onCapture = ::capture,
                        onImport = { imagePicker.launch(arrayOf("image/*")) }, onEnable = ::enableAssistant,
                        onNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                        })
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent)
        command = intent.getStringExtra("mode").orEmpty().ifBlank { "setup" } to command.second + 1
    }
    private fun executeInput(mode: String) {
        when (mode) { "capture" -> capture(); "import" -> imagePicker.launch(arrayOf("image/*")); "paste" -> model.open("paste") }
    }
    private fun capture() {
        if (consentPending || model.state.value.stage == FloatingStage.WAITING) return
        consentPending = true
        try { capturePermission.launch(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent()) }
        catch (_: RuntimeException) { consentPending = false; model.captureDenied() }
    }
    private fun enableAssistant() {
        if (Settings.canDrawOverlays(this)) startAssistant()
        else try { overlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))) }
        catch (_: RuntimeException) { model.error("Display-over-apps settings are unavailable on this device. Use Share or Paste instead.") }
    }
    private fun startAssistant() {
        try { FloatingAssistantControl.start(this) } catch (_: RuntimeException) { model.error("Floating assistant could not start. Open SafeX AI and try again.") }
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putBoolean("consent_pending", consentPending); super.onSaveInstanceState(outState) }
    override fun onResume() {
        super.onResume(); FloatingAssistantControl.helperVisible = true; model.foreground = true
        getSystemService(NotificationManager::class.java).cancel(FloatingNotifications.READY_ID)
        getSystemService(NotificationManager::class.java).cancel(915013)
        if (FloatingAssistantControl.running.value) FloatingAssistantControl.command(this, "hide")
    }
    override fun onStop() {
        FloatingAssistantControl.helperVisible = false
        model.foreground = false
        model.requestId?.let(CaptureSessionStore::sourceVisible)
        if (FloatingAssistantControl.running.value && model.state.value.stage != FloatingStage.WAITING && !consentPending) FloatingAssistantControl.command(this, "show")
        super.onStop()
    }
    override fun onDestroy() { unregisterReceiver(screenOff); super.onDestroy() }
    private fun close() { model.reset(); finish() }
    companion object {
        fun intent(context: Context, mode: String, sessionId: String? = null) = Intent(context, FloatingAssistantActivity::class.java)
            .putExtra("mode", mode).putExtra("session_id", sessionId)
            .setData(sessionId?.let { android.net.Uri.parse("safex-private://session/$it/$mode") }).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
