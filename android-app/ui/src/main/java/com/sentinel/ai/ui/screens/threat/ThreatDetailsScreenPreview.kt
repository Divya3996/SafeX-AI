package com.sentinel.ai.ui.screens.threat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.Threat
import com.sentinel.ai.ui.theme.SentinelTheme
import com.sentinel.ai.ui.util.SenderPresentation

private fun sampleThreat() = Threat(
    id = "t1",
    source = "com.whatsapp",
    senderDisplayName = "Mom",
    senderIdentifier = "+15551234567",
    content = "Urgent payment request with a shortened link and pressure to act now.",
    riskLevel = RiskLevel.CRITICAL,
    riskScore = 92f,
    explanation = "This message combines urgency, financial pressure, and a link to an unknown destination.",
    recommendation = "Do not tap the link. Contact the sender through a trusted channel.",
    timestamp = System.currentTimeMillis() - 3_600_000
)

private val previewResolvers: (String?, String?) -> SenderPresentation =
    { name, identifier ->
        SenderPresentation(
            primaryText = name ?: "Unknown sender",
            secondaryText = identifier
        )
    }

@Preview(name = "Dark", showBackground = true)
@Composable
private fun ThreatDetailsDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ThreatDetailsContent(
                threat = sampleThreat(),
                sourceLabelResolver = { it },
                senderPresentationResolver = previewResolvers,
                onBack = {}
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ThreatDetailsLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ThreatDetailsContent(
                threat = sampleThreat(),
                sourceLabelResolver = { it },
                senderPresentationResolver = previewResolvers,
                onBack = {}
            )
        }
    }
}

@Preview(name = "No data", showBackground = true)
@Composable
private fun ThreatDetailsEmptyPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ThreatDetailsContent(
                threat = null,
                sourceLabelResolver = { it },
                senderPresentationResolver = previewResolvers,
                onBack = {}
            )
        }
    }
}
