package com.sentinel.ai.core.network

/**
 * Centralised configuration for the networking layer.
 *
 * All timeout, retry, and identification values live here. Every component
 * that performs network I/O reads from this object rather than hard-coding
 * literals, which makes tuning a single-file change.
 *
 * Provided as a Hilt singleton by [com.sentinel.ai.core.di.NetworkModule].
 */
data class NetworkConfig(
    /** TCP connection establishment timeout in seconds. */
    val connectTimeoutSeconds: Long = 10L,

    /** Socket read timeout in seconds (time between two packets). */
    val readTimeoutSeconds: Long = 15L,

    /** Socket write timeout in seconds. */
    val writeTimeoutSeconds: Long = 10L,

    /**
     * Maximum number of retry attempts for transient failures.
     *
     * Only [java.net.SocketTimeoutException], other [java.io.IOException]s,
     * and HTTP 5xx responses are retried. HTTP 4xx responses and offline
     * conditions are never retried.
     */
    val maxRetries: Int = 2,

    /**
     * Linear delay in milliseconds between successive retry attempts.
     *
     * Exponential backoff is out of scope for Phase 3.8.1 and may be
     * introduced in a later phase.
     */
    val retryDelayMs: Long = 500L,

    /**
     * Value sent in the `User-Agent` header on every outgoing request.
     *
     * Follows the "product/version" convention understood by most APIs.
     */
    val userAgent: String = "SentinelAI-Android/1.0"
)
