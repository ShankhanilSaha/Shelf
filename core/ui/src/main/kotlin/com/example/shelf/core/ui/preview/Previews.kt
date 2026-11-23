package com.example.shelf.core.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Renders a preview in light and dark theme. */
@Preview(name = "Light", showBackground = true)
@Preview(
    name = "Dark",
    showBackground = true,
    uiMode =
        Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class ThemePreviews

/** Renders a preview at the default font scale and at 1.3x, the scale the app must support without clipping. */
@Preview(name = "Font 1.0", showBackground = true)
@Preview(name = "Font 1.3", showBackground = true, fontScale = 1.3f)
annotation class FontScalePreviews

/**
 * A whole screen at phone size (where space is tightest) in light and dark theme and at font scale
 * 1.3. Used by the screenshot tests; names avoid dots, which the screenshot plugin cuts off.
 */
@Preview(name = "phone", widthDp = 400, heightDp = 860, showBackground = true)
@Preview(
    name = "phone-dark",
    widthDp = 400,
    heightDp = 860,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Preview(name = "phone-font130", widthDp = 400, heightDp = 860, showBackground = true, fontScale = 1.3f)
annotation class ScreenPreviews
