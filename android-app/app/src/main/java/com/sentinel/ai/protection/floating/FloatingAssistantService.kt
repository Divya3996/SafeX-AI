package com.sentinel.ai.protection.floating

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import com.sentinel.ai.core.feature.FloatingAssistantControl
import com.sentinel.ai.core.i18n.I18n
import kotlin.math.*

/** User-enabled shortcut service. It has no screen/clipboard/message reader. */
class FloatingAssistantService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windows: WindowManager
    private var bubble: View? = null
    private var menu: View? = null
    private var hidden = false
    private var receiverAdded = false
    private val prefs by lazy { getSharedPreferences("safex_floating", MODE_PRIVATE) }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) { removeWindows(); stopService(Intent(this@FloatingAssistantService, OneShotScreenCaptureService::class.java)); CaptureSessionStore.clear() }
            else attach()
        }
    }
    private val permissionWatch = object : Runnable {
        override fun run() {
            if (!Settings.canDrawOverlays(this@FloatingAssistantService)) { stopSelf(); return }
            handler.postDelayed(this, 10000)
        }
    }
    override fun onCreate() {
        super.onCreate()
        windows = getSystemService(WindowManager::class.java)
        try {
            val notification = FloatingNotifications.status(this)
            if (Build.VERSION.SDK_INT >= 34) startForeground(FloatingNotifications.BUBBLE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(FloatingNotifications.BUBBLE_ID, notification)
            ContextCompat.registerReceiver(this, receiver, IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT) }, ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverAdded = true
            FloatingAssistantControl.setRunning(true)
            attach(); handler.post(permissionWatch)
        } catch (_: RuntimeException) {
            Toast.makeText(this, label("Floating assistant could not start. Open SafeX AI and try again."), Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "stop" -> stopSelf()
            "hide" -> { hidden = true; removeWindows() }
            "show" -> { hidden = false; attach() }
        }
        return START_NOT_STICKY
    }
    private fun label(text: String) = I18n.translate(this, text)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun bounds(): Rect = if (Build.VERSION.SDK_INT >= 30) windows.maximumWindowMetrics.bounds else Rect(0, 0, resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
    private fun attach() {
        if (FloatingAssistantControl.helperVisible || hidden || bubble != null || !Settings.canDrawOverlays(this) || getSystemService(KeyguardManager::class.java).isKeyguardLocked || CaptureSessionStore.state.value is CaptureSessionStore.State.Waiting) return
        val screen = bounds()
        val size = dp(60)
        val params = WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (prefs.getBoolean("left", false)) 0 else screen.width() - size
            y = (prefs.getFloat("y", .35f) * (screen.height() - size)).toInt().coerceIn(dp(32), max(dp(32), screen.height() - size - dp(48)))
        }
        val view = Shield(this).apply {
            contentDescription = label("SafeX AI floating assistant. Tap for scan actions.")
            isFocusable = true
            setOnClickListener { toggleMenu(params) }
        }
        var initialX = 0; var initialY = 0; var downX = 0f; var downY = 0f; var moved = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { initialX = params.x; initialY = params.y; downX = event.rawX; downY = event.rawY; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX; val dy = event.rawY - downY
                    if (hypot(dx, dy) > dp(8)) moved = true
                    if (moved) {
                        removeMenu()
                        params.x = (initialX + dx).toInt().coerceIn(0, max(0, screen.width() - size))
                        params.y = (initialY + dy).toInt().coerceIn(dp(32), max(dp(32), screen.height() - size - dp(48)))
                        runCatching { windows.updateViewLayout(view, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        val left = params.x + size / 2 < screen.width() / 2
                        params.x = if (left) 0 else screen.width() - size
                        prefs.edit().putBoolean("left", left).putFloat("y", params.y.toFloat() / max(1, screen.height() - size)).apply()
                        runCatching { windows.updateViewLayout(view, params) }
                    } else view.performClick()
                    true
                }
                else -> true
            }
        }
        // TalkBack can move the bubble without a drag gesture.
        view.setAccessibilityDelegate(object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: android.view.accessibility.AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(91451, label("Move to other edge")))
            }
            override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
                if (action != 91451) return super.performAccessibilityAction(host, action, args)
                prefs.edit().putBoolean("left", !prefs.getBoolean("left", false)).apply()
                removeWindows(); attach(); return true
            }
        })
        try { windows.addView(view, params); bubble = view } catch (_: RuntimeException) { stopSelf() }
    }
    private fun toggleMenu(anchor: WindowManager.LayoutParams) {
        if (menu != null) { removeMenu(); return }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply { setColor(Color.rgb(18, 44, 55)); cornerRadius = dp(22).toFloat(); setStroke(dp(1), Color.rgb(39, 211, 182)) }
            elevation = dp(12).toFloat()
        }
        panel.addView(TextView(this).apply { text = label("SafeX AI"); setTextColor(Color.WHITE); textSize = 18f; setPadding(0, 0, 0, dp(8)) })
        fun action(title: String, run: () -> Unit) {
            panel.addView(Button(this).apply {
                text = label(title); isAllCaps = false; setTextColor(Color.WHITE); minHeight = dp(48)
                textSize = 14f * com.sentinel.ai.core.feature.DisplayPreferences.current.textScale
                background = GradientDrawable().apply { setColor(Color.rgb(24, 64, 72)); cornerRadius = dp(12).toFloat() }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6) }
                setOnClickListener { removeMenu(); run() }
            })
        }
        val hasReview = CaptureSessionStore.state.value is CaptureSessionStore.State.Ready || CaptureSessionStore.state.value is CaptureSessionStore.State.Failed || FloatingAssistantControl.privateReviewAvailable
        action(if (hasReview) "Resume private review" else "Scan screen area") { launch(if (hasReview) "review" else "capture") }
        action("Paste link or message") { launch("paste") }
        action("Choose screenshot") { launch("import") }
        action("Move to other edge") { prefs.edit().putBoolean("left", !prefs.getBoolean("left", false)).apply(); removeWindows(); attach() }
        action("Pause assistant") { stopSelf() }
        action("Close") { removeMenu() }
        val screen = bounds()
        val width = min(dp(290), screen.width() - dp(24))
        val scroll = ScrollView(this).apply { addView(panel); isFillViewport = false }
        panel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        val menuHeight = min(panel.measuredHeight, screen.height() - dp(100))
        val params = WindowManager.LayoutParams(width, menuHeight, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (anchor.x - width + dp(60)).coerceIn(dp(12), max(dp(12), screen.width() - width - dp(12)))
            y = anchor.y.coerceIn(dp(32), max(dp(32), screen.height() - menuHeight - dp(48)))
        }
        try { windows.addView(scroll, params); menu = scroll } catch (_: RuntimeException) { stopSelf() }
    }
    private fun launch(mode: String) { hidden = true; removeWindows(); try { startActivity(FloatingAssistantActivity.intent(this, mode)) } catch (_: RuntimeException) { hidden = false; attach() } }
    private fun removeMenu() { menu?.let { runCatching { windows.removeView(it) } }; menu = null }
    private fun removeWindows() { removeMenu(); bubble?.let { runCatching { windows.removeView(it) } }; bubble = null }
    override fun onConfigurationChanged(config: android.content.res.Configuration) { super.onConfigurationChanged(config); removeWindows(); attach() }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null); removeWindows()
        if (receiverAdded) unregisterReceiver(receiver)
        stopService(Intent(this, OneShotScreenCaptureService::class.java)); CaptureSessionStore.clear()
        FloatingAssistantControl.setRunning(false); stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private class Shield(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()
        override fun performClick(): Boolean { super.performClick(); return true }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val s = min(width, height).toFloat()
            paint.color = Color.rgb(7, 27, 36); paint.style = Paint.Style.FILL
            canvas.drawCircle(s / 2, s / 2, s * .46f, paint)
            path.reset()
            path.apply { moveTo(s * .5f, s * .15f); lineTo(s * .77f, s * .25f); lineTo(s * .73f, s * .56f); quadTo(s * .67f, s * .76f, s * .5f, s * .85f); quadTo(s * .33f, s * .76f, s * .27f, s * .56f); lineTo(s * .23f, s * .25f); close() }
            paint.color = Color.rgb(39, 211, 182); canvas.drawPath(path, paint)
            paint.color = Color.rgb(7, 27, 36); paint.strokeWidth = s * .065f; paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(s * .39f, s * .37f, s * .61f, s * .6f, paint); canvas.drawLine(s * .61f, s * .37f, s * .39f, s * .6f, paint)
        }
    }
}
