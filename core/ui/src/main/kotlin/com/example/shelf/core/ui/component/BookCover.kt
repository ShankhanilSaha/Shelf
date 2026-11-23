package com.example.shelf.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.shelf.core.model.CoverSize
import com.example.shelf.core.model.coverUrl
import com.example.shelf.core.ui.R
import com.example.shelf.core.ui.icon.ShelfIcons

/** Book covers are roughly 2:3 (width:height). */
const val COVER_ASPECT_RATIO = 2f / 3f

/**
 * A book cover loaded with Coil: a flat placeholder while loading, a crossfade when the image
 * arrives, and a book glyph when there is no cover or it fails to load.
 */
@Composable
fun BookCover(coverId: Long?, title: String, modifier: Modifier = Modifier, size: CoverSize = CoverSize.MEDIUM) {
    val description = stringResource(R.string.ui_cover_description, title)
    val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHighest
    var failed by remember(coverId) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .aspectRatio(COVER_ASPECT_RATIO)
            .clip(RoundedCornerShape(4.dp))
            .background(placeholderColor)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (coverId == null || failed) {
            Icon(
                imageVector = ShelfIcons.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (coverId != null) {
            val context = LocalContext.current
            val request = remember(coverId, size) {
                ImageRequest.Builder(context)
                    .data(coverUrl(coverId, size))
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                // The Box carries the description so the image is announced once.
                contentDescription = null,
                placeholder = ColorPainter(placeholderColor),
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                onSuccess = { failed = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
