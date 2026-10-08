package com.sentinel.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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

@Preview(name = "Light Theme - ActionButton", showBackground = true)
@Composable
private fun ActionButtonLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ActionButton(text = "Scan now", onClick = {})
                ActionButton(text = "Loading", onClick = {}, loading = true, enabled = false)
                ActionButton(text = "Disabled", onClick = {}, enabled = false)
            }
        }
    }
}

@Preview(name = "Dark Theme - ActionButton", showBackground = true)
@Composable
private fun ActionButtonDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                ActionButton(text = "Scan now", onClick = {})
                ActionButton(text = "Loading", onClick = {}, loading = true, enabled = false)
                ActionButton(text = "Disabled", onClick = {}, enabled = false)
            }
        }
    }
}

@Preview(name = "Light Theme - SecondaryButton", showBackground = true)
@Composable
private fun SecondaryButtonLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(modifier = Modifier.padding(SentinelSpacing.MD), horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                SecondaryButton(text = "Cancel", onClick = {}, variant = ButtonVariant.Outlined)
                SecondaryButton(text = "Learn more", onClick = {}, variant = ButtonVariant.Text)
            }
        }
    }
}

@Preview(name = "Dark Theme - SecondaryButton", showBackground = true)
@Composable
private fun SecondaryButtonDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(modifier = Modifier.padding(SentinelSpacing.MD), horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                SecondaryButton(text = "Cancel", onClick = {}, variant = ButtonVariant.Outlined)
                SecondaryButton(text = "Learn more", onClick = {}, variant = ButtonVariant.Text)
            }
        }
    }
}

@Preview(name = "Light Theme - IconTextRow", showBackground = true)
@Composable
private fun IconTextRowLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)) {
                IconTextRow(
                    icon = { Icon(imageVector = Icons.Default.Check, contentDescription = null) },
                    title = "Protection active",
                    subtitle = "Monitoring notifications",
                    trailing = { Text(text = "ON", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                )
                IconTextRow(
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                    title = "Settings",
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Dark Theme - IconTextRow", showBackground = true)
@Composable
private fun IconTextRowDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)) {
                IconTextRow(
                    icon = { Icon(imageVector = Icons.Default.Check, contentDescription = null) },
                    title = "Protection active",
                    subtitle = "Monitoring notifications",
                    trailing = { Text(text = "ON", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                )
                IconTextRow(
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                    title = "Settings",
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Light Theme - InfoRow", showBackground = true)
@Composable
private fun InfoRowLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                InfoRow(label = "Version", value = "1.0.0", icon = {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null)
                })
                InfoRow(label = "Status", value = "Online", showDivider = true)
                InfoRow(label = "Last scan", value = "Just now", showDivider = true)
            }
        }
    }
}

@Preview(name = "Dark Theme - InfoRow", showBackground = true)
@Composable
private fun InfoRowDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                InfoRow(label = "Version", value = "1.0.0", icon = {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null)
                })
                InfoRow(label = "Status", value = "Online", showDivider = true)
                InfoRow(label = "Last scan", value = "Just now", showDivider = true)
            }
        }
    }
}

@Preview(name = "Light Theme - SettingRow", showBackground = true)
@Composable
private fun SettingRowLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)) {
                SettingRow(
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                    title = "Notifications",
                    description = "Allow real-time alerts",
                    trailing = { Text(text = "ON", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                )
                SettingRow(
                    icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null) },
                    title = "High contrast",
                    description = "Increase visual contrast",
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Dark Theme - SettingRow", showBackground = true)
@Composable
private fun SettingRowDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(SentinelSpacing.MD), verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)) {
                SettingRow(
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                    title = "Notifications",
                    description = "Allow real-time alerts",
                    trailing = { Text(text = "ON", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                )
                SettingRow(
                    icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null) },
                    title = "High contrast",
                    description = "Increase visual contrast",
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Light Theme - EmptyState", showBackground = true)
@Composable
private fun EmptyStateLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            EmptyState(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(SentinelSize.IconXL),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                title = "No detections yet",
                description = "The backend will populate this list when threat events arrive.",
                action = {
                    ActionButton(text = "Run scan", onClick = {})
                }
            )
        }
    }
}

@Preview(name = "Dark Theme - EmptyState", showBackground = true)
@Composable
private fun EmptyStateDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            EmptyState(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(SentinelSize.IconXL),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                title = "No detections yet",
                description = "The backend will populate this list when threat events arrive.",
                action = {
                    ActionButton(text = "Run scan", onClick = {})
                }
            )
        }
    }
}

@Preview(name = "Light Theme - LoadingState", showBackground = true)
@Composable
private fun LoadingStateLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoadingState(message = "Scanning messages...", progress = 0.4f)
        }
    }
}

@Preview(name = "Dark Theme - LoadingState", showBackground = true)
@Composable
private fun LoadingStateDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoadingState(message = "Scanning messages...", progress = 0.4f)
        }
    }
}

@Preview(name = "Light Theme - ErrorState", showBackground = true)
@Composable
private fun ErrorStateLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ErrorState(
                title = "Something went wrong",
                description = "Unable to reach the protection service. Please check your connection and try again.",
                onRetry = {}
            )
        }
    }
}

@Preview(name = "Dark Theme - ErrorState", showBackground = true)
@Composable
private fun ErrorStateDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ErrorState(
                title = "Something went wrong",
                description = "Unable to reach the protection service. Please check your connection and try again.",
                onRetry = {}
            )
        }
    }
}
