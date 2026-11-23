package com.example.shelf.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

private val Default = Typography()

/**
 * Material 3 type scale with the device serif for display and headline styles (a bookish
 * touch without bundling font files). Body and label styles keep the default sans-serif
 * for legibility at small sizes.
 */
internal val ShelfTypography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = FontFamily.Serif),
    displayMedium = Default.displayMedium.copy(fontFamily = FontFamily.Serif),
    displaySmall = Default.displaySmall.copy(fontFamily = FontFamily.Serif),
    headlineLarge = Default.headlineLarge.copy(fontFamily = FontFamily.Serif),
    headlineMedium = Default.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = Default.headlineSmall.copy(fontFamily = FontFamily.Serif),
)
