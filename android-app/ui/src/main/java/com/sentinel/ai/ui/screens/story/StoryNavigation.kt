package com.sentinel.ai.ui.screens.story

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.sentinel.ai.core.i18n.I18n
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.core.model.ReviewScope
import com.sentinel.ai.core.story.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StoryEntryPoint { fun story(): StoryController }

object StoryNavigation {
    const val REQUEST = "safex_open_story"
    fun canOffer(result: ScanResult) = !result.target.isNullOrBlank() && result.target!!.length <= StoryLimits.ITEM_CHARS &&
        result.contentType != "file" && !result.target!!.startsWith("content://")
    fun offer(context: Context, result: ScanResult, floating: Boolean = false) {
        if (!canOffer(result)) return
        val controller = EntryPointAccessors.fromApplication(context.applicationContext, StoryEntryPoint::class.java).story()
        val source = when {
            result.provenance?.scope == ReviewScope.QR || result.contentType.contains("qr", true) -> StorySource.QR
            result.contentType == "link" -> StorySource.LINK
            floating -> StorySource.FLOATING
            else -> StorySource.RESULT
        }
        val note = if (floating) "Added from a reviewed floating scan. Confirm that it belongs to this situation."
            else "Added from a scan result. Confirm that it belongs to this situation."
        if (!controller.prepareHandoff(result.target!!, source, result.provenance?.edited == true, note, result))
            Toast.makeText(context, I18n.translate(context, "Finish the current reviewed input before adding another scan. Your existing draft was kept."), Toast.LENGTH_LONG).show()
        context.startActivity(Intent(context, com.sentinel.ai.ui.MainActivity::class.java)
            .putExtra(REQUEST, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }
}
