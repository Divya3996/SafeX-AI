package com.sentinel.ai.ui.screens.alert

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.sentinel.ai.core.model.Alert
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.ui.theme.SentinelTheme
import com.sentinel.ai.ui.util.SenderPresentation

private fun sampleAlert(level: RiskLevel, id: String) = Alert(
    id = id, threatId = "threat-$id", title = "com.whatsapp", senderDisplayName = "Unknown",
    summary = "Message claims you won a prize and asks for a payment to release it.",
    riskLevel = level, timestamp = System.currentTimeMillis()
)

private val previewResolvers: (String?, String?) -> SenderPresentation =
    { name, _ -> SenderPresentation(name ?: "Unknown sender") }

@Preview(name = "Dark - Notification list", showBackground = true)
@Composable
private fun AlertListDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AlertContent(
                uiState = AlertUiState(
                    alerts = listOf(
                        sampleAlert(RiskLevel.CRITICAL, "a1"),
                        sampleAlert(RiskLevel.RED, "a2"),
                        sampleAlert(RiskLevel.YELLOW, "a3"),
                        sampleAlert(RiskLevel.GREEN, "a4")
                    )
                ),
                onAction = {},
                onNavigateToDetails = {},
                appLabelResolver = { it },
                senderPresentationResolver = previewResolvers
            )
        }
    }
}

@Preview(name = "Light - Scan sheet (dangerous)", showBackground = true)
@Composable
private fun ScanSheetDangerousLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            NotificationScanSheetContent(
                alert = sampleAlert(RiskLevel.CRITICAL, "a1"),
                appLabel = "WhatsApp",
                senderPresentation = SenderPresentation("Unknown"),
                onClose = {},
                onViewDetails = {}
            )
        }
    }
}

@Preview(name = "Dark - Scan sheet (safe)", showBackground = true)
@Composable
private fun ScanSheetSafeDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            NotificationScanSheetContent(
                alert = sampleAlert(RiskLevel.GREEN, "a4"),
                appLabel = "WhatsApp",
                senderPresentation = SenderPresentation("Mom"),
                onClose = {},
                onViewDetails = {}
            )
        }
    }
}
