package app.tutora.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing

/**
 * The readable width (spec 017): content takes the whole width up to `size-content-max`, and
 * past it sits centered with the background on both sides (AC-017-4).
 */
fun Modifier.readableWidth(): Modifier =
    this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = Size.contentMax)
        .fillMaxWidth()

/**
 * A panel over the right side of the screen for a phone held sideways (spec 017): what was tapped
 * opens in it while the screen stays under a scrim. A tap on the scrim calls [onDismiss]; Back is
 * the host's. It is at most `size-content-max` wide and never more than [WIDTH_SHARE] of the
 * screen, clears the cut-out and system bars on its side, and slides in unless motion is reduced.
 */
@Composable
fun SidePanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = TutoraTheme.colors
    val motion = TutoraTheme.reduceMotion
    val dismiss = rememberUpdatedState(onDismiss)
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible,
            enter = if (motion) EnterTransition.None else fadeIn(),
            exit = if (motion) ExitTransition.None else fadeOut(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(c.scrim)
                    .testTag("side-panel-scrim")
                    // A tap outside closes it; screen readers close it with Back, so the scrim
                    // is not a control of its own.
                    .pointerInput(Unit) { detectTapGestures { dismiss.value() } },
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = if (motion) EnterTransition.None else slideInHorizontally { it },
            exit = if (motion) ExitTransition.None else slideOutHorizontally { it },
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(WIDTH_SHARE)
                    .widthIn(max = Size.contentMax)
                    .shadow(Spacing.s4)
                    .background(c.background)
                    .testTag("side-panel"),
            ) { content() }
        }
    }
}

/** The most of the screen's width a [SidePanel] takes. */
const val WIDTH_SHARE = 0.55f
