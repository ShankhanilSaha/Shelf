package com.example.shelf

import app.cash.turbine.test
import com.example.shelf.core.model.UserPreferences
import com.example.shelf.core.testing.MainDispatcherRule
import com.example.shelf.core.testing.awaitUntil
import com.example.shelf.core.testing.repository.FakeUserDataRepository
import com.example.shelf.navigation.DeepLinkTarget
import com.example.shelf.navigation.TopLevelDestination
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainActivityViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userData = FakeUserDataRepository(UserPreferences(onboardingCompleted = false))

    @Test
    fun `preferences load, and finishing onboarding is remembered`() = runTest {
        val viewModel = MainActivityViewModel(userData)

        viewModel.uiState.test {
            val ready = awaitUntil { it is MainActivityUiState.Ready } as MainActivityUiState.Ready
            assertEquals(false, ready.preferences.onboardingCompleted)

            viewModel.onOnboardingFinished()

            awaitUntil { (it as? MainActivityUiState.Ready)?.preferences?.onboardingCompleted == true }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a deep link waits until the UI has handled it`() = runTest {
        val viewModel = MainActivityViewModel(userData)

        val target = DeepLinkTarget(TopLevelDestination.STATS, backStack = emptyList())
        viewModel.pendingDeepLink.test {
            assertNull(awaitItem())

            viewModel.onDeepLink(target)
            assertEquals(target, awaitItem())

            viewModel.onDeepLink(null) // a launch without a link changes nothing
            viewModel.onDeepLinkHandled()
            assertNull(awaitItem())
        }
    }
}
