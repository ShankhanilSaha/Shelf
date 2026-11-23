package com.example.shelf.core.ui.text

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Text that a ViewModel can put into UI state without holding a Context. Resolved to a string
 * at the UI edge, so every user-visible string still comes from strings.xml. Arguments may
 * themselves be [UiText] (for example a shelf name inside "Added to %1$s"); they are resolved first.
 */
sealed interface UiText {
    data class Resource(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    data class Plural(@param:PluralsRes val id: Int, val quantity: Int, val args: List<Any> = listOf(quantity)) :
        UiText

    /** Text that is data rather than copy, such as a book title. */
    data class Raw(val value: String) : UiText
}

// The Android string APIs take varargs; the argument lists are tiny, so the copy is irrelevant.
@Suppress("SpreadOperator")
@Composable
@ReadOnlyComposable
fun UiText.asString(): String = when (this) {
    is UiText.Resource -> stringResource(id, *resolve(args))
    is UiText.Plural -> pluralStringResource(id, quantity, *resolve(args))
    is UiText.Raw -> value
}

@Suppress("SpreadOperator")
fun UiText.asString(context: Context): String = when (this) {
    is UiText.Resource -> context.getString(id, *args.map { it.resolve(context) }.toTypedArray())

    is UiText.Plural -> context.resources.getQuantityString(
        id,
        quantity,
        *args.map {
            it.resolve(context)
        }.toTypedArray(),
    )

    is UiText.Raw -> value
}

@Composable
@ReadOnlyComposable
private fun resolve(args: List<Any>): Array<Any> = args.map { if (it is UiText) it.asString() else it }.toTypedArray()

private fun Any.resolve(context: Context): Any = if (this is UiText) asString(context) else this
