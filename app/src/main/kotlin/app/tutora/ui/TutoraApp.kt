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
    liveRequest: LiveRequest? = null,
    openRequest: Any? = null,
    onOpenRequestHandled: () -> Unit = {},
    onLiveRequestHandled: () -> Unit = {},
    lock: app.tutora.lock.AppLock? = null,
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
                textScale =
                    app.tutora.domain.TextSize
                        .of(settings.textScale)
                        .factor,
                uiScale =
                    app.tutora.domain.UiSize
                        .of(settings.uiScale)
                        .factor,
            ) {
                // Reports leave the phone through the share sheet and the clipboard (spec 010).
                val context = androidx.compose.ui.platform.LocalContext.current
                val exports =
                    androidx.compose.runtime.remember(context) {
                        app.tutora.adapters.export
                            .AndroidExports(context)
                    }
                val receiptImages =
                    androidx.compose.runtime.remember {
                        app.tutora.adapters.receipts
                            .AndroidReceiptImages()
                    }
                CompositionLocalProvider(
                    LocalServices provides s,
                    app.tutora.adapters.export.LocalExports provides exports,
                    app.tutora.ui.receipts.LocalReceiptImages provides receiptImages,
                    app.tutora.lock.LocalAppLock provides (lock ?: app.tutora.lock.LocalAppLock.current),
                ) {
                    // The app lock stands in front of everything; requests from notifications are
                    // still handled behind it, so Tutora opens on them once unlocked (AC-015-10).
                    app.tutora.lock.LockGate {
                        TutoraNav(
                            appInfo,
                            s.schemaVersion,
                            root,
                            liveRequest = liveRequest,
                            onLiveRequestHandled = onLiveRequestHandled,
                            openRequest = openRequest,
                            onOpenRequestHandled = onOpenRequestHandled,
                        )
                    }
                }
            }
        }
    }
}
