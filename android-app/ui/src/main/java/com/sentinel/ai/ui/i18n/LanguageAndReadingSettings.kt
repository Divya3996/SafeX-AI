package com.sentinel.ai.ui.i18n

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.ui.i18n.LocalizedText as Text
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageAndReadingSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by DisplayPreferences.settings.collectAsState()
    var readingScale by remember(settings.textScale) { mutableFloatStateOf(settings.textScale) }
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Language & reading", style = MaterialTheme.typography.titleLarge)
            Text("Choose the language for menus, explanations and warnings.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    FilterChip(selected = settings.language == language,
                        onClick = { DisplayPreferences.setLanguage(context, language) },
                        modifier = Modifier.testTag("language_${language.tag}"),
                        label = { Text(language.nativeName, localize = false) })
                }
            }
            Text("Text size", style = MaterialTheme.typography.titleMedium)
            Text("${(readingScale * 100).roundToInt()}%", modifier = Modifier.testTag("text_scale"), localize = false)
            Slider(value = readingScale, onValueChange = { readingScale = it },
                onValueChangeFinished = { DisplayPreferences.setTextScale(readingScale) },
                valueRange = .85f..1.5f, steps = 12, modifier = Modifier.testTag("text_size_slider"))
            Text("Adjust text size without changing your phone settings.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Preview: clear guidance, in your language.", style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = { DisplayPreferences.setTextScale(1f) }) { Text("Reset text size") }
        }
    }
}
