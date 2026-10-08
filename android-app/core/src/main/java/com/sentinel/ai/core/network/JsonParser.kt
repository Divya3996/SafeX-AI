package com.sentinel.ai.core.network

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Thin Gson wrapper for deserialising JSON response bodies.
 *
 * Providers call [parse] with the raw response body string received from
 * [HttpClientWrapper]. Parse failures are swallowed here and return `null`
 * — providers treat a `null` result as an inability to evaluate and return
 * `null` from `ReputationProvider.evaluate()`, which [ReputationManagerImpl]
 * already handles safely.
 *
 * Provided as a Hilt singleton by [com.sentinel.ai.core.di.NetworkModule].
 */
@Singleton
class JsonParser @Inject constructor(
    @PublishedApi internal val gson: Gson
) {

    /**
     * Deserialises [json] into type [T].
     *
     * @return The parsed object, or `null` if [json] is blank or malformed.
     */
    inline fun <reified T> parse(json: String): T? {
        if (json.isBlank()) return null
        return try {
            gson.fromJson(json, T::class.java)
        } catch (e: JsonSyntaxException) {
            Timber.w(e, "JsonParser: failed to parse response body into ${T::class.java.simpleName}")
            null
        }
    }
}
