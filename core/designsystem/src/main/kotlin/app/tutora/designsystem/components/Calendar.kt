package app.tutora.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
 * One square of a [MonthGrid]. [inMonth] false greys out the neighbors' days; [more] is how
 * many classes the dots leave out ("+N"); [a11y] is read aloud ("28 de outubro, 2 aulas").
 */
data class MonthDay(
    val day: Int,
    val inMonth: Boolean,
    val dots: List<DotTone>,
    val more: Int,
    val a11y: String,
    val today: Boolean = false,
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
) {
    val c = TutoraTheme.colors
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
        Column(
            Modifier.semantics { contentDescription = label }.selectableGroup(),
            // Room between days, so each reads on its own on any phone (Victor, 29 Oct 2026).
            verticalArrangement = Arrangement.spacedBy(Spacing.s1_5),
        ) {
            days.chunked(7).forEachIndexed { w, week ->
                // 4 dp across keeps a day 40 dp wide on a 360 dp phone with Large text.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                    week.forEachIndexed { i, d ->
                        val index = w * 7 + i
                        DayCell(d, index == selected, Modifier.weight(1f)) { onSelect(index) }
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
        modifier
            .height(Size.monthDay)
            .clip(RoundedCornerShape(Radius.xl))
            .background(bg)
            .then(if (d.inMonth) Modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick) else Modifier)
            .clearAndSetSemantics {
                contentDescription = d.a11y
                this.selected = selected
                if (!d.inMonth) disabled()
                role = Role.Tab
            }.padding(top = Spacing.s1_5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
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
                            Text(m.count, style = TutoraTheme.type.meta, color = c.inkMuted, maxLines = 1)
                        }
                    }
                }
            }
        }
        Button(todayLabel, onToday, variant = ButtonVariant.OUTLINE, fullWidth = true)
    }
}
