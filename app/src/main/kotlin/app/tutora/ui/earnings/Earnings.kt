package app.tutora.ui.earnings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.EarningsStat
import app.tutora.designsystem.components.EarningsSummary
import app.tutora.designsystem.components.EmptyState
import app.tutora.designsystem.components.IconButton
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.Money
import app.tutora.designsystem.components.MoneyTone
import app.tutora.designsystem.components.MonthBar
import app.tutora.designsystem.components.MonthBars
import app.tutora.designsystem.components.ProgressBar
import app.tutora.designsystem.components.ProgressTone
import app.tutora.designsystem.components.SectionHeader
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Language
import app.tutora.domain.Lesson
import app.tutora.domain.MonthPoint
import app.tutora.domain.Period
import app.tutora.domain.PeriodKind
import app.tutora.domain.TypeFilter
import app.tutora.domain.keptValue
import app.tutora.domain.localOf
import app.tutora.domain.weekStart
import app.tutora.state.LocalServices
import app.tutora.ui.EarningsGraph
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.localeOf
import app.tutora.ui.payments.payerRoute
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter

/** "out" / "Oct". */
private fun shortMonth(
    month: LocalDate,
    language: Language,
): String = DateTimeFormatter.ofPattern("LLL", localeOf(language)).format(month.toJavaLocalDate()).removeSuffix(".")

/** "Outubro 2026", "Ago – Out 2026" or "2026" (AC-009-9). */
private fun rangeLabel(
    p: Period,
    language: Language,
): String {
    val locale = localeOf(language)

    fun cap(s: String) = s.replaceFirstChar { it.uppercase() }
    return when (p.kind) {
        PeriodKind.MONTH -> cap(DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(p.firstMonth.toJavaLocalDate()))
        PeriodKind.QUARTER ->
            cap(shortMonth(p.firstMonth, language)) + " – " + cap(shortMonth(p.lastMonth, language)) + " " + p.lastMonth.year
        PeriodKind.YEAR -> p.anchor.year.toString()
    }
}

/**
 * Ganhos: period and type, what was earned, received and is still to receive, the last six
 * months, and where it came from (AC-009-9 to 14).
 */
@Composable
fun EarningsScreen(nav: NavHostController) {
    val services = LocalServices.current
    val language = currentLanguage()
    val zone = services.zone
    val today = remember { localOf(services.now(), zone).date }
    var kind by rememberSaveable { mutableStateOf(PeriodKind.MONTH) }
    var anchor by rememberSaveable { mutableStateOf(today.toString()) }
    var filter by rememberSaveable { mutableStateOf(TypeFilter.ALL) }
    val period = Period(kind, LocalDate.parse(anchor))
    val view by remember(period, filter) { services.earningsRepo.view(period, filter, today, zone) }.collectAsState(initial = null)
    val balances by remember { services.paymentRepo.balances() }.collectAsState(initial = emptyMap())
    ScreenScaffold(stringResource(R.string.tab_earnings), "earnings-screen") {
        SegmentedControl(
            listOf(
                PeriodKind.MONTH to stringResource(R.string.earn_month),
                PeriodKind.QUARTER to stringResource(R.string.earn_quarter),
                PeriodKind.YEAR to stringResource(R.string.earn_year),
            ),
            kind,
            {
                kind = it
                anchor = today.toString()
            },
            stringResource(R.string.earn_period),
        )
        // ‹ and › move by the period; › stops at the one holding today (AC-009-9).
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            IconButton(IconName.CHEVRON_LEFT, stringResource(R.string.earn_previous), { anchor = period.previous().anchor.toString() })
            Text(
                rangeLabel(period, language),
                Modifier.weight(1f).testTag("earn-range"),
                style = TutoraTheme.type.titleMd,
                color = TutoraTheme.colors.ink,
                textAlign = TextAlign.Center,
            )
            IconButton(
                IconName.CHEVRON_RIGHT,
                stringResource(R.string.earn_next),
                { anchor = period.next().anchor.toString() },
                variant = IconButtonVariant.SUNKEN,
                enabled = !period.isCurrent(today),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            listOf(
                TypeFilter.ALL to R.string.earn_all,
                TypeFilter.PRIVATE to R.string.earn_private,
                TypeFilter.JOB to R.string.earn_job,
            ).forEach { (f, label) -> Chip(stringResource(label), filter == f, { filter = f }) }
        }
        val v = view ?: return@ScreenScaffold
        val s = v.summary
        if (s.earned == 0L && v.sources.isEmpty()) {
            EmptyState(stringResource(R.string.earn_empty), icon = IconName.WALLET)
            return@ScreenScaffold
        }
        // Earned, received and to receive, with what commission and bonuses took (AC-009-11).
        EarningsSummary(
            stringResource(R.string.earn_earned),
            s.earned,
            Modifier.testTag("earn-summary"),
            stats =
                listOf(
                    EarningsStat(stringResource(R.string.earn_received), s.received, MoneyTone.PAID),
                    EarningsStat(stringResource(R.string.pay_toReceive), s.toReceive, MoneyTone.OWED),
                ),
            receivedShare = if (s.earned > 0) s.received.toFloat() / s.earned else 0f,
            receivedLabel = stringResource(R.string.earn_received),
            footnote =
                listOfNotNull(
                    s.commission.takeIf { it > 0 }?.let { stringResource(R.string.earn_commission, formatMoney(it, language)) },
                    s.bonus.takeIf { it > 0 }?.let { stringResource(R.string.earn_bonus, formatMoney(it, language)) },
                ).joinToString(" · ").ifEmpty { null },
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            SectionHeader(stringResource(R.string.earn_trend))
            MonthBars(
                v.trend.map { MonthBar(shortMonth(it.month, language), it.earned, it.received) },
                trendDescription(v.trend, language),
                Modifier.testTag("earn-trend"),
            )
        }
        // De onde veio: largest first, each on its own card with its share of the total, opening
        // its detail (AC-009-13).
        if (v.sources.isNotEmpty()) {
            val total = v.sources.sumOf { it.cents }.coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                SectionHeader(stringResource(R.string.earn_sources), aside = stringResource(R.string.earn_sourcesHint))
                v.sources.forEach { src ->
                    val owes = (balances[src.payer] ?: 0).takeIf { it > 0 }
                    val share = src.cents.toFloat() / total
                    val percent = stringResource(R.string.earn_share, Math.round(share * 100))
                    Card(Modifier.clickable(role = Role.Button) { nav.navigate(payerRoute(src.payer)) }, label = src.name) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                            Column(Modifier.weight(1f)) {
                                Text(src.name, style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.ink)
                                owes?.let {
                                    Text(
                                        stringResource(R.string.earn_owes, formatMoney(it, language, compact = true)),
                                        style = TutoraTheme.type.meta,
                                        color = TutoraTheme.colors.dangerInk,
                                    )
                                }
                            }
                            Money(src.cents, tone = MoneyTone.EARNINGS)
                        }
                        ProgressBar(share, "${src.name}: $percent", tone = ProgressTone.ACCENT)
                        Text(percent, style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
                    }
                }
            }
        }
    }
}

/** "mai: R$ 3.000 ganhos, R$ 2.400 recebidos; …" for screen readers. */
@Composable
private fun trendDescription(
    points: List<MonthPoint>,
    language: Language,
): String {
    val parts =
        points.map {
            stringResource(
                R.string.earn_trendMonth,
                shortMonth(it.month, language),
                formatMoney(it.earned, language, compact = true),
                formatMoney(it.received, language, compact = true),
            )
        }
    return stringResource(R.string.earn_trend) + ": " + parts.joinToString("; ")
}

/**
 * Hoje's earnings card: Ganho hoje against what today's classes are expected to bring, then the
 * week, the month and what everyone owes; it opens Ganhos (AC-009-8).
 */
@Composable
fun TodayEarnings(
    nav: NavHostController,
    lessons: List<Lesson>,
    today: LocalDate,
) {
    val services = LocalServices.current
    val language = currentLanguage()
    val zone = services.zone
    val earnedToday by remember(today) { services.earningsRepo.earnedBetween(today, today, zone) }.collectAsState(initial = 0L)
    val week by remember(today) { services.earningsRepo.earnedBetween(weekStart(today), today, zone) }.collectAsState(initial = 0L)
    val month by remember(today) {
        services.earningsRepo.view(Period(PeriodKind.MONTH, today), TypeFilter.ALL, today, zone)
    }.collectAsState(initial = null)
    val balances by remember { services.paymentRepo.balances() }.collectAsState(initial = emptyMap())
    val expected = lessons.mapNotNull { it.keptValue() }.sum()
    EarningsSummary(
        stringResource(R.string.earn_today),
        earnedToday,
        Modifier.testTag("today-earnings").clickable(role = Role.Button) { nav.navigate(EarningsGraph) },
        expectedCents = expected,
        expectedLabel = stringResource(R.string.earn_expected, formatMoney(expected, language)),
        stats =
            listOf(
                EarningsStat(stringResource(R.string.earn_week), week, MoneyTone.EARNINGS),
                EarningsStat(
                    DateTimeFormatter
                        .ofPattern(
                            "LLLL",
                            localeOf(language),
                        ).format(today.toJavaLocalDate())
                        .replaceFirstChar { it.uppercase() },
                    month?.summary?.earned ?: 0,
                    MoneyTone.EARNINGS,
                ),
                EarningsStat(stringResource(R.string.earn_studentsOwe), balances.values.filter { it > 0 }.sum(), MoneyTone.OWED),
            ),
    )
}
