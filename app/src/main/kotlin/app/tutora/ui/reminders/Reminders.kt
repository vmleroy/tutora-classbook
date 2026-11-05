package app.tutora.ui.reminders

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.components.AlertBanner
import app.tutora.designsystem.components.AlertTone
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.DurationField
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.TimeField
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Language
import app.tutora.domain.PastClasses
import app.tutora.domain.REMIND_BEFORE_CHOICES
import app.tutora.domain.REMIND_BEFORE_DEFAULT
import app.tutora.domain.REMIND_MAX
import app.tutora.domain.REMIND_MIN
import app.tutora.state.LocalServices
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.clockTime
import app.tutora.ui.localeOf
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/** Whether the phone lets Tutora notify, asking for it, and its settings page (AC-014-16). */
interface NotificationPermission {
    fun allowed(): Boolean

    fun request(onResult: (Boolean) -> Unit)

    fun openSettings()
}

val LocalNotificationPermission = staticCompositionLocalOf<NotificationPermission?> { null }

@Composable
fun rememberNotificationPermission(): NotificationPermission {
    LocalNotificationPermission.current?.let { return it }
    val context = LocalContext.current
    var pending by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pending?.invoke(granted)
            pending = null
        }
    return remember(launcher) {
        object : NotificationPermission {
            override fun allowed() =
                app.tutora.reminders
                    .notificationsAllowed(context)

            override fun request(onResult: (Boolean) -> Unit) {
                // Before Android 13 notifications need no asking, so they are always allowed.
                if (allowed()) {
                    onResult(true)
                    return
                }
                pending = onResult
                launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }

            override fun openSettings() {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}

/** Whether exact alarms are allowed, and the phone's page that allows them in one tap (AC-014-27). */
interface ExactAlarmPermission {
    fun allowed(): Boolean

    fun open()
}

val LocalExactAlarmPermission = staticCompositionLocalOf<ExactAlarmPermission?> { null }

@Composable
fun rememberExactAlarmPermission(): ExactAlarmPermission {
    LocalExactAlarmPermission.current?.let { return it }
    val context = LocalContext.current
    return remember(context) {
        object : ExactAlarmPermission {
            override fun allowed() =
                app.tutora.reminders
                    .exactAllowed(context)

            override fun open() {
                // Before Android 12 exact alarms need no asking, so this row never shows there.
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}

/**
 * The bell on Hoje and the Agenda: opens Notificações with how many are unseen; with none, a dot
 * while the phone has Tutora's notifications off, read again on coming back (AC-014-32, 39, 40).
 */
@Composable
fun ReminderBell(
    nav: NavHostController,
    variant: app.tutora.designsystem.components.IconButtonVariant,
) {
    val services = LocalServices.current
    val permission = rememberNotificationPermission()
    var allowed by remember { mutableStateOf(permission.allowed()) }
    val unseen by remember { services.notices.unseen() }.collectAsState(initial = 0)
    androidx.lifecycle.compose.LifecycleResumeEffect(permission) {
        allowed = permission.allowed()
        onPauseOrDispose { }
    }
    app.tutora.designsystem.components.IconButton(
        IconName.BELL,
        stringResource(R.string.ntf_title),
        { nav.navigate(app.tutora.ui.NoticesHome) },
        Modifier.testTag("bell"),
        variant = variant,
        dot = !allowed,
        size = app.tutora.designsystem.components.ControlSize.SM,
        count = unseen,
    )
}

/** "Seg" / "Mon", for the row of days (AC-014-26). */
fun dayShort(
    day: DayOfWeek,
    language: Language,
): String =
    java.time.DayOfWeek
        .of(day.ordinal + 1)
        .getDisplayName(java.time.format.TextStyle.SHORT, localeOf(language))
        .removeSuffix(".")
        .replaceFirstChar { it.uppercase() }

/** "Segunda" / "Monday". */
fun dayName(
    day: DayOfWeek,
    language: Language,
): String =
    java.time.DayOfWeek
        .of(day.ordinal + 1)
        .getDisplayName(java.time.format.TextStyle.FULL, localeOf(language))
        .removeSuffix("-feira")
        .replaceFirstChar { it.uppercase() }

/** "15 min antes", "2h antes", "1h30 antes": how long before class (AC-014-12, 36). */
@Composable
fun remindText(minutes: Int): String =
    when {
        minutes < MINUTES_IN_HOUR -> stringResource(R.string.set_remindMinutes, minutes)
        minutes % MINUTES_IN_HOUR == 0 -> stringResource(R.string.set_remindHours, minutes / MINUTES_IN_HOUR)
        else -> stringResource(R.string.set_remindHoursMinutes, minutes / MINUTES_IN_HOUR, minutes % MINUTES_IN_HOUR)
    }

private const val MINUTES_IN_HOUR = 60

/** "Outro" among the choices: not a number of minutes. */
private const val OTHER = -1

/** "15 min antes · Quem te deve · Aulas para confirmar": what is on; "Desligado" when nothing is. */
@Composable
fun remindersSummary(s: app.tutora.domain.Settings): String =
    listOfNotNull(
        remindText(s.remindBefore).takeIf { s.remindBefore > 0 },
        stringResource(R.string.set_owedWeekly).takeIf { s.owedWeekly },
        stringResource(R.string.set_confirmDaily).takeIf { s.confirmDaily && s.pastClasses == PastClasses.ASK },
    ).joinToString(" · ").ifEmpty { stringResource(R.string.set_remindOff) }

/**
 * Lembretes: Aviso antes da aula, Quem te deve on its day and hour, and Aulas para confirmar at
 * its hour; turning one on asks for notifications once, and refused, says so (AC-014-12 to 16).
 */
@Composable
fun RemindersScreen(nav: NavHostController) {
    val services = LocalServices.current
    val language = currentLanguage()
    val permission = rememberNotificationPermission()
    val exact = rememberExactAlarmPermission()
    // Read again on coming back from the phone's settings.
    var exactAllowed by remember { mutableStateOf(exact.allowed()) }
    androidx.lifecycle.compose.LifecycleResumeEffect(exact) {
        exactAllowed = exact.allowed()
        onPauseOrDispose { }
    }
    val scope = rememberCoroutineScope()
    val settings by services.settings.collectAsState(initial = null)
    var allowed by remember { mutableStateOf(permission.allowed()) }
    // "Outro" open: an hh:mm field under the choices (AC-014-38).
    var other by remember { mutableStateOf<Boolean?>(null) }
    val s = settings ?: return

    fun save(next: app.tutora.domain.Settings) {
        val turnedOn =
            (next.remindBefore > 0 && s.remindBefore == 0) ||
                (next.owedWeekly && !s.owedWeekly) ||
                (next.confirmDaily && !s.confirmDaily)
        scope.launch { services.saveSettings(next) }
        if (turnedOn && !allowed) permission.request { allowed = it }
    }

    fun timeText(
        h: Int,
        m: Int,
    ) = clockTime(LocalTime(h, m), language)
    val anyOn = s.remindBefore > 0 || s.owedWeekly || (s.confirmDaily && s.pastClasses == PastClasses.ASK)
    ScreenScaffold(
        stringResource(R.string.set_reminders),
        "reminders-screen",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
    ) {
        if (anyOn && !allowed) {
            AlertBanner(
                stringResource(R.string.set_notificationsOff),
                Modifier.testTag("notifications-off"),
                tone = AlertTone.DANGER,
                value = stringResource(R.string.set_openPhoneSettings),
                onClick = { permission.openSettings() },
            )
        }
        // With the build's exact-alarm permission this never shows; otherwise one tap to allow it (AC-014-27).
        if (s.remindBefore > 0 && !exactAllowed) {
            AlertBanner(
                stringResource(R.string.set_exactTitle),
                Modifier.testTag("exact-off"),
                tone = AlertTone.SECONDARY,
                icon = IconName.CLOCK,
                value = stringResource(R.string.set_exactAction),
                onClick = { exact.open() },
            )
        }
        // Aviso antes da aula: one switch; on, the choices under it and Outro's field (AC-014-38).
        val custom = other ?: (s.remindBefore > 0 && s.remindBefore !in REMIND_BEFORE_CHOICES)
        ListGroup(
            listOfNotNull(
                {
                    ListRow(
                        stringResource(R.string.set_remindBefore),
                        subtitle = if (s.remindBefore > 0) remindText(s.remindBefore) else stringResource(R.string.set_remindOff),
                        icon = IconName.CLOCK,
                        trailing = RowTrailing.SWITCH,
                        switchValue = s.remindBefore > 0,
                        onSwitchChange = { on ->
                            other = null
                            save(s.copy(remindBefore = if (on) REMIND_BEFORE_DEFAULT else 0))
                        },
                    )
                },
                if (s.remindBefore > 0) {
                    {
                        Column(
                            Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2_5),
                            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
                        ) {
                            app.tutora.designsystem.components.SegmentedControl(
                                REMIND_BEFORE_CHOICES.filter { it > 0 }.map { it to stringResource(R.string.set_remindShort, it) } +
                                    (OTHER to stringResource(R.string.set_remindCustom)),
                                if (custom) OTHER else s.remindBefore,
                                { v ->
                                    if (v == OTHER) {
                                        other = true
                                    } else {
                                        other = false
                                        save(s.copy(remindBefore = v))
                                    }
                                },
                                stringResource(R.string.set_remindBefore),
                                Modifier.testTag("remind-choices"),
                                size = ControlSize.SM,
                            )
                            if (custom) {
                                DurationField(
                                    stringResource(R.string.set_remindHowLong),
                                    s.remindBefore,
                                    { v -> v?.takeIf { it in REMIND_MIN..REMIND_MAX }?.let { save(s.copy(remindBefore = it)) } },
                                    Modifier.testTag("remind-duration"),
                                    maxMinutes = REMIND_MAX,
                                    minMinutes = REMIND_MIN,
                                    step = 5,
                                )
                            }
                        }
                    }
                } else {
                    null
                },
            ),
        )
        ListGroup(
            listOfNotNull(
                {
                    ListRow(
                        stringResource(R.string.set_owedWeekly),
                        subtitle = stringResource(R.string.set_owedWhen, dayName(s.owedDay, language), timeText(s.owedHour, s.owedMinute)),
                        icon = IconName.WALLET,
                        trailing = RowTrailing.SWITCH,
                        switchValue = s.owedWeekly,
                        onSwitchChange = { save(s.copy(owedWeekly = it)) },
                    )
                },
                if (s.owedWeekly) {
                    {
                        Column(
                            Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2_5),
                            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
                        ) {
                            // The seven days in one row of short names (AC-014-26).
                            app.tutora.designsystem.components.SegmentedControl(
                                DayOfWeek.entries.map { it to dayShort(it, language) },
                                s.owedDay,
                                { save(s.copy(owedDay = it)) },
                                stringResource(R.string.set_owedWeekly),
                                Modifier.testTag("owed-days"),
                                size = ControlSize.SM,
                            )
                            // A clock time: the time picker (AC-014-37).
                            TimeField(stringResource(R.string.set_hour), s.owedHour, s.owedMinute, { h, m ->
                                save(s.copy(owedHour = h, owedMinute = m))
                            })
                        }
                    }
                } else {
                    null
                },
            ),
        )
        // With "Marcar como dadas sozinhas" there is nothing to confirm, so no notification (AC-014-14).
        if (s.pastClasses == PastClasses.ASK) {
            ListGroup(
                listOfNotNull(
                    {
                        ListRow(
                            stringResource(R.string.set_confirmDaily),
                            subtitle = stringResource(R.string.set_confirmDailyWhen, timeText(s.confirmHour, s.confirmMinute)),
                            icon = IconName.CALENDAR_CHECK,
                            trailing = RowTrailing.SWITCH,
                            switchValue = s.confirmDaily,
                            onSwitchChange = { save(s.copy(confirmDaily = it)) },
                        )
                    },
                    if (s.confirmDaily) {
                        {
                            Column(Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2_5)) {
                                TimeField(stringResource(R.string.set_hour), s.confirmHour, s.confirmMinute, { h, m ->
                                    save(s.copy(confirmHour = h, confirmMinute = m))
                                })
                            }
                        }
                    } else {
                        null
                    },
                ),
            )
        }
    }
}
