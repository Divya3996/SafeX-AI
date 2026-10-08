package com.sentinel.ai.core.i18n

import android.content.Context
import com.sentinel.ai.core.feature.AppLanguage
import com.sentinel.ai.core.feature.DisplayPreferences

/** Translates trusted app copy. Never pass raw user messages, filenames or sender names here. */
object I18n {
    private val placeholder = Regex("%([0-9]+)\\\$s")
    private data class Template(val source: String, val regex: Regex, val parameters: List<Int>)
    private val templates by lazy {
        TranslationCatalog.entries.mapNotNull { (source, _) ->
            val parts = placeholder.findAll(source).toList()
            if (parts.isEmpty()) null else {
                var position = 0
                val pattern = buildString {
                    append("^")
                    for (part in parts) {
                        append(Regex.escape(source.substring(position, part.range.first)))
                        append("(.+?)")
                        position = part.range.last + 1
                    }
                    append(Regex.escape(source.substring(position))); append("$")
                }
                Template(source, Regex(pattern, RegexOption.DOT_MATCHES_ALL), parts.map { it.groupValues[1].toInt() })
            }
        }.sortedByDescending { it.source.length }
    }
    private val catalogs = mutableMapOf<AppLanguage, Map<String, String>>()
    @Synchronized private fun catalog(context: Context, language: AppLanguage): Map<String, String> =
        catalogs.getOrPut(language) {
            val resources = DisplayPreferences.localizedContext(context.applicationContext, language).resources
            TranslationCatalog.entries.associate { (source, id) -> source to resources.getString(id) }
        }

    fun translate(context: Context, text: String, language: AppLanguage = DisplayPreferences.current.language): String {
        if (language == AppLanguage.ENGLISH || text.isEmpty()) return text
        val strings = catalog(context, language)
        strings[text]?.let { return it }
        for (template in templates) {
            val match = template.regex.matchEntire(text) ?: continue
            val values = template.parameters.zip(match.groupValues.drop(1)).toMap()
            return placeholder.replace(strings.getValue(template.source)) { token ->
                val value = values.getValue(token.groupValues[1].toInt())
                // Nested canonical evidence is translated; unknown values such as URLs remain intact.
                translate(context, value, language)
            }
        }
        if ('\n' in text) return text.split('\n').joinToString("\n") { translate(context, it, language) }
        if (", " in text) return text.split(", ").joinToString(", ") { translate(context, it, language) }
        if (" • " in text) return text.split(" • ").joinToString(" • ") { translate(context, it, language) }
        if (". " in text) return text.split(". ").joinToString(". ") { part ->
            strings[part] ?: strings["$part."]?.trimEnd('.', '।') ?: translate(context, part, language)
        }
        return text
    }
}
