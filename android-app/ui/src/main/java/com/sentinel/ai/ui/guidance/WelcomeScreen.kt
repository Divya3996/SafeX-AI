package com.sentinel.ai.ui.guidance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sentinel.ai.ui.components.SafeXLogo
import com.sentinel.ai.ui.i18n.LocalizedText as Text

private val WelcomePages = listOf(
    "Welcome to SafeX AI" to "Private security checks for messages, links, screenshots, QR codes and files. Learn what each finding means before deciding what to do.",
    "Your language. Your reading size." to "Choose the language and text size that feels comfortable. You can change both later in Settings.",
    "Private checks, honest limits" to "Scanning runs on your device without uploading the content. Warnings describe checked signals; a clear result is not proof of safety. Review OCR text and verify unexpected requests independently.",
    "Start with the features you need" to "Manual scans need no notification or overlay access. Optional permissions enable incoming-message checks and the floating shield. Next, review permissions at your pace and take a guided tour."
)

@Composable
fun WelcomeScreen(onIncidentHelp: () -> Unit = {}, onComplete: (tour: Boolean) -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SafeXLogo(Modifier.size(52.dp))
            TextButton(onClick = { onComplete(false) }, modifier = Modifier.testTag("welcome_skip")) { Text("Skip introduction") }
        }
        LinearProgressIndicator(progress = { (page + 1f) / WelcomePages.size }, modifier = Modifier.fillMaxWidth())
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(WelcomePages[page].first, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("welcome_title"))
            Text(WelcomePages[page].second, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (page == 1) com.sentinel.ai.ui.i18n.LanguageAndReadingSettings()
            if (page == 3) OutlinedCard(Modifier.fillMaxWidth()) { Text("You stay in control. The tour does not scan, grant permissions, place calls or send reports for you.", Modifier.padding(20.dp)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (page > 0) OutlinedButton(onClick = { page-- }, modifier = Modifier.testTag("welcome_back")) { Text("Back") }
            Button(onClick = { if (page == WelcomePages.lastIndex) onComplete(true) else page++ }, modifier = Modifier.weight(1f).testTag("welcome_next")) {
                Text(if (page == WelcomePages.lastIndex) "Review setup and take tour" else "Next")
            }
        }
        TextButton(onClick = onIncidentHelp, modifier = Modifier.fillMaxWidth().testTag("welcome_incident_help")) { Text("Need help after a scam?") }
    }
}

@Composable
fun FeatureGuideScreen(onStartTour: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Learn SafeX AI", style = MaterialTheme.typography.headlineLarge)
        Text("Choose a feature to see its real control highlighted. Close the tour anytime, then try it yourself. No scan or permission is triggered by the tour.")
        Button(onClick = { onStartTour(0) }, modifier = Modifier.fillMaxWidth().testTag("replay_full_tour")) { Text("Take the full feature tour") }
        AppGuideSteps.forEachIndexed { index, step ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(step.title, style = MaterialTheme.typography.titleMedium)
                    Text(step.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onStartTour(index) }, modifier = Modifier.testTag("replay_${step.target}")) { Text("Show me this feature") }
                }
            }
        }
    }
}
