package com.example.shelf.feature.stats

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.example.shelf.core.ui.component.LabeledExtendedFab
import com.example.shelf.core.ui.icon.ShelfIcons
import com.example.shelf.core.ui.theme.ShelfTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

/** Regression test: the "Log reading" FAB must expose its label to accessibility services. */
@RunWith(RobolectricTestRunner::class)
class LabeledFabTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun extendedFab_isAnnouncedWithItsLabel() {
        composeRule.setContent {
            ShelfTheme(dynamicColor = false) {
                LabeledExtendedFab(text = "Log reading", icon = ShelfIcons.Add, onClick = {})
            }
        }

        composeRule.onNodeWithContentDescription("Log reading").assertHasClickAction()
    }
}
