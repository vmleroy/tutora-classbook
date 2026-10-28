package app.tutora.usecases

import app.tutora.data.LessonRow
import app.tutora.data.RuleRow
import app.tutora.data.SkipRow
import app.tutora.data.TutoraDatabase
import app.tutora.data.inTransaction
import app.tutora.domain.Cents
import app.tutora.domain.HORIZON_DAYS
import app.tutora.domain.LessonStatus
import app.tutora.domain.Repeat
import app.tutora.domain.RuleDates
import app.tutora.domain.isoText
import app.tutora.domain.localOf
import app.tutora.domain.localToInstant
import app.tutora.domain.occurrences
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Who a class is for, by id. */
sealed interface WhoId {
    data class OfStudent(
        val id: String,
    ) : WhoId

    data class OfGroup(
        val id: String,
    ) : WhoId
}

data class LessonDraft(
    val who: WhoId,
    val placeId: String,
    val date: LocalDate,
    val time: LocalTime,
    val plannedMin: Int,
    val valueCents: Cents?,
    val travelFeeCents: Cents?,
    val repeat: Repeat?,
    val notes: String? = null,
)

data class LessonChange(
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val plannedMin: Int? = null,
    val valueCents: Cents? = null,
    val clearValue: Boolean = false,
    val travelFeeCents: Cents? = null,
    val notes: String? = null,
    /** Another of the student's linked places; a group's class stays at the group's place. */
    val placeId: String? = null,
)

enum class Scope { ONE, FOLLOWING }

enum class ScheduleError { NOT_FOUND, PLACE_NOT_LINKED, STARTED, NOT_STARTED, FUTURE_DATE, NO_PRICE, NOT_SCHEDULED }

sealed interface ScheduleResult {
    data class Ok(
        val lessonId: String?,
    ) : ScheduleResult

    data class Err(
        val error: ScheduleError,
    ) : ScheduleResult
}

/** Only the student's linked places, or the group's own place (AC-005-2). */
internal suspend fun placeAllowed(
    db: TutoraDatabase,
    who: WhoId,
    placeId: String,
): Boolean =
    when (who) {
        is WhoId.OfGroup -> db.groups().get(who.id)?.placeId == placeId
        is WhoId.OfStudent -> db.students().visibleLinks(who.id).any { it.placeId == placeId }
    }

/** Scheduling classes (spec 005). Every write runs in one transaction. */
class Schedule(
    private val ctx: Ctx,
) {
    private val db get() = ctx.db
    private val dao get() = db.schedule()

    private fun today(): LocalDate = localOf(ctx.now(), ctx.zone()).date

    private fun horizon(): LocalDate = today().plus(DatePeriod(days = HORIZON_DAYS))

    /** Fills one series from where it stopped up to [until]; existing dates are left alone. */
    private suspend fun fillRule(
        rule: RuleRow,
        until: LocalDate,
    ) {
        val generated = LocalDate.parse(rule.generatedUntil)
        if (generated >= until) return
        val startsOn = LocalDate.parse(rule.startsOn)
        val from = generated.plus(DatePeriod(days = 1)).let { if (it > startsOn) it else startsOn }
        val dates =
            occurrences(
                RuleDates(
                    startsOn,
                    rule.endsOn?.let(LocalDate::parse),
                    Repeat.valueOf(rule.repeat),
                    dao.skips(rule.id).map(LocalDate::parse).toSet(),
                ),
                from,
                until,
            )
        val time = LocalTime.parse(rule.startTime)
        dao.insertLessons(
            dates.map { date ->
                LessonRow(
                    id = ctx.newId(),
                    ruleId = rule.id,
                    occurrenceDate = date.toString(),
                    studentId = rule.studentId,
                    groupId = rule.groupId,
                    placeId = rule.placeId,
                    startAt = localToInstant(date, time, rule.timeZone).isoText(),
                    plannedMin = rule.durationMin,
                    valueCents = rule.valueCents,
                    travelFeeCents = rule.travelFeeCents,
                    notes = null,
                    createdAt = ctx.now(),
                    updatedAt = ctx.now(),
                )
            },
        )
        dao.setGeneratedUntil(rule.id, until.toString())
    }

    private suspend fun placeAllowed(
        who: WhoId,
        placeId: String,
    ) = placeAllowed(db, who, placeId)

    private fun newRule(
        d: LessonDraft,
        repeat: Repeat,
        zone: String,
    ) = RuleRow(
        id = ctx.newId(),
        studentId = (d.who as? WhoId.OfStudent)?.id,
        groupId = (d.who as? WhoId.OfGroup)?.id,
        placeId = d.placeId,
        startTime = d.time.toString(),
        durationMin = d.plannedMin,
        repeat = repeat.name,
        startsOn = d.date.toString(),
        endsOn = null,
        timeZone = zone,
        valueCents = d.valueCents,
        travelFeeCents = d.travelFeeCents,
        generatedUntil = d.date.minus(DatePeriod(days = 1)).toString(),
        createdAt = ctx.now(),
    )

    private fun until(date: LocalDate) = if (horizon() > date) horizon() else date

    /**
     * Agendar: a one-off class, or a series filled 8 weeks ahead. Only the student's linked
     * places, or the group's own place, are allowed (AC-005-2).
     */
    suspend fun scheduleLesson(draft: LessonDraft): ScheduleResult =
        db.inTransaction {
            if (!placeAllowed(draft.who, draft.placeId)) return@inTransaction ScheduleResult.Err(ScheduleError.PLACE_NOT_LINKED)
            if (draft.repeat != null) {
                val rule = newRule(draft, draft.repeat, ctx.zone())
                dao.insertRule(rule)
                fillRule(rule, until(draft.date))
                val first = dao.lessonAt(rule.id, draft.date.toString())
                if (first != null && draft.notes != null) dao.upsertLesson(first.copy(notes = draft.notes))
                return@inTransaction ScheduleResult.Ok(first?.id)
            }
            val id = ctx.newId()
            dao.insertLessons(
                listOf(
                    LessonRow(
                        id = id,
                        ruleId = null,
                        occurrenceDate = null,
                        studentId = (draft.who as? WhoId.OfStudent)?.id,
                        groupId = (draft.who as? WhoId.OfGroup)?.id,
                        placeId = draft.placeId,
                        startAt = localToInstant(draft.date, draft.time, ctx.zone()).isoText(),
                        plannedMin = draft.plannedMin,
                        valueCents = draft.valueCents,
                        travelFeeCents = draft.travelFeeCents,
                        notes = draft.notes,
                        createdAt = ctx.now(),
                        updatedAt = ctx.now(),
                    ),
                ),
            )
            ScheduleResult.Ok(id)
        }

    /** Keeps every running series filled 8 weeks from today. Safe to run any number of times (AC-005-8). */
    suspend fun fillSchedule() =
        db.inTransaction {
            for (rule in dao.activeRules(today().toString())) fillRule(rule, horizon())
        }

    /**
     * Editar aula. "Só esta aula" changes this class and detaches it from its series; any class
     * can be changed this way, past ones included. "Esta e as próximas" ends the series the day
     * before and starts a new one from this class with the change; later classes changed on
     * their own keep their changes. It only starts from a class still ahead (AC-005-11 to 15).
     */
    suspend fun editLesson(
        id: String,
        change: LessonChange,
        scope: Scope,
    ): ScheduleResult =
        db.inTransaction {
            val row = dao.lesson(id) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
            if (scope == Scope.FOLLOWING && row.startAt <= ctx.now()) return@inTransaction ScheduleResult.Err(ScheduleError.STARTED)
            val rule = row.ruleId?.let { dao.rule(it) }
            val zone = rule?.timeZone ?: ctx.zone()
            val local = localOf(row.startAt, zone)
            val date = change.date ?: local.date
            val time = change.time ?: local.time
            val planned = change.plannedMin ?: row.plannedMin
            val value = if (change.clearValue) null else change.valueCents ?: row.valueCents
            val placeId = change.placeId ?: row.placeId
            val who =
                row.studentId?.let { WhoId.OfStudent(it) }
                    ?: row.groupId?.let { WhoId.OfGroup(it) }
                    ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
            if (placeId != row.placeId &&
                !placeAllowed(who, placeId)
            ) {
                return@inTransaction ScheduleResult.Err(ScheduleError.PLACE_NOT_LINKED)
            }
            // A new place starts with only the travel given now; the old place's fee does not follow.
            val travel = if (placeId != row.placeId) change.travelFeeCents else change.travelFeeCents ?: row.travelFeeCents
            val notes = change.notes ?: row.notes
            val occurrenceText = row.occurrenceDate
            if (scope == Scope.ONE || rule == null || occurrenceText == null) {
                dao.upsertLesson(
                    row.copy(
                        placeId = placeId,
                        startAt = localToInstant(date, time, zone).isoText(),
                        plannedMin = planned,
                        valueCents = value,
                        travelFeeCents = travel,
                        notes = notes,
                        isDetached = rule != null,
                        updatedAt = ctx.now(),
                    ),
                )
                return@inTransaction ScheduleResult.Ok(id)
            }
            val occurrence = LocalDate.parse(occurrenceText)
            dao.endRule(rule.id, occurrence.minus(DatePeriod(days = 1)).toString())
            dao.deleteSeriesFrom(rule.id, occurrence.toString(), ctx.now(), detachedToo = false)
            dao.deleteLesson(id)
            val fresh =
                newRule(
                    LessonDraft(who, placeId, date, time, planned, value, travel, Repeat.valueOf(rule.repeat)),
                    Repeat.valueOf(rule.repeat),
                    zone,
                )
            dao.insertRule(fresh)
            fillRule(fresh, until(date))
            val first = dao.lessonAt(fresh.id, date.toString())
            if (first != null && notes != null) dao.upsertLesson(first.copy(notes = notes))
            ScheduleResult.Ok(first?.id)
        }

    /**
     * Remover. "Só esta aula" removes it, past ones included, and remembers the date so filling
     * never brings it back. "Esta e as próximas" ends the series from a class still ahead (AC-005-14).
     */
    suspend fun removeLesson(
        id: String,
        scope: Scope,
    ): ScheduleResult {
        // A live class removed takes its notification with it (AC-007-16).
        val wasLive = dao.lesson(id)?.status == LessonStatus.LIVE.key
        return remove(id, scope).also { if (wasLive) ctx.live.hide() }
    }

    private suspend fun remove(
        id: String,
        scope: Scope,
    ): ScheduleResult =
        db.inTransaction {
            val row = dao.lesson(id) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
            if (scope == Scope.FOLLOWING && row.startAt <= ctx.now()) return@inTransaction ScheduleResult.Err(ScheduleError.STARTED)
            val ruleId = row.ruleId
            val occurrence = row.occurrenceDate
            if (scope == Scope.FOLLOWING && ruleId != null && occurrence != null) {
                dao.endRule(ruleId, LocalDate.parse(occurrence).minus(DatePeriod(days = 1)).toString())
                dao.deleteSeriesFrom(ruleId, occurrence, ctx.now(), detachedToo = true)
                return@inTransaction ScheduleResult.Ok(null)
            }
            if (ruleId != null && occurrence != null) dao.addSkip(SkipRow(ruleId, occurrence))
            dao.deleteLesson(id)
            ScheduleResult.Ok(null)
        }

    /** The travel fee to suggest at Casa do Aluno: the last one this student paid there (AC-005-5). */
    suspend fun suggestedTravel(
        studentId: String,
        placeId: String,
    ): Cents? = dao.lastTravelFee(studentId, placeId)
}
