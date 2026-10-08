package com.sentinel.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sentinel.ai.ui.theme.SentinelSpacing
import com.sentinel.ai.ui.theme.SentinelSize
import com.sentinel.ai.ui.theme.SentinelTheme

@Preview(name = "Light Theme - LoadingShield", showBackground = true)
@Composable
private fun LoadingShieldLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.LG)) {
                LoadingShield(loadingText = "Scanning for threats...")
                LoadingShield(loadingText = "Analyzing patterns...", progress = 0.65f)
            }
        }
    }
}

@Preview(name = "Dark Theme - LoadingShield", showBackground = true)
@Composable
private fun LoadingShieldDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.LG)) {
                LoadingShield(loadingText = "Scanning for threats...")
                LoadingShield(loadingText = "Analyzing patterns...", progress = 0.65f)
            }
        }
    }
}

@Preview(name = "Light Theme - ScanProgressIndicator", showBackground = true)
@Composable
private fun ScanProgressIndicatorLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.LG)) {
                ScanProgressIndicator(progress = 0.25f, title = "Scanning", subtitle = "Checking notifications")
                ScanProgressIndicator(progress = 0.75f, title = "Analyzing", subtitle = "Identifying risks")
                ScanProgressIndicator(progress = 1f, showCircular = false, title = "Complete")
            }
        }
    }
}

@Preview(name = "Dark Theme - ScanProgressIndicator", showBackground = true)
@Composable
private fun ScanProgressIndicatorDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.LG)) {
                ScanProgressIndicator(progress = 0.25f, title = "Scanning", subtitle = "Checking notifications")
                ScanProgressIndicator(progress = 0.75f, title = "Analyzing", subtitle = "Identifying risks")
                ScanProgressIndicator(progress = 1f, showCircular = false, title = "Complete")
            }
        }
    }
}

@Preview(name = "Light Theme - ScanChecklistItem", showBackground = true)
@Composable
private fun ScanChecklistItemLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)) {
                ScanChecklistItem(title = "Check permissions", state = ScanStepState.Completed)
                ScanChecklistItem(title = "Scan notifications", state = ScanStepState.Active, description = "Processing 128 messages")
                ScanChecklistItem(title = "Analyze risks", state = ScanStepState.Pending)
                ScanChecklistItem(title = "Review patterns", state = ScanStepState.Failed, description = "Service unavailable")
            }
        }
    }
}

@Preview(name = "Dark Theme - ScanChecklistItem", showBackground = true)
@Composable
private fun ScanChecklistItemDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)) {
                ScanChecklistItem(title = "Check permissions", state = ScanStepState.Completed)
                ScanChecklistItem(title = "Scan notifications", state = ScanStepState.Active, description = "Processing 128 messages")
                ScanChecklistItem(title = "Analyze risks", state = ScanStepState.Pending)
                ScanChecklistItem(title = "Review patterns", state = ScanStepState.Failed, description = "Service unavailable")
            }
        }
    }
}

@Preview(name = "Light Theme - ScanStep", showBackground = true)
@Composable
private fun ScanStepLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)) {
                ScanStep(stepNumber = 1, title = "Initialize", subtitle = "Setup scan environment", state = ScanStepState.Completed)
                ScanStep(stepNumber = 2, title = "Scan notifications", subtitle = "Processing incoming messages", state = ScanStepState.Active)
                ScanStep(stepNumber = 3, title = "Analyze threats", subtitle = "Risk assessment", state = ScanStepState.Pending)
                ScanStep(stepNumber = 4, title = "Generate report", state = ScanStepState.Failed)
            }
        }
    }
}

@Preview(name = "Dark Theme - ScanStep", showBackground = true)
@Composable
private fun ScanStepDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)) {
                ScanStep(stepNumber = 1, title = "Initialize", subtitle = "Setup scan environment", state = ScanStepState.Completed)
                ScanStep(stepNumber = 2, title = "Scan notifications", subtitle = "Processing incoming messages", state = ScanStepState.Active)
                ScanStep(stepNumber = 3, title = "Analyze threats", subtitle = "Risk assessment", state = ScanStepState.Pending)
                ScanStep(stepNumber = 4, title = "Generate report", state = ScanStepState.Failed)
            }
        }
    }
}

@Preview(name = "Light Theme - ScanStatusRow", showBackground = true)
@Composable
private fun ScanStatusRowLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)) {
                ScanStatusRow(
                    icon = {
                        Text(
                            text = "\u2713",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    statusText = "Scan complete",
                    supportingText = "No threats found"
                )
                ScanStatusRow(
                    icon = {
                        Text(
                            text = "\u26A0",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    },
                    statusText = "Scanning",
                    supportingText = "Checking 142 notifications",
                    trailing = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(SentinelSize.IconSmall),
                            strokeWidth = 2.dp
                        )
                    }
                )
            }
        }
    }
}

@Preview(name = "Dark Theme - ScanStatusRow", showBackground = true)
@Composable
private fun ScanStatusRowDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)) {
                ScanStatusRow(
                    icon = {
                        Text(
                            text = "\u2713",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    statusText = "Scan complete",
                    supportingText = "No threats found"
                )
                ScanStatusRow(
                    icon = {
                        Text(
                            text = "\u26A0",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    },
                    statusText = "Scanning",
                    supportingText = "Checking 142 notifications",
                    trailing = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(SentinelSize.IconSmall),
                            strokeWidth = 2.dp
                        )
                    }
                )
            }
        }
    }
}
