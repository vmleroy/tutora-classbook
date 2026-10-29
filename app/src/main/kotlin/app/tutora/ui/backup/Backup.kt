package app.tutora.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.adapters.export.Cell
import app.tutora.adapters.export.LocalExports
import app.tutora.adapters.export.MIME_XLSX
import app.tutora.adapters.export.Sheet
import app.tutora.adapters.export.Xlsx
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.AlertBanner
import app.tutora.designsystem.components.AlertTone
import app.tutora.designsystem.components.BadgeTone
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.DateField
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.Stepper
import app.tutora.designsystem.components.TileTone
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.BACKUP_KEEP_MAX
import app.tutora.domain.BACKUP_KEEP_MIN
import app.tutora.domain.BackupEvery
import app.tutora.domain.BackupFile
import app.tutora.domain.BackupLogEntry
import app.tutora.domain.BackupRead
import app.tutora.domain.BackupState
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.Language
import app.tutora.domain.PayMethod
import app.tutora.domain.PlaceKind
import app.tutora.domain.SheetRange
import app.tutora.domain.backupSummary
import app.tutora.domain.backupWarningDays
import app.tutora.domain.instantOf
import app.tutora.domain.isoText
import app.tutora.domain.localOf
import app.tutora.domain.nextAutoBackup
import app.tutora.state.LocalServices
import app.tutora.ui.BackupHome
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.TodayGraph
import app.tutora.ui.countText
import app.tutora.ui.kindName
import app.tutora.ui.localeOf
import app.tutora.ui.payments.methodName
import app.tutora.ui.reports.rememberReportWords
import app.tutora.usecases.RestoreResult
import app.tutora.usecases.SheetData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import java.time.format.DateTimeFormatter

// Backup e planilha (spec 011, Backup.dc.html): automatic backup to a folder, a backup or a
// spreadsheet made now, restore, and the latest backups.

/** The system's folder and file pickers; tests provide their own answers. */
interface BackupPicker {
    /** Asks for a folder; null when the teacher cancels (AC-011-5). */
    fun folder(onPicked: (String?) -> Unit)

    /** Asks for a backup file; null when cancelled (AC-011-12). */
    fun file(onPicked: (String?) -> Unit)
}

val LocalBackupPicker = staticCompositionLocalOf<BackupPicker?> { null }

@Composable
private fun rememberBackupPicker(): BackupPicker {
    LocalBackupPicker.current?.let { return it }
    var pending by remember { mutableStateOf<((String?) -> Unit)?>(null) }
    val folder =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            pending?.invoke(uri?.toString())
            pending = null
        }
    val file =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            pending?.invoke(uri?.toString())
            pending = null
        }
    return remember(folder, file) {
        object : BackupPicker {
            override fun folder(onPicked: (String?) -> Unit) {
                pending = onPicked
                folder.launch(null)
            }

            override fun file(onPicked: (String?) -> Unit) {
                pending = onPicked
                file.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
            }
        }
    }
}

private fun format(
    p: String,
    language: Language,
    t: LocalDateTime,
) = DateTimeFormatter.ofPattern(p, localeOf(language)).format(t.toJavaLocalDateTime()).replace(".", "")

/** "dom, 1 nov, 20:00" / "Sun, Nov 1, 20:00". */
private fun whenText(
    t: LocalDateTime,
    language: Language,
) = format(if (language == Language.PT_BR) "EEE, d MMM, HH:mm" else "EEE, MMM d, HH:mm", language, t)

/** "Dom, 25 out · 20:00", for Recentes. */
private fun recentText(
    t: LocalDateTime,
    language: Language,
) = format(if (language == Language.PT_BR) "EEE, d MMM · HH:mm" else "EEE, MMM d · HH:mm", language, t).replaceFirstChar { it.uppercase() }

/** "hoje", "ontem" or "25/10", for the last backup. */
@Composable
private fun lastText(
    iso: String,
    today: LocalDate,
): String {
    val services = LocalServices.current
    val language = currentLanguage()
    val day = localOf(iso, services.zone)
    return when (day.date.daysUntil(today)) {
        0 -> stringResource(R.string.bk_today)
        1 -> stringResource(R.string.bk_yesterday)
        else -> format(if (language == Language.PT_BR) "dd/MM" else "MM/dd", language, day)
    }
}

@Composable
private fun everyLower(every: BackupEvery) =
    stringResource(
        when (every) {
            BackupEvery.DAY -> R.string.bk_everyDayLower
            BackupEvery.WEEK -> R.string.bk_everyWeekLower
            BackupEvery.MONTH -> R.string.bk_everyMonthLower
        },
    )

@Composable
private fun today(): LocalDate {
    val services = LocalServices.current
    return localOf(services.now(), services.zone).date
}

/** Days without a backup, once it is 14 or more (AC-011-10). */
@Composable
private fun warningDays(s: BackupState): Int? {
    val services = LocalServices.current
    val first by remember { services.backups.firstLessonAt() }.collectAsState(initial = null)
    return backupWarningDays(
        s.lastBackupAt.ifEmpty { null }?.let(::instantOf),
        first?.let(::instantOf),
        instantOf(services.now()),
        services.zone,
    )
}

/** Ajustes › Seus dados › Backup e planilha, with its state or its warning in red (AC-011-1, 9, 10). */
@Composable
fun BackupSettingsRow(onClick: () -> Unit) {
    val services = LocalServices.current
    val state by remember { services.backups.observe() }.collectAsState(initial = null)
    val s = state ?: return
    val days = warningDays(s)
    val today = today()
    val subtitle =
        when {
            s.auto && s.failed -> stringResource(R.string.bk_folderFailed)
            days != null -> pluralStringResource(R.plurals.bk_warnRow, days, days)
            s.auto && s.lastBackupAt.isNotEmpty() ->
                stringResource(
                    R.string.bk_rowAuto,
                    everyLower(s.every),
                    lastText(s.lastBackupAt, today),
                )
            s.auto -> stringResource(R.string.bk_rowAutoNever, everyLower(s.every))
            s.lastBackupAt.isNotEmpty() -> stringResource(R.string.bk_rowManual, lastText(s.lastBackupAt, today))
            else -> stringResource(R.string.bk_none)
        }
    ListRow(
        stringResource(R.string.bk_screen),
        Modifier.testTag("backup-row"),
        subtitle = subtitle,
        icon = IconName.CLOUD,
        iconTone = if (days != null || (s.auto && s.failed)) TileTone.DANGER else TileTone.SECONDARY,
        onClick = onClick,
    )
}

/** Hoje's alert when backups stopped: "Sem backup há 16 dias" opens Backup e planilha (AC-011-10). */
@Composable
fun BackupAlert(nav: NavHostController) {
    val services = LocalServices.current
    val state by remember { services.backups.observe() }.collectAsState(initial = null)
    val days = state?.let { warningDays(it) } ?: return
    AlertBanner(
        pluralStringResource(R.plurals.bk_warnHome, days, days),
        Modifier.testTag("backup-alert"),
        icon = IconName.CLOUD,
        onClick = { nav.navigate(BackupHome) },
    )
}

/** The words of the full spreadsheet, in the app language (AC-011-19, 21). */
data class SheetWords(
    val classes: String,
    val payments: String,
    val students: String,
    val places: String,
    val summary: String,
    val date: String,
    val minutes: String,
    val who: String,
    val place: String,
    val situation: String,
    val value: String,
    val method: String,
    val amount: String,
    val name: String,
    val subject: String,
    val phone: String,
    val archived: String,
    val yes: String,
    val kind: String,
    val month: String,
    val earned: String,
    val received: String,
    val toReceive: String,
    val status: (String) -> String,
    val methodName: (PayMethod) -> String,
    val kindName: (PlaceKind) -> String,
    val builtinName: (BuiltinPlace) -> String,
    val monthName: (LocalDate) -> String,
)

@Composable
private fun rememberSheetWords(): SheetWords {
    val w = rememberReportWords()
    val statuses =
        mapOf(
            "scheduled" to stringResource(R.string.status_scheduled),
            "done" to stringResource(R.string.status_done),
            "cancelled" to stringResource(R.string.status_cancelled),
            "missed" to stringResource(R.string.status_missed),
            "live" to stringResource(R.string.status_live),
        )
    val methods = PayMethod.entries.associateWith { methodName(it) }
    val kinds = PlaceKind.entries.associateWith { kindName(it) }
    val builtins =
        mapOf(
            BuiltinPlace.STUDENT_HOME to stringResource(R.string.places_builtin_student_home),
            BuiltinPlace.ONLINE to stringResource(R.string.places_builtin_online),
        )
    return SheetWords(
        classes = w.sheetClasses,
        payments = w.sheetPayments,
        students = stringResource(R.string.bk_sheetStudents),
        places = stringResource(R.string.bk_sheetPlaces),
        summary = w.sheetSummary,
        date = w.date,
        minutes = w.duration,
        who = stringResource(R.string.bk_who),
        place = w.where,
        situation = w.situation,
        value = w.value,
        method = w.method,
        amount = w.amount,
        name = stringResource(R.string.bk_name),
        subject = stringResource(R.string.bk_subject),
        phone = stringResource(R.string.bk_phone),
        archived = stringResource(R.string.bk_archived),
        yes = stringResource(R.string.bk_yes),
        kind = stringResource(R.string.bk_kind),
        month = stringResource(R.string.bk_month),
        earned = w.earned,
        received = w.received,
        toReceive = w.toReceive,
        status = { statuses[it] ?: it },
        methodName = { methods.getValue(it) },
        kindName = { kinds.getValue(it) },
        builtinName = { builtins.getValue(it) },
        monthName = w.month,
    )
}

private fun day(d: LocalDate) = Cell.Day(d.year, d.month.ordinal + 1, d.day)

/** Aulas, Pagamentos, Alunos, Locais and Resumo; dates as dates, amounts as numbers (AC-011-19). */
fun backupSheets(
    d: SheetData,
    w: SheetWords,
): List<Sheet> {
    fun placeName(id: String?): String {
        val p = d.places.firstOrNull { it.id == id } ?: return ""
        return BuiltinPlace.entries.firstOrNull { it.id == p.id }?.let(w.builtinName) ?: p.name.orEmpty()
    }
    return listOf(
        Sheet(
            w.classes,
            listOf(w.date, w.minutes, w.who, w.place, w.situation, w.value),
            d.classes.map {
                listOf(
                    day(it.day),
                    Cell.Num(it.minutes.toLong()),
                    Cell.Text(it.who),
                    Cell.Text(placeName(it.placeId)),
                    Cell.Text(w.status(it.status)),
                    it.cents?.let { c -> Cell.Money(c) } ?: Cell.Text(""),
                )
            },
        ),
        Sheet(
            w.payments,
            listOf(w.date, w.who, w.method, w.amount),
            d.payments.map {
                listOf(day(it.day), Cell.Text(it.who), Cell.Text(it.method?.let(w.methodName).orEmpty()), Cell.Money(it.cents))
            },
        ),
        Sheet(
            w.students,
            listOf(w.name, w.subject, w.phone, w.archived),
            d.students.map {
                listOf(
                    Cell.Text(it.name),
                    Cell.Text(it.subject.orEmpty()),
                    Cell.Text(it.phone.orEmpty()),
                    Cell.Text(if (it.archived) w.yes else ""),
                )
            },
        ),
        Sheet(
            w.places,
            listOf(w.name, w.kind),
            d.places.map { listOf(Cell.Text(placeName(it.id)), Cell.Text(w.kindName(PlaceKind.fromKey(it.kind)))) },
        ),
        Sheet(
            w.summary,
            listOf(w.month, w.earned, w.received, w.toReceive),
            d.months.map {
                listOf(
                    Cell.Text(w.monthName(it.month)),
                    Cell.Money(it.earned),
                    Cell.Money(it.received),
                    Cell.Money(it.toReceive),
                )
            },
        ),
    )
}

/** "Tutora planilha outubro 2026.xlsx", or the dates when the period isn't one month (AC-011-19). */
fun sheetFileName(
    base: String,
    from: LocalDate,
    to: LocalDate,
    language: Language,
): String {
    fun p(
        pattern: String,
        d: LocalDate,
    ) = DateTimeFormatter.ofPattern(pattern, localeOf(language)).format(d.toJavaLocalDate())
    val period =
        if (from.year == to.year && from.month == to.month) {
            p("LLLL yyyy", from)
        } else {
            "${p("dd-MM-yyyy", from)} ${if (language == Language.PT_BR) "a" else "to"} ${p("dd-MM-yyyy", to)}"
        }
    return "$base $period.xlsx"
}

private enum class Problem { NOT_TUTORA, NEWER, LIVE, GONE }

/** After a restore every screen's state belongs to the old data: start again on Hoje (AC-011-14). */
private fun NavHostController.reopenOnToday() =
    navigate(TodayGraph) {
        popUpTo(this@reopenOnToday.graph.id) { inclusive = true }
    }

@Composable
fun BackupScreen(nav: NavHostController) {
    val services = LocalServices.current
    val exports = LocalExports.current
    val picker = rememberBackupPicker()
    val language = currentLanguage()
    val scope = rememberCoroutineScope()
    val state by remember { services.backups.observe() }.collectAsState(initial = null)
    var problem by rememberSaveable { mutableStateOf<Problem?>(null) }
    var confirming by remember { mutableStateOf<BackupFile?>(null) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var savedIn by rememberSaveable { mutableStateOf<String?>(null) }
    var removing by remember { mutableStateOf<BackupLogEntry?>(null) }
    var opened by remember { mutableStateOf<BackupLogEntry?>(null) }
    val words = rememberSheetWords()
    val sheetBase = stringResource(R.string.bk_sheetFile)
    val s = state ?: return
    val today = today()

    fun setAuto(next: BackupState) =
        scope.launch {
            withContext(Dispatchers.IO) { services.backups.setAuto(next.auto, next.every, next.keep, next.folderUri, next.folderName) }
        }

    // Picking a folder turns automatic backup on; cancelling leaves it as it was (AC-011-5).
    fun pickFolder(turnOn: Boolean = false) =
        picker.folder { uri ->
            if (uri != null) {
                scope.launch {
                    val name = withContext(Dispatchers.IO) { runCatching { services.ctx.backupFolder.adopt(uri) }.getOrDefault("") }
                    if (turnOn) {
                        setAuto(s.copy(auto = true, folderUri = uri, folderName = name))
                    } else {
                        withContext(Dispatchers.IO) { services.backups.setFolder(uri, name) }
                    }
                }
            }
        }

    fun restoreFromFile() {
        problem = null
        picker.file { uri ->
            if (uri != null) {
                scope.launch {
                    val bytes = withContext(Dispatchers.IO) { services.ctx.backupFolder.read(uri) } ?: ByteArray(0)
                    when (val read = services.backups.inspect(bytes)) {
                        is BackupRead.Ok -> confirming = read.file
                        is BackupRead.Newer -> problem = Problem.NEWER
                        BackupRead.NotTutora -> problem = Problem.NOT_TUTORA
                    }
                }
            }
        }
    }

    fun restoreRecent(e: BackupLogEntry) {
        problem = null
        scope.launch {
            when (val read = withContext(Dispatchers.IO) { services.backups.fromRecent(e) }) {
                is BackupRead.Ok -> confirming = read.file
                is BackupRead.Newer -> problem = Problem.NEWER
                BackupRead.NotTutora -> problem = Problem.NOT_TUTORA
                null -> problem = Problem.GONE
            }
        }
    }

    fun shareRecent(e: BackupLogEntry) {
        problem = null
        scope.launch {
            val bytes = withContext(Dispatchers.IO) { services.backups.fileOf(e) }
            if (bytes == null) problem = Problem.GONE else exports.share(e.name, "application/json", bytes)
        }
    }

    fun share() =
        scope.launch {
            val (name, bytes) = withContext(Dispatchers.IO) { services.backups.makeBackup() }
            exports.share(name, "application/json", bytes)
        }

    // A backup from Recentes: what can be done with it (AC-011-11).
    opened?.let { e ->
        // Shared backups keep a copy in the app; ones in the folder are read from it.
        val hasFile = e.ok && e.name.isNotEmpty() && (e.where.isEmpty() || s.folderUri.isNotEmpty())
        BottomSheet(
            recentText(localOf(e.at, services.zone), language),
            { opened = null },
            subtitle =
                when {
                    !e.ok -> stringResource(R.string.bk_detailFailed)
                    hasFile -> recentLine(e)
                    else -> stringResource(R.string.bk_detailNoCopy)
                },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                if (hasFile) {
                    Button(stringResource(R.string.bk_confirm), {
                        opened = null
                        restoreRecent(e)
                    }, fullWidth = true)
                    Button(stringResource(R.string.bk_shareThis), {
                        opened = null
                        shareRecent(e)
                    }, fullWidth = true, variant = ButtonVariant.OUTLINE)
                } else if (e.ok) {
                    Button(stringResource(R.string.bk_findFile), {
                        opened = null
                        restoreFromFile()
                    }, fullWidth = true, variant = ButtonVariant.OUTLINE)
                }
                Button(stringResource(R.string.bk_remove), {
                    opened = null
                    removing = e
                }, fullWidth = true, variant = ButtonVariant.GHOST)
            }
        }
    }

    // Apagar a backup from Recentes, always confirmed (AC-011-11).
    removing?.let { e ->
        val inFolder = e.name.isNotEmpty() && e.where.isNotEmpty() && s.folderUri.isNotEmpty()
        BottomSheet(
            stringResource(R.string.bk_removeTitle, recentText(localOf(e.at, services.zone), language)),
            { removing = null },
            subtitle = if (inFolder) stringResource(R.string.bk_removeFolder, s.folderName) else stringResource(R.string.bk_removeShared),
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.catalog_cancel), { removing = null }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.bk_remove), {
                            removing = null
                            scope.launch { withContext(Dispatchers.IO) { services.backups.remove(e) } }
                        }, m, variant = ButtonVariant.DANGER)
                    },
                ),
        )
    }

    confirming?.let { file ->
        val sum = backupSummary(file)
        val at = runCatching { localOf(sum.exportedAt, services.zone) }.getOrNull()
        BottomSheet(
            stringResource(R.string.bk_confirmTitle, at?.let { format("dd/MM/yyyy, HH:mm", language, it) }.orEmpty()),
            { confirming = null },
            subtitle =
                stringResource(
                    R.string.bk_confirmBody,
                    countText(R.string.bk_studentsNone, R.plurals.bk_students, sum.students),
                    countText(R.string.bk_lessonsNone, R.plurals.bk_lessons, sum.lessons),
                    countText(R.string.bk_paymentsNone, R.plurals.bk_payments, sum.payments),
                ),
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.catalog_cancel), { confirming = null }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.bk_confirm), {
                            confirming = null
                            scope.launch {
                                when (withContext(Dispatchers.IO) { services.backups.restore(file) }) {
                                    RestoreResult.LiveClass -> problem = Problem.LIVE
                                    // The restored data opens on Hoje (AC-011-14).
                                    else -> nav.reopenOnToday()
                                }
                            }
                        }, m, variant = ButtonVariant.DANGER)
                    },
                ),
        )
    }

    if (sheetOpen) {
        SheetPeriodSheet(
            onDismiss = { sheetOpen = false },
            onGenerate = { range ->
                sheetOpen = false
                scope.launch {
                    val data = withContext(Dispatchers.IO) { services.backups.sheetData(range) }
                    exports.share(sheetFileName(sheetBase, data.from, data.to, language), MIME_XLSX, Xlsx.write(backupSheets(data, words)))
                }
            },
        )
    }

    ScreenScaffold(stringResource(R.string.bk_screen), "backup-screen", variant = HeaderVariant.PUSH, onBack = { nav.popBackStack() }) {
        problem?.let {
            AlertBanner(
                stringResource(
                    when (it) {
                        Problem.NOT_TUTORA -> R.string.bk_notTutora
                        Problem.NEWER -> R.string.bk_newer
                        Problem.LIVE -> R.string.bk_live
                        Problem.GONE -> R.string.bk_gone
                    },
                ),
                Modifier.testTag("backup-problem"),
            )
        }
        if (s.failed && s.folderUri.isNotEmpty()) {
            AlertBanner(stringResource(R.string.bk_folderFailed), Modifier.testTag("backup-failed"), onClick = { pickFolder() })
        }
        savedIn?.let {
            AlertBanner(
                stringResource(R.string.bk_savedIn, it),
                Modifier.testTag("backup-saved"),
                tone = AlertTone.SUCCESS,
                icon = IconName.CHECK,
            )
        }
        // Backup automático (AC-011-5 to 7).
        val next = nextAutoBackup(instantOf(services.now()), s.every, services.zone)
        val nextText = whenText(localOf(next.isoText(), services.zone), language)
        val autoLine =
            when {
                !s.auto -> stringResource(R.string.bk_autoOff)
                s.lastBackupAt.isEmpty() -> stringResource(R.string.bk_autoNextFirst, nextText)
                else -> stringResource(R.string.bk_autoNext, nextText, lastText(s.lastBackupAt, today))
            }
        ListGroup(
            listOfNotNull<@Composable () -> Unit>(
                {
                    ListRow(
                        stringResource(R.string.bk_auto),
                        Modifier.testTag("backup-auto"),
                        subtitle = autoLine,
                        icon = IconName.CLOUD,
                        trailing = RowTrailing.SWITCH,
                        switchValue = s.auto,
                        onSwitchChange = { on ->
                            if (on && s.folderUri.isEmpty()) pickFolder(turnOn = true) else setAuto(s.copy(auto = on))
                        },
                    )
                },
                // The folder can be chosen or changed at any time, automatic backup on or off (AC-011-5).
                {
                    if (s.folderUri.isEmpty()) {
                        ListRow(
                            stringResource(R.string.bk_chooseFolder),
                            Modifier.testTag("backup-folder"),
                            subtitle = stringResource(R.string.bk_chooseFolderHint),
                            icon = IconName.FILE,
                            onClick = { pickFolder() },
                        )
                    } else {
                        ListRow(
                            stringResource(R.string.bk_saveTo, s.folderName),
                            Modifier.testTag("backup-folder"),
                            subtitle = stringResource(R.string.bk_change),
                            icon = IconName.FILE,
                            onClick = { pickFolder() },
                        )
                    }
                },
                // How many stay in the folder, at most five (AC-011-7).
                if (s.folderUri.isNotEmpty()) {
                    { KeepRow(s.keep) { setAuto(s.copy(keep = it)) } }
                } else {
                    null
                },
            ),
        )
        if (s.auto) {
            SegmentedControl(
                listOf(
                    BackupEvery.DAY to stringResource(R.string.bk_everyDay),
                    BackupEvery.WEEK to stringResource(R.string.bk_everyWeek),
                    BackupEvery.MONTH to stringResource(R.string.bk_everyMonth),
                ),
                s.every,
                { setAuto(s.copy(every = it)) },
                stringResource(R.string.bk_every),
            )
        }
        Text(stringResource(R.string.bk_hint), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)

        // Fazer agora (AC-011-2, 12, 16, 19).
        Text(stringResource(R.string.bk_now), style = TutoraTheme.type.section, color = TutoraTheme.colors.inkMuted)
        // With a folder, the backup is saved there, easy to find; without one, it goes to the share sheet (AC-011-2).
        Button(stringResource(R.string.bk_full), {
            savedIn = null
            if (s.folderUri.isEmpty()) {
                share()
            } else {
                scope.launch { savedIn = withContext(Dispatchers.IO) { services.backups.saveToFolder() } }
            }
        }, fullWidth = true, icon = IconName.CLOUD)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Button(stringResource(R.string.bk_sheet), { sheetOpen = true }, Modifier.weight(1f), variant = ButtonVariant.OUTLINE)
            Button(stringResource(R.string.bk_restore), ::restoreFromFile, Modifier.weight(1f), variant = ButtonVariant.OUTLINE)
        }
        if (s.restoreUndo) {
            Button(stringResource(R.string.bk_undo), {
                scope.launch {
                    when (withContext(Dispatchers.IO) { services.backups.undoRestore() }) {
                        RestoreResult.LiveClass -> problem = Problem.LIVE
                        else -> nav.reopenOnToday()
                    }
                }
            }, fullWidth = true, variant = ButtonVariant.GHOST)
        }

        // Recentes: every row opens what can be done with that backup (AC-011-11).
        if (s.log.isNotEmpty()) {
            ListGroup(
                title = stringResource(R.string.bk_recent),
                rows = s.log.map { e -> @Composable { RecentRow(e) { opened = e } } },
            )
        }
    }
}

/** Guardar os últimos N, as a row of the group: the title and its effect, a small stepper on the right. */
@Composable
private fun KeepRow(
    keep: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.s4, vertical = Spacing.s2_5),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.bk_keep), style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.ink)
            Text(
                pluralStringResource(R.plurals.bk_keepValue, keep, keep),
                style = TutoraTheme.type.meta,
                color = TutoraTheme.colors.inkMuted,
            )
        }
        Stepper(
            keep.toLong(),
            { onChange(it.toInt()) },
            Modifier.weight(0.8f),
            step = 1,
            min = BACKUP_KEEP_MIN.toLong(),
            max = BACKUP_KEEP_MAX.toLong(),
            valueLabel = stringResource(R.string.bk_keep),
            size = app.tutora.designsystem.components.ControlSize.SM,
        )
    }
}

/** "Automático · Google Drive · 184 KB", "Manual · Download · 24 KB" or "Manual · compartilhado · 24 KB". */
@Composable
private fun recentLine(e: BackupLogEntry): String {
    val how =
        when {
            e.auto -> stringResource(R.string.bk_recentAuto, e.where)
            e.where.isNotEmpty() -> stringResource(R.string.bk_recentManualIn, e.where)
            else -> stringResource(R.string.bk_recentManual)
        }
    return how + if (e.ok) " · ${(e.bytes / 1024).coerceAtLeast(1)} KB" else ""
}

@Composable
private fun RecentRow(
    e: BackupLogEntry,
    onClick: () -> Unit,
) {
    val services = LocalServices.current
    val language = currentLanguage()
    ListRow(
        recentText(localOf(e.at, services.zone), language),
        Modifier.testTag("backup-recent"),
        subtitle = recentLine(e),
        icon = if (e.auto) IconName.CLOUD else IconName.FILE,
        iconTone = if (e.ok) TileTone.SUCCESS else TileTone.DANGER,
        badge = if (e.ok) null else BadgeTone.DANGER to stringResource(R.string.bk_failed),
        onClick = onClick,
    )
}

/** The spreadsheet's period: quick choices and Personalizado with De and Até (AC-011-19). */
@Composable
private fun SheetPeriodSheet(
    onDismiss: () -> Unit,
    onGenerate: (SheetRange) -> Unit,
) {
    val language = currentLanguage()
    val today = today()
    var key by rememberSaveable { mutableStateOf("month") }
    var from by rememberSaveable { mutableStateOf(today.toString()) }
    var to by rememberSaveable { mutableStateOf(today.toString()) }
    val range =
        when (key) {
            "last" -> SheetRange.LastMonth
            "year" -> SheetRange.ThisYear
            "all" -> SheetRange.All
            "custom" -> SheetRange.Custom(LocalDate.parse(from), LocalDate.parse(to))
            else -> SheetRange.ThisMonth
        }
    val dayPattern = if (language == Language.PT_BR) "dd/MM/yyyy" else "MM/dd/yyyy"

    fun shown(d: String) = DateTimeFormatter.ofPattern(dayPattern, localeOf(language)).format(LocalDate.parse(d).toJavaLocalDate())
    BottomSheet(
        stringResource(R.string.bk_sheet),
        onDismiss,
        actions = listOf({ m -> Button(stringResource(R.string.bk_generate), { onGenerate(range) }, m) }),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            listOf(
                "month" to R.string.rep_thisMonth,
                "last" to R.string.rep_lastMonth,
                "year" to R.string.bk_thisYear,
                "all" to R.string.bk_all,
                "custom" to R.string.bk_custom,
            ).forEach { (k, label) -> Chip(stringResource(label), key == k, { key = k }) }
        }
        if (key == "custom") {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                DateField(
                    stringResource(R.string.rep_from),
                    shown(from),
                    LocalDate.parse(from),
                    { from = it.toString() },
                    Modifier.weight(1f),
                )
                DateField(stringResource(R.string.rep_to), shown(to), LocalDate.parse(to), { to = it.toString() }, Modifier.weight(1f))
            }
        }
    }
}
