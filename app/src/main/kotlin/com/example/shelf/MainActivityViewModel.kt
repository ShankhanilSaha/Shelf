package com.example.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelf.core.data.repository.UserDataRepository
import com.example.shelf.core.model.UserPreferences
import com.example.shelf.navigation.DeepLinkTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(private val userDataRepository: UserDataRepository) : ViewModel() {

    val uiState: StateFlow<MainActivityUiState> = userDataRepository.userPreferences
        .map<UserPreferences, MainActivityUiState>(MainActivityUiState::Ready)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MainActivityUiState.Loading)

    private val pending = MutableStateFlow<DeepLinkTarget?>(null)

    /** A deep link waiting to be shown; survives rotation until the UI has applied it. */
    val pendingDeepLink: StateFlow<DeepLinkTarget?> = pending.asStateFlow()

    fun onDeepLink(target: DeepLinkTarget?) {
        if (target != null) pending.value = target
    }

    fun onDeepLinkHandled() {
        pending.value = null
    }

    fun onOnboardingFinished() {
        viewModelScope.launch { userDataRepository.setOnboardingCompleted(true) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState

    data class Ready(val preferences: UserPreferences) : MainActivityUiState
}
