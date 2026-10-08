package com.sentinel.ai.core.feature

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppLanguage(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"), HINDI("hi", "हिन्दी"), GUJARATI("gu", "ગુજરાતી");
    companion object { fun fromTag(tag: String) = entries.firstOrNull { it.tag == tag } ?: ENGLISH }
}
data class DisplaySettings(val language: AppLanguage = AppLanguage.ENGLISH, val textScale: Float = 1f)

/** Independent of phone language and font size; applied immediately across Compose screens. */
object DisplayPreferences {
    private var preferences: android.content.SharedPreferences? = null
    private val state = MutableStateFlow(DisplaySettings())
    val settings = state.asStateFlow()
    val current get() = state.value
    fun init(context: Context) {
        if (preferences != null) return
        preferences = context.applicationContext.getSharedPreferences("safex_display", Context.MODE_PRIVATE)
        state.value = DisplaySettings(AppLanguage.fromTag(preferences!!.getString("language", "en")!!),
            preferences!!.getFloat("textScale", 1f).coerceIn(.85f, 1.5f))
        syncSystemLanguage(context)
    }
    fun setLanguage(context: Context, language: AppLanguage) {
        state.value = current.copy(language = language)
        preferences?.edit()?.putString("language", language.tag)?.apply()
        if (Build.VERSION.SDK_INT >= 33) context.getSystemService(android.app.LocaleManager::class.java)
            .applicationLocales = LocaleList.forLanguageTags(language.tag)
    }
    fun setTextScale(value: Float) {
        val scale = value.coerceIn(.85f, 1.5f)
        state.value = current.copy(textScale = scale)
        preferences?.edit()?.putFloat("textScale", scale)?.apply()
    }
    fun syncSystemLanguage(context: Context) {
        if (Build.VERSION.SDK_INT >= 33) {
            val tags = context.getSystemService(android.app.LocaleManager::class.java).applicationLocales.toLanguageTags()
            if (tags.isNotBlank()) {
                val language = AppLanguage.fromTag(tags.substringBefore(',').substringBefore('-'))
                state.value = current.copy(language = language)
                preferences?.edit()?.putString("language", language.tag)?.apply()
            }
        }
    }
    fun localizedContext(context: Context, language: AppLanguage = current.language): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(language.tag))
        return context.createConfigurationContext(config)
    }
}
