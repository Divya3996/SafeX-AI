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
import androidx.compose.ui.graphics.toArgb
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.core.i18n.I18n
import com.sentinel.ai.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlin.math.*

/** User-enabled shortcut. A full window exists only while its dismissible menu is open. */
@AndroidEntryPoint
class FloatingAssistantService : Service() {
    @Inject lateinit var sessions: FloatingSessionController
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windows: WindowManager
    private var bubble: View? = null
    private var menu: View? = null
    private var hidden = false
    private var docking: android.animation.ValueAnimator? = null
    private var receiverAdded = false
    private val prefs by lazy { getSharedPreferences("safex_floating", MODE_PRIVATE) }
    private val light get() = ThemePreferences.get(this) == SentinelThemeMode.System && resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK != android.content.res.Configuration.UI_MODE_NIGHT_YES
    private val surface get() = (if (light) SentinelLightSurface else SentinelSurface).toArgb()
    private val secondarySurface get() = (if (light) SentinelLightSurfaceVariant else SentinelSurfaceVariant).toArgb()
    private val textColor get() = (if (light) SentinelLightTextPrimary else SentinelTextPrimary).toArgb()
    private val accent get() = if (light) Color.rgb(0, 122, 104) else SentinelCyan.toArgb()
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) { removeWindows(); sessions.reset() } else attach()
        }
    }
    private val permissionWatch = object : Runnable {
        override fun run() { if (!Settings.canDrawOverlays(this@FloatingAssistantService)) { stopSelf(); return }; handler.postDelayed(this, 10000) }
    }
    override fun onCreate() {
        super.onCreate(); windows = getSystemService(WindowManager::class.java)
        try {
            val notification = FloatingNotifications.status(this)
            if (Build.VERSION.SDK_INT >= 34) startForeground(FloatingNotifications.BUBBLE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(FloatingNotifications.BUBBLE_ID, notification)
            ContextCompat.registerReceiver(this, receiver, IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT) }, ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverAdded = true; FloatingAssistantControl.setRunning(true); attach(); handler.post(permissionWatch)
            serviceScope.launch { DisplayPreferences.settings.collect { removeWindows(); attach() } }
        } catch (_: RuntimeException) { Toast.makeText(this, label("Floating assistant could not start. Open SafeX AI and try again."), Toast.LENGTH_LONG).show(); stopSelf() }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) { "stop" -> stopSelf(); "hide" -> { hidden = true; removeWindows() }; "show" -> { hidden = false; attach() }; "refresh" -> { removeWindows(); attach() } }
        return START_NOT_STICKY
    }
    private fun label(text: String) = I18n.translate(this, text)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun bounds(): Rect = if (Build.VERSION.SDK_INT >= 30) windows.maximumWindowMetrics.bounds else Rect(0, 0, resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
    private fun safeBounds(): Rect {
        val screen = bounds()
        if (Build.VERSION.SDK_INT >= 30) {
            val inset = windows.maximumWindowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val gestures = windows.maximumWindowMetrics.windowInsets.getInsets(WindowInsets.Type.systemGestures())
            return Rect(max(inset.left, gestures.left) + dp(4), inset.top + dp(4), screen.width() - max(inset.right, gestures.right) - dp(4), screen.height() - max(inset.bottom, gestures.bottom) - dp(4))
        }
        return Rect(dp(4), dp(28), screen.width() - dp(4), screen.height() - dp(48))
    }
    private fun attach() {
        if (FloatingAssistantControl.helperVisible || hidden || bubble != null || !Settings.canDrawOverlays(this) || getSystemService(KeyguardManager::class.java).isKeyguardLocked || CaptureSessionStore.state.value is CaptureSessionStore.State.Waiting) return
        val screen = safeBounds(); val size = dp(prefs.getInt("size", 60).coerceIn(52, 72))
        val params = WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (prefs.getBoolean("left", false)) screen.left else screen.right - size
            y = screen.top + (prefs.getFloat("y", .35f) * max(1, screen.height() - size)).toInt().coerceIn(0, max(0, screen.height() - size))
        }
        val view = ImageView(this).apply {
            setImageResource(com.sentinel.ai.core.R.drawable.safex_logo); setPadding(dp(5), dp(5), dp(5), dp(5))
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(SentinelBackground.toArgb()); setStroke(dp(1), SentinelCyan.toArgb()) }
            elevation = dp(6).toFloat(); contentDescription = label("SafeX AI floating assistant. Tap for scan actions.")
            isFocusable = true; setOnClickListener { toggleMenu(params) }
        }
        var initialX = 0; var initialY = 0; var downX = 0f; var downY = 0f; var moved = false
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { docking?.cancel(); initialX = params.x; initialY = params.y; downX = event.rawX; downY = event.rawY; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX; val dy = event.rawY - downY
                    if (hypot(dx, dy) > slop) moved = true
                    if (moved) { removeMenu(); params.x = (initialX + dx).toInt().coerceIn(screen.left, max(screen.left, screen.right - size)); params.y = (initialY + dy).toInt().coerceIn(screen.top, max(screen.top, screen.bottom - size)); runCatching { windows.updateViewLayout(view, params) } }; true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        val left = params.x + size / 2 < (screen.left + screen.right) / 2
                        val target = if (left) screen.left else screen.right - size
                        if (!prefs.getBoolean("reduced_motion", true) && android.animation.ValueAnimator.areAnimatorsEnabled()) {
                            docking?.cancel()
                            docking = android.animation.ValueAnimator.ofInt(params.x, target).apply {
                                duration = 120; addUpdateListener { params.x = it.animatedValue as Int; runCatching { windows.updateViewLayout(view, params) } }; start()
                            }
                        } else params.x = target
                        prefs.edit().putBoolean("left", left).putFloat("y", (params.y - screen.top).toFloat() / max(1, screen.height() - size)).apply()
                        runCatching { windows.updateViewLayout(view, params) }
                    } else view.performClick(); true
                }
                MotionEvent.ACTION_CANCEL -> { params.x = initialX; params.y = initialY; runCatching { windows.updateViewLayout(view, params) }; moved = false; true }
                else -> true
            }
        }
        view.setAccessibilityDelegate(object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: android.view.accessibility.AccessibilityNodeInfo) { super.onInitializeAccessibilityNodeInfo(host, info); info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(91451, label("Move to other edge"))) }
            override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
                if (action != 91451) return super.performAccessibilityAction(host, action, args)
                prefs.edit().putBoolean("left", !prefs.getBoolean("left", false)).apply(); removeWindows(); attach(); return true
            }
        })
        try { windows.addView(view, params); bubble = view } catch (_: RuntimeException) { stopSelf() }
    }
    private fun toggleMenu(anchor: WindowManager.LayoutParams) {
        if (menu != null) { removeMenu(); return }
        val screen = safeBounds(); val width = min(dp(320), screen.width() - dp(16))
        val root = object : FrameLayout(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) { if (event.action == KeyEvent.ACTION_UP) removeMenu(); return true }
                return super.dispatchKeyEvent(event)
            }
        }.apply {
            setBackgroundColor(Color.argb(38, 0, 0, 0)); isFocusableInTouchMode = true
            setOnClickListener { removeMenu() }
            setOnKeyListener { _, key, event -> if (key == KeyEvent.KEYCODE_BACK) { if (event.action == KeyEvent.ACTION_UP) removeMenu(); true } else false }
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12)); isClickable = true
            background = GradientDrawable().apply { setColor(surface); cornerRadius = dp(22).toFloat(); setStroke(dp(1), accent) }; elevation = dp(12).toFloat()
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(ImageView(this).apply { setImageResource(com.sentinel.ai.core.R.drawable.safex_logo) }, LinearLayout.LayoutParams(dp(32), dp(32)))
        header.addView(TextView(this).apply { text = label("SafeX AI"); setTextColor(textColor); textSize = 18f * DisplayPreferences.current.textScale; setPadding(dp(10), 0, 0, dp(4)) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        androidx.core.view.ViewCompat.setAccessibilityHeading(header, true)
        androidx.core.view.ViewCompat.setAccessibilityPaneTitle(root, label("SafeX AI"))
        panel.addView(header)
        fun action(title: String, container: LinearLayout = panel, run: () -> Unit) {
            container.addView(Button(this).apply {
                text = label(title); isAllCaps = false; setTextColor(textColor); minHeight = dp(52); gravity = Gravity.CENTER_VERTICAL or Gravity.START; setPadding(dp(14), dp(8), dp(14), dp(8))
                textSize = 14f * DisplayPreferences.current.textScale; background = GradientDrawable().apply { setColor(secondarySurface); cornerRadius = dp(12).toFloat() }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
                setOnClickListener { removeMenu(); run() }
            })
        }
        if (sessions.hasPrivateContent || CaptureSessionStore.state.value is CaptureSessionStore.State.Ready) action("Resume private review") { launch("review") }
        action("Scan screen area") { launch("capture") }
        action("Paste link or message") { launch("paste") }
        action("Choose screenshot") { launch("import") }
        val extras = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        lateinit var scroll: ScrollView
        fun fitPanel() {
            panel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            val height = min(panel.measuredHeight, screen.height() - dp(24))
            val placement = scroll.layoutParams as? FrameLayout.LayoutParams ?: return
            placement.height = height
            placement.topMargin = anchor.y.coerceIn(screen.top + dp(8), max(screen.top + dp(8), screen.bottom - height - dp(8)))
            scroll.layoutParams = placement
        }
        val more = Button(this).apply {
            text = label("More"); isAllCaps = false; setTextColor(accent); minHeight = dp(48); textSize = 14f * DisplayPreferences.current.textScale
            background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            setOnClickListener { extras.visibility = if (extras.visibility == View.GONE) View.VISIBLE else View.GONE; text = label(if (extras.visibility == View.VISIBLE) "Less" else "More"); fitPanel() }
        }
        panel.addView(more); panel.addView(extras)
        action("Move to other edge", extras) { prefs.edit().putBoolean("left", !prefs.getBoolean("left", false)).apply(); removeWindows(); attach() }
        action("Assistant preferences", extras) { launch("setup") }
        action("Pause assistant", extras) { stopSelf() }
        action("Close", extras) { removeMenu() }
        scroll = ScrollView(this).apply { addView(panel); isFillViewport = false; isClickable = true }
        panel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        val panelHeight = min(panel.measuredHeight, screen.height() - dp(24))
        val frameParams = FrameLayout.LayoutParams(width, panelHeight).apply {
            leftMargin = (anchor.x - width + anchor.width).coerceIn(screen.left + dp(8), max(screen.left + dp(8), screen.right - width - dp(8)))
            topMargin = anchor.y.coerceIn(screen.top + dp(8), max(screen.top + dp(8), screen.bottom - panelHeight - dp(8)))
        }
        root.addView(scroll, frameParams)
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START }
        try { windows.addView(root, params); menu = root; root.requestFocus() } catch (_: RuntimeException) { removeMenu() }
    }
    private fun launch(mode: String) { hidden = true; removeWindows(); try { startActivity(FloatingAssistantActivity.intent(this, mode)) } catch (_: RuntimeException) { hidden = false; attach() } }
    private fun removeMenu() { menu?.let { runCatching { windows.removeView(it) } }; menu = null }
    private fun removeWindows() { docking?.cancel(); docking = null; removeMenu(); bubble?.let { runCatching { windows.removeView(it) } }; bubble = null }
    override fun onConfigurationChanged(config: android.content.res.Configuration) { super.onConfigurationChanged(config); removeWindows(); attach() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); serviceScope.cancel(); removeWindows(); if (receiverAdded) unregisterReceiver(receiver); sessions.reset(); FloatingAssistantControl.setRunning(false); stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
