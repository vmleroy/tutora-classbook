package app.tutora.ui.earnings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.EarningsStat
import app.tutora.designsystem.components.EarningsSummary
import app.tutora.designsystem.components.EmptyState
import app.tutora.designsystem.components.Icon
import app.tutora.designsystem.components.IconButton
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.Money
import app.tutora.designsystem.components.MoneyTone
import app.tutora.designsystem.components.MonthBar
import app.tutora.designsystem.components.MonthBars
import app.tutora.designsystem.components.MonthPicker
import app.tutora.designsystem.components.PickerMonth
import app.tutora.designsystem.components.ProgressBar
import app.tutora.designsystem.components.ProgressTone
import app.tutora.designsystem.components.SectionHeader
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
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
import app.tutora.ui.reports.EarningsReportSheet
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
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
    var reporting by rememberSaveable { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }
    // Relatório: the earnings report for the period and type on screen (AC-010-13).
    ScreenScaffold(
        stringResource(R.string.tab_earnings),
        "earnings-screen",
        trailing = {
            Button(stringResource(R.string.rep_screen), { reporting = true }, icon = IconName.FILE, size = ControlSize.SM)
        },
    ) {
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
            // Tapping the period opens a picker of months or years (Victor, 29 Oct 2026).
            val range = rangeLabel(period, language)
            val pickLabel = stringResource(R.string.earn_pickPeriod, range)
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.lg))
                    .clickable(role = Role.Button) { picking = true }
                    .semantics(mergeDescendants = true) { contentDescription = pickLabel }
                    .padding(vertical = Spacing.s2),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s1, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(range, Modifier.testTag("earn-range"), style = TutoraTheme.type.titleMd, color = TutoraTheme.colors.ink)
                Icon(IconName.CHEVRON_DOWN, tint = TutoraTheme.colors.ink, size = Size.iconSm)
            }
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
        if (picking) {
            PeriodPickerSheet(period, today, { picking = false }) {
                anchor = it.toString()
                picking = false
            }
        }
        val v = view ?: return@ScreenScaffold
        if (reporting) {
            val typeLabel =
                stringResource(
                    when (filter) {
                        TypeFilter.ALL -> R.string.earn_all
                        TypeFilter.PRIVATE -> R.string.earn_private
                        TypeFilter.JOB -> R.string.earn_job
                    },
                )
            EarningsReportSheet(
                v,
                "${rangeLabel(period, language)} · $typeLabel",
                "${stringResource(R.string.rep_earningsTitle)} - ${rangeLabel(period, language)}",
            ) { reporting = false }
        }
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
            // A white card, as on Hoje (Victor, 29 Oct 2026).
            onCard = true,
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
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                SectionHeader(stringResource(R.string.earn_sources), aside = stringResource(R.string.earn_sourcesHint))
                ListGroup(
                    v.sources.map<app.tutora.domain.Source, @Composable () -> Unit> { src ->
                        {
                            val owes = (balances[src.payer] ?: 0).takeIf { it > 0 }
                            val share = src.cents.toFloat() / total
                            val percent = stringResource(R.string.earn_share, Math.round(share * 100))
                            // One compact row: who, what they still owe, the amount and its share (AC-009-13).
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) { nav.navigate(payerRoute(src.payer)) }
                                    .semantics(mergeDescendants = true) {}
                                    .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
                                verticalArrangement = Arrangement.spacedBy(Spacing.s1_5),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                                ) {
                                    Text(
                                        src.name,
                                        style = TutoraTheme.type.bodyStrong,
                                        color = TutoraTheme.colors.ink,
                                        modifier = Modifier.weight(1f),
                                    )
                                    owes?.let {
                                        Text(
                                            stringResource(R.string.earn_owes, formatMoney(it, language, compact = true)),
                                            style = TutoraTheme.type.meta,
                                            color = TutoraTheme.colors.dangerInk,
                                        )
                                    }
                                    Money(src.cents, tone = MoneyTone.EARNINGS, compact = true)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                                ) {
                                    ProgressBar(
                                        share,
                                        "${src.name}: $percent",
                                        Modifier.weight(1f),
                                        tone = ProgressTone.ACCENT,
                                        thin = true,
                                    )
                                    Text(percent, style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
                                }
                            }
                        }
                    },
                )
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
        // Hoje's white card over the Topo band (Victor, 28 Oct 2026).
        onCard = true,
    )
}

/**
 * Choose the period at once: the months of a year (Mês; Trimestre shows the three months ending
 * at each), or nine years (Ano). Later than now cannot be chosen (Victor, 29 Oct 2026).
 */
@Composable
private fun PeriodPickerSheet(
    period: Period,
    today: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    val language = currentLanguage()
    // Mês and Trimestre page by year; Ano pages nine years at a time, ending at [page].
    var page by rememberSaveable { mutableIntStateOf(period.anchor.year) }
    val years = period.kind == PeriodKind.YEAR
    BottomSheet(stringResource(R.string.earn_pickTitle), onDismiss) {
        if (years) {
            val list = (page - YEARS_SHOWN + 1..page).toList()
            MonthPicker(
                "${list.first()} – ${list.last()}",
                list.map { y -> PickerMonth(y.toString(), "", y.toString(), y <= today.year) },
                list.indexOf(period.anchor.year).takeIf { it >= 0 },
                { onPick(LocalDate(list[it], 1, 1)) },
                stringResource(R.string.earn_prevYears),
                { page -= YEARS_SHOWN },
                stringResource(R.string.earn_nextYears),
                { page += YEARS_SHOWN },
                stringResource(R.string.earn_backNow),
                { onPick(today) },
                nextEnabled = page < today.year,
            )
        } else {
            val months = (1..12).map { LocalDate(page, it, 1) }

            fun label(m: LocalDate): String {
                val name = shortMonth(m, language).replaceFirstChar { it.uppercase() }
                if (period.kind == PeriodKind.MONTH) return name
                val start = m.minus(DatePeriod(months = 2))
                return shortMonth(start, language).replaceFirstChar { it.uppercase() } + "–" + name
            }
            MonthPicker(
                page.toString(),
                months.map { m -> PickerMonth(label(m), "", rangeLabel(Period(period.kind, m), language), m <= today) },
                months.indexOfFirst { it.year == period.anchor.year && it.month == period.anchor.month }.takeIf { it >= 0 },
                { onPick(months[it]) },
                stringResource(R.string.schedule_prevYear),
                { page -= 1 },
                stringResource(R.string.schedule_nextYear),
                { page += 1 },
                stringResource(R.string.earn_backNow),
                { onPick(today) },
                nextEnabled = page < today.year,
            )
        }
    }
}

private const val YEARS_SHOWN = 9
