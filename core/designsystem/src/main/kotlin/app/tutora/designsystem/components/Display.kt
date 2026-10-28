package app.tutora.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Cents

enum class BadgeTone { NEUTRAL, SUCCESS, DANGER, ACCENT, SECONDARY, VIOLET }

/** A short status pill: Pago, Devendo, Padrão. */
@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.NEUTRAL,
    icon: IconName? = null,
) {
    val c = TutoraTheme.colors
    val (bg, fg) =
        when (tone) {
            BadgeTone.NEUTRAL -> c.surfaceSunken to c.inkMuted
            BadgeTone.SUCCESS -> c.successSoft to c.successInk
            BadgeTone.DANGER -> c.dangerSoft to c.dangerInk
            BadgeTone.ACCENT -> c.accentSoft to c.accentInk
            BadgeTone.SECONDARY -> c.secondarySoft to c.secondaryInk
            BadgeTone.VIOLET -> c.tagVioletSoft to c.tagVioletInk
        }
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(Radius.sm))
                .background(bg)
                .padding(horizontal = Spacing.s2, vertical = Spacing.s0_5),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, tint = fg, size = Size.iconSm)
        Text(text, style = TutoraTheme.type.badge, color = fg, maxLines = 1)
    }
}

enum class AvatarSize { SM, MD, LG }

/** A person's initials on their tag color; decorative, the name is read beside it. */
@Composable
fun Avatar(
    initials: String,
    tag: Tag,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.MD,
) {
    val t = tag.colors()
    val (box, style) =
        when (size) {
            AvatarSize.SM -> Size.avatarSm to TutoraTheme.type.caption
            AvatarSize.MD -> Size.avatarMd to TutoraTheme.type.body
            AvatarSize.LG -> Size.avatarLg to TutoraTheme.type.section
        }
    Box(
        modifier =
            modifier
                .size(box)
                .clip(CircleShape)
                .background(t.soft),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = style.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), color = t.ink)
    }
}

/** A student's or group's color, as a dot. */
@Composable
fun TagDot(
    tag: Tag,
    modifier: Modifier = Modifier,
    small: Boolean = false,
) {
    Box(
        modifier
            .size(if (small) Size.dotSm else Size.dot)
            .clip(CircleShape)
            .background(tag.colors().fill),
    )
}

enum class MoneySize { DISPLAY, XL, LG, MD, SM }

enum class MoneyTone { DEFAULT, EARNINGS, OWED, PAID, ON_HEADER }

/** Integer cents of reais in the app language, always in tabular figures (AC-002-13). */
@Composable
fun Money(
    cents: Cents,
    modifier: Modifier = Modifier,
    size: MoneySize = MoneySize.MD,
    tone: MoneyTone = MoneyTone.DEFAULT,
    compact: Boolean = false,
    showSign: Boolean = false,
) {
    val c = TutoraTheme.colors
    val type = TutoraTheme.type
    val style: TextStyle =
        when (size) {
            MoneySize.DISPLAY -> type.moneyDisplay
            MoneySize.XL -> type.moneyXl
            MoneySize.LG -> type.moneyLg
            MoneySize.MD -> type.moneyMd
            MoneySize.SM -> type.rowTitle.copy(fontFeatureSettings = "tnum")
        }
    val color =
        when (tone) {
            MoneyTone.DEFAULT -> c.ink
            MoneyTone.EARNINGS -> c.earnings
            MoneyTone.OWED -> c.dangerInk
            MoneyTone.PAID -> c.successInk
            MoneyTone.ON_HEADER -> c.earningsOnHeader
        }
    val text = formatMoney(cents, currentLanguage(), compact)
    Text(if (showSign && cents > 0) "+ $text" else text, style = style, color = color, modifier = modifier, maxLines = 1)
}

enum class ProgressTone { ACCENT, SUCCESS, PRIMARY, SECONDARY }

enum class ProgressTrack { ACCENT, DANGER, SUNKEN }

/** A share of a whole (earned of expected); [value] is 0..1. */
@Composable
fun ProgressBar(
    value: Float,
    label: String,
    modifier: Modifier = Modifier,
    tone: ProgressTone = ProgressTone.ACCENT,
    track: ProgressTrack = ProgressTrack.SUNKEN,
    thin: Boolean = false,
) {
    val c = TutoraTheme.colors
    val fill =
        when (tone) {
            ProgressTone.ACCENT -> c.accent
            ProgressTone.SUCCESS -> c.success
            ProgressTone.PRIMARY -> c.primary
            ProgressTone.SECONDARY -> c.secondary
        }
    val ground =
        when (track) {
            ProgressTrack.ACCENT -> c.accentSoft
            ProgressTrack.DANGER -> c.dangerSoft
            ProgressTrack.SUNKEN -> c.surfaceSunken
        }
    val v = value.coerceIn(0f, 1f)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(if (thin) Size.progressThin else Size.progress)
                .clip(CircleShape)
                .background(ground)
                .semantics {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(v, 0f..1f)
                },
    ) {
        Box(
            Modifier
                .fillMaxWidth(v)
                .height(if (thin) Size.progressThin else Size.progress)
                .clip(CircleShape)
                .background(fill),
        )
    }
}

enum class CardElevation { FLAT, RAISED, OUTLINE }

/** A white block that groups one thing; one xl figure per card at most. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    elevation: CardElevation = CardElevation.FLAT,
    label: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(Radius.xl)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (elevation == CardElevation.RAISED) Modifier.shadow(Spacing.s1, shape) else Modifier)
                .clip(shape)
                .background(c.surface, shape)
                .then(if (elevation == CardElevation.OUTLINE) Modifier.border(Size.border, c.line, shape) else Modifier)
                .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
                .padding(Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        content = content,
    )
}

data class EarningsStat(
    val label: String,
    val cents: Cents,
    val tone: MoneyTone = MoneyTone.DEFAULT,
)

/** Earned vs expected with a bar and up to three compact stats (Hoje, Ganhos). */
@Composable
fun EarningsSummary(
    title: String,
    earnedCents: Cents,
    modifier: Modifier = Modifier,
    expectedCents: Cents? = null,
    expectedLabel: String? = null,
    stats: List<EarningsStat> = emptyList(),
    /** Ganhos: the share received, drawn success on danger, read aloud as [receivedLabel]. */
    receivedShare: Float? = null,
    receivedLabel: String = "",
    /** A line under the stats ("comissão dos locais: R$ 30,00"). */
    footnote: String? = null,
) {
    val c = TutoraTheme.colors
    Card(modifier, label = title) {
        Text(title, style = TutoraTheme.type.caption, color = c.inkMuted)
        Money(earnedCents, size = MoneySize.XL, tone = MoneyTone.EARNINGS)
        if (expectedCents != null && expectedCents > 0) {
            if (expectedLabel != null) Text(expectedLabel, style = TutoraTheme.type.meta, color = c.inkMuted)
            ProgressBar(earnedCents.toFloat() / expectedCents, expectedLabel ?: title)
        }
        if (receivedShare != null) {
            ProgressBar(receivedShare, receivedLabel, tone = ProgressTone.SUCCESS, track = ProgressTrack.DANGER)
        }
        if (stats.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                stats.forEach { s ->
                    Column {
                        Text(s.label, style = TutoraTheme.type.meta, color = c.inkMuted)
                        Money(s.cents, size = MoneySize.MD, tone = s.tone, compact = true)
                    }
                }
            }
        }
        if (footnote != null) Text(footnote, style = TutoraTheme.type.meta, color = c.inkMuted)
    }
}

/** One month in [MonthBars]: its label, the bar's total and the part filled in. */
data class MonthBar(
    val label: String,
    val total: Cents,
    val filled: Cents,
)

/**
 * A few months side by side as bars (the 6-month trend on Ganhos): each bar's height follows its
 * total against the largest, with the filled part (received) drawn inside it. The whole chart is
 * read aloud as [description].
 */
@Composable
fun MonthBars(
    bars: List<MonthBar>,
    description: String,
    modifier: Modifier = Modifier,
) {
    val c = TutoraTheme.colors
    val max = bars.maxOfOrNull { it.total }?.takeIf { it > 0 } ?: 1L
    val chart = Size.touch * 2
    Row(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { b ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.s1),
            ) {
                Box(Modifier.fillMaxWidth().height(chart), contentAlignment = Alignment.BottomCenter) {
                    val share = b.total.toFloat() / max
                    val part = if (b.total > 0) (b.filled.toFloat() / b.total).coerceIn(0f, 1f) else 0f
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(chart * share)
                            .clip(RoundedCornerShape(Radius.xs))
                            .background(c.accentSoft),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(Modifier.fillMaxWidth().height(chart * share * part).background(c.earnings))
                    }
                }
                Text(b.label, style = TutoraTheme.type.caption, color = c.inkMuted, maxLines = 1)
            }
        }
    }
}
