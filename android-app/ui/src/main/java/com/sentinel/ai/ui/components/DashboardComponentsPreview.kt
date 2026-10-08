package com.sentinel.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.sentinel.ai.ui.theme.SentinelSize

@Preview(name = "Light Theme - ScoreCard", showBackground = true)
@Composable
private fun ScoreCardLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD)) {
                ScoreCard(score = 87, trend = "+12 from last week", supportingText = "Strong protection posture")
                Spacer(modifier = Modifier.height(SentinelSpacing.MD))
                ScoreCard(score = 45, trend = "-8 from last week", supportingText = "Attention needed")
            }
        }
    }
}

@Preview(name = "Dark Theme - ScoreCard", showBackground = true)
@Composable
private fun ScoreCardDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD)) {
                ScoreCard(score = 87, trend = "+12 from last week", supportingText = "Strong protection posture")
                Spacer(modifier = Modifier.height(SentinelSpacing.MD))
                ScoreCard(score = 45, trend = "-8 from last week", supportingText = "Attention needed")
            }
        }
    }
}

@Preview(name = "Light Theme - StatisticCard", showBackground = true)
@Composable
private fun StatisticCardLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                StatisticCard(title = "Scans today", value = "1,284", subtitle = "+14% vs yesterday")
                StatisticCard(title = "Blocked", value = "37", icon = {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.size(SentinelSize.IconMedium),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "\u26A0", style = MaterialTheme.typography.titleLarge)
                    }
                })
            }
        }
    }
}

@Preview(name = "Dark Theme - StatisticCard", showBackground = true)
@Composable
private fun StatisticCardDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                StatisticCard(title = "Scans today", value = "1,284", subtitle = "+14% vs yesterday")
                StatisticCard(title = "Blocked", value = "37", icon = {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.size(SentinelSize.IconMedium),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "\u26A0", style = MaterialTheme.typography.titleLarge)
                    }
                })
            }
        }
    }
}

@Preview(name = "Light Theme - MetricCard", showBackground = true)
@Composable
private fun MetricCardLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(modifier = Modifier.padding(SentinelSpacing.MD), horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                MetricCard(label = "Threats", value = "12", state = RiskState.Safe, supportingText = "All clear")
                MetricCard(label = "Warnings", value = "3", state = RiskState.Suspicious, supportingText = "Review recommended")
                MetricCard(label = "Critical", value = "1", state = RiskState.Dangerous, supportingText = "Immediate action")
            }
        }
    }
}

@Preview(name = "Dark Theme - MetricCard", showBackground = true)
@Composable
private fun MetricCardDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(modifier = Modifier.padding(SentinelSpacing.MD), horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                MetricCard(label = "Threats", value = "12", state = RiskState.Safe, supportingText = "All clear")
                MetricCard(label = "Warnings", value = "3", state = RiskState.Suspicious, supportingText = "Review recommended")
                MetricCard(label = "Critical", value = "1", state = RiskState.Dangerous, supportingText = "Immediate action")
            }
        }
    }
}

@Preview(name = "Light Theme - ProtectionStatusCard", showBackground = true)
@Composable
private fun ProtectionStatusCardLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ProtectionStatusCard(state = RiskState.Safe, title = "Shield Online", description = "Monitoring notifications and messages.")
                ProtectionStatusCard(state = RiskState.Scanning, title = "Scanning", description = "Inspecting new message patterns.")
                ProtectionStatusCard(state = RiskState.Dangerous, title = "Threat Detected", description = "High-risk activity requires review.")
            }
        }
    }
}

@Preview(name = "Dark Theme - ProtectionStatusCard", showBackground = true)
@Composable
private fun ProtectionStatusCardDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ProtectionStatusCard(state = RiskState.Safe, title = "Shield Online", description = "Monitoring notifications and messages.")
                ProtectionStatusCard(state = RiskState.Scanning, title = "Scanning", description = "Inspecting new message patterns.")
                ProtectionStatusCard(state = RiskState.Dangerous, title = "Threat Detected", description = "High-risk activity requires review.")
            }
        }
    }
}

@Preview(name = "Light Theme - QuickActionCard", showBackground = true)
@Composable
private fun QuickActionCardLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                QuickActionCard(
                    icon = {
                        Text(text = "\uD83D\uDD04", style = MaterialTheme.typography.titleLarge)
                    },
                    title = "Run scan",
                    subtitle = "Check for new threats now",
                    onClick = {}
                )
                QuickActionCard(
                    icon = {
                        Text(text = "\uD83D\uDC40", style = MaterialTheme.typography.titleLarge)
                    },
                    title = "Review history",
                    subtitle = "See past detections",
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Dark Theme - QuickActionCard", showBackground = true)
@Composable
private fun QuickActionCardDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                QuickActionCard(
                    icon = {
                        Text(text = "\uD83D\uDD04", style = MaterialTheme.typography.titleLarge)
                    },
                    title = "Run scan",
                    subtitle = "Check for new threats now",
                    onClick = {}
                )
                QuickActionCard(
                    icon = {
                        Text(text = "\uD83D\uDC40", style = MaterialTheme.typography.titleLarge)
                    },
                    title = "Review history",
                    subtitle = "See past detections",
                    onClick = {}
                )
            }
        }
    }
}
