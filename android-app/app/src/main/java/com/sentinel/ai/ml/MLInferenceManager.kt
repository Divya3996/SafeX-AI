package com.sentinel.ai.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MLInferenceManager(context: Context) : java.io.Closeable {

    private val interpreter: Interpreter
    private val scaler: Scaler
    private val inputByteCount: Int
    val warningThreshold: Float

    init {
        val card = org.json.JSONObject(context.assets.open("url-model-card.json").bufferedReader().use { it.readText() })
        warningThreshold = card.getDouble("warning_threshold").toFloat()
        require(warningThreshold.isFinite() && warningThreshold in 0f..1f)
        val model = context.assets.open("model.tflite").use { it.readBytes() }
        val modelHash = java.security.MessageDigest.getInstance("SHA-256").digest(model)
            .joinToString("") { "%02x".format(it) }
        require(modelHash == card.getString("sha256")) { "URL model does not match its metadata" }
        val buffer = ByteBuffer.allocateDirect(model.size).apply {
            order(ByteOrder.nativeOrder())
            put(model)
            rewind()
        }

        interpreter = Interpreter(buffer)
        scaler = Scaler(context)

        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)
        require(inputTensor.shape().contentEquals(EXPECTED_INPUT_SHAPE)) {
            "Expected model input shape ${EXPECTED_INPUT_SHAPE.contentToString()}, " +
                "found ${inputTensor.shape().contentToString()}"
        }
        require(inputTensor.dataType() == DataType.FLOAT32) {
            "Expected FLOAT32 model input, found ${inputTensor.dataType()}"
        }
        require(outputTensor.shape().contentEquals(EXPECTED_OUTPUT_SHAPE)) {
            "Expected model output shape ${EXPECTED_OUTPUT_SHAPE.contentToString()}, " +
                "found ${outputTensor.shape().contentToString()}"
        }
        require(outputTensor.dataType() == DataType.FLOAT32) {
            "Expected FLOAT32 model output, found ${outputTensor.dataType()}"
        }

        inputByteCount = inputTensor.numBytes()
        require(inputByteCount == EXPECTED_INPUT_BYTE_COUNT) {
            "Expected $EXPECTED_INPUT_BYTE_COUNT input bytes, " +
                "found $inputByteCount"
        }
        Log.d(
            TAG,
            "Model ready: input=${inputTensor.shape().contentToString()}, " +
                "output=${outputTensor.shape().contentToString()}"
        )
    }

    @Synchronized
    fun predict(features: FloatArray): Float {
        require(features.size == FeatureExtractor.FEATURE_COUNT) {
            "Expected ${FeatureExtractor.FEATURE_COUNT} features, received ${features.size}"
        }
        val normalized = scaler.transform(features)


        val inputBuffer = ByteBuffer.allocateDirect(EXPECTED_INPUT_BYTE_COUNT).apply {
            order(ByteOrder.nativeOrder())
        }
        inputBuffer.rewind()
        normalized.forEach(inputBuffer::putFloat)
        inputBuffer.rewind()


        val output = Array(1) { FloatArray(1) }

        try {
            interpreter.run(inputBuffer, output)
        } catch (exception: Exception) {
            Log.e(TAG, "TFLite inference failed", exception)
            throw exception
        }

        val result = output[0][0]
        require(result.isFinite()) { "Model returned a non-finite probability: $result" }

        return result
    }

    @Synchronized override fun close() { interpreter.close() }

    private companion object {
        const val TAG = "ML_DEBUG"
        const val EXPECTED_INPUT_BYTE_COUNT = 15 * Float.SIZE_BYTES
        val EXPECTED_INPUT_SHAPE = intArrayOf(1, FeatureExtractor.FEATURE_COUNT)
        val EXPECTED_OUTPUT_SHAPE = intArrayOf(1, 1)
    }
}
