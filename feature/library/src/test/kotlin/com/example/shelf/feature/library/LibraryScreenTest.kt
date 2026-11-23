package com.example.shelf.feature.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.testing.MainDispatcherRule
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.testing.repository.FakeLibraryRepository
import com.example.shelf.core.ui.theme.ShelfTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class LibraryScreenTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int, vararg args: Any) = RuntimeEnvironment.getApplication().getString(id, *args)

    private val library = FakeLibraryRepository().apply {
        addBook(TestData.book(1, title = "Piranesi"), TestData.entry(1, shelf = ShelfType.READING, currentPage = 30))
        addBook(TestData.book(2, title = "Dune"), TestData.entry(2, shelf = ShelfType.FINISHED, rating = 5))
    }

    private fun tab(shelf: Int, count: Int) = string(R.string.library_tab, string(shelf), count)

    @Test
    fun switchingShelves_showsTheBooksOfTheSelectedShelf() {
        val viewModel = LibraryViewModel(library, SavedStateHandle())
        var opened: String? = null
        composeRule.setContent {
            ShelfTheme(dynamicColor = false) {
                ShelvesRoute(onBookClick = { opened = it }, onSearchClick = {}, onScanClick = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText(tab(com.example.shelf.core.ui.R.string.ui_shelf_reading, 1)).assertIsSelected()
        composeRule.onNodeWithText("Piranesi").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.library_page_of, 30, 300)).assertIsDisplayed()

        composeRule.onNodeWithText(tab(com.example.shelf.core.ui.R.string.ui_shelf_finished, 1)).performClick()
        composeRule.onNodeWithText("Dune").assertIsDisplayed()
        composeRule.onNodeWithText("Piranesi").assertDoesNotExist()

        composeRule.onNodeWithText(tab(com.example.shelf.core.ui.R.string.ui_shelf_want_to_read, 0)).performClick()
        composeRule.onNodeWithText(string(R.string.library_empty_want_title)).assertIsDisplayed()

        composeRule.onNodeWithText(tab(com.example.shelf.core.ui.R.string.ui_shelf_finished, 1)).performClick()
        composeRule.onNodeWithText("Dune").performClick()
        assertEquals("OL2W", opened)
    }
}
