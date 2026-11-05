package app.tutora.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import app.tutora.designsystem.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.tokens.TutoraTokens.Opacity
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing

enum class HeaderVariant { HERO, TAB, PUSH, MODAL }

data class HeaderAction(
    val icon: IconName,
    val label: String,
    val variant: IconButtonVariant = IconButtonVariant.SUNKEN,
    val onClick: () -> Unit,
)

/**
 * A screen's title bar. It never paints a band itself: HERO is Hoje's date over the greeting,
 * in on-header colors for the Topo band the screen draws behind it (spec 016); TAB a tab title,
 * PUSH a pushed screen with back, MODAL a sheet.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    variant: HeaderVariant = HeaderVariant.TAB,
    eyebrow: String? = null,
    actions: List<HeaderAction> = emptyList(),
    onBack: (() -> Unit)? = null,
    /** A labelled action after the icon actions, such as Ganhos' Relatório button. */
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = TutoraTheme.colors
    val big = variant == HeaderVariant.TAB || variant == HeaderVariant.HERO
    val hero = variant == HeaderVariant.HERO
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("screen-header")
                .padding(horizontal = Spacing.s5, vertical = Spacing.s3),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) IconButton(IconName.CHEVRON_LEFT, stringResource(R.string.ds_back), onBack)
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) Text(eyebrow, style = TutoraTheme.type.caption, color = if (hero) c.onHeaderMuted else c.inkMuted)
            Text(
                title,
                style = if (big) TutoraTheme.type.titleXl else TutoraTheme.type.titleLg,
                color = if (hero) c.onHeader else c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        actions.forEach {
            // On Hoje's Topo band the + is round and the other actions are tinted for it.
            val v =
                when {
                    !hero -> it.variant
                    it.variant == IconButtonVariant.ACCENT || it.variant == IconButtonVariant.ACCENT_SQUARE -> IconButtonVariant.ACCENT
                    else -> IconButtonVariant.ON_PRIMARY
                }
            IconButton(it.icon, it.label, it.onClick, variant = v)
        }
        trailing?.invoke()
    }
}

data class TabItem(
    val key: String,
    val label: String,
    val icon: IconName,
    /** A number on the icon, such as the Agenda's classes to confirm (AC-014-4); 0 shows none. */
    val count: Int = 0,
)

private const val MAX_TAB_COUNT = 99

/** The bottom bar on the sunken tone: five tabs, labels always shown, the active one on a Menu ativo pill. */
@Composable
fun TabBar(
    tabs: List<TabItem>,
    active: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = TutoraTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("tab-bar")
                .background(c.surfaceSunken)
                .navigationBarsPadding()
                .height(Size.tabbar)
                .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val on = tab.key == active
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .sizeIn(minHeight = Size.touch)
                        .testTag("tab-${tab.key}")
                        .selectable(selected = on, role = Role.Tab, onClick = { onSelect(tab.key) })
                        .semantics { contentDescription = if (tab.count > 0) "${tab.label}, ${tab.count}" else tab.label },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.s1),
            ) {
                Box(
                    Modifier
                        .testTag("tab-pill-${tab.key}")
                        .clip(RoundedCornerShape(Radius.full))
                        .background(if (on) c.tabActiveSoft else Color.Transparent)
                        .padding(horizontal = Spacing.s4, vertical = Spacing.s1),
                ) {
                    Icon(tab.icon, tint = if (on) c.tabActive else c.inkMuted)
                    if (tab.count > 0) {
                        Text(
                            if (tab.count > MAX_TAB_COUNT) "$MAX_TAB_COUNT+" else tab.count.toString(),
                            style = TutoraTheme.type.badge.copy(fontFeatureSettings = "tnum"),
                            color = c.surface,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .testTag("tab-count-${tab.key}")
                                    .align(Alignment.TopEnd)
                                    .offset(x = Spacing.s3, y = -Spacing.s0_5)
                                    .clip(RoundedCornerShape(Radius.full))
                                    .background(c.danger)
                                    .padding(horizontal = Spacing.s1),
                        )
                    }
                }
                Text(
                    tab.label,
                    style = if (on) TutoraTheme.type.tab.copy(fontWeight = FontWeight.Black) else TutoraTheme.type.tab,
                    color = if (on) c.ink else c.inkMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

data class WeekDay(
    val weekday: String,
    val day: Int,
    val dot: Tag? = null,
    val a11y: String? = null,
    /** False past the Agenda's horizon: greyed and not pressable (Victor, 4 Nov 2026). */
    val enabled: Boolean = true,
)

/**
 * Seven days, each its weekday over a square tile like the month's (Victor, 4 Nov 2026): the day
 * being shown is filled, today is outlined, and a dot under the tile marks days with classes.
 */
@Composable
fun WeekStrip(
    days: List<WeekDay>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    today: Int? = null,
    label: String = stringResource(R.string.ds_week_days),
) {
    val c = TutoraTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label }.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1_5),
    ) {
        days.forEachIndexed { i, d ->
            val on = i == selected
            val isToday = i == today
            val shape = RoundedCornerShape(Radius.sm)
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .alpha(if (d.enabled) 1f else Opacity.disabled)
                        .selectable(selected = on, enabled = d.enabled, role = Role.Tab, onClick = { onSelect(i) })
                        .semantics { contentDescription = d.a11y ?: "${d.weekday} ${d.day}" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.s1),
            ) {
                Text(d.weekday, style = TutoraTheme.type.badge, color = if (on || isToday) c.btnPrimary else c.inkMuted)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(shape)
                        .background(if (on) c.btnPrimary else c.surfaceSunken)
                        .then(if (isToday && !on) Modifier.border(Size.borderStrong, c.btnPrimary, shape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        d.day.toString(),
                        style = TutoraTheme.type.bodyStrong.copy(fontFeatureSettings = "tnum"),
                        color =
                            if (on) {
                                c.onBtnPrimary
                            } else if (isToday) {
                                c.btnPrimary
                            } else {
                                c.ink
                            },
                    )
                }
                Box(Modifier.size(Size.weekDot).clip(CircleShape).background(d.dot?.colors()?.fill ?: Color.Transparent))
            }
        }
    }
}

/**
 * MUTED is a cancelled or missed class: neutral fill and the title struck through. LIVE is the
 * class running now: accent outline and "Ao vivo", like NEXT with its own word.
 */
enum class BlockState {
    DEFAULT,
    DONE,
    NEXT,
    MUTED,
    LIVE,

    /** A class worked out from its series past the planned days: lighter, marked "Prevista" (AC-016-35). */
    FORECAST,

    /** A class that ended still scheduled: dimmed, marked "A confirmar" (AC-014-3). */
    TO_CONFIRM,
}

/** A lesson on the Agenda grid, sized by its duration; the screen places it. */
@Composable
fun AgendaBlock(
    tag: Tag,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    state: BlockState = BlockState.DEFAULT,
    height: androidx.compose.ui.unit.Dp = Size.agendaMin,
    onClick: (() -> Unit)? = null,
) {
    val t = tag.colors()
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(Radius.xl)
    val badge =
        when (state) {
            BlockState.NEXT -> stringResource(R.string.ds_next)
            BlockState.LIVE -> stringResource(R.string.ds_live)
            else -> null
        }
    val muted = state == BlockState.MUTED
    val forecast = state == BlockState.FORECAST
    val pending = state == BlockState.TO_CONFIRM
    val pendingWord = stringResource(R.string.ds_toConfirm)
    val ink = if (muted) c.inkMuted else t.ink
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .height(maxOf(height, Size.agendaMin))
                .alpha(
                    when {
                        forecast -> Opacity.disabled
                        pending -> Opacity.pending
                        else -> 1f
                    },
                ).clip(shape)
                .background(if (muted) c.surfaceSunken else t.soft)
                .then(if (badge != null) Modifier.border(Size.borderStrong, c.accent, shape) else Modifier)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(role = Role.Button, onClick = onClick).semantics {
                            contentDescription =
                                listOfNotNull(title, subtitle, badge, pendingWord.takeIf { pending }).joinToString(", ")
                        }
                    } else {
                        Modifier
                    },
                ).padding(horizontal = Spacing.s2, vertical = Spacing.s1),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s1), verticalAlignment = Alignment.CenterVertically) {
            if (state == BlockState.DONE) Icon(IconName.CHECK, tint = t.ink, size = Size.iconSm)
            Text(
                title,
                style = TutoraTheme.type.caption.copy(textDecoration = if (muted) TextDecoration.LineThrough else null),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (badge != null) Badge(badge, tone = BadgeTone.ACCENT)
            if (forecast) Badge(stringResource(R.string.ds_forecast), tone = BadgeTone.NEUTRAL)
            if (pending) Badge(pendingWord, tone = BadgeTone.SECONDARY)
        }
        if (subtitle != null) Text(subtitle, style = TutoraTheme.type.meta, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** For screens that need the padding the AgendaBlock reserves around itself. */
val AgendaBlockGap = Spacing.s0_5

/**
 * Another appointment from the phone's calendars on the Agenda (spec 012): grey, behind the
 * classes, and not pressable, since Tutora never opens or edits it. [band] is an all-day event: a
 * thin bar at the top of its day.
 */
@Composable
fun BusyBlock(
    title: String,
    modifier: Modifier = Modifier,
    time: String? = null,
    band: Boolean = false,
    height: androidx.compose.ui.unit.Dp = Size.agendaMin,
) {
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(if (band) Radius.sm else Radius.xl)
    androidx.compose.foundation.layout.Column(
        modifier
            .then(if (band) Modifier else Modifier.height(height))
            .clip(shape)
            .background(c.surfaceSunken, shape)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = Spacing.s2, vertical = if (band) Spacing.s0_5 else Spacing.s1),
    ) {
        Text(
            title,
            style = TutoraTheme.type.caption,
            color = c.inkMuted,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        if (!band && time != null) Text(time, style = TutoraTheme.type.caption, color = c.inkMuted, maxLines = 1)
    }
}

/**
 * A class in the Agenda's Semana list (Victor, 4 Nov 2026, after Teams' agenda): its start and end
 * times, a bar in the student's color, who and where, and its value or state. NEXT and LIVE are
 * outlined in the highlight color; MUTED (cancelled, missed) is struck through on a grey bar.
 */
@Composable
fun AgendaCard(
    start: String,
    end: String,
    tag: Tag,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String? = null,
    status: Pair<BadgeTone, String>? = null,
    state: BlockState = BlockState.DEFAULT,
    onClick: (() -> Unit)? = null,
) {
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(Radius.xl)
    val outlined = state == BlockState.NEXT || state == BlockState.LIVE
    val muted = state == BlockState.MUTED
    val forecast = state == BlockState.FORECAST
    val flag =
        when (state) {
            BlockState.NEXT -> stringResource(R.string.ds_next)
            BlockState.LIVE -> stringResource(R.string.ds_live)
            BlockState.FORECAST -> stringResource(R.string.ds_forecast)
            BlockState.TO_CONFIRM -> stringResource(R.string.ds_toConfirm)
            else -> null
        }
    androidx.compose.foundation.layout.Row(
        modifier
            .fillMaxWidth()
            .alpha(
                when (state) {
                    BlockState.FORECAST -> Opacity.disabled
                    BlockState.TO_CONFIRM -> Opacity.pending
                    else -> 1f
                },
            ).height(androidx.compose.foundation.layout.IntrinsicSize.Min)
            .clip(shape)
            .background(c.surface, shape)
            .then(if (outlined) Modifier.border(Size.borderStrong, c.accent, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = Spacing.s3, vertical = Spacing.s2_5),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.End) {
            Text(
                start,
                style = TutoraTheme.type.bodyStrong.copy(fontFeatureSettings = "tnum"),
                color = c.ink,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                end,
                style = TutoraTheme.type.caption.copy(fontFeatureSettings = "tnum"),
                color = c.inkMuted,
                maxLines = 1,
                softWrap = false,
            )
        }
        Box(
            Modifier
                .width(Spacing.s1)
                .fillMaxHeight()
                .clip(RoundedCornerShape(Radius.full))
                .background(if (muted) c.lineStrong else tag.colors().fill),
        )
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            flag?.let { Text(it, style = TutoraTheme.type.badge, color = c.accentInk) }
            Text(
                title,
                style =
                    TutoraTheme.type.rowTitle.copy(
                        textDecoration = if (muted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    ),
                color = if (muted) c.inkMuted else c.ink,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    it,
                    style = TutoraTheme.type.meta,
                    color = c.inkMuted,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
        when {
            status != null -> Badge(status.second, tone = status.first)
            trailing != null ->
                Text(
                    trailing,
                    style = TutoraTheme.type.caption.copy(fontFeatureSettings = "tnum"),
                    color = c.inkMuted,
                    maxLines = 1,
                )
        }
    }
}

/**
 * A day's heading in the Semana list, which stays on top while its classes scroll under it: the
 * date on a small square like the month's tiles (filled for today), the day's name, and an aside.
 */
@Composable
fun DayHeading(
    day: Int,
    title: String,
    modifier: Modifier = Modifier,
    aside: String? = null,
    today: Boolean = false,
    /** The day being read, at the top of the list: filled tile and title in the main color, like the strip. */
    current: Boolean = false,
    /** The short month over the day, like a calendar page ("Dez" over 1; Victor, 4 Nov 2026). */
    month: String? = null,
) {
    val c = TutoraTheme.colors
    val tile = RoundedCornerShape(Radius.sm)
    androidx.compose.foundation.layout.Row(
        modifier
            .fillMaxWidth()
            .background(c.background)
            .semantics(mergeDescendants = true) { heading() }
            .padding(vertical = Spacing.s2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Today is outlined; the day being read is filled, as on the strip (Victor, 4 Nov 2026).
        val ink =
            if (current) {
                c.onBtnPrimary
            } else if (today) {
                c.btnPrimary
            } else {
                c.ink
            }
        androidx.compose.foundation.layout.Column(
            Modifier
                .size(if (month != null) Size.touch else Size.tile)
                .clip(tile)
                .background(if (current) c.btnPrimary else c.surfaceSunken)
                .then(if (today && !current) Modifier.border(Size.borderStrong, c.btnPrimary, tile) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            month?.let { Text(it, style = TutoraTheme.type.badge, color = if (current) c.onBtnPrimary else c.inkMuted, maxLines = 1) }
            Text(
                day.toString(),
                style = TutoraTheme.type.bodyStrong.copy(fontFeatureSettings = "tnum"),
                color =
                    if (current) {
                        c.onBtnPrimary
                    } else if (today) {
                        c.btnPrimary
                    } else {
                        c.ink
                    },
            )
        }
        Text(
            title,
            style = TutoraTheme.type.section,
            color = if (current) c.btnPrimary else c.ink,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        aside?.let { Text(it, style = TutoraTheme.type.caption.copy(fontFeatureSettings = "tnum"), color = c.inkMuted, maxLines = 1) }
    }
}
