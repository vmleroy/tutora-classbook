package app.tutora.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing

enum class HeaderVariant { HERO, TAB, PUSH, MODAL }

/** An icon button in a header, or with [text] a small pill that shows its label ("Hoje"). */
data class HeaderAction(
    val icon: IconName,
    val label: String,
    val variant: IconButtonVariant = IconButtonVariant.SUNKEN,
    val text: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * A screen's title bar, always on the background (spec 016: no colored band): HERO is Hoje's
 * date over the greeting, TAB a tab title, PUSH a pushed screen with back, MODAL a sheet.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    variant: HeaderVariant = HeaderVariant.TAB,
    eyebrow: String? = null,
    actions: List<HeaderAction> = emptyList(),
    onBack: (() -> Unit)? = null,
    /** Makes the title a button with a ▾, read as [titleLabel] (the Agenda's month and year). */
    onTitle: (() -> Unit)? = null,
    titleLabel: String? = null,
) {
    val c = TutoraTheme.colors
    val big = variant == HeaderVariant.TAB || variant == HeaderVariant.HERO
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("screen-header")
                .padding(horizontal = Spacing.s5, vertical = Spacing.s3),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(
                IconName.CHEVRON_LEFT,
                stringResource(R.string.ds_back),
                onBack,
            )
        }
        Column(
            Modifier
                .weight(1f)
                .then(
                    if (onTitle != null) {
                        Modifier
                            .testTag("header-title")
                            .sizeIn(minHeight = Size.touch)
                            .clip(RoundedCornerShape(Radius.lg))
                            .clickable(role = Role.Button, onClick = onTitle)
                            .semantics(mergeDescendants = true) { contentDescription = titleLabel ?: title }
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (eyebrow != null) Text(eyebrow, style = TutoraTheme.type.caption, color = c.inkMuted)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                Text(
                    title,
                    // A tappable title (the Agenda's month and year) shares its row with more actions.
                    style = if (big && onTitle == null) TutoraTheme.type.titleXl else TutoraTheme.type.titleLg,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).semantics { heading() },
                )
                if (onTitle != null) Icon(IconName.CHEVRON_DOWN, tint = c.ink)
            }
        }
        actions.forEach {
            if (it.text) {
                Box(
                    Modifier
                        .sizeIn(minWidth = Size.touch, minHeight = Size.touch)
                        .clip(RoundedCornerShape(Radius.full))
                        .background(c.surfaceSunken)
                        .clickable(role = Role.Button, onClick = it.onClick)
                        .padding(horizontal = Spacing.s4),
                    contentAlignment = Alignment.Center,
                ) { Text(it.label, style = TutoraTheme.type.button, color = c.ink, maxLines = 1) }
            } else {
                IconButton(it.icon, it.label, it.onClick, variant = it.variant)
            }
        }
    }
}

data class TabItem(
    val key: String,
    val label: String,
    val icon: IconName,
)

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
                        .semantics { contentDescription = tab.label },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.s1),
            ) {
                Box(
                    Modifier
                        .testTag("tab-pill-${tab.key}")
                        .clip(RoundedCornerShape(Radius.full))
                        .background(if (on) c.tabActiveSoft else Color.Transparent)
                        .padding(horizontal = Spacing.s4, vertical = Spacing.s1),
                ) { Icon(tab.icon, tint = if (on) c.tabActive else c.inkMuted) }
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
)

/** Seven days; the selected one is filled, today's number is primary, a dot marks days with lessons. */
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
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
    ) {
        days.forEachIndexed { i, d ->
            val on = i == selected
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(Size.weekDay)
                        .clip(RoundedCornerShape(Radius.x2xl))
                        .background(if (on) c.btnPrimary else c.surface)
                        .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) })
                        .semantics { contentDescription = d.a11y ?: "${d.weekday} ${d.day}" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.s0_5, Alignment.CenterVertically),
            ) {
                Text(d.weekday, style = TutoraTheme.type.badge, color = if (on) c.onPrimaryMuted else c.inkMuted)
                Text(
                    d.day.toString(),
                    style = TutoraTheme.type.section.copy(fontFeatureSettings = "tnum"),
                    color =
                        if (on) {
                            c.onBtnPrimary
                        } else if (i == today) {
                            c.primary
                        } else {
                            c.ink
                        },
                )
                Box(
                    Modifier
                        .size(Size.weekDot)
                        .clip(CircleShape)
                        .background(
                            if (d.dot == null) {
                                Color.Transparent
                            } else if (on) {
                                c.accent
                            } else {
                                d.dot.colors().fill
                            },
                        ),
                )
            }
        }
    }
}

/**
 * MUTED is a cancelled or missed class: neutral fill and the title struck through. LIVE is the
 * class running now: accent outline and "Ao vivo", like NEXT with its own word.
 */
enum class BlockState { DEFAULT, DONE, NEXT, MUTED, LIVE }

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
    val ink = if (muted) c.inkMuted else t.ink
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .height(maxOf(height, Size.agendaMin))
                .clip(shape)
                .background(if (muted) c.surfaceSunken else t.soft)
                .then(if (badge != null) Modifier.border(Size.borderStrong, c.accent, shape) else Modifier)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(role = Role.Button, onClick = onClick).semantics {
                            contentDescription =
                                listOfNotNull(title, subtitle, badge).joinToString(", ")
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
        }
        if (subtitle != null) Text(subtitle, style = TutoraTheme.type.meta, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** For screens that need the padding the AgendaBlock reserves around itself. */
val AgendaBlockGap = Spacing.s0_5
