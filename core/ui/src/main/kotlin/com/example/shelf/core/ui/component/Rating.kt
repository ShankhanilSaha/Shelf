package com.example.shelf.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.shelf.core.model.Rating
import com.example.shelf.core.ui.R
import com.example.shelf.core.ui.icon.ShelfIcons

/**
 * Editable 1–5 star rating. Each star is one 48dp radio-button target inside a selectable group,
 * so TalkBack announces "3 stars, selected, 3 of 5". Tapping the selected star clears the rating.
 */
@Composable
fun RatingInput(rating: Int?, onRatingChange: (Int?) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier = modifier.selectableGroup()) {
        Rating.RANGE.forEach { value ->
            val selected = rating == value
            val filled = rating != null && value <= rating
            val label = pluralStringResource(R.plurals.ui_rating_option, value, value)
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .selectable(
                        selected = selected,
                        interactionSource = null,
                        indication = ripple(bounded = false, radius = 24.dp),
                        role = Role.RadioButton,
                        onClick = { onRatingChange(if (selected) null else value) },
                    )
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (filled) ShelfIcons.StarFilled else ShelfIcons.StarOutlined,
                    contentDescription = null,
                    tint = if (filled) colors.primary else colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** Read-only star rating announced as a single phrase ("Rated 4 stars out of 5"). */
@Composable
fun RatingDisplay(rating: Int?, modifier: Modifier = Modifier, starSize: Dp = 16.dp) {
    val description = if (rating == null) {
        stringResource(R.string.ui_rating_none)
    } else {
        pluralStringResource(R.plurals.ui_rating_value, rating, rating)
    }
    Row(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Rating.RANGE.forEach { value ->
            val filled = rating != null && value <= rating
            Icon(
                imageVector = if (filled) ShelfIcons.StarFilled else ShelfIcons.StarOutlined,
                contentDescription = null,
                tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(starSize),
            )
        }
    }
}
