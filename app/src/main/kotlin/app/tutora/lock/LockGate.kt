package app.tutora.lock

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.EmptyState
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.LocalCovered
import app.tutora.designsystem.components.readableWidth
import app.tutora.state.LocalServices

/** The app's lock; the default one is always open (screens shown on their own in tests). */
val LocalAppLock = staticCompositionLocalOf { AppLock().also { it.follow(app.tutora.domain.Settings(app.tutora.domain.Language.EN)) } }

/**
 * The gate in front of the app (spec 015). While locked, the app stays composed, so it is
 * exactly where it was once unlocked (AC-015-10), but it draws nothing, can't be read by a
 * screen reader and takes no touch; sheets and dialogs hide themselves (AC-015-9).
 */
@Composable
fun LockGate(content: @Composable () -> Unit) {
    val lock = LocalAppLock.current
    val services = LocalServices.current
    LaunchedEffect(lock, services) { services.settings.collect { lock.follow(it) } }
    val gate by lock.gate.collectAsState()
    val covered = gate != Gate.OPEN
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(covered) {
        if (covered) {
            focus.clearFocus(force = true)
            keyboard?.hide()
        }
    }
    Box(Modifier.fillMaxSize().background(TutoraTheme.colors.background)) {
        CompositionLocalProvider(LocalCovered provides covered) {
            Box(if (covered) Modifier.fillMaxSize().alpha(0f).clearAndSetSemantics {} else Modifier.fillMaxSize()) { content() }
        }
        if (covered) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(TutoraTheme.colors.background)
                    // Every touch stops here: nothing behind can be reached (AC-015-9).
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
            ) {
                if (gate == Gate.LOCKED) LockScreen(lock)
            }
        }
    }
}

/** The lock screen: the app's name and Desbloquear, which asks at once when it shows (AC-015-6). */
@Composable
private fun LockScreen(lock: AppLock) {
    val activity = LocalActivity.current
    // Back leaves Tutora as it is, locked.
    BackHandler { activity?.moveTaskToBack(true) }
    LaunchedEffect(Unit) { lock.unlock() }
    Box(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .testTag("lock-screen"),
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            stringResource(R.string.app_name),
            Modifier.readableWidth(),
            icon = IconName.LOCK,
            body = stringResource(R.string.lock_body),
            action = { Button(stringResource(R.string.lock_unlock), { lock.unlock() }, Modifier.testTag("lock-unlock")) },
        )
    }
}
