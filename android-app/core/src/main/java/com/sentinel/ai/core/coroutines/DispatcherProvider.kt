package com.sentinel.ai.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

open class DispatcherProvider {
    open val main: CoroutineDispatcher = Dispatchers.Main
    open val io: CoroutineDispatcher = Dispatchers.IO
    open val default: CoroutineDispatcher = Dispatchers.Default
}
