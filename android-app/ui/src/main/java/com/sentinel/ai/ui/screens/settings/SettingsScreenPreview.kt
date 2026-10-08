package com.sentinel.ai.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.sentinel.ai.ui.protection.ProtectionSnapshot
import com.sentinel.ai.ui.theme.SentinelSpacing
import com.sentinel.ai.ui.theme.SentinelTheme

private fun sampleProtection() = ProtectionSnapshot(
    protectionEnabled = true,
    guardServiceRunning = true,
    monitorServiceRunning = true,
    notificationListenerEnabled = true,
    notificationPermissionGranted = true,
    overlayPermissionGranted = true,
    contactsPermissionGranted = true,
    missingPermissions = emptyList()
)

@Preview(name = "Dark", showBackground = true)
@Composable
private fun SettingsDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsUiPreview(protection = sampleProtection())
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun SettingsLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsUiPreview(protection = sampleProtection())
        }
    }
}

@Composable
private fun SettingsUiPreview(protection: ProtectionSnapshot) {
    Column(
        modifier = Modifier.fillMaxSize().padding(SentinelSpacing.ScreenHorizontal)
    ) {
        androidx.compose.material3.Text(text = "Settings", style = MaterialTheme.typography.displaySmall)
        Spacer(modifier = Modifier.height(SentinelSpacing.LG))
        androidx.compose.material3.Text(text = "Protection: ${protection.protectionEnabled}", style = MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.Text(text = "Missing: ${protection.missingPermissions.joinToString()}", style = MaterialTheme.typography.bodyLarge)
    }
}
