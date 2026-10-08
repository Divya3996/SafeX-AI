package com.sentinel.ai.protection.intent

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sentinel.ai.core.data.ScanRepository
import com.sentinel.ai.core.model.*
import com.sentinel.ai.protection.intent.link.BrowserLauncher
import com.sentinel.ai.ui.screens.scanner.AnalysisResultContent
import com.sentinel.ai.ui.theme.SentinelTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

@AndroidEntryPoint
class ScanLoadingActivity : ComponentActivity() {
    @Inject lateinit var repository: ScanRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val type = intent.getStringExtra(IntentPayloadExtras.EXTRA_PAYLOAD_TYPE)
        val value = intent.getStringExtra(IntentPayloadExtras.EXTRA_PAYLOAD_VALUE).orEmpty()
        setContent {
            SentinelTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                        var result by remember { mutableStateOf<ScanResult?>(null) }
                        var error by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(type, value) {
                            try {
                                result = when (type) {
                                    IntentPayloadExtras.TYPE_URL -> repository.scanLink(value)
                                    IntentPayloadExtras.TYPE_FILE -> repository.scanFile(Uri.parse(value))
                                    IntentPayloadExtras.TYPE_TEXT -> repository.scanText(value)
                                    "image" -> repository.scanImage(Uri.parse(value))
                                    else -> error("Unsupported shared content.")
                                }
                            } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "Unable to analyze this item." }
                        }
                        result?.let { completed ->
                            AnalysisResultContent(completed, onClose = { finish() }, onOpen = {
                                if (DecisionPolicy.canOpen(completed)) {
                                    if (BrowserLauncher().launch(this@ScanLoadingActivity, completed.target!!)) finish()
                                    else Toast.makeText(this@ScanLoadingActivity, com.sentinel.ai.core.i18n.I18n.translate(this@ScanLoadingActivity, "No browser available."), Toast.LENGTH_LONG).show()
                                }
                            })
                        } ?: Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            if (error == null) { CircularProgressIndicator(); Spacer(Modifier.height(20.dp)); Text("Analyzing on your device…") }
                            else { Text(error!!); Spacer(Modifier.height(20.dp)); Button(onClick = { finish() }) { Text("Close") } }
                        }
                    }
                }
            }
        }
    }
}
internal fun continueButtonText(decision: ProtectionDecision): String? = when (decision) {
    ProtectionDecision.ALLOW -> "Continue"
    ProtectionDecision.WARN -> "Continue Anyway"
    ProtectionDecision.BLOCK -> null
}
