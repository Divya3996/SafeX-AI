package com.sentinel.ai.ml

import android.content.Context
import com.sentinel.ai.core.model.MessageSignals
import org.json.JSONObject
import kotlin.math.exp

/** Small trained n-gram logistic model. Synthetic training scope is disclosed in the UI/docs. */
class TextInferenceManager(context: Context) {
    private val weights: Map<String, Double>
    private val intercept: Double
    init {
        val json = JSONObject(context.assets.open("text-model.json").bufferedReader().use { it.readText() })
        val vocab = json.getJSONArray("vocabulary")
        val coefficients = json.getJSONArray("weights")
        require(vocab.length() == coefficients.length())
        weights = (0 until vocab.length()).associate { vocab.getString(it) to coefficients.getDouble(it) }
        intercept = json.getDouble("intercept")
        require(intercept.isFinite() && weights.values.all { it.isFinite() })
    }
    fun predict(text: String): Float {
        val tokens = Regex("[\\p{L}\\p{M}\\p{N}_]{2,}").findAll(MessageSignals.normalize(text)).map { it.value }.toList()
        val terms = (tokens + tokens.zipWithNext { a, b -> "$a $b" }).toSet()
        val logit = terms.fold(intercept) { sum, term -> sum + (weights[term] ?: 0.0) }.coerceIn(-30.0, 30.0)
        return (1.0 / (1.0 + exp(-logit))).toFloat()
    }
}
