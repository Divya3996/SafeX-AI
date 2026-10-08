package com.sentinel.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sentinel.ai.ui.theme.SentinelSize
import com.sentinel.ai.ui.theme.SentinelTheme

@Preview(name = "Light Theme", showBackground = true)
@Composable
private fun SentinelShieldLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SentinelShield(
                modifier = Modifier.size(SentinelSize.IconXL),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = "Sentinel shield"
            )
        }
    }
}

@Preview(name = "Dark Theme", showBackground = true)
@Composable
private fun SentinelShieldDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SentinelShield(
                modifier = Modifier.size(SentinelSize.IconXL),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = "Sentinel shield"
            )
        }
    }
}

@Preview(name = "Scanning State", showBackground = true)
@Composable
private fun AnimatedSentinelShieldScanningPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(verticalArrangement = Arrangement.Center) {
                AnimatedSentinelShield(
                    state = ShieldState.Scanning,
                    modifier = Modifier.size(SentinelSize.IconXL),
                    contentDescription = "Scanning"
                )
            }
        }
    }
}

@Preview(name = "Safe State", showBackground = true)
@Composable
private fun AnimatedSentinelShieldSafePreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(verticalArrangement = Arrangement.Center) {
                AnimatedSentinelShield(
                    state = ShieldState.Safe,
                    modifier = Modifier.size(SentinelSize.IconXL),
                    contentDescription = "Safe"
                )
            }
        }
    }
}

@Preview(name = "Dangerous State", showBackground = true)
@Composable
private fun AnimatedSentinelShieldDangerousPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(verticalArrangement = Arrangement.Center) {
                AnimatedSentinelShield(
                    state = ShieldState.Dangerous,
                    modifier = Modifier.size(SentinelSize.IconXL),
                    contentDescription = "Dangerous"
                )
            }
        }
    }
}
