// Drawing code: the numbers are proportions of a grid unit, naming each one would not help.
@file:Suppress("MagicNumber")

package com.example.shelf.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * Decorative empty-state art drawn from simple shapes (no image assets): a few books on a shelf
 * with one leaning over. Hidden from accessibility services because it carries no information.
 */
@Composable
fun BookshelfIllustration(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val spines = listOf(colors.primary, colors.tertiary, colors.secondary, colors.primaryContainer)
    val plank = colors.outline
    val accent = colors.surface
    Canvas(
        modifier = modifier
            .size(width = 180.dp, height = 120.dp)
            .clearAndSetSemantics {},
    ) {
        val unit = size.width / 18f
        val shelfTop = size.height - unit * 1.5f
        // Upright books: (x offset, width, height) in units.
        val books = listOf(Triple(3f, 1.6f, 7.5f), Triple(4.8f, 2f, 9f), Triple(7f, 1.4f, 8f))
        books.forEachIndexed { index, (x, width, height) ->
            drawSpine(
                color = spines[index],
                accent = accent,
                topLeft = Offset(x * unit, shelfTop - height * unit),
                size = Size(width * unit, height * unit),
                unit = unit,
            )
        }
        // A leaning book resting against the last upright one.
        rotate(degrees = 18f, pivot = Offset(9f * unit, shelfTop)) {
            drawSpine(
                color = spines[3],
                accent = accent,
                topLeft = Offset(8.6f * unit, shelfTop - 8.5f * unit),
                size = Size(1.8f * unit, 8.5f * unit),
                unit = unit,
            )
        }
        // A small stack lying flat on the right.
        drawRoundRect(
            color = spines[1],
            topLeft = Offset(11.5f * unit, shelfTop - 1.2f * unit),
            size = Size(4.5f * unit, 1.2f * unit),
            cornerRadius = CornerRadius(unit * 0.2f),
        )
        drawRoundRect(
            color = spines[0],
            topLeft = Offset(12f * unit, shelfTop - 2.3f * unit),
            size = Size(3.8f * unit, 1.1f * unit),
            cornerRadius = CornerRadius(unit * 0.2f),
        )
        // The shelf plank.
        drawRoundRect(
            color = plank,
            topLeft = Offset(unit, shelfTop),
            size = Size(size.width - 2 * unit, unit * 0.8f),
            cornerRadius = CornerRadius(unit * 0.3f),
        )
    }
}

private fun DrawScope.drawSpine(color: Color, accent: Color, topLeft: Offset, size: Size, unit: Float) {
    drawRoundRect(color = color, topLeft = topLeft, size = size, cornerRadius = CornerRadius(unit * 0.25f))
    // Two bands across the spine, like a printed title block.
    val bandHeight = unit * 0.3f
    listOf(0.18f, 0.3f).forEach { fraction ->
        drawRect(
            color = accent.copy(alpha = 0.6f),
            topLeft = Offset(topLeft.x + unit * 0.25f, topLeft.y + size.height * fraction),
            size = Size(size.width - unit * 0.5f, bandHeight),
        )
    }
}
