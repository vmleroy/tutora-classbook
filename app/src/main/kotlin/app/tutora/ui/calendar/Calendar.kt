package app.tutora.ui.calendar

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.adapters.export.LocalExports
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.AlertBanner
import app.tutora.designsystem.components.AlertTone
import app.tutora.designsystem.components.BadgeTone
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.Tag
import app.tutora.designsystem.components.currentLanguage
import app.tutora.domain.CalendarState
import app.tutora.domain.localOf
import app.tutora.state.LocalServices
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.clockTime
import app.tutora.usecases.CalendarTarget
import app.tutora.usecases.PhoneCalendarInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Calendário (spec 012, Calendar.dc.html): the classes of the next 8 weeks into one calendar of
// the phone, busy time on the Agenda, the .ics export and cleaning up Tutora's events.

/** The calendar permission and the phone's settings; tests answer for the system. */
interface CalendarPermission {
    fun request(onResult: (Boolean) -> Unit)

    fun openSettings()
}

val LocalCalendarPermission = staticCompositionLocalOf<CalendarPermission?> { null }

@Composable
private fun rememberCalendarPermission(): CalendarPermission {
    LocalCalendarPermission.current?.let { return it }
    val context = LocalContext.current
    var pending by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            pending?.invoke(granted.values.all { it })
            pending = null
        }
    return remember(launcher) {
        object : CalendarPermission {
            override fun request(onResult: (Boolean) -> Unit) {
                pending = onResult
                launcher.launch(arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR))
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

/** "10:42", the time of the last update. */
@Composable
private fun syncedTime(s: CalendarState): String {
    val services = LocalServices.current
    return if (s.syncedAt.isEmpty()) "" else clockTime(localOf(s.syncedAt, services.zone).time, currentLanguage())
}

/** Ajustes › Geral › Calendário, with the calendar in use or Desligado (AC-012-1). */
@Composable
fun CalendarSettingsRow(onClick: () -> Unit) {
    val services = LocalServices.current
    val state by remember { services.calendar.observe() }.collectAsState(initial = null)
    val s = state ?: return
    ListRow(
        stringResource(R.string.cal_screen),
        Modifier.testTag("calendar-row"),
        subtitle = if (s.on) stringResource(R.string.cal_rowOn, s.calendarName, syncedTime(s)) else stringResource(R.string.cal_off),
        icon = IconName.CALENDAR,
        onClick = onClick,
    )
}

private enum class Notice { NO_ACCESS, NONE_TO_CLEAN }

@Composable
fun CalendarScreen(nav: NavHostController) {
    val services = LocalServices.current
    val exports = LocalExports.current
    val permission = rememberCalendarPermission()
    val scope = rememberCoroutineScope()
    val state by remember { services.calendar.observe() }.collectAsState(initial = null)
    var notice by rememberSaveable { mutableStateOf<Notice?>(null) }
    var calendars by remember { mutableStateOf<List<PhoneCalendarInfo>>(emptyList()) }
    var access by remember { mutableIntStateOf(0) }
    var cleaning by rememberSaveable { mutableStateOf<Int?>(null) }
    // Before sync is on, the calendar chosen to start with: Tutora's own, or an account's (AC-012-3).
    var pickedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val ownName = stringResource(R.string.cal_ownName)
    val ownColor = TutoraTheme.colors.btnPrimary.toArgb()
    val icsName = stringResource(R.string.cal_icsFile)
    LaunchedEffect(access) { calendars = withContext(Dispatchers.IO) { services.calendar.calendars() } }
    val s = state ?: return

    fun target(id: Long?): CalendarTarget =
        calendars.firstOrNull { it.id == id }?.let { CalendarTarget.Account(it) } ?: CalendarTarget.Own(ownName, ownColor)

    /** Runs [then] with the calendar permission, asking once; refused, it says so (AC-012-2). */
    fun withAccess(then: () -> Unit) {
        notice = null
        permission.request { granted ->
            if (granted) {
                access++
                then()
            } else {
                notice = Notice.NO_ACCESS
            }
        }
    }

    fun turnOn(id: Long?) =
        withAccess {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { runCatching { services.calendar.turnOn(target(id)) }.getOrDefault(false) }
                if (!ok) notice = Notice.NO_ACCESS
            }
        }

    cleaning?.let { count ->
        BottomSheet(
            pluralStringResource(R.plurals.cal_cleanTitle, count, count),
            { cleaning = null },
            subtitle = stringResource(R.string.cal_cleanBody),
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.catalog_cancel), { cleaning = null }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.cal_delete), {
                            cleaning = null
                            scope.launch { withContext(Dispatchers.IO) { runCatching { services.calendar.clean() } } }
                        }, m, variant = ButtonVariant.DANGER)
                    },
                ),
        )
    }

    ScreenScaffold(stringResource(R.string.cal_screen), "calendar-screen", variant = HeaderVariant.PUSH, onBack = { nav.popBackStack() }) {
        when (notice) {
            Notice.NO_ACCESS ->
                AlertBanner(
                    stringResource(R.string.cal_noAccess),
                    Modifier.testTag("calendar-no-access"),
                    value = stringResource(R.string.cal_openSettings),
                    onClick = { permission.openSettings() },
                )
            Notice.NONE_TO_CLEAN ->
                AlertBanner(
                    stringResource(R.string.cal_cleanNone),
                    Modifier.testTag("calendar-none"),
                    tone = AlertTone.SECONDARY,
                    icon = IconName.CHECK,
                )
            null -> Unit
        }
        // Enviar aulas para o calendário (AC-012-2, 10).
        val status =
            when {
                !s.on -> stringResource(R.string.cal_off)
                s.syncedAt.isEmpty() -> stringResource(R.string.cal_updating)
                else -> pluralStringResource(R.plurals.cal_status, s.count, syncedTime(s), s.count)
            }
        ListGroup(
            listOf {
                ListRow(
                    stringResource(R.string.cal_send),
                    subtitle = status,
                    icon = IconName.CALENDAR_CHECK,
                    trailing = RowTrailing.SWITCH,
                    switchValue = s.on,
                    onSwitchChange = { on ->
                        if (on) {
                            turnOn(
                                pickedId,
                            )
                        } else {
                            scope.launch { withContext(Dispatchers.IO) { runCatching { services.calendar.turnOff() } } }
                        }
                    },
                )
            },
        )
        // Em qual calendário? Tutora's own on the phone, or one of an account's (AC-012-3, 12).
        val inUse = if (s.on) (if (s.own) null else s.calendarId) else pickedId
        val inUseLabel = stringResource(R.string.cal_inUse)

        fun choose(id: Long?) {
            if (s.on) turnOn(id) else pickedId = id
        }
        ListGroup(
            title = stringResource(R.string.cal_which),
            rows =
                listOf<@Composable () -> Unit> {
                    ListRow(
                        stringResource(R.string.cal_own),
                        Modifier.testTag("calendar-own"),
                        subtitle = stringResource(R.string.cal_ownSub),
                        avatar = "Tutora" to Tag.GREEN,
                        trailing = RowTrailing.NONE,
                        badge = if (inUse == null) BadgeTone.SUCCESS to inUseLabel else null,
                        onClick = { choose(null) },
                    )
                } +
                    calendars.map { cal ->
                        @Composable {
                            ListRow(
                                cal.name,
                                Modifier.testTag("calendar-${cal.id}"),
                                subtitle = listOf(cal.provider, cal.account).filter { it.isNotBlank() }.joinToString(" · "),
                                avatar = (cal.provider.ifBlank { cal.name }) to Tag.BLUE,
                                trailing = RowTrailing.NONE,
                                badge = if (inUse == cal.id) BadgeTone.SUCCESS to inUseLabel else null,
                                onClick = { if (calendars.isEmpty()) withAccess {} else choose(cal.id) },
                            )
                        }
                    },
        )
        // The two options (AC-012-7, 17).
        ListGroup(
            listOf(
                {
                    ListRow(
                        stringResource(R.string.cal_busy),
                        subtitle = stringResource(R.string.cal_busySub),
                        icon = IconName.CLOCK,
                        trailing = RowTrailing.SWITCH,
                        switchValue = s.showBusy,
                        onSwitchChange = { on ->
                            val save = {
                                scope.launch {
                                    withContext(
                                        Dispatchers.IO,
                                    ) { runCatching { services.calendar.setOptions(s.includeValue, on) } }
                                }
                            }
                            if (on) withAccess { save() } else save()
                        },
                    )
                },
                {
                    ListRow(
                        stringResource(R.string.cal_withValue),
                        subtitle = stringResource(R.string.cal_withValueSub),
                        icon = IconName.WALLET,
                        trailing = RowTrailing.SWITCH,
                        switchValue = s.includeValue,
                        onSwitchChange = { on ->
                            scope.launch { withContext(Dispatchers.IO) { runCatching { services.calendar.setOptions(on, s.showBusy) } } }
                        },
                    )
                },
            ),
        )
        Text(stringResource(R.string.cal_note), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
        // .ics, with sync on or off and no permission (AC-012-20).
        Button(stringResource(R.string.cal_ics), {
            scope.launch {
                val text = withContext(Dispatchers.IO) { runCatching { services.calendar.ics() }.getOrNull() } ?: return@launch
                exports.share(icsName, "text/calendar", text.encodeToByteArray())
            }
        }, fullWidth = true, variant = ButtonVariant.OUTLINE, icon = IconName.FILE)
        // Limpar eventos do Tutora: counts first, asks, deletes only marked events (AC-012-15, 16).
        Button(stringResource(R.string.cal_clean), {
            withAccess {
                scope.launch {
                    val count = withContext(Dispatchers.IO) { runCatching { services.calendar.cleanCount() }.getOrDefault(0) }
                    if (count == 0) notice = Notice.NONE_TO_CLEAN else cleaning = count
                }
            }
        }, fullWidth = true, variant = ButtonVariant.GHOST, icon = IconName.TRASH)
    }
}
