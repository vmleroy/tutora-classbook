package com.vmleroy.tutora.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vmleroy.tutora.app.usecases.StartApp
import com.vmleroy.tutora.app.usecases.StartResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the app can show: nothing yet (splash), the tabs, or the start-up error. */
sealed interface AppState {
    data object Loading : AppState

    data class Ready(
        val result: StartResult.Ready,
    ) : AppState

    data class Failed(
        val result: StartResult.Failed,
    ) : AppState
}

class AppViewModel(
    startApp: StartApp,
) : ViewModel() {
    private val _state = MutableStateFlow<AppState>(AppState.Loading)
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value =
                when (val r = startApp()) {
                    is StartResult.Ready -> AppState.Ready(r)
                    is StartResult.Failed -> AppState.Failed(r)
                }
        }
    }
}
