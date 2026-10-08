package com.sentinel.ai.ui.screens.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.sentinel.ai.core.model.Alert
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.ui.protection.ProtectionSnapshot
import com.sentinel.ai.ui.theme.SentinelTheme
import com.sentinel.ai.ui.util.SenderPresentation

private fun sampleProtection(enabled: Boolean, operational: Boolean) = ProtectionSnapshot(
    protectionEnabled = enabled,
    guardServiceRunning = operational,
    monitorServiceRunning = operational,
    notificationListenerEnabled = true,
    missingPermissions = if (enabled) emptyList() else listOf("Overlay permission")
)

private fun sampleAlerts() = listOf(
    Alert(
        id = "a1", threatId = "t1", title = "com.whatsapp", senderDisplayName = "Mom",
        summary = "Urgent payment request with a shortened link and pressure to act now.",
        riskLevel = RiskLevel.CRITICAL, timestamp = System.currentTimeMillis() - 3_600_000
    ),
    Alert(
        id = "a2", threatId = "t2", title = "com.google.android.gm", senderDisplayName = "Bank",
        summary = "Account verification needed. Contains a suspicious sign-in prompt.",
        riskLevel = RiskLevel.RED, timestamp = System.currentTimeMillis() - 86_400_000
    ),
    Alert(
        id = "a3", threatId = "t3", title = "org.telegram.messenger", senderDisplayName = "Alex",
        summary = "Forwarded message about a crypto giveaway.",
        riskLevel = RiskLevel.YELLOW, timestamp = System.currentTimeMillis() - 172_800_000
    )
)

private val previewResolvers: (String?, String?) -> SenderPresentation =
    { name, _ -> SenderPresentation(name ?: "Unknown sender") }

@Preview(name = "Dark - Protected", showBackground = true)
@Composable
private fun DashboardProtectedDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            DashboardContent(
                uiState = DashboardUiState(
                    protection = sampleProtection(enabled = true, operational = true),
                    recentAlerts = sampleAlerts()
                ),
                onAction = {},
                onThreatSelected = {},
                onNavigateToScanner = {},
                appLabelResolver = { it },
                senderPresentationResolver = previewResolvers
            )
        }
    }
}

@Preview(name = "Light - Protection off", showBackground = true)
@Composable
private fun DashboardOffLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            DashboardContent(
                uiState = DashboardUiState(
                    protection = sampleProtection(enabled = false, operational = false),
                    recentAlerts = emptyList()
                ),
                onAction = {},
                onThreatSelected = {},
                onNavigateToScanner = {},
                appLabelResolver = { it },
                senderPresentationResolver = previewResolvers
            )
        }
    }
}
