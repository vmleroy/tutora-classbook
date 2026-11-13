package app.tutora.ui.earnings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.Choice
import app.tutora.designsystem.components.ChoiceField
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconButton
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.MonthBar
import app.tutora.designsystem.components.MonthBars
import app.tutora.designsystem.components.PeriodField
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Bar
import app.tutora.domain.EvolutionFilter
import app.tutora.domain.MonthDetail
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.PeriodKind
import app.tutora.domain.PlaceKind
import app.tutora.domain.TypeFilter
import app.tutora.domain.compareBars
import app.tutora.domain.dateOf
import app.tutora.domain.evolutionPeriodOk
import app.tutora.domain.instantOf
import app.tutora.domain.monthOf
import app.tutora.state.LocalServices
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.countText
import app.tutora.ui.durationText
import app.tutora.ui.monthYear
import app.tutora.ui.placeName
import app.tutora.ui.reports.pattern
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Evolução's ranges: the last 6 or 12 months, a calendar year, or any gap of dates (AC-014-31, AC-019-1). */
enum class EvolutionRange { SIX, TWELVE, YEAR, CUSTOM }

private const val MONTHS_IN_YEAR = 12
private const val SHORT_RANGE = 6

/**
 * Evolução: earned and received month by month for a range, following the type filter; tapping
 * two months compares them, the later against the earlier (AC-014-31).
 */
@Composable
fun EvolutionScreen(nav: NavHostController) {
    val services = LocalServices.current
    val language = currentLanguage()
    val zone = services.zone
    val today = remember { dateOf(instantOf(services.now()), zone) }
    var range by rememberSaveable { mutableStateOf(EvolutionRange.SIX) }
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    var filter by rememberSaveable { mutableStateOf(TypeFilter.ALL) }
    // Any number of payers ("student:<id>") and place kinds; none is everyone (AC-019-8, 12).
    var payerKeys by rememberSaveable { mutableStateOf(listOf<String>()) }
    var kindKeys by rememberSaveable { mutableStateOf(listOf<String>()) }
    // Personalizado's De and Até, kept through a turn of the phone (AC-019-6).
    var customFrom by rememberSaveable { mutableStateOf(today.toString()) }
    var customTo by rememberSaveable { mutableStateOf(today.toString()) }
    var tooLong by rememberSaveable { mutableStateOf(false) }
    var picked by rememberSaveable { mutableStateOf(listOf<Int>()) }
    val payers = rememberPayers()
    val chosenPayers =
        payerKeys
            .mapNotNull { k ->
                PayerKind.entries.firstOrNull { k.startsWith(it.key + ":") }?.let { PayerRef(it, k.substringAfter(":")) }
            }.toSet()
    val chosenKinds = kindKeys.mapNotNull { k -> PlaceKind.entries.firstOrNull { it.name == k } }.toSet()
    val endOfMonth = monthOf(today).plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
    val (from, to) =
        when (range) {
            EvolutionRange.SIX -> monthOf(today).minus(DatePeriod(months = SHORT_RANGE - 1)) to endOfMonth
            EvolutionRange.TWELVE -> monthOf(today).minus(DatePeriod(months = MONTHS_IN_YEAR - 1)) to endOfMonth
            EvolutionRange.YEAR -> LocalDate(year, 1, 1) to LocalDate(year, MONTHS_IN_YEAR, 31)
            EvolutionRange.CUSTOM -> LocalDate.parse(customFrom) to LocalDate.parse(customTo)
        }

    // A pick that breaks the rules keeps the last valid period (AC-019-2).
    fun pick(
        newFrom: LocalDate,
        newTo: LocalDate,
    ) {
        val start = minOf(newFrom, today)
        val end = maxOf(minOf(newTo, today), start)
        tooLong = !evolutionPeriodOk(start, end, today)
        if (tooLong) return
        customFrom = start.toString()
        customTo = end.toString()
        range = EvolutionRange.CUSTOM
        picked = emptyList()
    }
    val months by remember(from, to, filter, chosenPayers, chosenKinds) {
        services.earningsRepo.evolution(from, to, EvolutionFilter(filter, chosenPayers, chosenKinds), today, zone)
    }.collectAsState(initial = emptyList())
    val bars = months.map { it.bar }
    ScreenScaffold(
        stringResource(R.string.earn_evolution),
        "evolution-screen",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
    ) {
        // The period as a filter field, like Tipo under it: quick periods over De and Até; a date picked
        // by hand is a period of its own and lights none (AC-019-1, AC-020-31).
        PeriodField(
            stringResource(R.string.rep_period),
            listOf(
                EvolutionRange.SIX to stringResource(R.string.evo_six),
                EvolutionRange.TWELVE to stringResource(R.string.evo_twelve),
                EvolutionRange.YEAR to stringResource(R.string.evo_year),
            ),
            range.takeIf { it != EvolutionRange.CUSTOM },
            {
                range = it
                tooLong = false
                picked = emptyList()
            },
            from,
            pattern("dd/MM/yyyy", language, from),
            { pick(it, maxOf(it, to)) },
            to,
            pattern("dd/MM/yyyy", language, to),
            { pick(minOf(from, it), it) },
            stringResource(R.string.rep_from),
            stringResource(R.string.rep_to),
            tag = "evo",
        )
        if (tooLong) {
            Text(
                stringResource(R.string.evo_tooLong),
                Modifier.testTag("evo-too-long"),
                style = TutoraTheme.type.meta,
                color = TutoraTheme.colors.dangerInk,
            )
        }
        // A year goes back and forth with ‹ ›, never past this one.
        if (range == EvolutionRange.YEAR) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(IconName.CHEVRON_LEFT, stringResource(R.string.earn_previous), {
                    year--
                    picked = emptyList()
                }, variant = IconButtonVariant.PLAIN)
                Text(
                    year.toString(),
                    style = TutoraTheme.type.titleMd,
                    color = TutoraTheme.colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(IconName.CHEVRON_RIGHT, stringResource(R.string.earn_next), {
                    year++
                    picked = emptyList()
                }, variant = IconButtonVariant.PLAIN, enabled = year < today.year)
            }
        }
        // Tipo, Quem and Local, one dropdown each, combine (AC-014-35, AC-019-7 to 9).
        TypeFilterField(rememberTypeFilters(), filter, {
            filter = it
            picked = emptyList()
        })
        // Quem: search as you type and check any number (AC-019-12).
        ChoiceField(
            stringResource(R.string.evo_who),
            payers.map { (p, name) -> Choice("${p.kind.key}:${p.id}", name) },
            payerKeys.toSet(),
            {
                payerKeys = it.toList()
                picked = emptyList()
            },
            Modifier.testTag("evo-who"),
            multiple = true,
            allLabel = stringResource(R.string.evo_everyone),
            searchLabel = stringResource(R.string.evo_search),
            icon = IconName.USERS,
        )
        PlaceKindField(chosenKinds, {
            kindKeys = it.map { k -> k.name }
            picked = emptyList()
        })
        // Over more than one year a bar names its year too ("out 25").
        val years = bars.map { it.first.year }.distinct().size > 1
        val labels = bars.map { b -> barLabel(b, PeriodKind.YEAR, language).let { if (years) "$it ${b.first.year % 100}" else it } }
        MonthBars(
            bars.mapIndexed { i, b -> MonthBar(labels[i], b.earned, b.received) },
            barsDescription(bars, labels, language),
            Modifier.testTag("evolution-bars"),
            selected = picked.toSet(),
            // A third month replaces the older choice, so two are compared at most.
            onSelect = { i -> picked = if (i in picked) picked - i else (picked + i).takeLast(2) },
            pairs = stringResource(R.string.evo_earned) to stringResource(R.string.evo_received),
        )
        val chosen = picked.mapNotNull { bars.getOrNull(it) }
        when (chosen.size) {
            0 -> Text(stringResource(R.string.evo_hint), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
            1 -> Card(Modifier.testTag("evolution-month")) { MonthLine(chosen[0]) }
            else -> Comparison(chosen[0], chosen[1])
        }
        // Each month, newest first: totals, classes and hours, and the change from the month before (AC-014-34).
        if (months.isNotEmpty()) {
            ListGroup(
                months.reversed().map<MonthDetail, @Composable () -> Unit> { m -> { MonthRow(m) } },
                Modifier.testTag("evolution-months"),
            )
        }
    }
}

/** Everyone who can pay, by name: students, groups and places (AC-014-35). */
@Composable
private fun rememberPayers(): List<Pair<PayerRef, String>> {
    val services = LocalServices.current
    val students by remember { services.catalogRepo.students() }.collectAsState(initial = emptyList())
    val groups by remember { services.catalogRepo.groups() }.collectAsState(initial = emptyList())
    val places by remember { services.catalogRepo.places() }.collectAsState(initial = emptyList())
    return students.map { PayerRef(PayerKind.STUDENT, it.student.id) to it.student.name } +
        groups.map { PayerRef(PayerKind.GROUP, it.id) to it.name } +
        places.map { PayerRef(PayerKind.PLACE, it.place.id) to placeName(it.place) }
}

/** "Outubro 2026", its totals, "6 aulas · 07:30", and "+R$ 400,00 · +14%" against the month before. */
@Composable
private fun MonthRow(m: MonthDetail) {
    val language = currentLanguage()
    val c = TutoraTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
        verticalArrangement = Arrangement.spacedBy(Spacing.s0_5),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(monthYear(m.bar.first, language), style = TutoraTheme.type.bodyStrong, color = c.ink, modifier = Modifier.weight(1f))
            m.change?.let { ch ->
                Text(
                    changeText(ch),
                    style = TutoraTheme.type.caption.copy(fontFeatureSettings = "tnum"),
                    color =
                        if (ch.difference < 0) {
                            c.dangerInk
                        } else if (ch.difference > 0) {
                            c.successInk
                        } else {
                            c.inkMuted
                        },
                )
            }
        }
        Text(
            stringResource(
                R.string.evo_monthTotals,
                formatMoney(m.bar.earned, language),
                formatMoney(m.bar.received, language),
                formatMoney(m.toReceive, language),
            ),
            style = TutoraTheme.type.meta,
            color = c.inkMuted,
        )
        Text(
            stringResource(
                R.string.evo_monthClasses,
                countText(R.string.schedule_count_zero, R.plurals.schedule_count, m.classes),
                durationText(m.minutes),
            ),
            style = TutoraTheme.type.meta,
            color = c.inkMuted,
        )
    }
}

/** "+R$ 400,00 · +14%", "−R$ 100,00 · −5%", or just the reais after a month of R$ 0. */
@Composable
private fun changeText(c: app.tutora.domain.Comparison): String {
    val language = currentLanguage()

    fun sign(v: Long) =
        if (v > 0) {
            "+"
        } else if (v < 0) {
            "−"
        } else {
            ""
        }
    val money = sign(c.difference) + formatMoney(kotlin.math.abs(c.difference), language)
    return c.percent?.let { stringResource(R.string.evo_change, money, sign(it) + kotlin.math.abs(it)) } ?: money
}

@Composable
private fun MonthLine(b: Bar) {
    val language = currentLanguage()
    Text(monthYear(b.first, language), style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.ink)
    Text(
        stringResource(R.string.evo_monthLine, formatMoney(b.earned, language), formatMoney(b.received, language)),
        style = TutoraTheme.type.meta,
        color = TutoraTheme.colors.inkMuted,
    )
}

/** Both months and the difference in reais and percent: "+R$ 400,00 (+14%)" (AC-014-31). */
@Composable
private fun Comparison(
    a: Bar,
    b: Bar,
) {
    val language = currentLanguage()
    val c = compareBars(a, b)
    val sign =
        if (c.difference > 0) {
            "+"
        } else if (c.difference < 0) {
            "−"
        } else {
            ""
        }
    val money = sign + formatMoney(kotlin.math.abs(c.difference), language)
    val diff =
        c.percent?.let {
            stringResource(
                R.string.evo_diffPercent,
                money,
                (
                    if (it > 0) {
                        "+"
                    } else if (it < 0) {
                        "−"
                    } else {
                        ""
                    }
                ) + kotlin.math.abs(it),
            )
        }
            ?: money
    Card(Modifier.testTag("evolution-compare")) {
        MonthLine(c.later)
        MonthLine(c.earlier)
        Text(
            stringResource(R.string.evo_difference, diff),
            style = TutoraTheme.type.bodyStrong,
            color = if (c.difference < 0) TutoraTheme.colors.dangerInk else TutoraTheme.colors.successInk,
        )
    }
}
