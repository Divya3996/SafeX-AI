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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sentinel.ai.core.model.RiskLevel
import com.sentinel.ai.ui.theme.SentinelSpacing
import com.sentinel.ai.ui.theme.SentinelTheme

@Preview(name = "Light Theme - ThreatCard", showBackground = true)
@Composable
private fun ThreatCardLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ThreatCard(
                    title = "Suspicious login attempt",
                    source = "Banking App",
                    riskLevel = RiskLevel.RED,
                    timestampLabel = "Jun 12, 2026 - 3:45 PM",
                    description = "Unusual login detected from an unrecognized device in a different country."
                )
                ThreatCard(
                    title = "Phishing link detected",
                    source = "Messages",
                    riskLevel = RiskLevel.YELLOW,
                    timestampLabel = "Jun 11, 2026 - 11:20 AM",
                    description = "A shortened URL was found in an incoming message.",
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Dark Theme - ThreatCard", showBackground = true)
@Composable
private fun ThreatCardDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ThreatCard(
                    title = "Suspicious login attempt",
                    source = "Banking App",
                    riskLevel = RiskLevel.RED,
                    timestampLabel = "Jun 12, 2026 - 3:45 PM",
                    description = "Unusual login detected from an unrecognized device in a different country."
                )
                ThreatCard(
                    title = "Phishing link detected",
                    source = "Messages",
                    riskLevel = RiskLevel.YELLOW,
                    timestampLabel = "Jun 11, 2026 - 11:20 AM",
                    description = "A shortened URL was found in an incoming message.",
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Light Theme - ThreatTimelineItem", showBackground = true)
@Composable
private fun ThreatTimelineItemLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ThreatTimelineItem(time = "3:45 PM", title = "Login blocked", isLast = true)
                ThreatTimelineItem(time = "3:44 PM", title = "Suspicious activity detected")
                ThreatTimelineItem(time = "3:42 PM", title = "Scan started", isLast = false, onClick = {})
            }
        }
    }
}

@Preview(name = "Dark Theme - ThreatTimelineItem", showBackground = true)
@Composable
private fun ThreatTimelineItemDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ThreatTimelineItem(time = "3:45 PM", title = "Login blocked", isLast = true)
                ThreatTimelineItem(time = "3:44 PM", title = "Suspicious activity detected")
                ThreatTimelineItem(time = "3:42 PM", title = "Scan started", isLast = false, onClick = {})
            }
        }
    }
}

@Preview(name = "Light Theme - ThreatHistoryCard", showBackground = true)
@Composable
private fun ThreatHistoryCardLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ThreatHistoryCard(
                    summary = "Potential phishing link in SMS",
                    status = RiskState.Suspicious,
                    risk = RiskLevel.YELLOW,
                    date = "Jun 11, 2026",
                    supportingInfo = "The message contained a shortened URL that redirected to a known fraudulent domain."
                )
                ThreatHistoryCard(
                    summary = "Fake support request",
                    status = RiskState.Dangerous,
                    risk = RiskLevel.RED,
                    date = "Jun 10, 2026",
                    supportingInfo = "Sender impersonated a trusted brand and requested OTP verification."
                )
            }
        }
    }
}

@Preview(name = "Dark Theme - ThreatHistoryCard", showBackground = true)
@Composable
private fun ThreatHistoryCardDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ThreatHistoryCard(
                    summary = "Potential phishing link in SMS",
                    status = RiskState.Suspicious,
                    risk = RiskLevel.YELLOW,
                    date = "Jun 11, 2026",
                    supportingInfo = "The message contained a shortened URL that redirected to a known fraudulent domain."
                )
                ThreatHistoryCard(
                    summary = "Fake support request",
                    status = RiskState.Dangerous,
                    risk = RiskLevel.RED,
                    date = "Jun 10, 2026",
                    supportingInfo = "Sender impersonated a trusted brand and requested OTP verification."
                )
            }
        }
    }
}

@Preview(name = "Light Theme - ThreatExplanationCard", showBackground = true)
@Composable
private fun ThreatExplanationCardLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ThreatExplanationCard(
                    explanation = "The message uses urgency pressure and requests an OTP code, which are common social-engineering signals.",
                    recommendation = "Do not share the OTP. Verify the sender through an official channel before taking any action.",
                    icon = {
                        Text(text = "\u26A0\uFE0F", style = MaterialTheme.typography.titleLarge)
                    }
                )
            }
        }
    }
}

@Preview(name = "Dark Theme - ThreatExplanationCard", showBackground = true)
@Composable
private fun ThreatExplanationCardDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ThreatExplanationCard(
                    explanation = "The message uses urgency pressure and requests an OTP code, which are common social-engineering signals.",
                    recommendation = "Do not share the OTP. Verify the sender through an official channel before taking any action.",
                    icon = {
                        Text(text = "\u26A0\uFE0F", style = MaterialTheme.typography.titleLarge)
                    }
                )
            }
        }
    }
}

@Preview(name = "Light Theme - ExpandableThreatCard", showBackground = true)
@Composable
private fun ExpandableThreatCardLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            var expanded by remember { mutableStateOf(false) }
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ExpandableThreatCard(
                    title = "Suspicious login attempt",
                    summary = "Unusual login detected from an unrecognized device in a different country. Immediate review recommended.",
                    isExpanded = expanded,
                    onExpandedChange = { expanded = it },
                    expandedContent = {
                        Text(
                            text = "The login originated from a region that has not been used before. Combined with the time of day, this triggers a high-risk classification.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    riskLevel = RiskLevel.RED
                )
            }
        }
    }
}

@Preview(name = "Dark Theme - ExpandableThreatCard", showBackground = true)
@Composable
private fun ExpandableThreatCardDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            var expanded by remember { mutableStateOf(false) }
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ExpandableThreatCard(
                    title = "Suspicious login attempt",
                    summary = "Unusual login detected from an unrecognized device in a different country. Immediate review recommended.",
                    isExpanded = expanded,
                    onExpandedChange = { expanded = it },
                    expandedContent = {
                        Text(
                            text = "The login originated from a region that has not been used before. Combined with the time of day, this triggers a high-risk classification.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    riskLevel = RiskLevel.RED
                )
            }
        }
    }
}
