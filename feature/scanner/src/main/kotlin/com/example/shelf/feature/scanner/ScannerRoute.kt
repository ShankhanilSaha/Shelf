package com.example.shelf.feature.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Entry point of the scanner feature. */
@Composable
fun ScannerRoute(
    onOpenBook: (workKey: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var permission by rememberSaveable { mutableStateOf(cameraPermission(context, permissionRequested)) }
    var cameraAvailable by rememberSaveable { mutableStateOf(true) }
    var torchAvailable by rememberSaveable { mutableStateOf(false) }
    var torchOn by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionRequested = true
        permission = cameraPermission(context, requestedBefore = true)
    }
    // Ask once when the scanner opens; later the screen explains and offers the request again.
    LaunchedEffect(Unit) {
        if (permission == CameraPermission.NOT_REQUESTED) launcher.launch(Manifest.permission.CAMERA)
    }
    // The user may grant the permission in system settings and come back.
    LifecycleResumeEffect(Unit) {
        if (hasCameraPermission(context)) permission = CameraPermission.GRANTED
        onPauseOrDispose {}
    }
    // Haptic confirmation when the camera reads a book's barcode (driven by the state change).
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is ScannerUiState.LookingUp &&
            state.fromCamera
        ) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }

    ScannerScreen(
        uiState = uiState,
        permission = permission,
        cameraAvailable = cameraAvailable,
        torchAvailable = torchAvailable,
        torchOn = torchOn,
        manualIsbn = viewModel.manualIsbn,
        onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) },
        onOpenSettings = { context.startActivity(appSettingsIntent(context)) },
        onBack = onBack,
        onToggleTorch = { torchOn = !torchOn },
        onManualIsbnChange = viewModel::onManualIsbnChange,
        onManualSubmit = viewModel::onManualSubmit,
        onOpenDetails = onOpenBook,
        onAddToShelf = viewModel::onAddToShelf,
        onScanAgain = viewModel::onScanAgain,
        onRetry = viewModel::onRetry,
        modifier = modifier,
        cameraPreview = { previewModifier ->
            CameraPreview(
                analysisEnabled = uiState is ScannerUiState.Scanning,
                torchOn = torchOn,
                onBarcode = viewModel::onBarcodeDetected,
                onCameraState = { available, hasTorch ->
                    cameraAvailable = available
                    torchAvailable = hasTorch
                },
                modifier = previewModifier,
            )
        },
    )
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/**
 * Android reports "should show rationale" as false both before the first request and after a
 * permanent denial, so whether we already asked decides which of the two it is.
 */
private fun cameraPermission(context: Context, requestedBefore: Boolean): CameraPermission {
    val activity = context.findActivity()
    return when {
        hasCameraPermission(context) -> CameraPermission.GRANTED

        !requestedBefore -> CameraPermission.NOT_REQUESTED

        activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) ->
            CameraPermission.DENIED

        else -> CameraPermission.PERMANENTLY_DENIED
    }
}

private fun appSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
