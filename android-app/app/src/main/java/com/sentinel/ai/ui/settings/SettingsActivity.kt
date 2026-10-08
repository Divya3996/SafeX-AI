package com.sentinel.ai.ui.settings

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.sentinel.ai.core.feature.FeatureManager
import com.sentinel.ai.ui.theme.SentinelTheme
import com.sentinel.ai.ui.theme.rememberThemeMode

class SettingsActivity : ComponentActivity() {
    private var state by mutableStateOf(FeatureState())
    private var showDefaultBrowserDialog by mutableStateOf(false)
    private var awaitingNotificationAccess = false
    private var awaitingDefaultBrowser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshState()
        setContent {
            val themeMode = rememberThemeMode(this)
            SentinelTheme(mode = themeMode.value) {
                FeatureSettingsScreen(
                    state = state,
                    onBack = ::finish,
                    onNotificationChanged = ::setNotificationEnabled,
                    onClickChanged = ::setClickEnabled,
                    onTextChanged = {
                        FeatureManager.setTextEnabled(it)
                        refreshState()
                    }
                )
                if (showDefaultBrowserDialog) {
                    AlertDialog(
                        onDismissRequest = { showDefaultBrowserDialog = false },
                        title = { Text("Default browser required") },
                        text = { Text("To enable this feature, set SafeX AI as your default browser") },
                        confirmButton = {
                            TextButton(onClick = {
                                showDefaultBrowserDialog = false
                                awaitingDefaultBrowser = true
                                requestDefaultBrowser()
                            }) { Text("Continue") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDefaultBrowserDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (awaitingNotificationAccess && hasNotificationAccess()) {
            FeatureManager.setNotificationEnabled(true)
            awaitingNotificationAccess = false
        }
        if (awaitingDefaultBrowser && isDefaultBrowser()) {
            FeatureManager.setClickEnabled(true)
            awaitingDefaultBrowser = false
        }
        refreshState()
    }

    private fun setNotificationEnabled(enabled: Boolean) {
        if (!enabled) {
            awaitingNotificationAccess = false
            FeatureManager.setNotificationEnabled(false)
        } else if (hasNotificationAccess()) {
            FeatureManager.setNotificationEnabled(true)
        } else {
            awaitingNotificationAccess = true
            openNotificationAccessSettings()
        }
        refreshState()
    }

    private fun setClickEnabled(enabled: Boolean) {
        if (!enabled) {
            awaitingDefaultBrowser = false
            FeatureManager.setClickEnabled(false)
        } else if (isDefaultBrowser()) {
            FeatureManager.setClickEnabled(true)
        } else {
            showDefaultBrowserDialog = true
        }
        refreshState()
    }

    private fun refreshState() {
        state = FeatureState(
            notificationEnabled = FeatureManager.isNotificationEnabled(),
            clickEnabled = FeatureManager.isClickEnabled(),
            textEnabled = FeatureManager.isTextEnabled()
        )
    }

    private fun hasNotificationAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

    private fun isDefaultBrowser(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            return roleManager?.isRoleHeld(RoleManager.ROLE_BROWSER) == true
        }
        return false
    }

    private fun requestDefaultBrowser() {
        val opened = launchSettingsIntent(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        if (!opened) openAppSettings()
    }

    private fun openNotificationAccessSettings() {
        val opened = launchSettingsIntent(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        if (!opened) openAppSettings()
    }

    private fun openAppSettings() {
        launchSettingsIntent(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        )
    }

    private fun launchSettingsIntent(intent: Intent): Boolean {
        return try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}

private data class FeatureState(
    val notificationEnabled: Boolean = true,
    val clickEnabled: Boolean = true,
    val textEnabled: Boolean = true
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FeatureSettingsScreen(
    state: FeatureState,
    onBack: () -> Unit,
    onNotificationChanged: (Boolean) -> Unit,
    onClickChanged: (Boolean) -> Unit,
    onTextChanged: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protection features") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Enable only the protection features you want.", style = MaterialTheme.typography.bodyLarge)
            FeatureToggle("Notification Protection", "Detect scams from incoming messages", "Requires notification access", state.notificationEnabled, onNotificationChanged)
            FeatureToggle("Click Protection", "Scan links before opening", "Requires SafeX AI as the default browser", state.clickEnabled, onClickChanged)
            FeatureToggle("Text Selection", "Analyze selected text", null, state.textEnabled, onTextChanged)
        }
    }
}

@Composable
private fun FeatureToggle(
    title: String,
    description: String,
    requirement: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium)
                requirement?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
