package com.sentinel.ai.protection.intent.model

import android.net.Uri

/**
 * Marker type for payloads routed through the future intent protection pipeline.
 */
sealed interface IntentPayload

/**
 * Immutable payload for a URL that was opened through an incoming intent.
 */
data class UrlPayload(
    val url: String,
) : IntentPayload

/**
 * Immutable payload for a file that was opened through an incoming intent.
 */
data class FilePayload(
    val uri: Uri,
) : IntentPayload

/** User-selected or shared message text. */
data class TextPayload(val text: String) : IntentPayload
