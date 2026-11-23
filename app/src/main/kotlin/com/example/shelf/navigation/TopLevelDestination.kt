package com.example.shelf.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.example.shelf.R
import com.example.shelf.core.ui.icon.ShelfIcons
import com.example.shelf.feature.library.LibraryNavKey
import com.example.shelf.feature.search.SearchNavKey
import com.example.shelf.feature.settings.SettingsNavKey
import com.example.shelf.feature.stats.StatsNavKey

/** Tabs of the navigation bar / rail. Each tab keeps its own back stack. */
enum class TopLevelDestination(
    val key: NavKey,
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    LIBRARY(LibraryNavKey.Shelves, R.string.nav_library, ShelfIcons.LibraryFilled, ShelfIcons.LibraryOutlined),
    SEARCH(SearchNavKey, R.string.nav_search, ShelfIcons.SearchFilled, ShelfIcons.SearchOutlined),
    STATS(StatsNavKey.Overview, R.string.nav_stats, ShelfIcons.StatsFilled, ShelfIcons.StatsOutlined),
    SETTINGS(SettingsNavKey, R.string.nav_settings, ShelfIcons.SettingsFilled, ShelfIcons.SettingsOutlined),
    ;

    companion object {
        val START = LIBRARY

        fun fromKey(key: NavKey): TopLevelDestination? = entries.firstOrNull { it.key == key }
    }
}
