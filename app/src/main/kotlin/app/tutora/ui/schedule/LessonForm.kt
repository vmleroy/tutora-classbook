package app.tutora.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.DateField
import app.tutora.designsystem.components.FieldError
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.MoneyField
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.SelectField
import app.tutora.designsystem.components.Tag
import app.tutora.designsystem.components.TextField
import app.tutora.designsystem.components.TimeField
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Cents
import app.tutora.domain.Group
import app.tutora.domain.LessonStatus
import app.tutora.domain.Place
import app.tutora.domain.PlaceKind
import app.tutora.domain.RateType
import app.tutora.domain.Repeat
import app.tutora.domain.TravelMode
import app.tutora.domain.Who
import app.tutora.domain.Worth
import app.tutora.domain.currentPrice
import app.tutora.domain.initials
import app.tutora.domain.lessonWorth
import app.tutora.domain.localOf
import app.tutora.domain.travelFor
import app.tutora.state.LocalServices
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.catalog.DurationStepper
import app.tutora.ui.catalog.Field
import app.tutora.ui.catalog.RequiredLegend
import app.tutora.ui.clockTime
import app.tutora.ui.placeName
import app.tutora.ui.shortDate
import app.tutora.ui.tag
import app.tutora.ui.weekdayLong
import app.tutora.usecases.LessonChange
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.ScheduleError
import app.tutora.usecases.ScheduleResult
import app.tutora.usecases.Scope
import app.tutora.usecases.WhoId
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** What the form edits. Ids only, so it survives process death as plain values. */
data class LessonValues(
    val who: WhoId?,
    val placeId: String?,
    val date: LocalDate,
    val time: LocalTime,
    val plannedMin: Int,
    val valueCents: Cents?,
    val travelFeeCents: Cents?,
    val repeat: Repeat?,
    val notes: String?,
    /** A done class's duration is its real one. */
    val status: LessonStatus = LessonStatus.SCHEDULED,
)

/** The series a class belongs to, shown as "Semanal · segunda-feira, 14:00". */
data class SeriesInfo(
    val repeat: Repeat,
    val date: LocalDate,
    val time: LocalTime,
)

@Composable
fun LessonForm(
    title: String,
    tag: String,
    onBack: () -> Unit,
    initial: LessonValues,
    editing: Boolean,
    series: SeriesInfo?,
    error: String?,
    onSubmit: (LessonValues, Scope) -> Unit,
    /** Nova aula's mode: true = Registrar aula dada, false = Agendar; null in the edit form. */
    log: Boolean? = null,
    onLog: (Boolean) -> Unit = {},
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val language = currentLanguage()
    // Each Save that fails counts, so the same error blinks again.
    var tries by remember { mutableIntStateOf(0) }
    val students by services.catalogRepo.students().collectAsState(initial = emptyList())
    val groups by services.catalogRepo.groups().collectAsState(initial = emptyList())
    val places by services.catalogRepo.places().collectAsState(initial = emptyList())
    var v by remember(initial) { mutableStateOf(initial) }
    var applyTo by rememberSaveable { mutableStateOf(Scope.ONE) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    val byId = places.associate { it.place.id to it.place }
    val student = (v.who as? WhoId.OfStudent)?.let { w -> students.firstOrNull { it.student.id == w.id } }
    val group = (v.who as? WhoId.OfGroup)?.let { w -> groups.firstOrNull { it.id == w.id } }
    val choices: List<Place> =
        student?.links?.mapNotNull { byId[it.placeId] } ?: group?.let { listOfNotNull(byId[it.placeId]) } ?: emptyList()
    val place = v.placeId?.let { byId[it] }

    fun choosePlace(
        p: Place,
        studentId: String?,
    ) {
        // A new class takes the place's usual duration; an existing one keeps its own.
        v = v.copy(placeId = p.id, plannedMin = if (editing) v.plannedMin else p.defaultDurationMin, travelFeeCents = null)
        // At Casa do Aluno with travel "varies", suggest the last fee for that student (AC-005-5).
        if (p.kind == PlaceKind.STUDENT_HOME && p.travelMode == TravelMode.VARIES && studentId != null) {
            scope.launch { v = v.copy(travelFeeCents = services.schedule.suggestedTravel(studentId, p.id) ?: 0) }
        }
    }

    fun chooseWho(who: WhoId) {
        sheet = null
        v = v.copy(who = who, placeId = null)
        when (who) {
            is WhoId.OfStudent -> {
                val s = students.firstOrNull { it.student.id == who.id } ?: return
                val star = s.links.firstOrNull { it.isDefault } ?: s.links.firstOrNull() ?: return
                byId[star.placeId]?.let { choosePlace(it, who.id) }
            }
            is WhoId.OfGroup -> groups.firstOrNull { it.id == who.id }?.let { g -> byId[g.placeId]?.let { choosePlace(it, null) } }
        }
    }

    // What the class is worth with a given hand value; null means "follow the price".
    fun worthWith(valueCents: Long?): Worth? =
        place?.let { p ->
            when {
                student != null ->
                    lessonWorth(
                        p,
                        Who.OfStudent(student.student, student.links.firstOrNull { it.placeId == p.id }),
                        v.plannedMin,
                        valueCents,
                        v.travelFeeCents,
                    )
                group != null -> lessonWorth(p, Who.OfGroup(group), v.plannedMin, valueCents, v.travelFeeCents)
                else -> null
            }
        }
    val worth = worthWith(v.valueCents)
    val fromPrice = worthWith(null)

    ScreenScaffold(title, tag, variant = HeaderVariant.PUSH, onBack = onBack, bottomBar = {
        // The error shows right above Save, where the eye is when it fails.
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            if (error != null) FieldError(error, key = tries)
            val saveLabel =
                when {
                    log == true && worth is Worth.Set -> stringResource(R.string.lesson_saveLog, formatMoney(worth.cents, language))
                    log == true -> stringResource(R.string.lesson_saveLogPlain)
                    editing -> stringResource(R.string.lesson_saveEdit)
                    else -> stringResource(R.string.lesson_save)
                }
            Button(saveLabel, {
                tries++
                onSubmit(v, applyTo)
            }, fullWidth = true)
        }
    }) {
        if (log != null) {
            SegmentedControl(
                listOf(true to stringResource(R.string.lesson_modes_log), false to stringResource(R.string.lesson_modes_schedule)),
                log,
                { toLog ->
                    onLog(toLog)
                    // A lesson given happened today or earlier, and has already started.
                    val now = localOf(services.now(), services.zone)
                    if (toLog && v.date >= now.date) v = v.copy(date = now.date, time = LocalTime((now.hour - 1).coerceAtLeast(0), 0))
                },
                stringResource(R.string.lesson_new),
                size = ControlSize.SM,
            )
        }
        if (!editing) RequiredLegend()
        if (series != null) {
            val repeat =
                stringResource(
                    if (series.repeat ==
                        Repeat.WEEKLY
                    ) {
                        R.string.lesson_repeats_weekly
                    } else {
                        R.string.lesson_repeats_biweekly
                    },
                )
            Text(
                stringResource(R.string.lesson_series, repeat, weekdayLong(series.date, language), clockTime(series.time, language)),
                style = TutoraTheme.type.meta,
                color = TutoraTheme.colors.inkMuted,
            )
        }
        SelectField(
            student?.student?.name ?: group?.name ?: stringResource(R.string.lesson_pickWho),
            if (editing) null else ({ sheet = "who" }),
            label = stringResource(R.string.lesson_who),
            sub = student?.student?.subject ?: group?.subject,
            avatar = student?.let { initials(it.student.name) to it.student.color.tag() } ?: group?.let { initials(it.name) to Tag.TEAL },
            actionLabel = if (editing) null else stringResource(R.string.lesson_change),
            // Saving without anyone chosen turns the field red.
            isError = error != null && v.who == null,
            required = !editing,
            errorKey = tries,
        )
        if (choices.isNotEmpty()) {
            Field(stringResource(if (group != null) R.string.lesson_groupPlace else R.string.lesson_placeLabel), required = !editing) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    choices.forEach { p ->
                        Chip(placeName(p), v.placeId == p.id, { choosePlace(p, (v.who as? WhoId.OfStudent)?.id) })
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            DateField(
                stringResource(R.string.lesson_date),
                shortDate(v.date, language),
                v.date,
                { v = v.copy(date = it) },
                Modifier.weight(1f),
                title = stringResource(R.string.lesson_pickDate),
            )
            TimeField(
                stringResource(R.string.lesson_start),
                v.time.hour,
                v.time.minute,
                { h, m -> v = v.copy(time = LocalTime(h, m)) },
                Modifier.weight(1f),
            )
        }
        // A lesson given, or one marked done, has a real duration (AC-006-6).
        val durationLabel =
            stringResource(if (log == true || v.status == LessonStatus.DONE) R.string.lesson_realDuration else R.string.lesson_duration)
        Field(durationLabel) {
            DurationStepper(durationLabel, v.plannedMin, {
                v =
                    v.copy(plannedMin = it)
            })
        }
        // Value first, then this lesson's travel, then what the two add up to.
        val travel = place?.let { travelFor(it, v.travelFeeCents) } ?: 0
        MoneyField(
            stringResource(R.string.lesson_value),
            v.valueCents,
            { v = v.copy(valueCents = it) },
            placeholder =
                when (fromPrice) {
                    is Worth.Set -> formatMoney(fromPrice.cents - travel, language)
                    Worth.Salary -> stringResource(R.string.groups_coveredBySalary)
                    Worth.Unset, null -> stringResource(R.string.lesson_setValue)
                },
            helper =
                when {
                    v.valueCents != null -> stringResource(R.string.lesson_handValueHint)
                    log == true && fromPrice is Worth.Set ->
                        howWorked(
                            place,
                            student?.let { Who.OfStudent(it.student, it.links.firstOrNull { l -> l.placeId == place?.id }) }
                                ?: group?.let { Who.OfGroup(it) },
                            v.plannedMin,
                            travel,
                        )
                    fromPrice is Worth.Set -> stringResource(R.string.lesson_fromPrice)
                    else -> null
                },
        )
        val travelVaries = place?.kind == PlaceKind.STUDENT_HOME && place.travelMode == TravelMode.VARIES
        if (travelVaries) {
            MoneyField(stringResource(R.string.lesson_travel), v.travelFeeCents, { v = v.copy(travelFeeCents = it) })
        }
        if (travelVaries || travel > 0) {
            MoneyField(
                stringResource(R.string.lesson_total),
                (worth as? Worth.Set)?.cents,
                {},
                placeholder = stringResource(R.string.places_noPrice),
                readOnly = true,
            )
        }
        if (!editing && log != true) {
            Field(stringResource(R.string.lesson_repeat)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    listOf(
                        null to R.string.lesson_repeats_none,
                        Repeat.WEEKLY to R.string.lesson_repeats_weekly,
                        Repeat.BIWEEKLY to R.string.lesson_repeats_biweekly,
                    ).forEach { (r, label) -> Chip(stringResource(label), v.repeat == r, { v = v.copy(repeat = r) }) }
                }
            }
        }
        if (series != null) {
            Field(stringResource(R.string.lesson_scope)) {
                SegmentedControl(
                    listOf(
                        Scope.ONE to stringResource(R.string.lesson_scopes_one),
                        Scope.FOLLOWING to stringResource(R.string.lesson_scopes_following),
                    ),
                    applyTo,
                    { applyTo = it },
                    stringResource(R.string.lesson_scope),
                    size = ControlSize.SM,
                )
            }
        }
        TextField(
            stringResource(R.string.lesson_notes),
            v.notes.orEmpty(),
            { v = v.copy(notes = it.ifEmpty { null }) },
            imeAction = ImeAction.Done,
        )
    }

    when (sheet) {
        "who" ->
            BottomSheet(stringResource(R.string.lesson_pickWho), { sheet = null }) {
                val studentRows: List<@Composable () -> Unit> =
                    students.filter { it.links.isNotEmpty() }.map { s ->
                        {
                            ListRow(
                                s.student.name,
                                subtitle = s.student.subject,
                                avatar = initials(s.student.name) to s.student.color.tag(),
                                trailing = RowTrailing.NONE,
                                onClick = { chooseWho(WhoId.OfStudent(s.student.id)) },
                            )
                        }
                    }
                val groupRows: List<@Composable () -> Unit> =
                    groups.map { g: Group ->
                        {
                            ListRow(
                                g.name,
                                subtitle = g.subject,
                                icon = IconName.USERS,
                                trailing = RowTrailing.NONE,
                                onClick = { chooseWho(WhoId.OfGroup(g.id)) },
                            )
                        }
                    }
                ListGroup(studentRows + groupRows)
            }
    }
}

/** "90 min × R$ 120,00/h", plus travel, for Registrar (AC-006-7). */
@Composable
private fun howWorked(
    place: Place?,
    who: Who?,
    minutes: Int,
    travel: Cents,
): String? {
    val price = place?.let { p -> who?.let { currentPrice(p, it) } } ?: return null
    val language = currentLanguage()
    val base =
        if (price.rateType == RateType.HOUR) {
            stringResource(R.string.lesson_howHour, minutes.toString(), formatMoney(price.cents, language))
        } else {
            stringResource(R.string.lesson_howLesson, formatMoney(price.cents, language))
        }
    return if (travel > 0) stringResource(R.string.lesson_howTravel, base, formatMoney(travel, language)) else base
}

@Composable
private fun scheduleErrorText(e: ScheduleError): String =
    stringResource(
        when (e) {
            ScheduleError.NOT_FOUND -> R.string.lesson_errors_notFound
            ScheduleError.PLACE_NOT_LINKED -> R.string.lesson_errors_placeNotLinked
            ScheduleError.STARTED -> R.string.lesson_errors_started
            ScheduleError.NOT_STARTED -> R.string.lesson_errors_notStarted
            ScheduleError.FUTURE_DATE -> R.string.lesson_errors_futureDate
            ScheduleError.NO_PRICE -> R.string.cancel_noPrice
        },
    )

/** Nova aula in Agendar mode (AC-005-1 to 7). */
@Composable
fun LessonNewScreen(
    nav: NavHostController,
    date: String?,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val now = remember { localOf(services.now(), services.zone) }
    var error by remember { mutableStateOf<String?>(null) }
    val whoRequired = stringResource(R.string.lesson_errors_whoRequired)
    val placeRequired = stringResource(R.string.lesson_errors_placeRequired)
    val errors = ScheduleError.entries.associateWith { scheduleErrorText(it) }
    var log by rememberSaveable { mutableStateOf(false) }
    val initial =
        remember {
            LessonValues(
                null,
                null,
                date?.let(LocalDate::parse) ?: now.date,
                LocalTime((now.hour + 1).coerceAtMost(23), 0),
                60,
                null,
                null,
                Repeat.WEEKLY,
                null,
            )
        }
    run {
        LessonForm(stringResource(R.string.lesson_new), "lesson-new", {
            nav.popBackStack()
        }, initial, editing = false, series = null, error = error, onSubmit = {
            v,
            _,
            ->
            val who = v.who
            val placeId = v.placeId
            when {
                who == null -> error = whoRequired
                placeId == null -> error = placeRequired
                else ->
                    scope.launch {
                        val d =
                            LessonDraft(
                                who,
                                placeId,
                                v.date,
                                v.time,
                                v.plannedMin,
                                v.valueCents,
                                v.travelFeeCents,
                                if (log) null else v.repeat,
                                v.notes,
                            )
                        // Registrar saves a class already given (AC-006-8); Agendar puts it on the schedule.
                        when (val r = if (log) services.lessons.logLesson(d) else services.schedule.scheduleLesson(d)) {
                            is ScheduleResult.Ok -> nav.popBackStack()
                            is ScheduleResult.Err -> error = errors.getValue(r.error)
                        }
                    }
            }
        }, log = log, onLog = { log = it })
    }
}

/** Editar aula: date, time, duration, value and notes, for this class or the series (AC-005-10 to 15). */
@Composable
fun LessonEditScreen(
    nav: NavHostController,
    id: String,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val lesson by remember(id) { services.scheduleRepo.lesson(id) }.collectAsState(initial = null)
    var error by remember { mutableStateOf<String?>(null) }
    val errors = ScheduleError.entries.associateWith { scheduleErrorText(it) }
    val l = lesson ?: return
    val zone = services.zone
    val local = localOf(l.startAt, zone)
    val today = localOf(services.now(), zone).date
    val initial =
        remember(l.id) {
            LessonValues(
                when (val w = l.who) {
                    is Who.OfStudent -> WhoId.OfStudent(w.student.id)
                    is Who.OfGroup -> WhoId.OfGroup(w.group.id)
                },
                l.place.id,
                local.date,
                local.time,
                l.plannedMin,
                l.valueCents,
                l.travelFeeCents,
                l.repeat,
                l.notes,
                l.status,
            )
        }
    // A class that already happened changes on its own; the series question is for classes ahead.
    val series =
        if (l.repeat != null &&
            !l.isDetached &&
            l.startAt > services.now()
        ) {
            SeriesInfo(l.repeat ?: Repeat.WEEKLY, local.date, local.time)
        } else {
            null
        }

    fun done(r: ScheduleResult) {
        when (r) {
            is ScheduleResult.Ok -> nav.popBackStack()
            is ScheduleResult.Err -> error = errors.getValue(r.error)
        }
    }
    run {
        LessonForm(stringResource(R.string.lesson_edit), "lesson-edit", {
            nav.popBackStack()
        }, initial, editing = true, series = series, error = error, onSubmit = {
            v,
            applyTo,
            ->
            scope.launch {
                done(
                    services.schedule.editLesson(
                        l.id,
                        LessonChange(
                            v.date,
                            v.time,
                            v.plannedMin,
                            v.valueCents,
                            clearValue = v.valueCents == null,
                            travelFeeCents = v.travelFeeCents,
                            notes = v.notes,
                            placeId = v.placeId,
                        ),
                        applyTo,
                    ),
                )
            }
        })
    }
}
