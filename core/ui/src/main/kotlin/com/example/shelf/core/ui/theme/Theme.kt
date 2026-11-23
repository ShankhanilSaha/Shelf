package com.example.shelf.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** True when the device can generate a Material You palette from the wallpaper (API 31+). */
val supportsDynamicColor: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * App theme. Dynamic color is used on API 31+ when [dynamicColor] is true; otherwise the
 * hand-made palette in Color.kt.
 */
@Composable
fun ShelfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && supportsDynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> ShelfDarkColors

        else -> ShelfLightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShelfTypography,
        content = content,
    )
}
