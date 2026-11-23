package com.example.shelf.feature.scanner

import android.os.SystemClock
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.Executors

/**
 * Back camera preview (CameraX) with ML Kit barcode analysis restricted to EAN-13 and EAN-8.
 *
 * @param analysisEnabled analysis runs only while true; it is detached as soon as a code is accepted
 * @param onBarcode called on the main thread with de-duplicated raw values
 * @param onCameraState reports whether a camera could be bound and whether it has a torch
 */
@Composable
internal fun CameraPreview(
    analysisEnabled: Boolean,
    torchOn: Boolean,
    onBarcode: (String) -> Unit,
    onCameraState: (available: Boolean, hasTorch: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBarcode by rememberUpdatedState(onBarcode)
    val currentOnCameraState by rememberUpdatedState(onCameraState)
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val analysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8)
                .build(),
        )
    }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val debouncer = remember { BarcodeDebouncer() }

    DisposableEffect(lifecycleOwner) {
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { request -> surfaceRequest = request }
        }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        providerFuture.addListener(
            {
                if (disposed) return@addListener
                val cameraProvider = providerFuture.get().also { provider = it }
                cameraProvider.unbindAll()
                // Binding fails on devices without a back camera; the screen then offers manual entry.
                camera = runCatching {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }.getOrNull()
                currentOnCameraState(camera != null, camera?.cameraInfo?.hasFlashUnit() == true)
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            disposed = true
            provider?.unbindAll()
            camera = null
            surfaceRequest = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            analyzerExecutor.shutdown()
        }
    }

    DisposableEffect(analysisEnabled) {
        if (analysisEnabled) {
            val mainExecutor = ContextCompat.getMainExecutor(context)
            analysis.setAnalyzer(
                analyzerExecutor,
                MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, analyzerExecutor) { result ->
                    val value = result.getValue(scanner)?.firstNotNullOfOrNull { it.rawValue }
                    if (value != null && debouncer.accept(value, SystemClock.elapsedRealtime())) {
                        mainExecutor.execute { currentOnBarcode(value) }
                    }
                },
            )
        }
        onDispose { analysis.clearAnalyzer() }
    }

    LaunchedEffect(camera, torchOn) {
        camera?.cameraControl?.enableTorch(torchOn)
    }

    Box(modifier = modifier) {
        surfaceRequest?.let { request ->
            CameraXViewfinder(surfaceRequest = request, modifier = Modifier.fillMaxSize())
        }
    }
}
