package com.example.shelf.ui

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass
import com.example.shelf.feature.scanner.ScannerNavKey
import com.example.shelf.feature.search.SearchNavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationSuiteTypeTest {
    @Test
    fun `phones get a navigation bar`() {
        assertEquals(
            NavigationSuiteType.NavigationBar,
            navigationSuiteTypeFor(SearchNavKey, WindowSizeClass(minWidthDp = 360, minHeightDp = 640)),
        )
    }

    @Test
    fun `medium and wider windows get a navigation rail`() {
        assertEquals(
            NavigationSuiteType.NavigationRail,
            navigationSuiteTypeFor(SearchNavKey, WindowSizeClass(minWidthDp = 600, minHeightDp = 480)),
        )
        assertEquals(
            NavigationSuiteType.NavigationRail,
            navigationSuiteTypeFor(SearchNavKey, WindowSizeClass(minWidthDp = 840, minHeightDp = 900)),
        )
    }

    @Test
    fun `the scanner hides navigation`() {
        assertEquals(
            NavigationSuiteType.None,
            navigationSuiteTypeFor(ScannerNavKey, WindowSizeClass(minWidthDp = 840, minHeightDp = 900)),
        )
    }
}
