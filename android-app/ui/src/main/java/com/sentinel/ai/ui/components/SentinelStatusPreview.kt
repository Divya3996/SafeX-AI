package com.sentinel.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sentinel.ai.ui.theme.SentinelSpacing
import com.sentinel.ai.ui.theme.SentinelTheme

@Preview(name = "Light Theme - Status Components", showBackground = true)
@Composable
private fun SentinelStatusLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StatusPreviewGrid(title = "Status Components")
        }
    }
}

@Preview(name = "Dark Theme - Status Components", showBackground = true)
@Composable
private fun SentinelStatusDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StatusPreviewGrid(title = "Status Components")
        }
    }
}

@Composable
private fun StatusPreviewGrid(title: String) {
    Column(
        modifier = Modifier.padding(SentinelSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(SentinelSpacing.LG)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )

        PreviewSection("RiskBadge") {
            RiskState.entries.forEach { state ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    val riskLevel = when (state) {
                        RiskState.Safe -> com.sentinel.ai.core.model.RiskLevel.GREEN
                        RiskState.Suspicious -> com.sentinel.ai.core.model.RiskLevel.YELLOW
                        RiskState.Dangerous -> com.sentinel.ai.core.model.RiskLevel.RED
                        RiskState.Neutral -> com.sentinel.ai.core.model.RiskLevel.GREEN
                        RiskState.Scanning -> com.sentinel.ai.core.model.RiskLevel.YELLOW
                    }
                    RiskBadge(riskLevel = riskLevel)
                }
            }
        }

        PreviewSection("StatusChip") {
            RiskState.entries.forEach { state ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
                ) {
                    StatusChip(state = state)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = state.displayLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        PreviewSection("SecurityIndicator") {
            RiskState.entries.forEach { state ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
                ) {
                    SecurityIndicator(state = state)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = state.displayLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        PreviewSection("ThreatLevelChip") {
            RiskState.entries.forEach { state ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
                ) {
                    ThreatLevelChip(state = state)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = state.displayLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(SentinelSpacing.XS)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}
