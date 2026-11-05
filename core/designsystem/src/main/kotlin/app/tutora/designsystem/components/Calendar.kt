package app.tutora.designsystem.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing

// The Agenda's month view and its month picker (spec 016).

/** The kinds of class a dot can stand for, each in its fixed status color. */
enum class DotTone { SECONDARY, SUCCESS, ACCENT, VIOLET }

@Composable
private fun DotTone.color(): Color {
    val c = TutoraTheme.colors
    return when (this) {
        DotTone.SECONDARY -> c.secondary
        DotTone.SUCCESS -> c.success
        DotTone.ACCENT -> c.accent
        DotTone.VIOLET -> c.tagViolet
    }
}

/**
 * One square of a [MonthGrid]. [inMonth] false greys out the neighbors' days, still tappable to
 * go to their month; [enabled] false (past the Agenda's horizon) is not. [more] is how many
 * classes the dots leave out ("+N"); [a11y] is read aloud ("28 de outubro, 2 aulas").
 */
data class MonthDay(
    val day: Int,
    val inMonth: Boolean,
    val dots: List<DotTone>,
    val more: Int,
    val a11y: String,
    val today: Boolean = false,
    val enabled: Boolean = true,
    /** A class on this day is waiting to be confirmed: a ring around the tile (AC-014-5). */
    val pending: Boolean = false,
)

/**
 * A month as weeks of seven days under [heads] (S T Q Q S S D), with a dot per class and a
 * [legend] under it. Today is filled with the main button color, the [selected] day tinted.
 */
@Composable
fun MonthGrid(
    days: List<MonthDay>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    heads: List<String>,
    label: String,
    modifier: Modifier = Modifier,
    legend: List<Pair<DotTone, String>> = emptyList(),
    /** Which month this is, as a growing number (year × 12 + month), so the slide goes the right way. */
    page: Int = 0,
    /** A swipe to the next (+1) or previous (−1) month; null turns swiping off (Victor, 4 Nov 2026). */
    onSwipe: ((Int) -> Unit)? = null,
    /** What screen readers call the two swipes ("Mês anterior", "Próximo mês"): a swipe has no other way in. */
    swipeLabels: Pair<String, String>? = null,
) {
    val c = TutoraTheme.colors
    val reduceMotion = TutoraTheme.reduceMotion
    val swipeAt = with(androidx.compose.ui.platform.LocalDensity.current) { Size.touch.toPx() }
    Column(
        modifier
            .fillMaxWidth()
            .testTag("month-grid")
            .clip(RoundedCornerShape(Radius.x3xl))
            .background(c.surface)
            .padding(horizontal = Spacing.s2, vertical = Spacing.s3),
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
    ) {
        Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            heads.forEach {
                Text(
                    it,
                    style = TutoraTheme.type.badge,
                    color = c.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // Swiping across the weeks changes month; the new month slides in from the side it came.
        // Keyed once, reading the latest callback: a recomposition mid-drag must not restart it.
        val latestSwipe = androidx.compose.runtime.rememberUpdatedState(onSwipe)
        var dragged by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
        val dragState =
            androidx.compose.foundation.gestures
                .rememberDraggableState { dragged += it }
        // A horizontal draggable lives well inside the screen's vertical scroll.
        val swipe =
            if (onSwipe == null) {
                Modifier
            } else {
                Modifier
                    .semantics {
                        swipeLabels?.let { (prev, next) ->
                            customActions =
                                listOf(
                                    androidx.compose.ui.semantics.CustomAccessibilityAction(prev) {
                                        latestSwipe.value?.invoke(-1)
                                        true
                                    },
                                    androidx.compose.ui.semantics.CustomAccessibilityAction(next) {
                                        latestSwipe.value?.invoke(1)
                                        true
                                    },
                                )
                        }
                    }.draggable(
                        dragState,
                        androidx.compose.foundation.gestures.Orientation.Horizontal,
                        onDragStarted = { dragged = 0f },
                        onDragStopped = {
                            if (dragged <= -swipeAt) {
                                latestSwipe.value?.invoke(1)
                            } else if (dragged >= swipeAt) {
                                latestSwipe.value?.invoke(-1)
                            }
                        },
                    )
            }
        androidx.compose.animation.AnimatedContent(
            targetState = Triple(page, days, selected),
            modifier = swipe,
            transitionSpec = {
                val forward = targetState.first > initialState.first
                if (reduceMotion || targetState.first == initialState.first) {
                    androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                } else {
                    (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it else it } + fadeOut())
                }
            },
            contentKey = { it.first },
            label = "month",
        ) { (_, shown, chosen) ->
            Column(
                Modifier.semantics { contentDescription = label }.selectableGroup(),
                // 8 dp between weeks (Victor, 4 Nov 2026).
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                shown.chunked(7).forEachIndexed { w, week ->
                    // 4 dp across keeps a day 40 dp wide on a 360 dp phone with Large text (AC-016-11).
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                        week.forEachIndexed { i, d ->
                            val index = w * 7 + i
                            DayCell(d, index == chosen, Modifier.weight(1f)) { onSelect(index) }
                        }
                    }
                }
            }
        }
        if (legend.isNotEmpty()) {
            FlowRow(
                Modifier.padding(horizontal = Spacing.s1, vertical = Spacing.s1),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                verticalArrangement = Arrangement.spacedBy(Spacing.s1),
            ) {
                legend.forEach { (tone, text) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s1_5), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(Size.dotSm).clip(CircleShape).background(tone.color()))
                        Text(text, style = TutoraTheme.type.meta, color = c.inkMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    d: MonthDay,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = TutoraTheme.colors
    val bg =
        when {
            d.today -> c.btnPrimary
            selected -> c.primarySoft
            d.inMonth -> c.surfaceSunken
            else -> Color.Transparent
        }
    val fg =
        when {
            d.today -> c.onBtnPrimary
            d.inMonth -> c.ink
            else -> c.inkMuted
        }
    Column(
        // Square tiles with small corners, not tall capsules (Victor, 4 Nov 2026).
        modifier
            // Sideways a square would be too tall for six weeks: a `size-month-day` row (spec 017).
            .then(if (TutoraTheme.landscape) Modifier.height(Size.monthDay) else Modifier.aspectRatio(1f))
            .clip(RoundedCornerShape(Radius.sm))
            .background(bg)
            .then(
                if (d.pending) {
                    Modifier
                        .testTag("month-pending")
                        .border(Size.borderStrong, c.secondary, RoundedCornerShape(Radius.sm))
                } else {
                    Modifier
                },
            ).then(if (d.enabled) Modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick) else Modifier)
            .clearAndSetSemantics {
                contentDescription = d.a11y
                this.selected = selected
                if (!d.enabled) disabled()
                role = Role.Tab
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s0_5, Alignment.CenterVertically),
    ) {
        Text(
            d.day.toString(),
            style = TutoraTheme.type.rowTitle.copy(fontFeatureSettings = "tnum"),
            color = if (d.inMonth || d.today) fg else c.lineStrong,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s0_5), verticalAlignment = Alignment.CenterVertically) {
            d.dots.forEach { Box(Modifier.size(Size.monthDot).clip(CircleShape).background(if (d.today) c.onBtnPrimary else it.color())) }
            if (d.more > 0) Text("+${d.more}", style = TutoraTheme.type.meta, color = fg)
        }
    }
}

/** One month in a [MonthPicker]: "Out" and "25 aulas"; [enabled] false past the Agenda's horizon. */
data class PickerMonth(
    val label: String,
    val count: String,
    val a11y: String,
    val enabled: Boolean = true,
)

/**
 * A year and its twelve months, three a row, each with its number of classes; the [selected]
 * month is filled. ‹ › change the year, and [onToday] goes back to today.
 */
@Composable
fun MonthPicker(
    year: String,
    months: List<PickerMonth>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    prevLabel: String,
    onPrev: () -> Unit,
    nextLabel: String,
    onNext: () -> Unit,
    todayLabel: String,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
    nextEnabled: Boolean = true,
) {
    val c = TutoraTheme.colors
    Column(modifier.fillMaxWidth().testTag("month-picker"), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(IconName.CHEVRON_LEFT, prevLabel, onPrev)
            Text(
                year,
                style = TutoraTheme.type.titleLg.copy(fontFeatureSettings = "tnum"),
                color = c.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(IconName.CHEVRON_RIGHT, nextLabel, onNext, enabled = nextEnabled)
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.s2_5)) {
            months.chunked(3).forEachIndexed { r, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2_5)) {
                    row.forEachIndexed { i, m ->
                        val index = r * 3 + i
                        val on = index == selected
                        val shape = RoundedCornerShape(Radius.full)
                        Column(
                            Modifier
                                .weight(1f)
                                .testTag("picker-month-$index")
                                .selectable(selected = on, enabled = m.enabled, role = Role.Tab, onClick = { onSelect(index) })
                                .clearAndSetSemantics {
                                    contentDescription = m.a11y
                                    this.selected = on
                                    if (!m.enabled) disabled()
                                    role = Role.Tab
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.s1),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .sizeIn(minHeight = Size.touch)
                                    .clip(shape)
                                    .background(if (on) c.btnPrimary else c.surfaceSunken)
                                    .then(if (!m.enabled) Modifier.border(Size.border, c.line, shape) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    m.label,
                                    style = TutoraTheme.type.rowTitle,
                                    color =
                                        when {
                                            on -> c.onBtnPrimary
                                            m.enabled -> c.ink
                                            else -> c.inkMuted
                                        },
                                )
                            }
                            if (m.count.isNotEmpty()) Text(m.count, style = TutoraTheme.type.meta, color = c.inkMuted, maxLines = 1)
                        }
                    }
                }
            }
        }
        Button(todayLabel, onToday, variant = ButtonVariant.OUTLINE, fullWidth = true)
    }
}
