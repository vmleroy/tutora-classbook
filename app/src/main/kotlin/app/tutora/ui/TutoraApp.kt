package app.tutora.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import app.tutora.AppInfo
import app.tutora.AppState
import app.tutora.designsystem.TutoraTheme
import app.tutora.state.AppServices
import app.tutora.state.LocalServices

/** The whole app for a start-up state: nothing while loading, the tabs, or the error. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TutoraApp(
    state: AppState,
    appInfo: AppInfo,
    services: AppServices? = null,
) {
    // Test tags double as resource ids so Maestro flows can find them.
    val root = Modifier.semantics { testTagsAsResourceId = true }
    when (state) {
        // The splash is still up; drawing anything now could flash the wrong theme (AC-001-4).
        AppState.Loading -> Unit
        is AppState.Failed -> TutoraTheme { StartupError(state.result.backupPath, state.result.message, root) }
        is AppState.Ready -> {
            val s = services ?: return
            // The saved settings drive the theme live: a change repaints at once (AC-002-5).
            val settings by s.settings.collectAsState(initial = state.result.settings)
            TutoraTheme(
                themeId = settings.themeId,
                mode = settings.themeMode,
                custom = settings.customColors,
                largeText = settings.textScale == "large",
            ) {
                CompositionLocalProvider(LocalServices provides s) { TutoraNav(appInfo, s.schemaVersion, root) }
            }
        }
    }
}
