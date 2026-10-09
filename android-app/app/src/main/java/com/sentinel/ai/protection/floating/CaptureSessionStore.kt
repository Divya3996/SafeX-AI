package com.sentinel.ai.protection.floating

import android.graphics.Bitmap
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** One in-memory pixel handoff. Pixels are never placed in files or Intent extras; fresh projection authorization is passed only to the one-shot service. */
object CaptureSessionStore {
    sealed interface State {
        data object Empty : State
        data class Waiting(val id: String) : State
        data class Ready(val id: String, val bitmap: Bitmap, val elapsedMs: Long? = null, val downsampled: Boolean = false) : State
        data class Failed(val id: String, val message: String) : State
    }
    private val mutable = MutableStateFlow<State>(State.Empty)
    val state = mutable.asStateFlow()
    @Volatile var sourceVisibleAt = 0L
        private set
    @Synchronized fun begin(): String {
        clear()
        val id = UUID.randomUUID().toString()
        mutable.value = State.Waiting(id)
        sourceVisibleAt = 0
        return id
    }
    fun sourceVisible(id: String) {
        if ((mutable.value as? State.Waiting)?.id == id) sourceVisibleAt = SystemClock.elapsedRealtime()
    }
    @Synchronized fun deliver(id: String, bitmap: Bitmap, downsampled: Boolean = false): Boolean {
        if ((mutable.value as? State.Waiting)?.id != id) { bitmap.recycle(); return false }
        mutable.value = State.Ready(id, bitmap, sourceVisibleAt.takeIf { it > 0 }?.let { SystemClock.elapsedRealtime() - it }, downsampled)
        return true
    }
    @Synchronized fun take(id: String): Bitmap? {
        val current = mutable.value as? State.Ready ?: return null
        if (current.id != id) return null
        mutable.value = State.Empty
        return current.bitmap
    }
    @Synchronized fun fail(id: String, message: String): Boolean {
        if ((mutable.value as? State.Waiting)?.id != id) return false
        mutable.value = State.Failed(id, message)
        return true
    }
    @Synchronized fun expire(id: String) {
        if ((mutable.value as? State.Ready)?.id == id) clear()
    }
    @Synchronized fun clear() {
        (mutable.value as? State.Ready)?.bitmap?.let { if (!it.isRecycled) it.recycle() }
        mutable.value = State.Empty
        sourceVisibleAt = 0
    }
}
