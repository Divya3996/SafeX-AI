package com.sentinel.ai.core.utils

import timber.log.Timber

object Logger {
    fun init(isDebug: Boolean) {
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
