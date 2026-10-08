package com.sentinel.ai.protection.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.sentinel.ai.ui.i18n.LocalizedText as Text
import com.sentinel.ai.ui.theme.SentinelTheme
import com.sentinel.ai.ui.theme.ThemePreferences
import java.util.concurrent.Executors

/** Foreground-only, opt-in camera. Bundled decoding; no photos, URLs or frames are uploaded. */
class LiveQrActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SentinelTheme(mode = ThemePreferences.get(this)) {
                Surface(Modifier.fillMaxSize()) {
                    LiveQrScreen(onClose = { finish() }, onDecoded = { content ->
                        if (content.isNotBlank() && content.length <= 12000) {
                            setResult(RESULT_OK, Intent().putExtra("qr_content", content))
                            finish()
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun LiveQrScreen(onClose: () -> Unit, onDecoded: (String) -> Unit) {
    val context = LocalContext.current
    var permitted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                permitted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    var asked by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var candidates by remember { mutableStateOf<List<String>>(emptyList()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permitted = it; asked = true }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Scan a QR code", style = MaterialTheme.typography.headlineMedium)
        Text("Point your camera at a QR code. SafeX AI checks its contents before you open anything.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!permitted) {
            Text(if (asked) "Camera access was not granted. You can choose a QR image instead, or enable camera access in app settings." else "Camera access is used only while this scanner is open. Frames are not saved.")
            Button(onClick = { permission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth()) { Text("Allow camera access") }
            if (asked) OutlinedButton(onClick = {
                context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}")))
            }, modifier = Modifier.fillMaxWidth()) { Text("Open app settings") }
        } else if (candidates.isEmpty() && error == null) {
            CameraPreview(onFound = { candidates = it }, onError = { error = it }, modifier = Modifier.fillMaxWidth().height(320.dp))
            Text("Keep the code steady and well lit. Decoding works offline.", style = MaterialTheme.typography.bodyMedium)
        } else if (candidates.isNotEmpty()) {
            Text("Choose a QR code to analyze", style = MaterialTheme.typography.titleMedium)
            Text("Only the code you choose will be analyzed. No destination opens automatically.", style = MaterialTheme.typography.bodySmall)
            candidates.forEach { candidate ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(candidate.take(320), localize = false, style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { onDecoded(candidate) }, modifier = Modifier.fillMaxWidth()) { Text("Analyze privately") }
                    }
                }
            }
            OutlinedButton(onClick = { candidates = emptyList(); error = null }, modifier = Modifier.fillMaxWidth()) { Text("Scan again") }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { error = null; candidates = emptyList() }, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
        }
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close camera") }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onFound: (List<String>) -> Unit, onError: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE; scaleType = PreviewView.ScaleType.FIT_CENTER } }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var streaming by remember { mutableStateOf(false) }
    DisposableEffect(previewView, lifecycle) {
        val observer = androidx.lifecycle.Observer<PreviewView.StreamState> { streaming = it == PreviewView.StreamState.STREAMING }
        previewView.previewStreamState.observe(lifecycle, observer)
        onDispose { previewView.previewStreamState.removeObserver(observer) }
    }
    val callback by rememberUpdatedState(onFound)
    val failure by rememberUpdatedState(onError)
    Column {
        AndroidView(factory = { previewView }, modifier = modifier.clipToBounds())
        if (streaming) Text("Camera ready", style = MaterialTheme.typography.labelSmall)
    }
    DisposableEffect(lifecycle, previewView) {
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
        val main = ContextCompat.getMainExecutor(context)
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        var delivered = false
        val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        analysis.setAnalyzer(executor) { proxy ->
            val frame = proxy.image
            if (frame == null) proxy.close()
            else {
                try {
                    scanner.process(InputImage.fromMediaImage(frame, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener(main) { codes ->
                            if (!disposed && !delivered) {
                                val values = codes.mapNotNull { it.rawValue }.filter { it.isNotBlank() }.distinct()
                                if (values.any { it.length > 12000 }) {
                                    delivered = true
                                    failure("This QR code is too large to analyze. Choose a smaller code.")
                                } else if (values.isNotEmpty()) { delivered = true; callback(values.take(8)) }
                            }
                        }
                        .addOnFailureListener(main) { if (!disposed && !delivered) { delivered = true; failure("QR recognition failed. Try again or choose an image.") } }
                        .addOnCompleteListener { proxy.close() }
                } catch (_: Exception) {
                    proxy.close()
                    main.execute { if (!disposed) failure("QR recognition failed. Try again or choose an image.") }
                }
            }
        }
        future.addListener({
            if (!disposed) try {
                provider = future.get()
                val selector = if (provider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                provider!!.bindToLifecycle(lifecycle, selector, preview, analysis)
            } catch (_: Exception) { failure("Camera could not start. Choose a QR image instead.") }
        }, main)
        onDispose {
            disposed = true
            analysis.clearAnalyzer()
            provider?.unbind(preview, analysis)
            scanner.close()
            executor.shutdown()
        }
    }
}
