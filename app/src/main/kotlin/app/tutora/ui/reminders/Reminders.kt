package app.tutora.ui.reminders

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.AlertBanner
import app.tutora.designsystem.components.AlertTone
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.Stepper
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Language
import app.tutora.domain.PastClasses
import app.tutora.domain.REMIND_BEFORE_CHOICES
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

/** "15 min antes" / "Desligado", Ajustes' line for Lembretes. */
@Composable
fun remindersSummary(s: app.tutora.domain.Settings): String =
    if (s.remindBefore > 0) {
        stringResource(R.string.set_remindMinutes, s.remindBefore)
    } else {
        stringResource(R.string.set_remindOff)
    }

/**
 * Lembretes: Aviso antes da aula, Quem te deve on its day and hour, and Aulas para confirmar at
 * its hour; turning one on asks for notifications once, and refused, says so (AC-014-12 to 16).
 */
@Composable
fun RemindersScreen(nav: NavHostController) {
    val services = LocalServices.current
    val language = currentLanguage()
    val permission = rememberNotificationPermission()
    val scope = rememberCoroutineScope()
    val settings by services.settings.collectAsState(initial = null)
    var allowed by remember { mutableStateOf(permission.allowed()) }
    val s = settings ?: return

    fun save(next: app.tutora.domain.Settings) {
        val turnedOn =
            (next.remindBefore > 0 && s.remindBefore == 0) ||
                (next.owedWeekly && !s.owedWeekly) ||
                (next.confirmDaily && !s.confirmDaily)
        scope.launch { services.saveSettings(next) }
        if (turnedOn && !allowed) permission.request { allowed = it }
    }

    fun hourText(h: Long) = clockTime(LocalTime(h.toInt(), 0), language)
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
        ListGroup(
            title = stringResource(R.string.set_remindBefore),
            rows =
                listOf {
                    FlowRow(
                        Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2_5),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
                    ) {
                        REMIND_BEFORE_CHOICES.forEach { m ->
                            Chip(
                                if (m == 0) stringResource(R.string.set_remindOff) else stringResource(R.string.set_remindMinutes, m),
                                s.remindBefore == m,
                                { save(s.copy(remindBefore = m)) },
                            )
                        }
                    }
                },
        )
        ListGroup(
            listOfNotNull(
                {
                    ListRow(
                        stringResource(R.string.set_owedWeekly),
                        subtitle = stringResource(R.string.set_owedWhen, dayName(s.owedDay, language), hourText(s.owedHour.toLong())),
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
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
                            ) {
                                DayOfWeek.entries.forEach { d ->
                                    Chip(dayName(d, language), s.owedDay == d, { save(s.copy(owedDay = d)) })
                                }
                            }
                            HourRow(s.owedHour, ::hourText) { save(s.copy(owedHour = it)) }
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
                            subtitle = stringResource(R.string.set_confirmDailyWhen, hourText(s.confirmHour.toLong())),
                            icon = IconName.CALENDAR_CHECK,
                            trailing = RowTrailing.SWITCH,
                            switchValue = s.confirmDaily,
                            onSwitchChange = { save(s.copy(confirmDaily = it)) },
                        )
                    },
                    if (s.confirmDaily) {
                        {
                            Column(Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2_5)) {
                                HourRow(s.confirmHour, ::hourText) { save(s.copy(confirmHour = it)) }
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

@Composable
private fun HourRow(
    hour: Int,
    text: (Long) -> String,
    onChange: (Int) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.set_hour),
            style = TutoraTheme.type.body,
            color = TutoraTheme.colors.ink,
            modifier = Modifier.weight(1f),
        )
        Stepper(hour.toLong(), { onChange(it.toInt()) }, step = 1, min = 0, max = LAST_HOUR, format = text, size = ControlSize.SM)
    }
}

private const val LAST_HOUR = 23L
