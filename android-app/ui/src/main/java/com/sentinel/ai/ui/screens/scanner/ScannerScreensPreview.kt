package com.sentinel.ai.ui.screens.scanner

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.ui.theme.SentinelTheme

private fun sampleResult(level: RiskLevel, source: String) = ScanResult(
    id = "preview", source = source, riskLevel = level, riskScore = 0.5f,
    explanation = "Sentinel analyzed this item and found signals consistent with the displayed risk level.",
    timestamp = System.currentTimeMillis()
)

@Preview(name = "Dark - Scan input (link)", showBackground = true)
@Composable
private fun ScanInputLinkDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ScanInputContent(
                scanType = ScanType.LINK,
                scanInput = "",
                onInputChange = {},
                onTypeChange = {},
                onRunScan = {},
                error = null
            )
        }
    }
}

@Preview(name = "Light - Live scanning", showBackground = true)
@Composable
private fun LiveScanLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LiveScanContent(
                scanType = ScanType.LINK
            )
        }
    }
}

@Preview(name = "Dark - URL safe", showBackground = true)
@Composable
private fun UrlSafeDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            UrlScanResultContent(
                result = sampleResult(RiskLevel.GREEN, "https://example.com"),
                onOpen = {}, onGoBack = {}, onBypass = {}, onScanAgain = {}
            )
        }
    }
}

@Preview(name = "Dark - URL dangerous", showBackground = true)
@Composable
private fun UrlDangerousDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            UrlScanResultContent(
                result = sampleResult(RiskLevel.CRITICAL, "http://free-prize.xyz/login"),
                onOpen = {}, onGoBack = {}, onBypass = {}, onScanAgain = {}
            )
        }
    }
}

@Preview(name = "Light - File suspicious", showBackground = true)
@Composable
private fun FileSuspiciousLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            FileScanResultContent(
                result = sampleResult(RiskLevel.YELLOW, "invoice_march.apk"),
                onOpen = {}, onGoBack = {}, onBypass = {}, onScanAgain = {}
            )
        }
    }
}
