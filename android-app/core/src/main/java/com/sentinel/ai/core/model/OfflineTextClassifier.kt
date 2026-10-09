package com.sentinel.ai.core.model

import kotlin.math.exp

/** Small immutable CPU classifier. A positive score can WARN, never independently BLOCK. */
class OfflineTextClassifier(
    vocabulary: List<String>, coefficients: List<Double>,
    private val intercept: Double, val warningThreshold: Float,
    val featureVersion: String? = null,
) {
    private val weights: Map<String, Double>

    init {
        require(vocabulary.size in 1..16000 && vocabulary.size == coefficients.size)
        require(vocabulary.distinct().size == vocabulary.size && vocabulary.all { it.isNotBlank() && it.length <= 256 })
        require(intercept.isFinite() && coefficients.all(Double::isFinite))
        require(warningThreshold.isFinite() && warningThreshold in 0.1f..0.999f)
        require(featureVersion == null || featureVersion == TextModelFeatures.VERSION)
        weights = vocabulary.zip(coefficients).toMap()
    }

    fun predict(text: String): Float {
        if (text.isBlank()) return 0f
        val terms = if (featureVersion == TextModelFeatures.VERSION) TextModelFeatures.extract(text) else {
            val tokens = Regex("[\\p{L}\\p{M}\\p{N}_]{2,}")
                .findAll(MessageSignals.normalize(text.take(12000))).take(512).map { it.value }.toList()
            (tokens + tokens.zipWithNext { a, b -> "$a $b" }).toSet()
        }
        val logit = terms.fold(intercept) { sum, term -> sum + (weights[term] ?: 0.0) }.coerceIn(-30.0, 30.0)
        return (1.0 / (1.0 + exp(-logit))).toFloat()
    }

    /** Conservative vocabulary guard for a separately calibrated WARN path.
     * This checks familiar Latin-script words, not language identity or proven coverage.
     * Native-script messages still use the existing action-context rules.
     */
    fun supportsContextFreeWarning(text: String): Boolean {
        if (featureVersion != TextModelFeatures.VERSION) return false
        val tokens = TextModelFeatures.tokens(text).toSet() - setOf("urltoken", "emailtoken", "numbertoken")
        if (tokens.size < 4) return false
        val letters = tokens.flatMap { it.toList() }.filter(Char::isLetter)
        if (letters.isEmpty() || letters.count { it in 'a'..'z' }.toDouble() / letters.size < .95) return false
        return tokens.count(weights::containsKey).toDouble() / tokens.size >= .60
    }
}
