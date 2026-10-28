package app.tutora.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tutora.app.state.AppServices
import app.tutora.app.usecases.StartApp
import app.tutora.app.usecases.StartResult
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

    /** Everything screens need once the database is open. */
    var services: AppServices? = null
        private set

    init {
        viewModelScope.launch {
            _state.value =
                when (val r = startApp()) {
                    is StartResult.Ready -> {
                        services = AppServices(r.db, r.ctx, r.schemaVersion)
                        AppState.Ready(r)
                    }
                    is StartResult.Failed -> AppState.Failed(r)
                }
        }
    }

    /** Back in the foreground, maybe on a new day: keep repeating classes 8 weeks ahead (AC-005-8). */
    fun onResume() {
        val s = services ?: return
        viewModelScope.launch { s.schedule.fillSchedule() }
    }
}
