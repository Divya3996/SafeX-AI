package com.sentinel.ai.ui.screens.about

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.sentinel.ai.ui.theme.SentinelTheme

@Preview(name = "Dark", showBackground = true)
@Composable
private fun AboutDarkPreview() {
    SentinelTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AboutScreen(appVersion = "1.0.0")
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun AboutLightPreview() {
    SentinelTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AboutScreen(appVersion = "1.0.0")
        }
    }
}
