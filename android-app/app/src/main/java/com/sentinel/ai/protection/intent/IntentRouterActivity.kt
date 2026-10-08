package com.sentinel.ai.protection.intent

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.sentinel.ai.core.feature.FeatureManager
import com.sentinel.ai.protection.intent.link.BrowserLauncher

class IntentRouterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            Toast.makeText(this, com.sentinel.ai.core.i18n.I18n.translate(this, "Share one item at a time for a complete scan."), Toast.LENGTH_LONG).show()
            finish(); return
        }
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim()
        val stream = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else { @Suppress("DEPRECATION") intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) }
        val uri = stream ?: intent.data ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        val type: String
        val value: String
        when {
            uri?.scheme in listOf("http", "https") -> { type = IntentPayloadExtras.TYPE_URL; value = uri.toString() }
            uri?.scheme == "content" -> { type = if (intent.type?.startsWith("image/") == true) "image" else IntentPayloadExtras.TYPE_FILE; value = uri.toString() }
            !text.isNullOrBlank() -> {
                type = if (com.sentinel.ai.core.validation.UrlInputValidator.isValid(text)) IntentPayloadExtras.TYPE_URL else IntentPayloadExtras.TYPE_TEXT
                value = text
            }
            else -> { Toast.makeText(this, com.sentinel.ai.core.i18n.I18n.translate(this, "Share a message, web link, or a supported file."), Toast.LENGTH_LONG).show(); finish(); return }
        }
        if (type == IntentPayloadExtras.TYPE_URL && !FeatureManager.isClickEnabled() && intent.action == Intent.ACTION_VIEW) {
            if (!BrowserLauncher().launch(this, value)) Toast.makeText(this, com.sentinel.ai.core.i18n.I18n.translate(this, "No browser available."), Toast.LENGTH_LONG).show()
            finish(); return
        }
        startActivity(Intent(this, ScanLoadingActivity::class.java).apply {
            putExtra(IntentPayloadExtras.EXTRA_PAYLOAD_TYPE, type)
            putExtra(IntentPayloadExtras.EXTRA_PAYLOAD_VALUE, value)
            if (uri?.scheme == "content") {
                data = uri; clipData = android.content.ClipData.newRawUri("Shared item", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        })
        finish()
    }
}
