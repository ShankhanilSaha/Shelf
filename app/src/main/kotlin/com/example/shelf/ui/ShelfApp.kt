package com.example.shelf.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.example.shelf.feature.scanner.ScannerNavKey
import com.example.shelf.navigation.BottomSheetSceneStrategy
import com.example.shelf.navigation.DeepLinkTarget
import com.example.shelf.navigation.Navigator
import com.example.shelf.navigation.TopLevelDestination
import com.example.shelf.navigation.rememberNavigationState
import com.example.shelf.navigation.shelfEntryProvider

/**
 * Root composable: a navigation bar on compact widths and a navigation rail from the medium
 * width breakpoint up (WindowSizeClass), around a Navigation 3 [NavDisplay].
 *
 * @param pendingDeepLink a deep link that still has to be shown. Modelled as state: it is
 *   applied once and then cleared through [onDeepLinkHandled].
 */
@Composable
fun ShelfApp(pendingDeepLink: DeepLinkTarget?, onDeepLinkHandled: () -> Unit, modifier: Modifier = Modifier) {
    val navigationState = rememberNavigationState()
    val navigator = remember(navigationState) { Navigator(navigationState) }

    LaunchedEffect(pendingDeepLink) {
        if (pendingDeepLink != null) {
            navigator.open(pendingDeepLink)
            onDeepLinkHandled()
        }
    }

    val navigationSuiteType = navigationSuiteTypeFor(
        currentKey = navigationState.currentKey,
        windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
    )
    val entryProvider = remember(navigator) { shelfEntryProvider(navigator) }
    val sceneStrategies = remember {
        listOf(BottomSheetSceneStrategy<NavKey>(), SinglePaneSceneStrategy())
    }

    NavigationSuiteScaffold(
        navigationItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = destination == navigationState.currentTopLevel
                NavigationSuiteItem(
                    selected = selected,
                    onClick = { navigator.select(destination) },
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                    navigationSuiteType = navigationSuiteType,
                )
            }
        },
        navigationSuiteType = navigationSuiteType,
        modifier = modifier,
    ) {
        NavDisplay(
            entries = navigationState.toDecoratedEntries(entryProvider),
            onBack = navigator::goBack,
            sceneStrategies = sceneStrategies,
        )
    }
}

/** Full-screen destinations hide the navigation UI; otherwise the window width decides. */
internal fun navigationSuiteTypeFor(currentKey: NavKey, windowSizeClass: WindowSizeClass): NavigationSuiteType = when {
    currentKey is ScannerNavKey -> NavigationSuiteType.None

    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
        NavigationSuiteType.NavigationRail

    else -> NavigationSuiteType.NavigationBar
}
