package app.tutora.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.tutora.AppInfo
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.TextField
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.AutoStart
import app.tutora.domain.Language
import app.tutora.domain.Settings
import app.tutora.state.LocalServices
import kotlinx.coroutines.launch

/** A string in a given language, whatever the app's (for the language samples). */
@Composable
private fun stringIn(
    language: Language,
    id: Int,
    vararg args: Any,
): String {
    val context = LocalContext.current
    val config = Configuration(LocalConfiguration.current).apply { setLocale(localeOf(language)) }
    return context.createConfigurationContext(config).resources.getString(id, *args)
}

/** Ajustes: Geral (language, default places), Aulas (live timer) and Sobre (AC-001-22, AC-003-11, AC-004-29, AC-007-17). */
@Composable
fun SettingsScreen(
    appInfo: AppInfo,
    schemaVersion: Int,
    onPersonal: () -> Unit = {},
) {
    val services = LocalServices.current
    val settings by services.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val s = settings ?: return

    fun save(next: Settings) = scope.launch { services.saveSettings(next) }

    val bonusHolders by remember { services.paymentRepo.bonusHolders() }.collectAsState(initial = 0)
    var confirmingOff by rememberSaveable { mutableStateOf(false) }
    if (confirmingOff) {
        BottomSheet(
            pluralStringResource(R.plurals.settings_creditsOffTitle, bonusHolders, bonusHolders),
            { confirmingOff = false },
            subtitle = stringResource(R.string.settings_creditsOffBody),
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.catalog_cancel), { confirmingOff = false }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.settings_creditsOff), {
                            confirmingOff = false
                            scope.launch { services.payments.turnCreditsOff() }
                        }, m, variant = ButtonVariant.DANGER)
                    },
                ),
        )
    }

    // A change to the timer or its automatic start sets or clears the alarm at once.
    fun saveTimer(next: Settings) =
        scope.launch {
            services.saveSettings(next)
            services.live.autoStart()
        }

    ScreenScaffold(stringResource(R.string.tab_settings), "settings-screen") {
        // Seus dados first, on its own page: name and Pix key for reports (AC-010-16).
        ListGroup(
            listOf {
                ListRow(
                    stringResource(R.string.rep_personal),
                    subtitle =
                        listOf(s.teacherName, s.pixKey).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty {
                            stringResource(R.string.rep_personalHint)
                        },
                    icon = IconName.USERS,
                    onClick = onPersonal,
                )
            },
        )
        ListGroup(
            title = stringResource(R.string.settings_general),
            rows =
                listOf(
                    { LanguageRow(s.language) { save(s.copy(language = it)) } },
                    {
                        ListRow(
                            stringResource(R.string.settings_showDefaultPlaces),
                            subtitle = stringResource(R.string.settings_showDefaultPlacesHint),
                            icon = IconName.PIN,
                            trailing = RowTrailing.SWITCH,
                            switchValue = s.showDefaultPlaces,
                            onSwitchChange = { save(s.copy(showDefaultPlaces = it)) },
                        )
                    },
                    {
                        ListRow(
                            stringResource(R.string.settings_clockDurations),
                            subtitle = stringResource(R.string.settings_clockDurationsHint),
                            icon = IconName.CLOCK,
                            trailing = RowTrailing.SWITCH,
                            switchValue = s.clockDurations,
                            onSwitchChange = { save(s.copy(clockDurations = it)) },
                        )
                    },
                ),
        )
        ListGroup(
            title = stringResource(R.string.settings_lessons),
            rows =
                listOf(
                    {
                        // Off hides Iniciar aula everywhere (AC-007-3, 17).
                        ListRow(
                            stringResource(R.string.settings_liveTimer),
                            subtitle = stringResource(R.string.settings_liveTimerHint),
                            icon = IconName.PLAY,
                            trailing = RowTrailing.SWITCH,
                            switchValue = s.liveTimer,
                            onSwitchChange = { saveTimer(s.copy(liveTimer = it)) },
                        )
                    },
                    {
                        // On, Marcar como dada shows on a class still ahead (AC-006-25).
                        ListRow(
                            stringResource(R.string.settings_earlyDone),
                            subtitle = stringResource(R.string.settings_earlyDoneHint),
                            icon = IconName.CALENDAR_CHECK,
                            trailing = RowTrailing.SWITCH,
                            switchValue = s.earlyDone,
                            onSwitchChange = { save(s.copy(earlyDone = it)) },
                        )
                    },
                ),
        )
        // Início automático, only while the timer is on (AC-007-19).
        if (s.liveTimer) {
            ListGroup(
                title = stringResource(R.string.settings_autoStart),
                rows =
                    listOfNotNull(
                        {
                            ListRow(
                                stringResource(R.string.settings_autoStartSwitch),
                                subtitle = stringResource(R.string.settings_autoStartHint),
                                icon = IconName.CLOCK,
                                trailing = RowTrailing.SWITCH,
                                switchValue = s.autoStart != AutoStart.OFF,
                                onSwitchChange = { on -> saveTimer(s.copy(autoStart = if (on) AutoStart.APP_OPEN else AutoStart.OFF)) },
                            )
                        },
                        if (s.autoStart != AutoStart.OFF) {
                            { AutoStartMode(s.autoStart) { saveTimer(s.copy(autoStart = it)) } }
                        } else {
                            null
                        },
                    ),
            )
        }
        // Créditos e bônus, off at first: off, a payment can't be more than is owed (AC-008-28).
        ListGroup(
            title = stringResource(R.string.pay_section),
            rows =
                listOf {
                    ListRow(
                        stringResource(R.string.settings_credits),
                        subtitle = stringResource(R.string.settings_creditsHint),
                        icon = IconName.WALLET,
                        trailing = RowTrailing.SWITCH,
                        switchValue = s.credits,
                        // Off with bonuses given asks first: they will be deleted (AC-008-38).
                        onSwitchChange = { on -> if (!on && bonusHolders > 0) confirmingOff = true else save(s.copy(credits = on)) },
                    )
                },
        )
        Card(label = stringResource(R.string.settings_about)) {
            Text(stringResource(R.string.settings_about), style = TutoraTheme.type.rowTitle, color = TutoraTheme.colors.ink)
            Text(
                stringResource(R.string.settings_about_value, appInfo.versionName, appInfo.build, schemaVersion),
                style = TutoraTheme.type.caption.copy(fontFeatureSettings = "tnum"),
                color = TutoraTheme.colors.inkMuted,
            )
        }
    }
}

/** Só com o app aberto / Também com o app fechado, with a line on what the choice does (AC-007-19). */
@Composable
private fun AutoStartMode(
    mode: AutoStart,
    onChange: (AutoStart) -> Unit,
) {
    Column(Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2_5), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        SegmentedControl(
            listOf(
                AutoStart.APP_OPEN to stringResource(R.string.settings_autoStartOpen),
                AutoStart.ALWAYS to stringResource(R.string.settings_autoStartAlways),
            ),
            mode,
            onChange,
            stringResource(R.string.settings_autoStart),
            size = ControlSize.SM,
        )
        Text(
            stringResource(if (mode == AutoStart.ALWAYS) R.string.settings_autoStartAlwaysHint else R.string.settings_autoStartOpenHint),
            style = TutoraTheme.type.meta,
            color = TutoraTheme.colors.inkMuted,
        )
    }
}

// A fixed sample, so each option shows how its dates and amounts are written.
private const val SAMPLE_CENTS = 123_456L

/** "Idioma do app": Português / English, each with a sample in its own language (AC-003-11). */
@Composable
private fun LanguageRow(
    language: Language,
    onChange: (Language) -> Unit,
) {
    val c = TutoraTheme.colors
    val languages = listOf(Language.PT_BR, Language.EN)
    Column(Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2_5), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Column {
            Text(stringResource(R.string.settings_language), style = TutoraTheme.type.bodyStrong, color = c.ink)
            Text(stringResource(R.string.settings_languageHint), style = TutoraTheme.type.meta, color = c.inkMuted)
        }
        SegmentedControl(
            languages.map { it to stringResource(if (it == Language.PT_BR) R.string.languages_pt_BR else R.string.languages_en) },
            language,
            onChange,
            stringResource(R.string.settings_language),
            size = ControlSize.SM,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            languages.forEach { l ->
                val day = if (l == Language.PT_BR) "26/10" else "10/26"
                Text(
                    stringIn(l, R.string.settings_languageSample, day, formatMoney(SAMPLE_CENTS, l)),
                    style = TutoraTheme.type.meta.copy(fontFeatureSettings = "tnum"),
                    color = c.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Seus dados: the teacher's name and Pix key, printed on reports when set (AC-010-16). */
@Composable
fun PersonalDataScreen(onBack: () -> Unit) {
    val services = LocalServices.current
    val settings by services.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val s = settings ?: return
    ScreenScaffold(stringResource(R.string.rep_personal), "personal-screen", variant = HeaderVariant.PUSH, onBack = onBack) {
        Text(stringResource(R.string.rep_personalBody), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
        TextField(stringResource(R.string.rep_settingsName), s.teacherName, { v ->
            scope.launch {
                services.saveSettings(s.copy(teacherName = v))
            }
        })
        TextField(stringResource(R.string.rep_settingsPix), s.pixKey, { v -> scope.launch { services.saveSettings(s.copy(pixKey = v)) } })
    }
}
