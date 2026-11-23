package com.example.shelf.core.ui.text

import androidx.annotation.StringRes
import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.ui.R

/** Display name of a shelf, shared by every screen that shows one. */
@get:StringRes
val ShelfType.labelRes: Int
    get() = when (this) {
        ShelfType.WANT_TO_READ -> R.string.ui_shelf_want_to_read
        ShelfType.READING -> R.string.ui_shelf_reading
        ShelfType.FINISHED -> R.string.ui_shelf_finished
    }

/** A user-facing explanation for a data-layer error. */
fun ShelfError.toUiText(): UiText = UiText.Resource(
    when (this) {
        ShelfError.Network -> R.string.ui_error_network
        ShelfError.NotFound -> R.string.ui_error_not_found
        ShelfError.Server -> R.string.ui_error_server
        ShelfError.Unavailable -> R.string.ui_error_unavailable
        ShelfError.Unknown -> R.string.ui_error_unknown
    },
)
