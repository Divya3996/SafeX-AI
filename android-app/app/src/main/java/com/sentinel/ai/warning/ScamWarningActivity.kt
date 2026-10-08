package com.sentinel.ai.warning

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.ui.screens.scanner.AnalysisResultContent
import com.sentinel.ai.ui.theme.SentinelTheme
import kotlinx.coroutines.flow.map

class ScamWarningActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); render() }
    private fun render() {
        enableEdgeToEdge()
        val id = intent.getStringExtra("scan_id") ?: return finish()
        setContent { SentinelTheme {
            val result by remember(id) { ThreatJournal.scanResults.map { list -> list.firstOrNull { it.id == id } } }.collectAsState(initial = null)
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    result?.let { AnalysisResultContent(it, { finish() }) } ?: Column(Modifier.fillMaxSize().padding(24.dp)) {
                        Text("This warning is no longer in your local history.")
                        Button(onClick = { finish() }) { Text("Close") }
                    }
                }
            }
        } }
    }
    companion object {
        fun newIntent(context: Context, result: ScanResult) = Intent(context, ScamWarningActivity::class.java).putExtra("scan_id", result.id)
    }
}
