package app.tutora

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import app.tutora.data.openTutoraDatabase
import app.tutora.usecases.AutoStartAlarm
import app.tutora.usecases.Catalog
import app.tutora.usecases.Ctx
import app.tutora.usecases.Lessons
import app.tutora.usecases.Live
import app.tutora.usecases.LiveNotifier
import app.tutora.usecases.Schedule
import java.io.File

/** A migrated database in a temp folder, a frozen clock and zone, and counting ids. */
class TestDb(
    dir: File,
    var now: String = "2026-10-26T12:00:00.000Z", // Monday 26 Oct 2026, 09:00 in São Paulo
    val zone: String = "America/Sao_Paulo",
    val notifier: LiveNotifier = LiveNotifier.None,
    val alarm: AutoStartAlarm = AutoStartAlarm.None,
    val folder: FakeBackupFolder = FakeBackupFolder(),
    val scheduler: FakeBackupScheduler = FakeBackupScheduler(),
    val calendar: FakePhoneCalendar = FakePhoneCalendar(),
) {
    private var n = 0
    val db = openTutoraDatabase(ApplicationProvider.getApplicationContext(), File(dir, "tutora.db"), AndroidSQLiteDriver()).first
    val ctx =
        Ctx(
            db,
            { now },
            { "id-" + (++n).toString().padStart(5, '0') },
            { zone },
            notifier,
            alarm,
            folder,
            scheduler,
            File(dir, "files"),
            calendar,
            { TEST_CALENDAR_WORDS },
        )
    val catalog = Catalog(ctx)
    val schedule = Schedule(ctx)
    val lessons = Lessons(ctx)
    val live = Live(ctx)
}

/** A backup folder in memory: what was written, and a switch to make writing fail (AC-011-9). */
class FakeBackupFolder : app.tutora.usecases.BackupFolder {
    val files = linkedMapOf<String, ByteArray>()

    /** Files picked in Restaurar backup, by uri. */
    val picked = mutableMapOf<String, ByteArray>()
    var failing = false

    override fun adopt(uri: String) = "Tutora backups"

    override fun read(uri: String) = picked[uri]

    override fun read(
        uri: String,
        name: String,
    ) = files[name]

    override fun write(
        uri: String,
        name: String,
        bytes: ByteArray,
    ): Long {
        check(!failing) { "Folder gone" }
        files[name] = bytes
        return bytes.size.toLong()
    }

    override fun delete(
        uri: String,
        name: String,
    ) {
        files.remove(name)
    }
}

class FakeBackupScheduler : app.tutora.usecases.BackupScheduler {
    var planned: String? = null

    override fun plan(at: String?) {
        planned = at
    }
}

/** The event texts in Portuguese, as the app's resources have them. */
val TEST_CALENDAR_WORDS =
    app.tutora.domain.CalendarWords(
        "Cancelada · ",
        "Faltou · ",
        "Valor: %1\$s",
        "Criado pelo Tutora",
        { if (it == app.tutora.domain.BuiltinPlace.ONLINE) "Online / Remoto" else "Casa do Aluno" },
        { "R$ ${it / 100},${(it % 100).toString().padStart(2, '0')}" },
    )

/** The phone's calendars in memory: Tutora's events carry the mark, the teacher's own don't (spec 012). */
class FakePhoneCalendar : app.tutora.usecases.PhoneCalendar {
    data class Event(
        val calendarId: Long,
        val event: app.tutora.domain.ClassEvent,
        val marked: Boolean,
    )

    var granted = true
    val accounts =
        mutableListOf(
            app.tutora.usecases.PhoneCalendarInfo(10, "Tutora – Aulas", "victor@gmail.com", "Google"),
            app.tutora.usecases.PhoneCalendarInfo(11, "Pessoal", "victor@outlook.com", "Outlook"),
        )
    var own: Long? = null
    val events = linkedMapOf<Long, Event>()
    private var next = 100L
    val busy = kotlinx.coroutines.flow.MutableStateFlow<List<app.tutora.domain.BusyEvent>>(emptyList())

    /** An event the teacher made: no mark. */
    fun theirs(
        calendarId: Long,
        title: String,
        startMs: Long = 0,
    ): Long {
        val id = next++
        events[id] =
            Event(calendarId, app.tutora.domain.ClassEvent("", startMs, startMs + 3_600_000, title, "", "Criado pelo Tutora"), false)
        return id
    }

    fun inCalendar(id: Long?) = events.values.filter { it.calendarId == id }

    override fun hasPermission() = granted

    override fun calendars() =
        accounts + listOfNotNull(own?.let { app.tutora.usecases.PhoneCalendarInfo(it, "Tutora – Aulas", "", "", own = true) })

    override fun ownCalendar(
        name: String,
        color: Int,
    ): Long = own ?: 1L.also { own = it }

    override fun marked(calendarId: Long) =
        events.filter { it.value.marked && it.value.calendarId == calendarId }.map { app.tutora.domain.MarkedEvent(it.key, it.value.event) }

    override fun markedEverywhere() = events.filter { it.value.marked }.keys.toList()

    override fun insert(
        calendarId: Long,
        event: app.tutora.domain.ClassEvent,
        zone: String,
    ): Long {
        val id = next++
        events[id] = Event(calendarId, event, true)
        return id
    }

    override fun update(
        eventId: Long,
        event: app.tutora.domain.ClassEvent,
        zone: String,
    ) {
        val e = events[eventId]?.takeIf { it.marked } ?: return
        events[eventId] = e.copy(event = event)
    }

    override fun delete(eventId: Long) {
        if (events[eventId]?.marked == true) events.remove(eventId)
    }

    override fun deleteOwnCalendar() {
        val id = own ?: return
        if (events.values.none { it.calendarId == id }) own = null
    }

    override fun busy(
        fromMs: Long,
        toMs: Long,
    ) = busy
}
