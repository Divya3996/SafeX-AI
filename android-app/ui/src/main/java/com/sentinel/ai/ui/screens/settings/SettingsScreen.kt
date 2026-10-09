package com.sentinel.ai.ui.screens.settings

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import android.content.Intent
import android.provider.Settings
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.ui.protection.ProtectionControl
import com.sentinel.ai.ui.theme.SentinelThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(appVersion: String, selectedTheme: SentinelThemeMode, onThemeSelected: (SentinelThemeMode) -> Unit,
    onNavigateToAbout: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf(ProtectionControl.snapshot(context)) }
    var notificationChecks by remember { mutableStateOf(FeatureManager.isNotificationEnabled()) }
    var clickChecks by remember { mutableStateOf(FeatureManager.isClickEnabled()) }
    var selectionChecks by remember { mutableStateOf(FeatureManager.isTextEnabled()) }
    var retention by remember { mutableIntStateOf(PrivacyPreferences.retentionDays) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var snapshotCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { snapshotCount = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.sentinel.ai.core.data.local.ThreatSnapshot.count(context) } }
    val importList = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { scope.launch {
            try { snapshotCount = com.sentinel.ai.core.data.local.ThreatSnapshot.import(context, it); status = "Imported $snapshotCount local threat URLs" }
            catch (e: Exception) { status = e.message ?: "Could not import threat list" }
        } }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { snapshot = ProtectionControl.snapshot(context) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) snapshot = ProtectionControl.snapshot(context) }
        lifecycle.lifecycle.addObserver(observer); onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    if (deleteConfirmation) AlertDialog(onDismissRequest = { deleteConfirmation = false }, title = { Text("Delete saved analyses?") },
        text = { Text("All history on this device will be removed permanently.") },
        confirmButton = { TextButton(onClick = { scope.launch { try { ThreatJournal.delete(); status = "Local history deleted" } catch (_: Exception) { status = "Could not delete history" } }; deleteConfirmation = false }) { Text("Delete all") } },
        dismissButton = { TextButton(onClick = { deleteConfirmation = false }) { Text("Cancel") } })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Protection, your way", style = MaterialTheme.typography.headlineLarge)
        Text("Your content stays on your phone. This build has no internet permission, no account and no cloud inference.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        com.sentinel.ai.ui.i18n.LanguageAndReadingSettings()
        Section("Floating assistant") {
            val floating by FloatingAssistantControl.running.collectAsState()
            Text(if (floating) "Floating assistant is running" else "Floating assistant is paused", style = MaterialTheme.typography.titleSmall)
            Text("Use a movable shield to crop a screen, review text or links, and analyze privately on your phone.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { FloatingAssistantControl.open(context) }) { Text("Set up floating assistant") }
            if (floating) TextButton(onClick = { FloatingAssistantControl.stop(context) }) { Text("Pause assistant") }
        }
        Section("Protection") {
            Toggle("Incoming-message protection", "Pause or resume supported notification checks", snapshot.protectionEnabled) {
                ProtectionControl.setProtectionEnabled(context, it); snapshot = ProtectionControl.snapshot(context)
            }
            Toggle("Notification analysis", "Scan available message content locally", notificationChecks) { notificationChecks = it; FeatureManager.setNotificationEnabled(it) }
            Toggle("Link handoff checks", "Analyze links routed through SafeX AI", clickChecks) { clickChecks = it; FeatureManager.setClickEnabled(it) }
            Toggle("Selected-text analysis", "Analyze a message from Android's selection menu", selectionChecks) { selectionChecks = it; FeatureManager.setTextEnabled(it) }
        }
        Section("Notification access") {
            Text(if (snapshot.notificationListenerEnabled) "Access enabled" else "Access not enabled", style = MaterialTheme.typography.titleSmall)
            Text("Only supported notifications are checked. Hidden previews and protected OTP notifications may not expose readable content.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) { Text("Manage notification access") }
            OutlinedButton(onClick = {
                if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }) { Text("Manage warning notifications") }
        }
        Section("Warning sound") {
            Text("Threat notifications use the SafeX AI warning tune. Android notification volume, channel settings and Do Not Disturb control playback.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = {
                status = if (WarningChannels.test(context)) "Test notification sent. If silent, check the channel sound and notification volume."
                    else "Enable warning notifications before testing the sound."
            }) { Text("Test warning notification") }
            listOf(false, true).forEach { high ->
                TextButton(onClick = {
                    WarningChannels.ensure(context)
                    context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .putExtra(Settings.EXTRA_CHANNEL_ID, WarningChannels.id(high)))
                }) { Text(if (high) "Urgent warning sound settings" else "Review warning sound settings") }
            }
        }
        Section("Apps to check") {
            listOf("WhatsApp" to "com.whatsapp", "WhatsApp Business" to "com.whatsapp.w4b", "Telegram" to "org.telegram.messenger",
                "Google Messages" to "com.google.android.apps.messaging", "Gmail" to "com.google.android.gm",
                "Instagram" to "com.instagram.android", "Messenger" to "com.facebook.orca", "Signal" to "org.thoughtcrime.securesms",
                "Discord" to "com.discord", "Slack" to "com.Slack").forEach { (name, pkg) ->
                var enabled by remember(pkg) { mutableStateOf(PrivacyPreferences.isAppEnabled(pkg)) }
                Toggle(name, "Notification content only", enabled) { enabled = it; PrivacyPreferences.setAppEnabled(pkg, it) }
            }
        }
        Section("Local history") {
            Text("Automatically remove records older than", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30, 90).forEach { days -> FilterChip(selected = retention == days, onClick = {
                    retention = days; PrivacyPreferences.retentionDays = days
                    scope.launch { try { ThreatJournal.applyRetention(days) } catch (_: Exception) { status = "Could not apply retention" } }
                }, label = { Text("$days days") }) }
            }
            Text("Private history is excluded from cloud backup and device transfer.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { deleteConfirmation = true }) { Text("Delete all history") }
        }
        Section("Offline threat snapshot") {
            Text("$snapshotCount imported URLs", style = MaterialTheme.typography.titleSmall)
            Text("Import a trusted list with one full URL per line. Matches from your imported list are blocked locally; lists can be incomplete or outdated.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { importList.launch(arrayOf("text/*")) }) { Text("Import threat list") }
            TextButton(onClick = { scope.launch { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.sentinel.ai.core.data.local.ThreatSnapshot.clear(context) }; snapshotCount = 0 } }) { Text("Remove imported list") }
        }
        Section("Appearance") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SentinelThemeMode.entries.forEach { mode -> FilterChip(selected = mode == selectedTheme, onClick = { onThemeSelected(mode) }, label = { Text(if (mode == SentinelThemeMode.Neon) "Emerald" else mode.name) }) }
            }
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        TextButton(onClick = onNavigateToAbout) { Text("About SafeX AI • $appVersion") }
    }
}
@Composable private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}
@Composable private fun Toggle(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
