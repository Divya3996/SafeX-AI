package com.sentinel.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

@Preview(name = "Light Theme - Filled", showBackground = true)
@Composable
private fun SentinelCardLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(SentinelSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)
            ) {
                SentinelCard {
                    Text(
                        text = "Filled Card",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(SentinelSpacing.SM))
                    Text(
                        text = "Default filled variant using design tokens.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(name = "Dark Theme - Filled", showBackground = true)
@Composable
private fun SentinelCardDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(SentinelSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)
            ) {
                SentinelCard {
                    Text(
                        text = "Filled Card",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(SentinelSpacing.SM))
                    Text(
                        text = "Default filled variant using design tokens.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Theme - Elevated", showBackground = true)
@Composable
private fun ElevatedSentinelCardLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(SentinelSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)
            ) {
                ElevatedSentinelCard {
                    Text(
                        text = "Elevated Card",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(SentinelSpacing.SM))
                    Text(
                        text = "Raised surface with subtle elevation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(name = "Dark Theme - Elevated", showBackground = true)
@Composable
private fun ElevatedSentinelCardDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(SentinelSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)
            ) {
                ElevatedSentinelCard {
                    Text(
                        text = "Elevated Card",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(SentinelSpacing.SM))
                    Text(
                        text = "Raised surface with subtle elevation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Theme - Outlined", showBackground = true)
@Composable
private fun OutlinedSentinelCardLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(SentinelSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)
            ) {
                OutlinedSentinelCard {
                    Text(
                        text = "Outlined Card",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(SentinelSpacing.SM))
                    Text(
                        text = "Transparent background with visible outline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(name = "Dark Theme - Outlined", showBackground = true)
@Composable
private fun OutlinedSentinelCardDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(SentinelSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)
            ) {
                OutlinedSentinelCard {
                    Text(
                        text = "Outlined Card",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(SentinelSpacing.SM))
                    Text(
                        text = "Transparent background with visible outline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
