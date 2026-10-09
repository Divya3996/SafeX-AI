package com.sentinel.ai.core.model

/** Versioned, bounded model features; addresses and numbers never become vocabulary. */
object TextModelFeatures {
    const val VERSION = "unicode_word_bigram_char3_masked_v1"
    private val words = Regex("[\\p{L}\\p{M}\\p{N}_]{2,}")
    private val urls = Regex("(?i)\\b(?:https?|hxxps?)://\\S+|\\bwww\\.\\S+")
    private val emails = Regex("[\\p{L}\\p{M}\\p{N}_.+-]+@[\\p{L}\\p{M}\\p{N}_.-]+\\.[a-z]{2,}")
    private val numbers = Regex("\\p{Nd}+")

    fun normalize(text: String): String = MessageSignals.normalize(text.take(12000))
        .replace(urls, " urltoken ")
        .replace(emails, " emailtoken ")
        .replace(numbers, " numbertoken ")

    fun tokens(text: String): List<String> = words.findAll(normalize(text)).take(512).map { it.value }.toList()

    fun extract(text: String): Set<String> {
        val tokens = tokens(text)
        return buildSet {
            addAll(tokens)
            tokens.zipWithNext { a, b -> "$a $b" }.forEach(::add)
            tokens.filter { it.length in 3..32 }.forEach { token ->
                val padded = "^$token$"
                for (i in 0 until padded.length - 2) add("~" + padded.substring(i, i + 3))
            }
        }
    }
}
