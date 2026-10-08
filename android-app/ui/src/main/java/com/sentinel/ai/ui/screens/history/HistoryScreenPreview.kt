package com.sentinel.ai.ui.screens.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.ui.theme.SentinelTheme
import com.sentinel.ai.ui.util.SenderPresentation
import com.sentinel.ai.ui.util.resolveSenderPresentation
import com.sentinel.ai.ui.util.toAppLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun sampleHistory() = listOf(
    ScanResult(
        id = "h1",
        source = "com.whatsapp",
        senderDisplayName = "Mom",
        senderIdentifier = "+15551234567",
        riskLevel = RiskLevel.CRITICAL,
        riskScore = 0.92f,
        explanation = "Urgent payment request with a shortened link and pressure to act now.",
        timestamp = System.currentTimeMillis() - 3_600_000
    ),
    ScanResult(
        id = "h2",
        source = "com.google.android.gm",
        senderDisplayName = "Bank",
        senderIdentifier = "alerts@bank.example",
        riskLevel = RiskLevel.RED,
        riskScore = 0.78f,
        explanation = "Account verification needed. Contains a suspicious sign-in prompt.",
        timestamp = System.currentTimeMillis() - 86_400_000
    ),
    ScanResult(
        id = "h3",
        source = "org.telegram.messenger",
        senderDisplayName = "Alex",
        senderIdentifier = null,
        riskLevel = RiskLevel.YELLOW,
        riskScore = 0.45f,
        explanation = "Forwarded message about a crypto giveaway.",
        timestamp = System.currentTimeMillis() - 172_800_000
    )
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
private fun HistoryDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HistoryUiPreview(history = sampleHistory())
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun HistoryLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HistoryUiPreview(history = sampleHistory())
        }
    }
}

@Composable
private fun HistoryUiPreview(history: List<ScanResult>) {
    val formatter = rememberHistoryFormatter()
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyColumnShim(history = history, formatter = formatter, context = context)
}

@Composable
private fun LazyColumnShim(
    history: List<ScanResult>,
    formatter: SimpleDateFormat,
    context: android.content.Context
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        history.take(5).forEach { item ->
            HistoryItemRow(
                item = item,
                appLabel = item.source.toAppLabel(context),
                senderPresentation = resolveSenderPresentation(
                    context = context,
                    senderDisplayName = item.senderDisplayName,
                    senderIdentifier = item.senderIdentifier
                ),
                timestampLabel = formatter.format(Date(item.timestamp))
            )
        }
    }
}
