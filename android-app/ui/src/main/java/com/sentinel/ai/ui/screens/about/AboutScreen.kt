package com.sentinel.ai.ui.screens.about

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sentinel.ai.ui.components.PremiumPanel
import com.sentinel.ai.ui.components.PremiumSectionTitle
import com.sentinel.ai.ui.components.RiskState
import com.sentinel.ai.ui.components.riskColor
import com.sentinel.ai.ui.theme.SentinelSpacing

@Composable
fun AboutScreen(
    appVersion: String
) {
    val displayVersion = if (appVersion == "1.0.0") "1.0" else appVersion

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = SentinelSpacing.LG),
        verticalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
    ) {
        AboutHeader()

        Spacer(modifier = Modifier.height(SentinelSpacing.MD))
        PremiumSectionTitle(text = "What it does")
        PremiumPanel {
            Text(
                text = "SafeX AI analyzes shared content and supported notifications locally, explains risk signals, and helps you make safer decisions.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(SentinelSpacing.XS))
        PremiumSectionTitle(text = "How it works")
        PremiumPanel {
            Column(verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)) {
                AboutBullet("Detects credential requests, financial pressure and suspicious links in English, Hindi and Gujarati.")
                AboutBullet("Screenshots are read offline in English, Hindi and Gujarati; unclear images may need a retry.")
                AboutBullet("Analyzes links before opening")
                AboutBullet("Uses on-device machine learning")
                AboutBullet("Checks available notification content in real time")
                AboutBullet("Text classifier trained on synthetic examples; real-world accuracy is not established")
                AboutBullet("Hidden previews, page content, and call audio are outside the current coverage")
            }
        }

        Spacer(modifier = Modifier.height(SentinelSpacing.XS))
        PremiumSectionTitle(text = "Privacy")
        PremiumPanel {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(riskColor(RiskState.Safe).copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = riskColor(RiskState.Safe),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Your data never leaves your device.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "All analysis is performed locally.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SentinelSpacing.XS))
        PremiumSectionTitle(text = "Version")
        Text(
            text = "Version $displayVersion",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = SentinelSpacing.XS)
        )
        Spacer(modifier = Modifier.height(SentinelSpacing.LG))
    }
}

@Composable
private fun AboutHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            com.sentinel.ai.ui.components.SafeXLogo(Modifier.size(64.dp))
        }
        Spacer(modifier = Modifier.height(SentinelSpacing.MD))
        Text(
            text = "SafeX AI",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(SentinelSpacing.XS))
        Text(
            text = "Private, on-device security assistance",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AboutBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(5.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
