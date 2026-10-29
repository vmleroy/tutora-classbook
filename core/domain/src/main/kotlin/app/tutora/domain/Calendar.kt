package app.tutora.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Calendar sync (spec 012): one event per class of the next 8 weeks, in one calendar of the
// phone, each with Tutora's mark. The phone's calendar is the only record of which event is
// which class; these rules decide what each event says and what to change.

/** The texts an event needs, in the app language; filled from string resources (rule 14). */
data class CalendarWords(
    /** "Cancelada · " and "Faltou · ", in front of the title. */
    val cancelledPrefix: String,
    val missedPrefix: String,
    /** "Valor: %1$s". */
    val value: String,
    /** "Criado pelo Tutora", the description's last line (AC-012-5). */
    val footer: String,
    val builtinName: (BuiltinPlace) -> String,
    val money: (Cents) -> String,
)

/** What one class's event says (AC-012-6, 7). Times are UTC milliseconds. */
data class ClassEvent(
    val lessonId: String,
    val startMs: Long,
    val endMs: Long,
    val title: String,
    val location: String,
    val description: String,
)

private fun Lesson.whoText(): String =
    when (val w = who) {
        is Who.OfStudent -> w.student.name
        is Who.OfGroup -> w.group.name
    }

private fun Lesson.subject(): String? =
    when (val w = who) {
        is Who.OfStudent -> w.student.subject
        is Who.OfGroup -> w.group.subject
    }?.takeIf { it.isNotBlank() }

/** One class as an event: who and the subject, the place, the notes, maybe the value, Tutora's line. */
fun eventFor(
    lesson: Lesson,
    words: CalendarWords,
    includeValue: Boolean,
): ClassEvent {
    val prefix =
        when (lesson.status) {
            LessonStatus.CANCELLED -> words.cancelledPrefix
            LessonStatus.MISSED -> words.missedPrefix
            else -> ""
        }
    val title = prefix + listOfNotNull(lesson.whoText(), lesson.subject()).joinToString(" · ")
    val place = lesson.place.builtin?.let(words.builtinName) ?: lesson.place.name.orEmpty()
    val location = listOfNotNull(place.ifBlank { null }, lesson.place.address?.ifBlank { null }).joinToString(", ")
    val value = (lesson.worth() as? Worth.Set)?.cents?.takeIf { includeValue }?.let { words.value.format(words.money(it)) }
    val description = listOfNotNull(lesson.notes?.ifBlank { null }, value, words.footer).joinToString("\n")
    val start = instantOf(lesson.startAt).toEpochMilliseconds()
    return ClassEvent(lesson.id, start, start + lesson.plannedMin * 60_000L, title, location, description)
}

/** From today 00:00 to the end of the 8 weeks the schedule is filled, as a UTC range (AC-012-6, 9). */
fun syncWindow(
    today: LocalDate,
    zone: String,
): Pair<String, String> = dayRange(today, agendaHorizon(today), zone)

/** An event with Tutora's mark, as the phone has it now. */
data class MarkedEvent(
    val eventId: Long,
    val event: ClassEvent,
)

data class EventDiff(
    val create: List<ClassEvent>,
    val update: List<Pair<Long, ClassEvent>>,
    val delete: List<Long>,
) {
    val isEmpty get() = create.isEmpty() && update.isEmpty() && delete.isEmpty()
}

/**
 * What to change so the calendar matches the classes (AC-012-8, 11). An event the teacher edited
 * or deleted in the calendar app no longer matches, so it is put back as Tutora has it. Marked
 * events before the window are past and stay as they were (AC-012-9); a second event for the same
 * class goes.
 */
fun diffEvents(
    desired: List<ClassEvent>,
    existing: List<MarkedEvent>,
    windowStartMs: Long,
): EventDiff {
    val wanted = desired.associateBy { it.lessonId }
    val byLesson = existing.groupBy { it.event.lessonId }
    val create = desired.filter { it.lessonId !in byLesson }
    val update = mutableListOf<Pair<Long, ClassEvent>>()
    val delete = mutableListOf<Long>()
    for ((lessonId, events) in byLesson) {
        val want = wanted[lessonId]
        val keep = events.first()
        events.drop(1).forEach { delete += it.eventId }
        when {
            want == null && keep.event.startMs < windowStartMs -> Unit
            want == null -> delete += keep.eventId
            keep.event != want -> update += keep.eventId to want
        }
    }
    return EventDiff(create, update, delete)
}

/** An appointment from the phone's other calendars, shown as busy time (AC-012-17). */
data class BusyEvent(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val allDay: Boolean,
    /** Carries Tutora's mark: one of its own classes, never busy time (AC-012-19). */
    val marked: Boolean = false,
)

/** A day's busy time: timed blocks in minutes from midnight, and all-day bands (AC-012-18). */
data class BusyDay(
    val blocks: List<Triple<String, Int, Int>>,
    val bands: List<String>,
)

fun busyFor(
    events: List<BusyEvent>,
    day: LocalDate,
    zone: String,
): BusyDay {
    val tz = TimeZone.of(zone)
    val dayStart = localToInstant(day, LocalTime(0, 0), zone).toEpochMilliseconds()
    val dayEnd = localToInstant(day.plus(DatePeriod(days = 1)), LocalTime(0, 0), zone).toEpochMilliseconds()
    val mine = events.filter { !it.marked }
    // An all-day event's times are midnights in UTC, whatever the zone.
    val bands =
        mine
            .filter { it.allDay }
            .filter { e ->
                val first = Instant.fromEpochMilliseconds(e.startMs).toLocalDateTime(TimeZone.UTC).date
                val last = Instant.fromEpochMilliseconds(e.endMs - 1).toLocalDateTime(TimeZone.UTC).date
                day in first..last
            }.map { it.title }
    val blocks =
        mine.filter { !it.allDay && it.startMs < dayEnd && it.endMs > dayStart }.map { e ->
            fun minutes(ms: Long): Int {
                val t = Instant.fromEpochMilliseconds(ms.coerceIn(dayStart, dayEnd)).toLocalDateTime(tz)
                return if (ms >= dayEnd) 24 * 60 else t.hour * 60 + t.minute
            }
            Triple(e.title, minutes(e.startMs), minutes(e.endMs))
        }
    return BusyDay(blocks, bands)
}

/**
 * The classes as an .ics file (AC-012-20, 21): one VEVENT per class, UID from the class's id so a
 * calendar that imports it again updates the same events; times in UTC.
 */
fun icsFile(
    events: List<ClassEvent>,
    stampMs: Long,
): String {
    fun time(ms: Long): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC)

        fun p(n: Int) = n.toString().padStart(2, '0')
        return "${t.year}${p(t.month.ordinal + 1)}${p(t.day)}T${p(t.hour)}${p(t.minute)}${p(t.second)}Z"
    }

    fun text(s: String) =
        s
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    val lines = mutableListOf("BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Tutora//Aulas//PT", "CALSCALE:GREGORIAN", "METHOD:PUBLISH")
    for (e in events) {
        lines +=
            listOf(
                "BEGIN:VEVENT",
                "UID:${e.lessonId}@tutora.app",
                "DTSTAMP:${time(stampMs)}",
                // Later exports win over earlier ones (AC-012-21).
                "SEQUENCE:${stampMs / 1000}",
                "DTSTART:${time(e.startMs)}",
                "DTEND:${time(e.endMs)}",
                "SUMMARY:${text(e.title)}",
                "LOCATION:${text(e.location)}",
                "DESCRIPTION:${text(e.description)}",
                "END:VEVENT",
            )
    }
    lines += "END:VCALENDAR"
    return lines.joinToString("\r\n", postfix = "\r\n")
}

/** This phone's calendar setup: never in a backup, kept through a restore (AC-012-14). */
data class CalendarState(
    val on: Boolean = false,
    /** The phone calendar the classes go to; empty until one is chosen. */
    val calendarId: Long = 0,
    /** "Tutora – Aulas · victor@gmail.com", for Ajustes and the page. */
    val calendarName: String = "",
    /** Tutora made this calendar on the phone ("Só neste aparelho"): it goes when sync stops. */
    val own: Boolean = false,
    /** Incluir valor na descrição (AC-012-7). */
    val includeValue: Boolean = false,
    /** Mostrar outros compromissos (AC-012-17). */
    val showBusy: Boolean = false,
    val syncedAt: String = "",
    val count: Int = 0,
)
