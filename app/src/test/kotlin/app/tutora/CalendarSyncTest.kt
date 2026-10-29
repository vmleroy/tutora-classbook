package app.tutora

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.data.BackupStore
import app.tutora.data.CalendarStore
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.CancelReason
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Repeat
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.usecases.Backups
import app.tutora.usecases.CalendarSync
import app.tutora.usecases.CalendarTarget
import app.tutora.usecases.Cancellation
import app.tutora.usecases.LessonChange
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
import app.tutora.usecases.ScheduleResult
import app.tutora.usecases.Scope
import app.tutora.usecases.WhoId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

// Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
class CalendarSyncTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var t: TestDb
    private lateinit var sync: CalendarSync
    private lateinit var ana: Student
    private val online = BuiltinPlace.ONLINE.id
    private val own = CalendarTarget.Own("Tutora – Aulas", 0)

    @Before
    fun setUp() =
        runTest {
            t = TestDb(tmp.root)
            sync = CalendarSync(t.ctx)
            t.catalog.ensureBuiltinPlaces()
            CalendarStore(t.db).ensureDefaults(t.now)
            BackupStore(t.db).ensureDefaults(t.now)
            ana =
                (
                    t.catalog.saveStudent(
                        Student("", "Ana Souza", "Matemática", null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(online, true, 12_000)),
                    ) as Result.Ok
                ).value
        }

    private suspend fun schedule(
        day: Int,
        repeat: Repeat? = null,
    ): String? =
        (
            t.schedule.scheduleLesson(
                LessonDraft(WhoId.OfStudent(ana.id), online, LocalDate(2026, 10, day), LocalTime(8, 0), 60, null, null, repeat),
            ) as ScheduleResult.Ok
        ).lessonId

    private fun titles(calendarId: Long?) = t.calendar.inCalendar(calendarId).map { it.event.title }

    @Test
    fun `AC-012-4 Tutora's own phone calendar gets the classes, and no other calendar is written`() =
        runTest {
            t.calendar.theirs(11, "Dentista")
            schedule(27)
            sync.turnOn(own)
            assertEquals(listOf("Ana Souza · Matemática"), titles(t.calendar.own))
            assertEquals(listOf("Dentista"), titles(11))
        }

    @Test
    fun `AC-012-8 Só esta aula and Esta e as próximas touch exactly the events they change, cancelled ones are retitled`() =
        runTest {
            val first = schedule(27, Repeat.WEEKLY)!!
            sync.turnOn(CalendarTarget.Account(t.calendar.accounts[0]))
            val weeks = titles(10).size
            t.schedule.editLesson(first, LessonChange(time = LocalTime(9, 0)), Scope.ONE)
            sync.sync()
            val starts =
                t.calendar
                    .inCalendar(10)
                    .map { it.event.startMs }
                    .sorted()
            assertEquals(weeks, starts.size)
            // Only the first moved: an hour later than a week before the second.
            assertEquals(7 * 24 * 3_600_000L - 3_600_000L, starts[1] - starts[0])
            t.lessons.cancel(first, Cancellation(CancelReason.STUDENT))
            sync.sync()
            assertTrue(titles(10).contains("Cancelada · Ana Souza · Matemática"))
            val second =
                t.calendar
                    .inCalendar(10)
                    .sortedBy { it.event.startMs }[1]
                    .event.lessonId
            t.schedule.removeLesson(second, Scope.FOLLOWING)
            sync.sync()
            assertEquals(listOf("Cancelada · Ana Souza · Matemática"), titles(10))
        }

    @Test
    fun `AC-012-10 the status keeps the time of the last update and how many classes went`() =
        runTest {
            schedule(27)
            schedule(28)
            sync.turnOn(own)
            val s = sync.state()
            assertEquals(t.now to 2, s.syncedAt to s.count)
        }

    @Test
    fun `AC-012-12 choosing another calendar moves Tutora's events and leaves the rest`() =
        runTest {
            t.calendar.theirs(11, "Dentista")
            schedule(27)
            sync.turnOn(CalendarTarget.Account(t.calendar.accounts[1]))
            sync.turnOn(CalendarTarget.Account(t.calendar.accounts[0]))
            assertEquals(listOf("Dentista"), titles(11))
            assertEquals(listOf("Ana Souza · Matemática"), titles(10))
        }

    @Test
    fun `AC-012-13 turning it off removes only Tutora's events and its own calendar`() =
        runTest {
            t.calendar.theirs(10, "Reunião")
            schedule(27)
            sync.turnOn(CalendarTarget.Account(t.calendar.accounts[0]))
            sync.turnOff()
            assertEquals(listOf("Reunião"), titles(10))
            sync.turnOn(own)
            sync.turnOff()
            assertEquals(null, t.calendar.own)
        }

    @Test
    fun `AC-012-14 after a restore the calendar has the restored classes, and the calendar setup stays`() =
        runTest {
            val backups = Backups(t.ctx)
            schedule(27)
            val file = (backups.inspect(backups.makeBackup().second) as app.tutora.domain.BackupRead.Ok).file
            sync.turnOn(own)
            schedule(28)
            sync.sync()
            assertEquals(2, titles(t.calendar.own).size)
            backups.restore(file)
            sync.sync()
            assertTrue(sync.state().on)
            assertEquals(1, titles(t.calendar.own).size)
        }

    @Test
    fun `AC-012-15 Limpar counts every marked event, wherever it is`() =
        runTest {
            schedule(27)
            sync.turnOn(CalendarTarget.Account(t.calendar.accounts[1]))
            // One left from an earlier install, in another calendar.
            t.calendar.insert(
                10,
                t.calendar
                    .inCalendar(11)
                    .first()
                    .event,
                t.zone,
            )
            t.calendar.theirs(10, "Aniversário")
            assertEquals(2, sync.cleanCount())
        }

    @Test
    fun `AC-012-16 Limpar deletes only marked events, never one that just says Criado pelo Tutora, and turns sync off`() =
        runTest {
            schedule(27)
            sync.turnOn(CalendarTarget.Account(t.calendar.accounts[0]))
            val typed = t.calendar.theirs(10, "Ana Souza · Matemática")
            sync.clean()
            assertEquals(
                listOf(typed),
                t.calendar.events.keys
                    .toList(),
            )
            assertTrue(!sync.state().on)
        }
}
