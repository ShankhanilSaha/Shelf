package com.example.shelf

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shelf.core.model.ThemeMode
import com.example.shelf.core.ui.theme.ShelfTheme
import com.example.shelf.navigation.ShelfDeepLinks
import com.example.shelf.ui.ShelfApp
import com.example.shelf.ui.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash until preferences are read, so the first frame already has the right theme.
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainActivityUiState.Loading }

        // A restored activity has already handled the link it was launched with.
        if (savedInstanceState == null) viewModel.onDeepLink(ShelfDeepLinks.resolve(intent))
        addOnNewIntentListener { newIntent -> viewModel.onDeepLink(ShelfDeepLinks.resolve(newIntent)) }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val preferences = (uiState as? MainActivityUiState.Ready)?.preferences ?: return@setContent
            val darkTheme = when (preferences.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Edge-to-edge with system bar icons that contrast with the app theme, not the system one.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }
            val pendingDeepLink by viewModel.pendingDeepLink.collectAsStateWithLifecycle()
            ShelfTheme(darkTheme = darkTheme, dynamicColor = preferences.useDynamicColor) {
                if (preferences.onboardingCompleted) {
                    ShelfApp(pendingDeepLink = pendingDeepLink, onDeepLinkHandled = viewModel::onDeepLinkHandled)
                } else {
                    WelcomeScreen(onGetStarted = viewModel::onOnboardingFinished)
                }
            }
        }
    }

    private companion object {
        // The default scrims used by enableEdgeToEdge() for three-button navigation.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
