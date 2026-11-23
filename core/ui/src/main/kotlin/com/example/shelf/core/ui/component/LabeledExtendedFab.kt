package com.example.shelf.core.ui.component

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Extended FAB whose label is announced by TalkBack. Material 3 1.5 clears the semantics of the
 * FAB's text slot (so the merged button node has no text), which left the button unlabelled;
 * the label is therefore set as the button's content description explicitly.
 */
@Composable
fun LabeledExtendedFab(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        text = { Text(text) },
        modifier = modifier.semantics { contentDescription = text },
    )
}
