package app.tutora.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
 * Two columns side by side for a phone held sideways (spec 017): [start] takes [startWeight] of
 * the width, [end] the rest, a `space-5` gutter between. Each scrolls on its own unless told not
 * to (a pane holding its own list or a fitted grid), and neither grows past the readable width.
 * Too narrow for two `size-pane-min` columns, panes that scroll stack in one scrolling column.
 */
@Composable
fun TwoPane(
    start: @Composable ColumnScope.() -> Unit,
    end: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    startWeight: Float = 0.5f,
    scrollStart: Boolean = true,
    scrollEnd: Boolean = true,
    gap: androidx.compose.ui.unit.Dp = Spacing.s3,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxSize().testTag("two-pane")) {
        val narrowest = (maxWidth - Spacing.s5) * minOf(startWeight, 1f - startWeight)
        if (narrowest < Size.paneMin && scrollStart && scrollEnd) {
            Column(
                Modifier.readableWidth().fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                start()
                end()
            }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Spacing.s5)) {
                Pane("pane-start", startWeight, scrollStart, gap, start)
                Pane("pane-end", 1f - startWeight, scrollEnd, gap, end)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Pane(
    tag: String,
    weight: Float,
    scroll: Boolean,
    gap: androidx.compose.ui.unit.Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.weight(weight).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = Size.contentMax)
                .fillMaxSize()
                .testTag(tag)
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(gap),
            content = content,
        )
    }
}
