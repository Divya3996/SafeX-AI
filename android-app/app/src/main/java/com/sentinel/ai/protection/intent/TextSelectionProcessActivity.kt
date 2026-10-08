package com.sentinel.ai.protection.intent

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sentinel.ai.core.feature.FeatureManager
import com.sentinel.ai.core.validation.UrlInputValidator

class TextSelectionProcessActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!FeatureManager.isTextEnabled()) { finish(); return }
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.trim().orEmpty()
        if (text.isBlank()) { finish(); return }
        startActivity(Intent(this, ScanLoadingActivity::class.java).apply {
            putExtra(IntentPayloadExtras.EXTRA_PAYLOAD_TYPE, if (UrlInputValidator.isValid(text)) IntentPayloadExtras.TYPE_URL else IntentPayloadExtras.TYPE_TEXT)
            putExtra(IntentPayloadExtras.EXTRA_PAYLOAD_VALUE, text)
        })
        finish()
    }
}
