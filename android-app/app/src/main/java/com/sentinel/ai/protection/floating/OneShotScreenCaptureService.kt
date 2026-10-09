package com.sentinel.ai.protection.floating

import android.app.Service
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.WindowManager
import com.sentinel.ai.BuildConfig
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

/** Fresh authorization per request. Stops projection before presenting any crop or OCR UI. */
class OneShotScreenCaptureService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private val thread = HandlerThread("SafeX-one-frame")
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var request = ""
    private var started = false
    private var downsampled = false
    private var surfaceRefreshes = 0
    private val done = AtomicBoolean(false)
    private val captureLock = Any()
    // Debug diagnostics contain only lifecycle/dimension information, never pixels, text or tokens.
    private fun diagnostic(message: String) { if (BuildConfig.DEBUG) android.util.Log.d("SafeXCapture", message) }
    private val callback = object : MediaProjection.Callback() {
        override fun onStop() {
            failCapture("Screen capture stopped. Choose a new capture or paste the content.")
            stopSelf()
        }
        override fun onCapturedContentResize(width: Int, height: Int) {
            if (Build.VERSION.SDK_INT >= 34 && !done.get()) resize(width, height)
        }
    }
    override fun onCreate() { super.onCreate(); thread.start() }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "stop") { stopSelf(); return START_NOT_STICKY }
        if (started) return START_NOT_STICKY
        request = intent?.getStringExtra("request_id").orEmpty()
        if (request.isBlank() || (CaptureSessionStore.state.value as? CaptureSessionStore.State.Waiting)?.id != request) { stopSelf(); return START_NOT_STICKY }
        started = true
        try {
            val notification = FloatingNotifications.status(this, capture = true)
            if (Build.VERSION.SDK_INT >= 29) startForeground(FloatingNotifications.CAPTURE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            else startForeground(FloatingNotifications.CAPTURE_ID, notification)
            val data = if (Build.VERSION.SDK_INT >= 33) intent!!.getParcelableExtra("authorization", Intent::class.java) else @Suppress("DEPRECATION") intent!!.getParcelableExtra<Intent>("authorization")
            require(data != null)
            projection = getSystemService(MediaProjectionManager::class.java).getMediaProjection(intent.getIntExtra("result_code", 0), data)
            projection!!.registerCallback(callback, main)
            // Start the display only after the source is visible. A static screen may
            // produce just one frame, so discarding its first frame can otherwise time out.
            main.post(::prepareDisplay)
            main.postDelayed({ if (!done.get()) {
                diagnostic("Capture timeout: display=${display != null}, reader=${reader != null}, sourceVisible=${CaptureSessionStore.sourceVisibleAt != 0L}")
                failCapture("Capture unavailable or unreadable. Try again, Share, or Paste."); stopSelf()
            } }, 10000)
        } catch (_: RuntimeException) {
            failCapture("Screen capture could not start. Please request a new capture.")
            stopSelf()
        }
        return START_NOT_STICKY
    }
    private fun failCapture(message: String) {
        done.set(true)
        releaseProjection()
        if (!CaptureSessionStore.fail(request, message)) return
        runCatching { FloatingNotifications.failure(this, message, request) }
        runCatching { startActivity(FloatingAssistantActivity.intent(this, "review", request)) }
    }
    private fun prepareDisplay() {
        if (done.get() || display != null) return
        if ((CaptureSessionStore.state.value as? CaptureSessionStore.State.Waiting)?.id != request) { stopSelf(); return }
        val visibleAt = CaptureSessionStore.sourceVisibleAt
        if (visibleAt == 0L || SystemClock.elapsedRealtime() - visibleAt < 250) { main.postDelayed(::prepareDisplay, 50); return }
        try {
            val windows = getSystemService(WindowManager::class.java)
            val metrics = resources.displayMetrics
            val bounds = if (Build.VERSION.SDK_INT >= 30) windows.maximumWindowMetrics.bounds else android.graphics.Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
            val (width, height) = dimensions(bounds.width(), bounds.height())
            downsampled = width < bounds.width() || height < bounds.height()
            synchronized(captureLock) {
                reader = makeReader(width, height)
                display = projection!!.createVirtualDisplay("SafeX-private-one-frame", width, height, metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, main)
                diagnostic("Display prepared: ${width}x${height}")
            }
            main.postDelayed(::refreshMissingFrame, 1500)
        } catch (_: RuntimeException) {
            failCapture("Screen capture could not start. Please request a new capture.")
            stopSelf()
        }
    }
    private fun dimensions(w: Int, h: Int): Pair<Int, Int> {
        require(w > 0 && h > 0)
        val scale = min(1.0, min(4096.0 / max(w, h), sqrt(6_000_000.0 / (w.toLong() * h))))
        return max(1, (w * scale).toInt()) to max(1, (h * scale).toInt())
    }
    /** Retry a stalled producer on the same authorized display, never create a second display. */
    private fun refreshMissingFrame() {
        if (done.get() || surfaceRefreshes >= 2 || (CaptureSessionStore.state.value as? CaptureSessionStore.State.Waiting)?.id != request) return
        try {
            synchronized(captureLock) {
                if (done.get()) return
                val active = display ?: return
                val old = reader ?: return
                val replacement = makeReader(old.width, old.height)
                try {
                    active.surface = null
                    active.surface = replacement.surface
                    reader = replacement
                    old.close()
                } catch (failure: RuntimeException) {
                    replacement.close()
                    throw failure
                }
                surfaceRefreshes++
                diagnostic("No frame yet; surface refreshed ($surfaceRefreshes/2): ${replacement.width}x${replacement.height}")
            }
            main.postDelayed(::refreshMissingFrame, 2000)
        } catch (_: RuntimeException) {
            failCapture("Capture unavailable or unreadable. Try again, Share, or Paste.")
            stopSelf()
        }
    }
    private fun makeReader(width: Int, height: Int): ImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).apply {
        setOnImageAvailableListener({ source ->
            synchronized(captureLock) {
                val image = runCatching { source.acquireLatestImage() }.getOrNull() ?: return@synchronized
                try {
                    val visibleAt = CaptureSessionStore.sourceVisibleAt
                    // The gateway must actually leave the foreground before accepting target pixels.
                    if (done.get() || visibleAt == 0L || SystemClock.elapsedRealtime() - visibleAt < 250) return@synchronized
                    if (!done.compareAndSet(false, true)) return@synchronized
                    val plane = image.planes.first()
                    diagnostic("Frame received: ${image.width}x${image.height}, rowStride=${plane.rowStride}, pixelStride=${plane.pixelStride}")
                    val pixels = RgbaRows.packed(image.width, image.height, plane.rowStride, plane.pixelStride, plane.buffer)
                    val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                    try { bitmap.copyPixelsFromBuffer(pixels) } catch (failure: Throwable) { bitmap.recycle(); throw failure }
                    main.post {
                        releaseProjection()
                        if (!getSystemService(PowerManager::class.java).isInteractive || getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked) {
                            bitmap.recycle()
                            failCapture("Screen capture stopped. Choose a new capture or paste the content.")
                            stopSelf()
                            return@post
                        }
                        if (CaptureSessionStore.deliver(request, bitmap, downsampled)) {
                            FloatingNotifications.ready(this@OneShotScreenCaptureService, request)
                            try { startActivity(FloatingAssistantActivity.intent(this@OneShotScreenCaptureService, "review", request)) } catch (_: RuntimeException) { /* status notification and bubble remain valid routes */ }
                            Handler(Looper.getMainLooper()).postDelayed({ CaptureSessionStore.expire(request) }, 60000)
                        }
                        stopSelf()
                    }
                } catch (_: OutOfMemoryError) {
                    main.post { failCapture("Not enough memory for this capture. Choose a smaller app window or paste the content."); stopSelf() }
                } catch (_: RuntimeException) {
                    main.post { failCapture("Capture unavailable or unreadable. Try again, Share, or Paste."); stopSelf() }
                } finally { image.close() }
            }
        }, Handler(thread.looper))
    }
    private fun resize(width: Int, height: Int) {
        if (display == null) return
        try {
            synchronized(captureLock) {
                if (done.get()) return
                val (w, h) = dimensions(width, height)
                downsampled = w < width || h < height
                // Android sends an initial resize callback even when size is unchanged.
                // Replacing that reader can discard the only frame of a static screen.
                if (reader?.width == w && reader?.height == h) { diagnostic("Same-size resize ignored: ${w}x${h}"); return }
                val old = reader
                reader = makeReader(w, h)
                display?.resize(w, h, resources.displayMetrics.densityDpi)
                display?.surface = reader!!.surface
                old?.close()
            }
        } catch (_: RuntimeException) { failCapture("Screen size changed during capture. Please capture again."); stopSelf() }
    }
    private fun releaseProjection() {
        synchronized(captureLock) {
            runCatching { display?.release() }; display = null
            runCatching { reader?.close() }; reader = null
            projection?.let { p -> runCatching { p.unregisterCallback(callback) }; runCatching { p.stop() } }; projection = null
        }
    }
    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        failCapture("Screen capture stopped. Choose a new capture or paste the content.")
        releaseProjection(); thread.quitSafely(); stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
