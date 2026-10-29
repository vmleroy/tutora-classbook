package app.tutora

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tutora.domain.dateOf
import app.tutora.domain.instantOf
import app.tutora.state.AppServices
import app.tutora.usecases.StartApp
import app.tutora.usecases.StartResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

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

    /** Today, set on start and on each return to the app, so the calendar's 8 weeks move with it (AC-012-9). */
    private val today = MutableStateFlow<LocalDate?>(null)

    init {
        viewModelScope.launch {
            _state.value =
                when (val r = startApp()) {
                    is StartResult.Ready -> {
                        services =
                            AppServices(r.db, r.ctx, r.schemaVersion).also { s ->
                                // A missed automatic backup runs now, once, without holding up Hoje (AC-011-8).
                                launch(kotlinx.coroutines.Dispatchers.IO) { runCatching { s.backups.runIfDue() } }
                                // The calendar follows every change to the classes, a moment later (AC-012-8, 14).
                                today.value = dayOf(s)
                                launch(kotlinx.coroutines.Dispatchers.IO) {
                                    s.calendar
                                        .changes(today.filterNotNull().distinctUntilChanged())
                                        .catch { }
                                        .collect { }
                                }
                            }
                        AppState.Ready(r)
                    }
                    is StartResult.Failed -> AppState.Failed(r)
                }
        }
    }

    private fun dayOf(s: AppServices) = dateOf(instantOf(s.now()), s.zone)

    /** Back in the foreground, maybe on a new day: keep repeating classes 8 weeks ahead (AC-005-8). */
    fun onResume() {
        val s = services ?: return
        today.value = dayOf(s)
        viewModelScope.launch { s.schedule.fillSchedule() }
    }
}
