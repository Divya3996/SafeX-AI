package com.sentinel.ai.ml

import android.content.Context
import com.sentinel.ai.core.model.OfflineTextClassifier
import org.json.JSONObject

/** Loads the bundled versioned research classifier; all inference remains local. */
class TextInferenceManager(context: Context) {
    private val classifier: OfflineTextClassifier
    val warningThreshold: Float get() = classifier.warningThreshold
    val usesResearchFeatures: Boolean get() = classifier.featureVersion != null
    private val contextFreeWarningThreshold: Float?
    init {
        val bytes = context.assets.open("text-model.json").use { input ->
            val data = input.readBytes()
            require(data.size <= 2 * 1024 * 1024) { "Text model exceeds loading limit" }
            data
        }
        val assetNames = context.assets.list("")?.toSet().orEmpty()
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        if ("text-model-card.json" in assetNames) {
            val card = JSONObject(context.assets.open("text-model-card.json").bufferedReader().use { it.readText() })
            require(hash == card.getString("sha256")) { "Text model does not match its metadata" }
        }
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        val vocab = json.getJSONArray("vocabulary")
        val coefficients = json.getJSONArray("weights")
        require(vocab.length() == coefficients.length())
        classifier = OfflineTextClassifier(
            (0 until vocab.length()).map { vocab.getString(it) },
            (0 until coefficients.length()).map { coefficients.getDouble(it) },
            json.getDouble("intercept"), json.optDouble("threshold", 0.60).toFloat(),
            if (json.has("feature_version")) json.getString("feature_version") else null,
        )
        contextFreeWarningThreshold = if (usesResearchFeatures && "text-warning-policy.json" in assetNames) {
            val policy = JSONObject(context.assets.open("text-warning-policy.json").bufferedReader().use { it.readText() })
            require(policy.getString("model_sha256") == hash && policy.getString("guard_version") == "latin_vocabulary_v1")
            require(policy.getInt("minimum_distinct_words") == 4 && policy.getDouble("minimum_latin_letter_fraction") == .95 && policy.getDouble("minimum_vocabulary_fraction") == .60)
            policy.getDouble("threshold").toFloat().also { require(it.isFinite() && it in .90f.. .999f && it >= warningThreshold) }
        } else null
    }
    fun predict(text: String): Float {
        return classifier.predict(text)
    }
    fun canWarnWithoutAction(text: String, probability: Float): Boolean = contextFreeWarningThreshold?.let {
        probability >= it && classifier.supportsContextFreeWarning(text)
    } ?: false
}
