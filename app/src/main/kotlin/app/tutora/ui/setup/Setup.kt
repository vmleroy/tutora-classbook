package app.tutora.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.Stepper
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.BACKUP_KEEP_DEFAULT
import app.tutora.domain.BackupEvery
import app.tutora.domain.Language
import app.tutora.domain.Teaching
import app.tutora.domain.TravelMode
import app.tutora.state.LocalServices
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.StudentEdit
import app.tutora.ui.backup.rememberBackupPicker
import app.tutora.usecases.SETUP_HOME_CENTS
import app.tutora.usecases.SETUP_ONLINE_CENTS
import app.tutora.usecases.SETUP_PRICE_STEP
import app.tutora.usecases.SETUP_TRAVEL_CENTS
import app.tutora.usecases.SetupChoices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val STEPS = 4

/** The price stepper's share of its row against the label's 1. */
private const val STEPPER_SHARE = 1.3f

/** "Aulas particulares · Salinha ou cursinho", Ajustes' line for Como você dá aula. */
@Composable
fun teachingSummary(chosen: Set<Teaching>): String =
    Teaching.entries
        .filter { it in chosen }
        .map { teachingName(it) }
        .joinToString(" · ")

@Composable
private fun teachingName(t: Teaching): String =
    stringResource(
        when (t) {
            Teaching.PRIVATE -> R.string.setup_private
            Teaching.SCHOOL -> R.string.setup_school
            Teaching.SALINHA -> R.string.setup_salinha
        },
    )

@Composable
private fun teachingHint(t: Teaching): String =
    stringResource(
        when (t) {
            Teaching.PRIVATE -> R.string.setup_privateHint
            Teaching.SCHOOL -> R.string.setup_schoolHint
            Teaching.SALINHA -> R.string.setup_salinhaHint
        },
    )

/** The three kinds of teaching, each on or off (AC-014-19). */
@Composable
private fun TeachingChoices(
    chosen: Set<Teaching>,
    onChange: (Set<Teaching>) -> Unit,
) {
    ListGroup(
        Teaching.entries.map { t ->
            {
                ListRow(
                    teachingName(t),
                    Modifier.testTag("teaching-${t.key}"),
                    subtitle = teachingHint(t),
                    trailing = RowTrailing.SWITCH,
                    switchValue = t in chosen,
                    onSwitchChange = { on -> onChange(if (on) chosen + t else chosen - t) },
                )
            }
        },
    )
}

/** Ajustes › Geral › Como você dá aula: what shows in Ganhos and when adding a place (AC-014-20). */
@Composable
fun TeachingScreen(nav: NavHostController) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val settings by services.settings.collectAsState(initial = null)
    val s = settings ?: return
    ScreenScaffold(
        stringResource(R.string.set_teaching),
        "teaching-screen",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
    ) {
        Text(stringResource(R.string.setup_teachingBody), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
        TeachingChoices(s.teaching) { scope.launch { services.saveSettings(s.copy(teaching = it)) } }
    }
}

/**
 * Configuração inicial: Idioma, Como você dá aula, Seus preços padrão and Seus dados, seguros,
 * each skippable, then a summary and two ways out (AC-014-17 to 23). Steps not seen keep their
 * defaults. The choices survive the screen being made again when the language changes.
 */
@Composable
fun SetupScreen(nav: NavHostController) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val language = currentLanguage()
    val picker = rememberBackupPicker()
    val backup by remember { services.backups.observe() }.collectAsState(initial = null)
    var step by rememberSaveable { mutableIntStateOf(1) }
    var teaching by rememberSaveable { mutableStateOf(setOf(Teaching.PRIVATE.key)) }
    var online by rememberSaveable { mutableLongStateOf(SETUP_ONLINE_CENTS) }
    var home by rememberSaveable { mutableLongStateOf(SETUP_HOME_CENTS) }
    var travel by rememberSaveable { mutableStateOf(TravelMode.VARIES) }
    var remind by rememberSaveable { mutableStateOf(true) }
    val chosen = teaching.mapNotNull(Teaching::fromKey).toSet()

    fun choices() = SetupChoices(language, chosen, online, home, travel, remind)

    fun finish(then: Any?) =
        scope.launch {
            withContext(Dispatchers.IO) { services.setup.save(choices()) }
            // Only the setup leaves the stack: Hoje stays under it, so Voltar from the new student lands there.
            nav.popBackStack(app.tutora.ui.SetupHome, inclusive = true)
            then?.let { nav.navigate(it) }
        }

    // Picking a folder turns automatic backup on, weekly, as Backup e planilha does (AC-014-22, AC-011-5).
    fun pickFolder() =
        picker.folder { uri ->
            if (uri != null) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val name = runCatching { services.ctx.backupFolder.adopt(uri) }.getOrDefault("")
                        services.backups.setAuto(true, BackupEvery.WEEK, BACKUP_KEEP_DEFAULT, uri, name)
                    }
                }
            }
        }
    val skip = stringResource(R.string.setup_skip)
    ScreenScaffold(
        stringResource(R.string.setup_title),
        "setup-screen",
        variant = HeaderVariant.MODAL,
        eyebrow = stringResource(R.string.setup_step, step),
        // Pular goes to the end, where the summary and the two ways out are (AC-014-17).
        trailing =
            if (step < STEPS) {
                { Button(skip, { step = STEPS }, Modifier.testTag("setup-skip"), variant = ButtonVariant.GHOST, size = ControlSize.SM) }
            } else {
                null
            },
        bottomBar = {
            if (step < STEPS) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    Button(
                        stringResource(R.string.setup_back),
                        { step-- },
                        Modifier.weight(1f),
                        variant = ButtonVariant.OUTLINE,
                        enabled = step > 1,
                    )
                    Button(stringResource(R.string.setup_next), { step++ }, Modifier.weight(1f).testTag("setup-next"))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    Button(stringResource(R.string.setup_firstStudent), { finish(StudentEdit()) }, fullWidth = true, icon = IconName.PLUS)
                    Button(stringResource(R.string.setup_goToday), { finish(null) }, fullWidth = true, variant = ButtonVariant.OUTLINE)
                }
            }
        },
    ) {
        StepBar(step)
        when (step) {
            1 -> {
                Heading(stringResource(R.string.setup_welcome), stringResource(R.string.setup_welcomeBody))
                Text(stringResource(R.string.setup_language), style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.ink)
                // The app language changes at once, and the setup's words with it (AC-014-18).
                SegmentedControl(
                    listOf(
                        Language.PT_BR to stringResource(R.string.languages_pt_BR),
                        Language.EN to stringResource(R.string.languages_en),
                    ),
                    language,
                    { l -> scope.launch { services.saveSettings(services.currentSettings().copy(language = l)) } },
                    stringResource(R.string.setup_language),
                )
            }
            2 -> {
                Heading(stringResource(R.string.setup_teaching), stringResource(R.string.setup_teachingBody))
                TeachingChoices(chosen) { teaching = it.map { t -> t.key }.toSet() }
                Text(stringResource(R.string.setup_teachingNote), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
            }
            3 -> {
                Heading(stringResource(R.string.setup_prices), stringResource(R.string.setup_pricesBody))
                PriceRow(stringResource(R.string.places_builtin_online), online) { online = it }
                PriceRow(stringResource(R.string.places_builtin_student_home), home) { home = it }
                Text(stringResource(R.string.setup_travel), style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.ink)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    listOf(
                        TravelMode.VARIES to stringResource(R.string.setup_travelVaries),
                        TravelMode.FIXED to "${stringResource(R.string.setup_travelFixed)} ${formatMoney(SETUP_TRAVEL_CENTS, language)}",
                        TravelMode.NONE to stringResource(R.string.setup_travelNone),
                    ).forEach { (m, label) -> Chip(label, travel == m, { travel = m }) }
                }
                Text(
                    when (travel) {
                        TravelMode.VARIES -> stringResource(R.string.setup_travelVariesHint)
                        TravelMode.FIXED -> stringResource(R.string.setup_travelFixedHint, formatMoney(SETUP_TRAVEL_CENTS, language))
                        TravelMode.NONE -> stringResource(R.string.setup_travelNoneHint)
                    },
                    style = TutoraTheme.type.meta,
                    color = TutoraTheme.colors.inkMuted,
                )
            }
            else -> {
                Heading(stringResource(R.string.setup_safe), stringResource(R.string.setup_safeBody))
                val b = backup
                ListGroup(
                    listOf(
                        {
                            ListRow(
                                stringResource(R.string.setup_backup),
                                Modifier.testTag("setup-backup"),
                                subtitle = b?.folderName?.ifEmpty { null } ?: stringResource(R.string.setup_backupHint),
                                icon = IconName.CLOUD,
                                trailing = RowTrailing.SWITCH,
                                switchValue = b?.auto == true,
                                onSwitchChange = { on ->
                                    when {
                                        on && b?.folderUri.isNullOrEmpty() -> pickFolder()
                                        b != null ->
                                            scope.launch {
                                                withContext(Dispatchers.IO) {
                                                    services.backups.setAuto(on, BackupEvery.WEEK, b.keep, b.folderUri, b.folderName)
                                                }
                                            }
                                    }
                                },
                            )
                        },
                        {
                            ListRow(
                                stringResource(R.string.setup_folder),
                                subtitle = stringResource(R.string.setup_folderHint),
                                icon = IconName.ARCHIVE,
                                onClick = { pickFolder() },
                            )
                        },
                        {
                            ListRow(
                                stringResource(R.string.setup_remind),
                                Modifier.testTag("setup-remind"),
                                subtitle = stringResource(R.string.setup_remindHint),
                                icon = IconName.ALERT,
                                trailing = RowTrailing.SWITCH,
                                switchValue = remind,
                                onSwitchChange = { remind = it },
                            )
                        },
                    ),
                )
                Summary(choices())
            }
        }
    }
}

@Composable
private fun Heading(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        Text(title, style = TutoraTheme.type.titleLg, color = TutoraTheme.colors.ink)
        Text(body, style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
    }
}

/** Four bars, filled up to the step the teacher is on (AC-014-17). */
@Composable
private fun StepBar(step: Int) {
    val c = TutoraTheme.colors
    Row(Modifier.fillMaxWidth().testTag("setup-steps"), horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        (1..STEPS).forEach { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(Size.progress)
                    .clip(RoundedCornerShape(Radius.full))
                    .background(if (i <= step) c.btnPrimary else c.line),
            )
        }
    }
}

/** A built-in place's price per hour, R$ 5 at a time (AC-014-21). */
@Composable
private fun PriceRow(
    place: String,
    cents: Long,
    onChange: (Long) -> Unit,
) {
    val language = currentLanguage()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        Column(Modifier.weight(1f)) {
            Text(place, style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.ink)
            Text(stringResource(R.string.setup_perHour), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
        }
        // The stepper takes a little more of the row than the label, so neither is squeezed.
        Stepper(
            cents,
            onChange,
            Modifier.weight(STEPPER_SHARE),
            step = SETUP_PRICE_STEP,
            min = SETUP_PRICE_STEP,
            // Whole reais ("R$ 100"): the step is R$ 5, and the full "R$ 100,00" doesn't fit beside − and +.
            format = { formatMoney(it, language, compact = true) },
            valueLabel = place,
            size = ControlSize.SM,
        )
    }
}

/** RESUMO: the choices in one line, as the mockup's (AC-014-23). */
@Composable
private fun Summary(c: SetupChoices) {
    val language = currentLanguage()
    val travel =
        when (c.travel) {
            TravelMode.VARIES -> " ${stringResource(R.string.setup_plusTravel)}"
            TravelMode.FIXED -> " + ${formatMoney(SETUP_TRAVEL_CENTS, language)}"
            TravelMode.NONE -> ""
        }
    val line =
        listOf(
            stringResource(if (c.language == Language.PT_BR) R.string.languages_pt_BR else R.string.languages_en),
            teachingSummary(c.teaching),
            stringResource(R.string.setup_rate, stringResource(R.string.places_builtin_online), formatMoney(c.onlineCents, language)),
            stringResource(R.string.setup_rate, stringResource(R.string.places_builtin_student_home), formatMoney(c.homeCents, language)) +
                travel,
        ).filter { it.isNotBlank() }.joinToString(" · ")
    Column(Modifier.testTag("setup-summary"), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        Text(stringResource(R.string.setup_summary), style = TutoraTheme.type.badge, color = TutoraTheme.colors.inkMuted)
        Text(line, style = TutoraTheme.type.body, color = TutoraTheme.colors.ink, modifier = Modifier.padding(bottom = Spacing.s2))
    }
}
