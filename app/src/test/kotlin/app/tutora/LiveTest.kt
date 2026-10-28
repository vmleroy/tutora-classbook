package app.tutora

import android.app.AlarmManager
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.data.ScheduleRepository
import app.tutora.data.SettingsRepository
import app.tutora.domain.AutoStart
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.Expected
import app.tutora.domain.Lesson
import app.tutora.domain.LessonStatus
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Repeat
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.Worth
import app.tutora.domain.counted
import app.tutora.domain.elapsedMs
import app.tutora.domain.expected
import app.tutora.domain.worth
import app.tutora.live.AndroidAutoStartAlarm
import app.tutora.usecases.AutoStartAlarm
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.LiveNote
import app.tutora.usecases.LiveNotifier
import app.tutora.usecases.LiveResult
import app.tutora.usecases.Result
import app.tutora.usecases.ScheduleResult
import app.tutora.usecases.Scope
import app.tutora.usecases.WhoId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows
import kotlin.time.Instant

/** Remembers what the notification was asked to show. */
private class FakeNotifier : LiveNotifier {
    var shown: LiveNote? = null

    override fun show(note: LiveNote) {
        shown = note
    }

    override fun hide() {
        shown = null
    }
}

/** Remembers the alarm the use cases set. */
private class FakeAlarm : AutoStartAlarm {
    var at: String? = "unset"

    override fun set(at: String?) {
        this.at = at
    }
}

// Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
class LiveTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var t: TestDb
    private val notifier = FakeNotifier()
    private val alarm = FakeAlarm()
    private lateinit var ana: String
    private val online = BuiltinPlace.ONLINE.id

    @Before
    fun setUp() =
        runTest {
            t = TestDb(tmp.root, notifier = notifier, alarm = alarm)
            t.catalog.ensureBuiltinPlaces()
            ana =
                (
                    t.catalog.saveStudent(
                        Student("", "Ana Souza", "Matemática", null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(online, true, 12_000)),
                    ) as Result.Ok
                ).value.id
        }

    private suspend fun lesson(
        date: String = "2026-10-26",
        time: LocalTime = LocalTime(9, 0),
        repeat: Repeat? = null,
    ): String =
        (
            t.schedule.scheduleLesson(
                LessonDraft(WhoId.OfStudent(ana), online, LocalDate.parse(date), time, 60, null, null, repeat),
            ) as ScheduleResult.Ok
        ).lessonId ?: ""

    private suspend fun one(id: String): Lesson? = ScheduleRepository(t.db).lesson(id).first()

    private suspend fun timer(id: String) = ScheduleRepository(t.db).timer(id).first()

    private fun later(time: String) {
        t.now = "2026-10-26T$time.000Z"
    }

    @Test
    fun `AC-007-1 any scheduled class can be started, whatever its day, and goes Ao vivo`() =
        runTest {
            val nextWeek = lesson(date = "2026-11-02")
            assertEquals(LiveResult.Ok, t.live.start(nextWeek))
            assertEquals(LessonStatus.LIVE, one(nextWeek)?.status)
            assertEquals(t.now, timer(nextWeek)?.startedAt)
        }

    @Test
    fun `AC-007-2 only one class is live at a time`() =
        runTest {
            val a = lesson()
            val b = lesson(time = LocalTime(10, 0))
            t.live.start(a)
            assertEquals(LiveResult.AlreadyLive(a), t.live.start(b))
            assertEquals(LessonStatus.SCHEDULED, one(b)?.status)
        }

    @Test
    fun `AC-007-8 the quick note is kept with the clock and added to the notes when the class ends`() =
        runTest {
            val id = lesson()
            t.schedule.editLesson(id, app.tutora.usecases.LessonChange(notes = "Lista 4"), Scope.ONE)
            t.live.start(id)
            t.live.setNote(id, "Revisar equações")
            assertEquals("Revisar equações", timer(id)?.note)
            t.live.end(id, 60)
            assertEquals("Lista 4\nRevisar equações", one(id)?.notes)
        }

    @Test
    fun `AC-007-10 ending saves the class as Dada with the confirmed minutes and its value`() =
        runTest {
            val id = lesson()
            t.live.start(id)
            later("13:02:00")
            t.live.end(id, 62)
            val l = one(id)
            assertEquals(LessonStatus.DONE, l?.status)
            assertEquals(62, l?.plannedMin)
            assertEquals(12_400L, (l?.worth() as Worth.Set).cents)
        }

    @Test
    fun `AC-007-11 Desfazer after ending puts the class back Ao vivo, paused where it ended, with its notes`() =
        runTest {
            val id = lesson()
            t.live.start(id)
            t.live.setNote(id, "Revisar")
            later("12:40:00")
            t.live.end(id, 40)
            later("12:45:00")
            t.live.undoEnd(id)
            val clock = timer(id)
            assertEquals(LessonStatus.LIVE, one(id)?.status)
            assertNull(one(id)?.notes)
            assertEquals(40 * 60_000L, clock?.let { elapsedMs(it, t.now) })
            assertEquals(false, clock?.running)
        }

    @Test
    fun `AC-007-12 Descartar puts the class back to Agendada with no clock`() =
        runTest {
            val id = lesson()
            t.live.start(id)
            t.live.discard(id)
            assertEquals(LessonStatus.SCHEDULED, one(id)?.status)
            assertNull(timer(id))
        }

    @Test
    fun `AC-007-14 a live class counts in Previsto at its value and is not the next class`() =
        runTest {
            val live = lesson(time = LocalTime(10, 0))
            val after = lesson(time = LocalTime(11, 0))
            t.live.start(live)
            val day = ScheduleRepository(t.db).between("2026-10-26T03:00:00.000Z", "2026-10-27T03:00:00.000Z").first()
            assertEquals(Expected(24_000, false), expected(day.mapNotNull { it.counted() }))
            assertEquals(after, ScheduleRepository(t.db).firstAfter(t.now).first()?.id)
        }

    @Test
    fun `AC-007-16 the notification shows while live and goes away when ended, discarded or removed`() =
        runTest {
            val a = lesson()
            t.live.start(a)
            assertEquals("Ana Souza", notifier.shown?.title)
            t.live.end(a, 60)
            assertNull(notifier.shown)
            val b = lesson(time = LocalTime(10, 0))
            t.live.start(b)
            t.live.discard(b)
            assertNull(notifier.shown)
            t.live.start(b)
            assertTrue(notifier.shown != null)
            t.schedule.removeLesson(b, Scope.ONE)
            assertNull(notifier.shown)
        }

    private suspend fun autoStart(mode: AutoStart) {
        val repo = SettingsRepository(t.db)
        repo.save(repo.load().copy(autoStart = mode), t.now)
    }

    @Test
    fun `AC-007-20 a class whose time has come starts by itself from its planned start, unless it ended, was discarded or it is off`() =
        runTest {
            val over = lesson(time = LocalTime(7, 0))
            val due = lesson(time = LocalTime(8, 30))
            t.live.autoStart()
            assertEquals(LessonStatus.SCHEDULED, one(due)?.status)
            autoStart(AutoStart.APP_OPEN)
            t.live.autoStart()
            assertEquals(LessonStatus.LIVE, one(due)?.status)
            assertEquals("2026-10-26T11:30:00.000Z", timer(due)?.startedAt)
            assertEquals(LessonStatus.SCHEDULED, one(over)?.status)
            t.live.discard(due)
            t.live.autoStart()
            assertEquals(LessonStatus.SCHEDULED, one(due)?.status)
        }

    @Test
    fun `AC-007-21 only with the app open sets no alarm`() =
        runTest {
            lesson(time = LocalTime(14, 0))
            autoStart(AutoStart.APP_OPEN)
            t.live.autoStart()
            assertNull(alarm.at)
        }

    @Test
    fun `AC-007-22 also with the app closed sets the alarm at the next class's start, and the phone alarm is set`() =
        runTest {
            lesson(time = LocalTime(14, 0))
            autoStart(AutoStart.ALWAYS)
            t.live.autoStart()
            assertEquals("2026-10-26T17:00:00.000Z", alarm.at)
            val context = ApplicationProvider.getApplicationContext<Application>()
            AndroidAutoStartAlarm(context).set(alarm.at)
            val manager = context.getSystemService(AlarmManager::class.java)
            assertEquals(
                Instant.parse("2026-10-26T17:00:00.000Z").toEpochMilliseconds(),
                Shadows.shadowOf(manager).peekNextScheduledAlarm()?.triggerAtMs,
            )
        }
}
