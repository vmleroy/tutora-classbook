package app.tutora.usecases

import app.tutora.data.CalendarStore
import app.tutora.data.ScheduleRepository
import app.tutora.data.SettingsRepository
import app.tutora.domain.BusyEvent
import app.tutora.domain.CalendarState
import app.tutora.domain.CalendarWords
import app.tutora.domain.Language
import app.tutora.domain.MarkedEvent
import app.tutora.domain.dateOf
import app.tutora.domain.diffEvents
import app.tutora.domain.eventFor
import app.tutora.domain.icsFile
import app.tutora.domain.instantOf
import app.tutora.domain.syncWindow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** A calendar on the phone Tutora can write to (AC-012-3). */
data class PhoneCalendarInfo(
    val id: Long,
    val name: String,
    /** The account's address ("victor@gmail.com"); empty for Tutora's own phone calendar. */
    val account: String,
    /** "Google", "Outlook" or another app's name, for the letter tile. */
    val provider: String,
    /** Tutora's own phone calendar ("Só neste aparelho"). */
    val own: Boolean = false,
)

/**
 * The phone's calendars (spec 012). Every event Tutora writes carries its mark, and Tutora only
 * reads back, changes or deletes events with it (AC-012-5).
 */
interface PhoneCalendar {
    fun hasPermission(): Boolean

    /** The writable calendars of the phone's accounts, and Tutora's own one if it exists. */
    fun calendars(): List<PhoneCalendarInfo>

    /** Finds or makes Tutora's own calendar, kept only on the phone (AC-012-4). */
    fun ownCalendar(
        name: String,
        color: Int,
    ): Long

    /** The events with Tutora's mark in one calendar. */
    fun marked(calendarId: Long): List<MarkedEvent>

    /** Every event with Tutora's mark, in any calendar (AC-012-15). */
    fun markedEverywhere(): List<Long>

    fun insert(
        calendarId: Long,
        event: app.tutora.domain.ClassEvent,
        zone: String,
    ): Long

    fun update(
        eventId: Long,
        event: app.tutora.domain.ClassEvent,
        zone: String,
    )

    /** Deletes one event, only if it has Tutora's mark. */
    fun delete(eventId: Long)

    /** Removes Tutora's own phone calendar, if it made one. */
    fun deleteOwnCalendar()

    /** The phone's appointments from [fromMs] to [toMs], updated as they change (AC-012-17). */
    fun busy(
        fromMs: Long,
        toMs: Long,
    ): Flow<List<BusyEvent>>

    object None : PhoneCalendar {
        override fun hasPermission() = false

        override fun calendars() = emptyList<PhoneCalendarInfo>()

        override fun ownCalendar(
            name: String,
            color: Int,
        ) = 0L

        override fun marked(calendarId: Long) = emptyList<MarkedEvent>()

        override fun markedEverywhere() = emptyList<Long>()

        override fun insert(
            calendarId: Long,
            event: app.tutora.domain.ClassEvent,
            zone: String,
        ) = 0L

        override fun update(
            eventId: Long,
            event: app.tutora.domain.ClassEvent,
            zone: String,
        ) = Unit

        override fun delete(eventId: Long) = Unit

        override fun deleteOwnCalendar() = Unit

        override fun busy(
            fromMs: Long,
            toMs: Long,
        ): Flow<List<BusyEvent>> = flowOf(emptyList())
    }
}

/** Where the classes go: Tutora's own phone calendar, or one of an account's (AC-012-3, 4). */
sealed interface CalendarTarget {
    data class Own(
        val name: String,
        val color: Int,
    ) : CalendarTarget

    data class Account(
        val calendar: PhoneCalendarInfo,
    ) : CalendarTarget
}

/**
 * Sends the classes of the next 8 weeks to one phone calendar and keeps it matching (spec 012).
 * The phone's calendar is the only record: each event's mark says which class it is.
 */
class CalendarSync(
    private val ctx: Ctx,
) {
    private val store = CalendarStore(ctx.db)
    private val phone get() = ctx.calendar

    fun observe() = store.observe()

    suspend fun state(): CalendarState = store.load()

    private suspend fun words(): CalendarWords = ctx.calendarWords(SettingsRepository(ctx.db).load().language)

    private fun today(): LocalDate = dateOf(instantOf(ctx.now()), ctx.zone())

    /** Enviar aulas para o calendário, into [target] (AC-012-2 to 4). False without the permission. */
    suspend fun turnOn(target: CalendarTarget): Boolean {
        if (!phone.hasPermission()) return false
        val s = store.load()
        if (s.on && s.calendarId != 0L) phone.marked(s.calendarId).forEach { phone.delete(it.eventId) }
        val (id, name, own) =
            when (target) {
                is CalendarTarget.Own -> Triple(phone.ownCalendar(target.name, target.color), target.name, true)
                is CalendarTarget.Account ->
                    Triple(
                        target.calendar.id,
                        listOf(target.calendar.name, target.calendar.account).filter { it.isNotBlank() }.joinToString(" · "),
                        false,
                    )
            }
        // Leaving Tutora's own calendar for an account's: it goes, as it would when turned off.
        if (s.own && !own) phone.deleteOwnCalendar()
        store.save(s.copy(on = true, calendarId = id, calendarName = name, own = own), ctx.now())
        sync()
        return true
    }

    /** Incluir valor na descrição and Mostrar outros compromissos (AC-012-7, 17). */
    suspend fun setOptions(
        includeValue: Boolean,
        showBusy: Boolean,
    ) {
        store.save(store.load().copy(includeValue = includeValue, showBusy = showBusy), ctx.now())
        sync()
    }

    /** Makes the chosen calendar match the classes of the next 8 weeks (AC-012-6 to 11). */
    suspend fun sync() {
        val s = store.load()
        if (!s.on || s.calendarId == 0L || !phone.hasPermission()) return
        val (from, to) = syncWindow(today(), ctx.zone())
        val lessons = ScheduleRepository(ctx.db).between(from, to).first()
        val words = words()
        val desired = lessons.map { eventFor(it, words, s.includeValue) }
        val diff = diffEvents(desired, phone.marked(s.calendarId), instantOf(from).toEpochMilliseconds())
        diff.delete.forEach { runCatching { phone.delete(it) } }
        diff.update.forEach { (id, e) -> runCatching { phone.update(id, e, ctx.zone()) } }
        diff.create.forEach { e -> runCatching { phone.insert(s.calendarId, e, ctx.zone()) } }
        store.save(store.load().copy(syncedAt = ctx.now(), count = desired.size), ctx.now())
    }

    /** Turning it off: every marked event goes, and Tutora's own calendar; nothing else (AC-012-13). */
    suspend fun turnOff() {
        if (phone.hasPermission()) {
            phone.markedEverywhere().forEach { runCatching { phone.delete(it) } }
            phone.deleteOwnCalendar()
        }
        store.save(store.load().copy(on = false, calendarId = 0, calendarName = "", own = false, count = 0), ctx.now())
    }

    /** How many events Limpar eventos do Tutora would delete (AC-012-15). */
    fun cleanCount(): Int = if (phone.hasPermission()) phone.markedEverywhere().size else 0

    /** Limpar eventos do Tutora: the same as turning off, so the events don't come back (AC-012-16). */
    suspend fun clean() = turnOff()

    /** The classes of the next 8 weeks as an .ics file, with sync on or off (AC-012-20). */
    suspend fun ics(): String {
        val s = store.load()
        val (from, to) = syncWindow(today(), ctx.zone())
        val words = words()
        val events = ScheduleRepository(ctx.db).between(from, to).first().map { eventFor(it, words, s.includeValue) }
        return icsFile(events, instantOf(ctx.now()).toEpochMilliseconds())
    }

    /**
     * Every change to the classes, the setup or the language syncs the calendar a moment later
     * (AC-012-8, 14); [days] moves the window as days pass (AC-012-9).
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun changes(days: Flow<LocalDate>): Flow<Unit> =
        days
            .flatMapLatest { day ->
                val (from, to) = syncWindow(day, ctx.zone())
                combine(
                    ScheduleRepository(ctx.db).between(from, to),
                    store.observe().map { listOf(it.on, it.calendarId, it.includeValue) },
                    SettingsRepository(ctx.db).observe().map { it.language },
                ) { lessons, setup, language -> Triple(lessons, setup, language) }
            }.debounce(SYNC_SETTLE_MS)
            .map { sync() }

    /** Busy time for the Agenda, when Mostrar outros compromissos is on (AC-012-17). */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun busy(
        fromMs: Long,
        toMs: Long,
    ): Flow<List<BusyEvent>> =
        store.observe().map { it.showBusy && phone.hasPermission() }.flatMapLatest { show ->
            if (show) phone.busy(fromMs, toMs) else flowOf(emptyList())
        }

    fun calendars(): List<PhoneCalendarInfo> = if (phone.hasPermission()) phone.calendars().filter { !it.own } else emptyList()

    companion object {
        /** Waits for a burst of writes (filling 8 weeks, a restore) to settle, then syncs once. */
        const val SYNC_SETTLE_MS = 800L
    }
}

/** Placeholder words for tests and for a Ctx built without the app's resources. */
val NO_CALENDAR_WORDS: (Language) -> CalendarWords = {
    CalendarWords("", "", "%1\$s", "", { b -> b.name }, { c -> c.toString() })
}
