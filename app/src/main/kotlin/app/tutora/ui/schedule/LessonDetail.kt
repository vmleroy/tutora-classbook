package app.tutora.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.Checkbox
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.HeaderAction
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.MoneyField
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.Stepper
import app.tutora.designsystem.components.Tag
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Cents
import app.tutora.domain.Lesson
import app.tutora.domain.LessonStatus
import app.tutora.domain.Payer
import app.tutora.domain.PlaceKind
import app.tutora.domain.Repeat
import app.tutora.domain.Who
import app.tutora.domain.Worth
import app.tutora.domain.initials
import app.tutora.domain.instantOf
import app.tutora.domain.localOf
import app.tutora.domain.worth
import app.tutora.state.LocalServices
import app.tutora.ui.LessonEdit
import app.tutora.ui.LiveLesson
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.catalog.DurationStepper
import app.tutora.ui.catalog.Field
import app.tutora.ui.clockTime
import app.tutora.ui.durationText
import app.tutora.ui.live.rememberStartLesson
import app.tutora.ui.longDate
import app.tutora.ui.payments.BonusChoice
import app.tutora.ui.payments.LessonMoney
import app.tutora.ui.payments.bonusOffer
import app.tutora.ui.placeName
import app.tutora.ui.tag
import app.tutora.ui.weekdayLong
import app.tutora.usecases.Scope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

/**
 * A class in full: when, how long, what it is worth, who and where, and the notes. Changing it
 * is one deliberate step, the pencil in the header (docs/product.md, "Detail first, then edit").
 */
@Composable
fun LessonDetailScreen(
    nav: NavHostController,
    id: String,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val language = currentLanguage()
    val lesson by remember(id) { services.scheduleRepo.lesson(id) }.collectAsState(initial = null)
    var seen by remember { mutableStateOf(false) }
    // A class removed from its edit form leaves nothing to show here.
    LaunchedEffect(lesson) {
        if (lesson != null) {
            seen = true
        } else if (seen) {
            nav.popBackStack()
        }
    }
    val l = lesson ?: return
    var removing by remember { mutableStateOf(false) }
    var marking by remember { mutableStateOf<Int?>(null) }
    var cancelling by remember { mutableStateOf(false) }
    val now = services.now()
    // "Esta e as próximas" only from a class still ahead in a running series (AC-005-14, 15).
    val inSeries = l.repeat != null && !l.isDetached && l.startAt > services.now()

    fun remove(s: Scope) {
        scope.launch { services.schedule.removeLesson(l.id, s) }
        removing = false
    }
    val started = l.startAt <= now
    val settings by services.settings.collectAsState(initial = null)
    val startLesson = rememberStartLesson(nav)
    // Iniciar aula when the timer is on, Cancelar aula any time, Marcar como dada once started;
    // a live class opens its screen; Desfazer once it is no longer scheduled (AC-006-9, 11, 13, AC-007-1, 3, 13).
    val actionsBar: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            when (l.status) {
                LessonStatus.LIVE -> Button(stringResource(R.string.live_open), { nav.navigate(LiveLesson(l.id)) }, fullWidth = true)
                LessonStatus.SCHEDULED -> {
                    if (settings?.liveTimer != false) {
                        Button(stringResource(R.string.live_start), { startLesson(l.id) }, fullWidth = true, icon = IconName.PLAY)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                        Button(
                            stringResource(R.string.cancel_action),
                            { cancelling = true },
                            Modifier.weight(1f),
                            variant = ButtonVariant.OUTLINE,
                        )
                        // Ahead of its time only with Ajustes › Marcar como dada antes do horário (AC-006-25).
                        if (started || settings?.earlyDone == true) {
                            Button(
                                stringResource(R.string.lesson_markDone),
                                { marking = l.plannedMin },
                                Modifier.weight(1f),
                                variant = ButtonVariant.OUTLINE,
                            )
                        }
                    }
                }
                else ->
                    Button(
                        stringResource(R.string.lesson_undo),
                        { scope.launch { services.lessons.undo(l.id) } },
                        variant = ButtonVariant.OUTLINE,
                        fullWidth = true,
                    )
            }
        }
    }
    val local = localOf(l.startAt, services.zone)
    val endMin = local.hour * 60 + local.minute + l.plannedMin
    val end = LocalTime((endMin / 60) % 24, endMin % 60)
    ScreenScaffold(
        stringResource(R.string.lesson_detail),
        "lesson-detail",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
        actions =
            listOf(
                HeaderAction(IconName.TRASH, stringResource(R.string.lesson_remove)) { removing = true },
                HeaderAction(IconName.PENCIL, stringResource(R.string.catalog_edit)) { nav.navigate(LessonEdit(id)) },
            ),
        bottomBar = actionsBar,
    ) {
        Card {
            Text(statusLine(l, now, services.zone), style = TutoraTheme.type.caption, color = TutoraTheme.colors.inkMuted)
            l.cancelNote?.let { Text(it, style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted) }
            Text(
                "${clockTime(local.time, language)} – ${clockTime(end, language)}",
                style = TutoraTheme.type.titleLg,
                color = TutoraTheme.colors.ink,
            )
            Text(longDate(local.date, language), style = TutoraTheme.type.body, color = TutoraTheme.colors.ink)
            l.repeat?.let { r ->
                Text(
                    stringResource(
                        R.string.lesson_series,
                        stringResource(if (r == Repeat.WEEKLY) R.string.lesson_repeats_weekly else R.string.lesson_repeats_biweekly),
                        weekdayLong(local.date, language),
                        clockTime(local.time, language),
                    ),
                    style = TutoraTheme.type.meta,
                    color = TutoraTheme.colors.inkMuted,
                )
            }
        }
        Card {
            Fact(stringResource(R.string.lesson_duration), durationText(l.plannedMin))
            val worth = l.worth()
            Fact(
                stringResource(R.string.lesson_value),
                worthText(worth),
                muted = worth == Worth.Unset,
            )
            Fact(stringResource(R.string.lesson_whoPays), payerText(l))
        }
        LessonMoney(nav, l)
        val (name, sub, avatar) =
            when (val w = l.who) {
                is Who.OfStudent -> Triple(w.student.name, w.student.subject, initials(w.student.name) to w.student.color.tag())
                is Who.OfGroup -> Triple(w.group.name, w.group.subject, initials(w.group.name) to Tag.TEAL)
            }
        ListGroup(
            listOf(
                { ListRow(name, subtitle = sub, avatar = avatar, trailing = RowTrailing.NONE) },
                {
                    ListRow(
                        placeName(l.place),
                        subtitle = stringResource(R.string.lesson_placeLabel),
                        icon = IconName.PIN,
                        trailing = RowTrailing.NONE,
                    )
                },
            ),
        )
        l.notes?.let { notes ->
            Card(label = stringResource(R.string.lesson_notes)) {
                Text(stringResource(R.string.lesson_notes), style = TutoraTheme.type.caption, color = TutoraTheme.colors.inkMuted)
                Text(notes, style = TutoraTheme.type.body, color = TutoraTheme.colors.ink)
            }
        }
    }
    if (cancelling) CancelSheet(l, started) { cancelling = false }
    marking?.let { start ->
        DoneSheet(l, start, stringResource(R.string.lesson_markDone), { marking = null }) { minutes, bonus ->
            scope.launch {
                services.lessons.markDone(l.id, minutes)
                // The bonus the teacher chose comes off the class once it is done (AC-008-39).
                bonus?.let { services.payments.useCredit(it.payer, listOf(l.id), it.minutes, it.amount) }
            }
            marking = null
        }
    }
    if (removing) {
        // Removing always asks first (constitution rule 3); a series asks which classes.
        BottomSheet(
            stringResource(R.string.lesson_removeTitle),
            { removing = false },
            subtitle = if (inSeries) stringResource(R.string.lesson_removeSeries) else null,
            actions =
                if (inSeries) {
                    listOf(
                        { m ->
                            Button(stringResource(R.string.lesson_scopes_one), { remove(Scope.ONE) }, m, variant = ButtonVariant.OUTLINE)
                        },
                        { m ->
                            Button(
                                stringResource(R.string.lesson_scopes_following),
                                { remove(Scope.FOLLOWING) },
                                m,
                                variant = ButtonVariant.DANGER,
                            )
                        },
                    )
                } else {
                    listOf(
                        { m -> Button(stringResource(R.string.catalog_cancel), { removing = false }, m, variant = ButtonVariant.OUTLINE) },
                        { m -> Button(stringResource(R.string.lesson_remove), { remove(Scope.ONE) }, m, variant = ButtonVariant.DANGER) },
                    )
                },
        )
    }
}

/** "Agendada · em 25 min" while it is ahead today; otherwise the status, with a kept fee (AC-006-14). */
@Composable
private fun statusLine(
    l: Lesson,
    now: String,
    zone: String,
): String {
    val minutes = (instantOf(l.startAt) - instantOf(now)).inWholeMinutes.toInt()
    val today = localOf(l.startAt, zone).date == localOf(now, zone).date
    if (l.status != LessonStatus.SCHEDULED || minutes <= 0 || !today) return statusWithFee(l)
    val rest = minutes % 60
    val until =
        if (minutes < 60) {
            stringResource(R.string.home_inMinutes, minutes.toString())
        } else {
            stringResource(R.string.home_inHours, "${minutes / 60}h${if (rest > 0) rest.toString().padStart(2, '0') else ""}")
        }
    return stringResource(R.string.lesson_statusWhen, statusLabel(l), until)
}

/** Who pays: the place when it pays the teacher (or a salary covers it), otherwise the student. */
@Composable
private fun payerText(l: Lesson): String =
    if (l.place.payer == Payer.PLACE ||
        l.place.kind == PlaceKind.SCHOOL
    ) {
        placeName(l.place)
    } else {
        stringResource(R.string.lesson_payerStudent)
    }

/**
 * The real duration, starting at [start], changed with − / +, the value as it changes, and
 * Confirmar with it. Marcar como dada and Encerrar aula share it (AC-006-1, AC-007-9).
 */
@Composable
fun DoneSheet(
    l: Lesson,
    start: Int,
    title: String,
    onDismiss: () -> Unit,
    dismissLabel: String? = null,
    onConfirm: (Int, BonusChoice?) -> Unit,
) {
    val language = currentLanguage()
    var minutes by remember(l.id, start) { mutableStateOf(start) }
    val value = l.copy(plannedMin = minutes).worth()
    // With Créditos e bônus on and credit or a bonus to use, ask whether to use it here (AC-008-39).
    val offer = bonusOffer(l)
    var useBonus by rememberSaveable { mutableStateOf(false) }
    // Money or minutes; until the teacher picks, whichever the payer has (the offer loads after the sheet opens).
    var picked by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val inMinutes = picked ?: (offer != null && offer.cents <= 0)
    var cents by rememberSaveable { mutableStateOf<Cents?>(null) }
    var bonusMinutes by rememberSaveable { mutableStateOf<Long?>(null) }
    val worthCents = (value as? Worth.Set)?.cents ?: 0
    val moneyUse = cents ?: minOf(offer?.cents ?: 0, worthCents)
    val minutesUse = bonusMinutes ?: minOf((offer?.minutes ?: 0).toLong(), minutes.toLong())
    val choice =
        if (useBonus && offer != null) {
            BonusChoice(offer.payer, inMinutes, if (inMinutes) minutesUse else moneyUse).takeIf { it.amount > 0 }
        } else {
            null
        }
    val confirm: @Composable (Modifier) -> Unit = { m ->
        Button(
            if (value is Worth.Set) {
                stringResource(R.string.lesson_confirmValue, formatMoney(value.cents, language))
            } else {
                stringResource(R.string.lesson_confirm)
            },
            { onConfirm(minutes, choice) },
            m,
        )
    }
    BottomSheet(
        title,
        onDismiss,
        actions =
            if (dismissLabel != null) {
                listOf({ m -> Button(dismissLabel, onDismiss, m, variant = ButtonVariant.OUTLINE) }, confirm)
            } else {
                listOf(confirm)
            },
    ) {
        Field(stringResource(R.string.lesson_realDuration)) {
            DurationStepper(stringResource(R.string.lesson_realDuration), minutes, { minutes = it })
        }
        Fact(stringResource(R.string.lesson_value), worthText(value), muted = value == Worth.Unset)
        if (offer != null) {
            Checkbox(stringResource(R.string.pay_useBonusHere) + " · " + offer.label, useBonus, { useBonus = it })
            if (useBonus) {
                if (offer.cents > 0 && offer.minutes > 0) {
                    SegmentedControl(
                        listOf(false to stringResource(R.string.pay_useMoney), true to stringResource(R.string.pay_useMinutes)),
                        inMinutes,
                        { picked = it },
                        stringResource(R.string.pay_useBonusAmount),
                        size = ControlSize.SM,
                    )
                }
                if (inMinutes) {
                    Stepper(
                        minutesUse,
                        { bonusMinutes = it },
                        step = 5,
                        min = 0,
                        max = minOf(offer.minutes, minutes).toLong(),
                        valueLabel = stringResource(R.string.pay_useBonusAmount),
                    )
                } else {
                    MoneyField(
                        stringResource(R.string.pay_useBonusAmount),
                        moneyUse,
                        { cents = it },
                        maxCents = minOf(offer.cents, worthCents.takeIf { it > 0 } ?: offer.cents),
                    )
                }
            }
        }
    }
}

/** One labelled fact on a line: the label on the left, the value on the right. */
@Composable
fun Fact(
    label: String,
    value: String,
    muted: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
        Text(
            value,
            style = TutoraTheme.type.bodyStrong,
            color = if (muted) TutoraTheme.colors.inkMuted else TutoraTheme.colors.ink,
            textAlign = TextAlign.End,
        )
    }
}
