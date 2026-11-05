package app.tutora.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing

/**
 * A panel from the bottom over a scrim, for choices, confirmations and pickers. It covers the
 * whole screen wherever it is placed, rises above the keyboard, and closes on the scrim or
 * the back button. With reduce motion it appears without sliding (AC-002-17).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: List<@Composable (Modifier) -> Unit> = emptyList(),
    content: @Composable () -> Unit = {},
) {
    val c = TutoraTheme.colors
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        modifier = modifier.semantics { contentDescription = title },
        containerColor = c.surface,
        scrimColor = c.scrim,
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(topStart = Radius.x3xl, topEnd = Radius.x3xl),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(start = Spacing.s5, end = Spacing.s5, bottom = Spacing.s7),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3_5),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s0_5)) {
                Text(title, style = TutoraTheme.type.titleLg, color = c.ink, modifier = Modifier.semantics { heading() })
                if (subtitle != null) Text(subtitle, style = TutoraTheme.type.caption, color = c.inkMuted)
            }
            // A tall body scrolls; the title and the buttons stay in view.
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) { content() }
            if (actions.isNotEmpty()) {
                // Outline + primary sit side by side, 1 : 2.
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2_5)) {
                    actions.forEachIndexed { i, action ->
                        action(Modifier.weight(if (actions.size > 1 && i == actions.lastIndex) 2f else 1f))
                    }
                }
            }
        }
    }
}

/** What a tab shows before it has anything: an icon, a title and a line of help. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: IconName? = null,
    body: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val c = TutoraTheme.colors
    Column(
        modifier.fillMaxWidth().padding(vertical = Spacing.s7),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        if (icon != null) Icon(icon, tint = c.inkMuted, size = Size.iconLg)
        Text(title, style = TutoraTheme.type.titleMd, color = c.ink, textAlign = TextAlign.Center)
        if (body != null) Text(body, style = TutoraTheme.type.body, color = c.inkMuted, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

/** The live lesson ring: elapsed time in the middle, [progress] (0..1) of the planned minutes. */
@Composable
fun TimerRing(
    elapsed: String,
    progress: Float,
    modifier: Modifier = Modifier,
    caption: String? = null,
    label: String? = null,
) {
    val c = TutoraTheme.colors
    val stroke = with(LocalDensity.current) { Size.timerStroke.toPx() }
    val p = progress.coerceIn(0f, 1f)
    Box(
        modifier
            .size(Size.timerRing)
            .semantics {
                contentDescription = label ?: elapsed
                progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(Size.timerRing)) {
            val inset = stroke / 2
            val arc =
                androidx.compose.ui.geometry
                    .Size(size.width - stroke, size.height - stroke)
            val topLeft =
                androidx.compose.ui.geometry
                    .Offset(inset, inset)
            drawArc(c.onHeader.copy(alpha = 0.2f), 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
            drawArc(c.accent, -90f, 360f * p, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // One line always: with Large text the clock shrinks to fit inside the ring, never wraps.
            val style = TutoraTheme.type.moneyDisplay
            androidx.compose.foundation.text.BasicText(
                elapsed,
                Modifier.padding(horizontal = Size.timerStroke * 2),
                style = style.copy(color = c.onHeader),
                maxLines = 1,
                softWrap = false,
                autoSize =
                    androidx.compose.foundation.text.TextAutoSize
                        .StepBased(maxFontSize = style.fontSize),
            )
            if (caption != null) Text(caption, style = TutoraTheme.type.caption, color = c.onHeaderMuted)
        }
    }
}

/**
 * A bar at the bottom after an action that changed many things at once: what was done and
 * "Desfazer", for a few seconds (AC-014-9). It closes by itself after [durationMs], calling
 * [onTimeout]; the screen keeps the change from then on.
 */
@Composable
fun UndoBar(
    message: String,
    actionLabel: String,
    onUndo: () -> Unit,
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
    durationMs: Long = UNDO_MS,
) {
    val c = TutoraTheme.colors
    val latest = androidx.compose.runtime.rememberUpdatedState(onTimeout)
    androidx.compose.runtime.LaunchedEffect(message) {
        kotlinx.coroutines.delay(durationMs)
        latest.value()
    }
    Row(
        modifier
            .fillMaxWidth()
            .testTag("undo-bar")
            .padding(horizontal = Spacing.s4, vertical = Spacing.s2)
            .navigationBarsPadding()
            .clip(RoundedCornerShape(Radius.xl))
            .background(c.ink)
            .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
            .padding(start = Spacing.s4, end = Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        Text(message, style = TutoraTheme.type.body, color = c.surface, modifier = Modifier.weight(1f).padding(vertical = Spacing.s3))
        Text(
            actionLabel,
            style = TutoraTheme.type.bodyStrong,
            color = c.accent,
            modifier =
                Modifier
                    .sizeIn(minHeight = Size.touch, minWidth = Size.touch)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onUndo)
                    .padding(horizontal = Spacing.s3)
                    .wrapContentHeight(),
        )
    }
}

/** How long Desfazer stays (AC-014-9). */
const val UNDO_MS = 6_000L
