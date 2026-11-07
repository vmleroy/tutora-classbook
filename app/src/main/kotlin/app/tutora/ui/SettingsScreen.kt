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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
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
import app.tutora.domain.ConfirmWhen
import app.tutora.domain.Language
import app.tutora.domain.PastClasses
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

/**
 * Ajustes: only its pages, each with a line of what is set (AC-014-28). Seus dados and Backup e
 * planilha first; Geral, Aulas, Lembretes and Pagamentos; then Sobre.
 */
@Composable
fun SettingsScreen(
    appInfo: AppInfo,
    schemaVersion: Int,
    onOpen: (Any) -> Unit,
) {
    val services = LocalServices.current
    val settings by services.settings.collectAsState(initial = null)
    val s = settings ?: return
    ScreenScaffold(stringResource(R.string.tab_settings), "settings-screen") {
        ListGroup(
            listOf(
                // Seus dados: name and Pix key for reports (AC-010-16).
                {
                    // The Pix key never in full here (AC-015-16); the lock said when on (AC-015-1).
                    ListRow(
                        stringResource(R.string.rep_personal),
                        subtitle =
                            (
                                listOf(s.teacherName, app.tutora.domain.maskPix(s.pixKey)) +
                                    listOfNotNull(stringResource(R.string.lock_onLine).takeIf { s.appLock })
                            ).filter { it.isNotBlank() }
                                .joinToString(" · ")
                                .ifEmpty {
                                    stringResource(R.string.rep_personalHint)
                                },
                        icon = IconName.USERS,
                        onClick = { onOpen(PersonalData) },
                    )
                },
                // Backup e planilha, with its state or its warning (AC-011-1, 10).
                {
                    app.tutora.ui.backup
                        .BackupSettingsRow { onOpen(BackupHome) }
                },
            ),
        )
        ListGroup(
            listOf(
                {
                    PageRow(
                        stringResource(R.string.settings_general),
                        listOfNotNull(
                            stringResource(if (s.language == Language.PT_BR) R.string.languages_pt_BR else R.string.languages_en),
                            stringResource(R.string.set_hhmm).takeIf { s.clockDurations },
                        ).joinToString(" · "),
                        IconName.GEAR,
                        "page-general",
                    ) { onOpen(SettingsGeneral) }
                },
                {
                    PageRow(
                        stringResource(R.string.settings_lessons),
                        listOfNotNull(
                            stringResource(if (s.pastClasses == PastClasses.AUTO) R.string.set_pastAuto else R.string.set_pastAsk),
                            stringResource(R.string.settings_liveTimer).takeIf { s.liveTimer },
                        ).joinToString(" · "),
                        IconName.CALENDAR_CHECK,
                        "page-lessons",
                    ) { onOpen(SettingsLessons) }
                },
                {
                    PageRow(
                        stringResource(R.string.set_reminders),
                        app.tutora.ui.reminders
                            .remindersSummary(s),
                        IconName.ALERT,
                        "reminders-row",
                    ) { onOpen(RemindersHome) }
                },
                {
                    PageRow(
                        stringResource(R.string.set_payments),
                        stringResource(if (s.credits) R.string.set_creditsOn else R.string.set_creditsOff),
                        IconName.WALLET,
                        "page-payments",
                    ) { onOpen(SettingsPayments) }
                },
            ),
        )
        ListGroup(
            listOf {
                PageRow(
                    stringResource(R.string.settings_about),
                    stringResource(R.string.set_aboutLine, appInfo.versionName, schemaVersion),
                    IconName.FILE,
                    "page-about",
                ) { onOpen(SettingsAbout) }
            },
        )
    }
}

@Composable
private fun PageRow(
    title: String,
    subtitle: String,
    icon: IconName,
    tag: String,
    onClick: () -> Unit,
) = ListRow(title, Modifier.testTag(tag), subtitle = subtitle.ifEmpty { null }, icon = icon, onClick = onClick)

/** Ajustes › Geral: Idioma, Aparência, Calendário, Como você dá aula, Locais padrão and Durações em HH:MM (AC-014-28). */
@Composable
fun SettingsGeneralScreen(nav: NavHostController) {
    val services = LocalServices.current
    val settings by services.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val s = settings ?: return

    fun save(next: Settings) = scope.launch { services.saveSettings(next) }
    ScreenScaffold(stringResource(R.string.settings_general), "settings-general", variant = HeaderVariant.PUSH, onBack = {
        nav.popBackStack()
    }) {
        ListGroup(
            listOf(
                { LanguageRow(s.language) { save(s.copy(language = it)) } },
                // Aparência: mode, theme, colors and text size (AC-013-1).
                {
                    app.tutora.ui.appearance
                        .AppearanceSettingsRow { nav.navigate(AppearanceHome) }
                },
                // Calendário: classes into a phone calendar (AC-012-1).
                {
                    app.tutora.ui.calendar
                        .CalendarSettingsRow { nav.navigate(CalendarHome) }
                },
                // Como você dá aula, to change what the setup chose (AC-014-20).
                {
                    ListRow(
                        stringResource(R.string.set_teaching),
                        Modifier.testTag("teaching-row"),
                        subtitle =
                            app.tutora.ui.setup
                                .teachingSummary(s.teaching),
                        icon = IconName.USERS,
                        onClick = { nav.navigate(TeachingHome) },
                    )
                },
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
    }
}

/**
 * Ajustes › Aulas: Aula ao vivo with Início automático, Aulas passadas with Pedir confirmação,
 * and Marcar como dada antes do horário (AC-014-28).
 */
@Composable
fun SettingsLessonsScreen(nav: NavHostController) {
    val services = LocalServices.current
    val settings by services.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val s = settings ?: return

    fun save(next: Settings) = scope.launch { services.saveSettings(next) }

    // A change to the timer or its automatic start sets or clears the alarm at once.
    fun saveTimer(next: Settings) =
        scope.launch {
            services.saveSettings(next)
            services.live.autoStart()
        }
    ScreenScaffold(stringResource(R.string.settings_lessons), "settings-lessons", variant = HeaderVariant.PUSH, onBack = {
        nav.popBackStack()
    }) {
        ListGroup(
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
        // Aulas passadas and Pedir confirmação (AC-014-1, 2).
        ListGroup(listOf { PastClassesRow(s) { save(it) } })
    }
}

/** Ajustes › Pagamentos: Créditos e bônus (AC-008-28, AC-014-28). */
@Composable
fun SettingsPaymentsScreen(nav: NavHostController) {
    val services = LocalServices.current
    val settings by services.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val bonusHolders by remember { services.paymentRepo.bonusHolders() }.collectAsState(initial = 0)
    var confirmingOff by rememberSaveable { mutableStateOf(false) }
    val s = settings ?: return
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
    ScreenScaffold(
        stringResource(R.string.set_payments),
        "settings-payments",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
    ) {
        // Créditos e bônus, off at first: off, a payment can't be more than is owed (AC-008-28).
        ListGroup(
            listOf {
                ListRow(
                    stringResource(R.string.settings_credits),
                    subtitle = stringResource(R.string.settings_creditsHint),
                    icon = IconName.WALLET,
                    trailing = RowTrailing.SWITCH,
                    switchValue = s.credits,
                    // Off with bonuses given asks first: they will be deleted (AC-008-38).
                    onSwitchChange = { on ->
                        if (!on && bonusHolders > 0) confirmingOff = true else scope.launch { services.saveSettings(s.copy(credits = on)) }
                    },
                )
            },
        )
    }
}

/** Ajustes › Sobre: the version and the database's (AC-001-22, AC-014-28). */
@Composable
fun SettingsAboutScreen(
    nav: NavHostController,
    appInfo: AppInfo,
    schemaVersion: Int,
) {
    ScreenScaffold(
        stringResource(R.string.settings_about),
        "settings-about",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
    ) {
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

/** Aulas passadas: one by one or by themselves; one by one, Pedir confirmação says when (AC-014-1, 2). */
@Composable
private fun PastClassesRow(
    s: Settings,
    onChange: (Settings) -> Unit,
) {
    val c = TutoraTheme.colors
    Column(
        Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2_5).testTag("past-classes"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        Text(stringResource(R.string.set_pastClasses), style = TutoraTheme.type.bodyStrong, color = c.ink)
        SegmentedControl(
            listOf(
                PastClasses.ASK to stringResource(R.string.set_pastAsk),
                PastClasses.AUTO to stringResource(R.string.set_pastAuto),
            ),
            s.pastClasses,
            { onChange(s.copy(pastClasses = it)) },
            stringResource(R.string.set_pastClasses),
            size = ControlSize.SM,
        )
        Text(
            stringResource(if (s.pastClasses == PastClasses.AUTO) R.string.set_pastAutoHint else R.string.set_pastAskHint),
            style = TutoraTheme.type.meta,
            color = c.inkMuted,
        )
        Text(stringResource(R.string.set_confirmWhen), style = TutoraTheme.type.bodyStrong, color = c.ink)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            listOf(
                ConfirmWhen.END to R.string.set_confirmEnd,
                ConfirmWhen.HOUR to R.string.set_confirmHour,
                ConfirmWhen.NEXT_DAY to R.string.set_confirmNextDay,
            ).forEach { (w, label) ->
                app.tutora.designsystem.components
                    .Chip(stringResource(label), s.confirmWhen == w, { onChange(s.copy(confirmWhen = w)) })
            }
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
        // Masked, with the eye to show it (AC-015-16); offered the lock once when first saved (AC-015-5).
        val lock = app.tutora.lock.LocalAppLock.current
        var offer by rememberSaveable { mutableStateOf(false) }
        TextField(
            stringResource(R.string.rep_settingsPix),
            s.pixKey,
            { v -> scope.launch { services.saveSettings(s.copy(pixKey = v)) } },
            Modifier.testTag("pix-key"),
            // E-mail and random keys keep their letters as typed.
            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
            secret = { app.tutora.domain.maskPix(it) },
            onFocusLost = {
                if (s.pixKey.isNotBlank() && !s.appLock && !s.lockOffered && lock.device.available()) offer = true
            },
        )
        // The reports' top line; empty keeps Tutora's (AC-019-15).
        TextField(
            stringResource(R.string.rep_settingsTitle),
            s.reportTitle,
            { v -> scope.launch { services.saveSettings(s.copy(reportTitle = v)) } },
            Modifier.testTag("report-title"),
            placeholder = stringResource(R.string.rep_brand),
        )
        LockSettings(s)
        if (offer) {
            fun answer(on: Boolean) {
                offer = false
                if (on) {
                    lock.ask { ok -> scope.launch { services.saveSettings(s.copy(appLock = ok, lockOffered = true)) } }
                } else {
                    scope.launch { services.saveSettings(s.copy(lockOffered = true)) }
                }
            }
            BottomSheet(
                stringResource(R.string.lock_offerTitle),
                { answer(false) },
                Modifier.testTag("lock-offer"),
                subtitle = stringResource(R.string.lock_offerBody),
                actions =
                    listOf(
                        { m -> Button(stringResource(R.string.lock_offerYes), { answer(true) }, m) },
                        { m -> Button(stringResource(R.string.lock_offerNo), { answer(false) }, m, variant = ButtonVariant.GHOST) },
                    ),
            )
        }
    }
}

/**
 * Privacidade: Bloquear o Tutora, asking the phone's unlock to turn it on and off (AC-015-1, 3),
 * Bloquear após (AC-015-4), and what to do on a phone with no screen lock (AC-015-2).
 */
@Composable
private fun LockSettings(s: Settings) {
    val services = LocalServices.current
    val lock = app.tutora.lock.LocalAppLock.current
    val scope = rememberCoroutineScope()
    val c = TutoraTheme.colors
    val available = lock.device.available()
    app.tutora.designsystem.components
        .SectionHeader(stringResource(R.string.lock_section))
    ListGroup(
        listOf(
            {
                ListRow(
                    stringResource(R.string.lock_title),
                    Modifier.testTag("lock-switch"),
                    subtitle = stringResource(if (available) R.string.lock_hint else R.string.lock_noScreenLock),
                    icon = IconName.LOCK,
                    trailing = if (available) RowTrailing.SWITCH else RowTrailing.NONE,
                    switchValue = s.appLock && available,
                    onSwitchChange = { on ->
                        lock.ask { ok -> if (ok) scope.launch { services.saveSettings(s.copy(appLock = on)) } }
                    },
                )
            },
        ),
    )
    if (!available) {
        Button(
            stringResource(R.string.lock_openSettings),
            { lock.device.openSecuritySettings() },
            Modifier.testTag("lock-open-settings"),
            variant = ButtonVariant.OUTLINE,
        )
    }
    if (s.appLock && available) {
        Text(stringResource(R.string.lock_after), style = TutoraTheme.type.bodyStrong, color = c.ink)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            listOf(
                app.tutora.domain.LockAfter.IMMEDIATELY to R.string.lock_afterNow,
                app.tutora.domain.LockAfter.ONE_MINUTE to R.string.lock_after1,
                app.tutora.domain.LockAfter.FIVE_MINUTES to R.string.lock_after5,
            ).forEach { (after, label) ->
                app.tutora.designsystem.components
                    .Chip(
                        stringResource(label),
                        s.lockAfter == after,
                        { scope.launch { services.saveSettings(s.copy(lockAfter = after)) } },
                    )
            }
        }
    }
}
