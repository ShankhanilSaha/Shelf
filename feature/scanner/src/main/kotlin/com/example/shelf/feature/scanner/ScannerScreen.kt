package com.example.shelf.feature.scanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.shelf.core.ui.component.BookCover
import com.example.shelf.core.ui.icon.ShelfIcons
import com.example.shelf.core.ui.text.asString
import com.example.shelf.core.ui.text.labelRes
import com.example.shelf.core.ui.R as UiR

private const val SCRIM_ALPHA = 0.55f
private const val WINDOW_WIDTH_FRACTION = 0.8f
private const val WINDOW_ASPECT = 0.45f

@Composable
internal fun ScannerScreen(
    uiState: ScannerUiState,
    permission: CameraPermission,
    cameraAvailable: Boolean,
    torchAvailable: Boolean,
    torchOn: Boolean,
    manualIsbn: String,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    onToggleTorch: () -> Unit,
    onManualIsbnChange: (String) -> Unit,
    onManualSubmit: () -> Boolean,
    onOpenDetails: (String) -> Unit,
    onAddToShelf: () -> Unit,
    onScanAgain: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    cameraPreview: @Composable (Modifier) -> Unit,
) {
    var showManualEntry by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (permission == CameraPermission.GRANTED && cameraAvailable) {
            cameraPreview(Modifier.fillMaxSize())
            ViewfinderOverlay(Modifier.fillMaxSize())
            if (uiState is ScannerUiState.Scanning) {
                Text(
                    text = uiState.hint?.asString() ?: stringResource(R.string.scanner_hint),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 220.dp, start = 32.dp, end = 32.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        } else {
            PermissionContent(
                permission = permission,
                cameraAvailable = cameraAvailable,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings,
                onEnterManually = { showManualEntry = true },
            )
        }

        TopControls(
            showTorch = permission == CameraPermission.GRANTED && torchAvailable,
            torchOn = torchOn,
            onBack = onBack,
            onToggleTorch = onToggleTorch,
            onEnterManually = { showManualEntry = true },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (uiState !is ScannerUiState.Scanning) {
            ResultPanel(
                state = uiState,
                onOpenDetails = onOpenDetails,
                onAddToShelf = onAddToShelf,
                onScanAgain = onScanAgain,
                onRetry = onRetry,
                onEnterManually = { showManualEntry = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
        }
    }

    if (showManualEntry) {
        ManualEntryDialog(
            value = manualIsbn,
            onValueChange = onManualIsbnChange,
            onSubmit = onManualSubmit,
            onDismiss = { showManualEntry = false },
        )
    }
}

@Composable
private fun TopControls(
    showTorch: Boolean,
    torchOn: Boolean,
    onBack: () -> Unit,
    onToggleTorch: () -> Unit,
    onEnterManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconColors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, colors = iconColors) {
            Icon(ShelfIcons.Back, contentDescription = stringResource(UiR.string.ui_navigate_up))
        }
        Spacer(Modifier.weight(1f))
        if (showTorch) {
            IconButton(onClick = onToggleTorch, colors = iconColors) {
                Icon(
                    imageVector = if (torchOn) ShelfIcons.TorchOn else ShelfIcons.TorchOff,
                    contentDescription = stringResource(
                        if (torchOn) R.string.scanner_torch_off else R.string.scanner_torch_on,
                    ),
                )
            }
        }
        TextButton(onClick = onEnterManually) {
            Icon(ShelfIcons.Keyboard, contentDescription = null, tint = Color.White)
            Text(
                stringResource(R.string.scanner_enter_isbn),
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Darkens everything except a barcode-shaped window with corner marks. Purely decorative. */
@Composable
private fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primaryContainer
    Canvas(
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .clearAndSetSemantics {},
    ) {
        val windowWidth = size.width * WINDOW_WIDTH_FRACTION
        val windowSize = Size(windowWidth, windowWidth * WINDOW_ASPECT)
        val topLeft = Offset((size.width - windowSize.width) / 2, (size.height - windowSize.height) / 2)
        val radius = CornerRadius(16.dp.toPx())
        drawRect(Color.Black.copy(alpha = SCRIM_ALPHA))
        drawRoundRect(Color.Transparent, topLeft, windowSize, radius, blendMode = BlendMode.Clear)
        drawRoundRect(accent, topLeft, windowSize, radius, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
private fun PermissionContent(
    permission: CameraPermission,
    cameraAvailable: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onEnterManually: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Icon(
                ShelfIcons.Scan,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            val (title, message) = when {
                !cameraAvailable && permission == CameraPermission.GRANTED ->
                    R.string.scanner_no_camera_title to R.string.scanner_no_camera_message

                permission == CameraPermission.PERMANENTLY_DENIED ->
                    R.string.scanner_permission_blocked_title to R.string.scanner_permission_blocked_message

                else -> R.string.scanner_permission_title to R.string.scanner_permission_message
            }
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 420.dp),
            )
            when (permission) {
                CameraPermission.PERMANENTLY_DENIED -> Button(onClick = onOpenSettings) {
                    Text(stringResource(R.string.scanner_open_settings))
                }

                CameraPermission.NOT_REQUESTED, CameraPermission.DENIED -> Button(onClick = onRequestPermission) {
                    Text(stringResource(R.string.scanner_allow_camera))
                }

                CameraPermission.GRANTED -> Unit
            }
            OutlinedButton(onClick = onEnterManually) {
                Text(stringResource(R.string.scanner_enter_manually))
            }
        }
    }
}

@Composable
private fun ResultPanel(
    state: ScannerUiState,
    onOpenDetails: (String) -> Unit,
    onAddToShelf: () -> Unit,
    onScanAgain: () -> Unit,
    onRetry: () -> Unit,
    onEnterManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.widthIn(max = 560.dp).fillMaxWidth().testTag(ScannerTestTags.RESULT)) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state) {
                is ScannerUiState.LookingUp -> Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Text(stringResource(R.string.scanner_looking_up, state.isbn))
                }

                is ScannerUiState.Found -> FoundContent(state, onOpenDetails, onAddToShelf, onScanAgain)

                is ScannerUiState.NotFound -> {
                    Text(stringResource(R.string.scanner_not_found_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.scanner_not_found_message, state.isbn))
                    ResultActions {
                        OutlinedButton(onClick = onEnterManually) {
                            Text(stringResource(R.string.scanner_enter_manually))
                        }
                        Button(onClick = onScanAgain) { Text(stringResource(R.string.scanner_scan_again)) }
                    }
                }

                is ScannerUiState.Failed -> {
                    Text(state.message.asString())
                    ResultActions {
                        OutlinedButton(onClick = onScanAgain) { Text(stringResource(R.string.scanner_scan_again)) }
                        Button(onClick = onRetry) { Text(stringResource(UiR.string.ui_retry)) }
                    }
                }

                is ScannerUiState.Scanning -> Unit
            }
        }
    }
}

@Composable
private fun FoundContent(
    state: ScannerUiState.Found,
    onOpenDetails: (String) -> Unit,
    onAddToShelf: () -> Unit,
    onScanAgain: () -> Unit,
) {
    val title = state.book?.title ?: state.isbn
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        BookCover(coverId = state.book?.coverId, title = title, modifier = Modifier.width(56.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            state.addedTo?.let { shelf ->
                Text(
                    text = stringResource(R.string.scanner_on_shelf, stringResource(shelf.labelRes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    ResultActions {
        TextButton(onClick = onScanAgain) { Text(stringResource(R.string.scanner_scan_again)) }
        if (state.addedTo == null) {
            OutlinedButton(onClick = onAddToShelf) {
                Text(stringResource(R.string.scanner_add_to, stringResource(state.defaultShelf.labelRes)))
            }
        }
        Button(onClick = { onOpenDetails(state.workKey) }) { Text(stringResource(R.string.scanner_open_details)) }
    }
}

/** Buttons aligned to the end that wrap onto another line instead of squeezing their labels. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultActions(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun ManualEntryDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Boolean,
    onDismiss: () -> Unit,
) {
    var invalid by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scanner_enter_isbn)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    invalid = false
                    onValueChange(it)
                },
                label = { Text(stringResource(R.string.scanner_isbn_label)) },
                isError = invalid,
                supportingText = {
                    Text(stringResource(if (invalid) R.string.scanner_isbn_invalid else R.string.scanner_isbn_help))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.testTag(ScannerTestTags.MANUAL_FIELD),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (onSubmit()) onDismiss() else invalid = true }) {
                Text(stringResource(R.string.scanner_look_up))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.ui_cancel)) } },
    )
}
