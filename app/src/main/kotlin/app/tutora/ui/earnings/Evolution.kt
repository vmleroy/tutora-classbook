package app.tutora.ui.earnings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconButton
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.MonthBar
import app.tutora.designsystem.components.MonthBars
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Bar
import app.tutora.domain.PeriodKind
import app.tutora.domain.TypeFilter
import app.tutora.domain.compareBars
import app.tutora.domain.dateOf
import app.tutora.domain.instantOf
import app.tutora.domain.monthOf
import app.tutora.state.LocalServices
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.monthYear
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** Evolução's ranges: the last 6 or 12 months, or a calendar year (AC-014-31). */
enum class EvolutionRange { SIX, TWELVE, YEAR }

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
    var picked by rememberSaveable { mutableStateOf(listOf<Int>()) }
    val (first, count) =
        when (range) {
            EvolutionRange.SIX -> monthOf(today).minus(DatePeriod(months = SHORT_RANGE - 1)) to SHORT_RANGE
            EvolutionRange.TWELVE -> monthOf(today).minus(DatePeriod(months = MONTHS_IN_YEAR - 1)) to MONTHS_IN_YEAR
            EvolutionRange.YEAR -> LocalDate(year, 1, 1) to MONTHS_IN_YEAR
        }
    val bars by remember(first, count, filter) {
        services.earningsRepo.evolution(first, count, filter, today, zone)
    }.collectAsState(initial = emptyList())
    ScreenScaffold(
        stringResource(R.string.earn_evolution),
        "evolution-screen",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            listOf(
                EvolutionRange.SIX to R.string.evo_six,
                EvolutionRange.TWELVE to R.string.evo_twelve,
                EvolutionRange.YEAR to R.string.evo_year,
            ).forEach { (r, label) ->
                Chip(stringResource(label), range == r, {
                    range = r
                    picked = emptyList()
                })
            }
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            listOf(
                TypeFilter.ALL to R.string.earn_all,
                TypeFilter.PRIVATE to R.string.earn_private,
                TypeFilter.JOB to R.string.earn_job,
            ).forEach { (f, label) -> Chip(stringResource(label), filter == f, { filter = f }) }
        }
        val labels = bars.map { barLabel(it, PeriodKind.YEAR, language) }
        MonthBars(
            bars.mapIndexed { i, b -> MonthBar(labels[i], b.earned, b.received) },
            barsDescription(bars, labels, language),
            Modifier.testTag("evolution-bars"),
            selected = picked.toSet(),
            // A third month replaces the older choice, so two are compared at most.
            onSelect = { i -> picked = if (i in picked) picked - i else (picked + i).takeLast(2) },
        )
        val chosen = picked.mapNotNull { bars.getOrNull(it) }
        when (chosen.size) {
            0 -> Text(stringResource(R.string.evo_hint), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
            1 -> Card(Modifier.testTag("evolution-month")) { MonthLine(chosen[0]) }
            else -> Comparison(chosen[0], chosen[1])
        }
    }
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
