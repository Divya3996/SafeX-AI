package com.sentinel.ai.protection.intent

/**
 * Intent extra keys used to hand intent payloads from the router to the loading activity.
 */
object IntentPayloadExtras {
    const val EXTRA_PAYLOAD_TYPE = "com.sentinel.ai.protection.intent.extra.PAYLOAD_TYPE"
    const val EXTRA_PAYLOAD_VALUE = "com.sentinel.ai.protection.intent.extra.PAYLOAD_VALUE"

    const val EXTRA_FROM_VIEW_INTENT = "com.sentinel.ai.protection.intent.extra.FROM_VIEW_INTENT"
    const val EXTRA_INTERNAL_LAUNCH = "com.sentinel.ai.protection.intent.extra.INTERNAL_LAUNCH"

    const val TYPE_URL = "url"
    const val TYPE_FILE = "file"
    const val TYPE_TEXT = "text"
}
