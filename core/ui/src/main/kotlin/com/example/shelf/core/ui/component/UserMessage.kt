package com.example.shelf.core.ui.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.example.shelf.core.ui.text.UiText
import com.example.shelf.core.ui.text.asString
import java.util.concurrent.atomic.AtomicLong

/**
 * A transient message (snackbar) modelled as UI state rather than as an event: the ViewModel sets
 * it, the UI shows it once and reports back through `onShown` / `onAction`, and the ViewModel
 * clears it. [id] makes two identical messages distinct.
 */
data class UserMessage(val text: UiText, val actionLabel: UiText? = null, val id: Long = nextId.incrementAndGet()) {
    private companion object {
        val nextId = AtomicLong()
    }
}

/** Shows [message] in [hostState] and reports the outcome exactly once per message. */
@Composable
fun UserMessageEffect(
    message: UserMessage?,
    hostState: SnackbarHostState,
    onShown: () -> Unit,
    onAction: () -> Unit = {},
) {
    val text = message?.text?.asString()
    val actionLabel = message?.actionLabel?.asString()
    val currentOnShown by rememberUpdatedState(onShown)
    val currentOnAction by rememberUpdatedState(onAction)
    LaunchedEffect(message?.id) {
        if (text == null) return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = text,
            actionLabel = actionLabel,
            withDismissAction = actionLabel == null,
            duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) currentOnAction() else currentOnShown()
    }
}
