package com.sentinel.ai.features

import android.content.*
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.protection.floating.FloatingAssistantService
import com.sentinel.ai.ui.theme.SentinelTheme
import org.junit.*
import org.junit.Assert.*
import kotlinx.coroutines.runBlocking

/** Drives Android's real projection prompt and frame reader; no decoded text/image injection. */
class FloatingCaptureEndToEndTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var monitor: android.app.Instrumentation.ActivityMonitor
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)
    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes().toString(Charsets.UTF_8) }
    @Before fun prepare() {
        monitor = instrumentation.addMonitor(com.sentinel.ai.protection.floating.FloatingAssistantActivity::class.java.name, null, false)
        shell("appops set ${compose.activity.packageName} SYSTEM_ALERT_WINDOW allow")
        if (android.os.Build.VERSION.SDK_INT >= 33) shell("pm grant ${compose.activity.packageName} android.permission.POST_NOTIFICATIONS")
        compose.runOnUiThread { DisplayPreferences.setLanguage(compose.activity, AppLanguage.ENGLISH); DisplayPreferences.setTextScale(1f) }
        compose.setContent {
            SentinelTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
                        Text("SAFE X CAPTURE FIXTURE", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(24.dp))
                        Text("URGENT BANK SUPPORT", style = MaterialTheme.typography.titleLarge)
                        Text("Share your OTP and password immediately to avoid account suspension.", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))
                        Text("https://paypal-secure.example/verify", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
    @After fun stop() { instrumentation.removeMonitor(monitor); compose.runOnUiThread { FloatingAssistantControl.stop(compose.activity) }; device.pressBack() }
    private fun waitUi(selector: BySelector, timeout: Long): Boolean = try {
        // Compose's instrumentation clock must advance even when Android UIAutomator drives consent.
        compose.waitUntil(timeout) { device.hasObject(selector) }
        true
    } catch (_: androidx.compose.ui.test.ComposeTimeoutException) { false }
    private fun privateState(): String {
        var value = "No helper activity"
        monitor.lastActivity?.let { activity -> instrumentation.runOnMainSync {
            val model = androidx.lifecycle.ViewModelProvider(activity as androidx.activity.ComponentActivity)[com.sentinel.ai.protection.floating.FloatingSessionViewModel::class.java]
            value = "stage=${model.state.value.stage}, error=${model.state.value.error}, finishing=${activity.isFinishing}, destroyed=${activity.isDestroyed}, sourceVisible=${com.sentinel.ai.protection.floating.CaptureSessionStore.sourceVisibleAt}, store=${com.sentinel.ai.protection.floating.CaptureSessionStore.state.value::class.java.simpleName}"
        } }
        return value
    }
    private fun screenshot(name: String) {
        // Only our authored test content is captured. Production previews remain FLAG_SECURE.
        monitor.lastActivity?.let { activity -> instrumentation.runOnMainSync { if (activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0) activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) } }
        compose.mainClock.advanceTimeBy(250)
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
        Thread.sleep(250) // Allow the emulator's RenderThread to present the committed frame.
        val file = java.io.File(compose.activity.getExternalFilesDir(null), "floating-screenshots/$name.png")
        file.parentFile!!.mkdirs()
        assertTrue(device.takeScreenshot(file))
    }
    private fun openCapture() {
        compose.runOnUiThread { FloatingAssistantControl.start(compose.activity) }
        assertTrue(device.wait(Until.hasObject(By.descContains("SafeX AI floating assistant")), 10000))
        device.findObject(By.descContains("SafeX AI floating assistant")).click()
        assertTrue(device.wait(Until.hasObject(By.text("Scan screen area")), 5000))
        screenshot("floating-menu")
        device.findObject(By.text("Scan screen area")).click()
    }
    @Test fun realScreenCaptureCropAndOcrReachThePrivateRiskResult() {
        openCapture()
        assertTrue(device.wait(Until.hasObject(By.text(java.util.regex.Pattern.compile("(?i)start (now|recording|sharing|casting)"))), 10000))
        // API 34 offers app-only/full-display capture. Select the complete display for the authored fixture.
        val single = device.findObject(By.textContains("single app")) ?: device.findObject(By.textContains("Single app"))
        if (single != null) {
            single.click()
            val entire = device.wait(Until.findObject(By.textContains("Entire screen")), 3000)
                ?: device.findObject(By.textContains("entire screen"))
            entire?.click()
        }
        val start = device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("(?i)start (now|recording|sharing|casting)"))), 5000)
            ?: device.findObject(By.res("android:id/button1"))
        assertNotNull("Android capture consent button must be present", start)
        start!!.click()
        val reachedCrop = waitUi(By.text("Crop the captured screen"), 15000)
        assertTrue("Real captured image should reach the crop screen: ${privateState()}", reachedCrop)
        screenshot("captured-crop")
        assertTrue("Projection must stop before crop editing", shell("dumpsys media_projection").trim().endsWith("null"))
        compose.onNodeWithText("Read selected area").performScrollTo().performClick()
        assertTrue(waitUi(By.text("Review extracted content"), 30000))
        screenshot("extracted-review")
        compose.onNodeWithText("Analyze reviewed message").performScrollTo().performClick()
        assertTrue(waitUi(By.text("Private review • not saved"), 15000))
        assertFalse(ThreatJournal.scanResults.value.any { it.source == "Floating screen crop" && it.target.orEmpty().contains("SAFE X CAPTURE FIXTURE") })
        assertTrue(waitUi(By.text(java.util.regex.Pattern.compile("High-risk content|Review before acting")), 5000))
        screenshot("private-risk-result")
        device.findObject(By.text("Save result")).click()
        compose.waitUntil(5000) { ThreatJournal.scanResults.value.any { it.source == "Floating screen crop" && it.target.orEmpty().contains("OTP") } }
        val result = ThreatJournal.scanResults.value.first { it.source == "Floating screen crop" && it.target.orEmpty().contains("OTP") }
        assertTrue(result.target.orEmpty().contains("paypal", true))
        assertNotEquals(com.sentinel.ai.core.model.ProtectionDecision.ALLOW, result.decision)
        runBlocking { ThreatJournal.delete(result.id) }
        device.findObject(By.text("Close"))?.click()
    }
    @Test fun declinedSystemCaptureReturnsToSetupWithoutSaving() {
        val dao = com.sentinel.ai.core.data.local.SentinelDatabase.getInstance(compose.activity).threatDao()
        val before = runBlocking { dao.getAllThreatRecords().map { it.id }.toSet() }
        openCapture()
        assertTrue(device.wait(Until.hasObject(By.text(java.util.regex.Pattern.compile("(?i)start (now|recording|sharing|casting)"))), 10000))
        device.waitForIdle()
        device.findObject(By.text(java.util.regex.Pattern.compile("(?i)cancel"))).click()
        val denied = waitUi(By.text("Screen capture was not allowed. You can choose a screenshot or paste content instead."), 10000)
        assertTrue("Cancellation must show recovery: ${privateState()}", denied)
        assertEquals(before, runBlocking { dao.getAllThreatRecords().map { it.id }.toSet() })
        device.findObject(By.text("Close"))?.click()
    }
}
