package app.tutora.ui.schedule

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.domain.FeeRule
import app.tutora.domain.FeeSuggestion
import app.tutora.domain.LessonStatus
import app.tutora.domain.suggestFee
import app.tutora.state.LocalServices
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.clockTime
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

// The worked example of the Fees mockup: a R$ 120 class at 14:00, cancelled at 11:00.
private const val EXAMPLE_CENTS = 12_000L
private const val EXAMPLE_START = "2026-10-26T17:00:00.000Z"
private const val EXAMPLE_NOTICE = "2026-10-26T14:00:00.000Z"
private val EXAMPLE_TIME = LocalTime(14, 0)
private val EXAMPLE_NOTICE_TIME = LocalTime(11, 0)

/** Ajustes › Cancelamentos e faltas: the default rule, with a live example (AC-006-18). */
@Composable
fun FeeRulesScreen(nav: NavHostController) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val settings by services.settings.collectAsState(initial = null)
    val saved = settings ?: return
    var rule by remember(saved.feeRule) { mutableStateOf(saved.feeRule) }
    ScreenScaffold(
        stringResource(R.string.fees_title),
        "fee-rules",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
        bottomBar = {
            Button(stringResource(R.string.fees_save), {
                scope.launch {
                    services.saveSettings(saved.copy(feeRule = rule))
                    nav.popBackStack()
                }
            }, fullWidth = true)
        },
    ) {
        HintLine(stringResource(R.string.fees_subtitle))
        FeeRuleEditor(rule) { rule = it }
        FeeExample(rule)
        HintLine(stringResource(R.string.fees_note))
    }
}

@Composable
private fun FeeExample(rule: FeeRule) {
    val language = currentLanguage()

    @Composable
    fun outcome(s: FeeSuggestion?): String =
        if (s is FeeSuggestion.Charge) {
            stringResource(R.string.fees_exampleFee, formatMoney(s.cents, language))
        } else {
            stringResource(R.string.fees_exampleNone)
        }
    val title =
        stringResource(R.string.fees_example, formatMoney(EXAMPLE_CENTS, language, compact = true), clockTime(EXAMPLE_TIME, language))
    Card(label = title) {
        Text(title.uppercase(), style = TutoraTheme.type.caption, color = TutoraTheme.colors.inkMuted)
        Text(
            stringResource(
                R.string.fees_exampleLate,
                clockTime(EXAMPLE_NOTICE_TIME, language),
                outcome(suggestFee(rule, LessonStatus.CANCELLED, EXAMPLE_CENTS, EXAMPLE_START, EXAMPLE_NOTICE)),
            ),
            style = TutoraTheme.type.body,
            color = TutoraTheme.colors.ink,
        )
        Text(
            stringResource(R.string.fees_exampleMiss, outcome(suggestFee(rule, LessonStatus.MISSED, EXAMPLE_CENTS, EXAMPLE_START, null))),
            style = TutoraTheme.type.body,
            color = TutoraTheme.colors.ink,
        )
    }
}
