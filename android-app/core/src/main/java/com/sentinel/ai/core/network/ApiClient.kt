package com.sentinel.ai.core.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Single DI entry point for all networking objects.
 *
 * Consumers that need HTTP networking inject this class. It carries the
 * shared [OkHttpClient] and [Retrofit] instances used before Phase 3.8.1,
 * and now also exposes [httpClientWrapper] — the primary networking
 * abstraction for reputation providers.
 *
 * Provided as a Hilt singleton by [com.sentinel.ai.core.di.NetworkModule].
 */
class ApiClient(
    val okHttpClient: OkHttpClient,
    val retrofit: Retrofit,
    val httpClientWrapper: HttpClientWrapper
)
