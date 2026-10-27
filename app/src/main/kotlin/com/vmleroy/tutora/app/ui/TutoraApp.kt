package com.vmleroy.tutora.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.vmleroy.tutora.app.AppInfo
import com.vmleroy.tutora.app.AppState
import com.vmleroy.tutora.designsystem.TutoraTheme

/** The whole app for a start-up state: nothing while loading, the tabs, or the error. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TutoraApp(
    state: AppState,
    appInfo: AppInfo,
) {
    // Test tags double as resource ids so Maestro flows can find them.
    val root = Modifier.semantics { testTagsAsResourceId = true }
    when (state) {
        // The splash is still up; drawing anything now could flash the wrong theme (AC-001-4).
        AppState.Loading -> Unit
        is AppState.Failed ->
            TutoraTheme { StartupError(state.result.backupPath, state.result.message, root) }
        is AppState.Ready ->
            TutoraTheme(themeId = state.result.settings.themeId) {
                TutoraTabs(appInfo, state.result.schemaVersion, root)
            }
    }
}
