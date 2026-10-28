package app.tutora

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.data.ScheduleRepository
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.CancelReason
import app.tutora.domain.Charge
import app.tutora.domain.Lesson
import app.tutora.domain.LessonStatus
import app.tutora.domain.PlaceKind
import app.tutora.domain.PlaceLink
import app.tutora.domain.PriceSource
import app.tutora.domain.RateType
import app.tutora.domain.Repeat
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.Worth
import app.tutora.domain.builtinPlace
import app.tutora.domain.localOf
import app.tutora.domain.worth
import app.tutora.usecases.Cancellation
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
import app.tutora.usecases.ScheduleError
import app.tutora.usecases.ScheduleResult
import app.tutora.usecases.WhoId
import kotlinx.coroutines.flow.first
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
class LessonsTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var t: TestDb
    private lateinit var ana: Student
    private val online = BuiltinPlace.ONLINE.id
    private val home = BuiltinPlace.STUDENT_HOME.id
    private val zone = "America/Sao_Paulo"

    @Before
    fun setUp() =
        runTest {
            t = TestDb(tmp.root)
            t.catalog.ensureBuiltinPlaces()
            t.catalog.savePlace(builtinPlace(BuiltinPlace.STUDENT_HOME).copy(rateCents = 10_000))
            ana =
                (
                    t.catalog.saveStudent(
                        Student("", "Ana Souza", "Matemática", null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(online, true, 12_000), PlaceLink(home, false, null)),
                    ) as Result.Ok
                ).value
        }

    private fun draft(
        date: String = "2026-10-26",
        time: LocalTime = LocalTime(8, 0),
        repeat: Repeat? = null,
        place: String = online,
        minutes: Int = 60,
    ) = LessonDraft(WhoId.OfStudent(ana.id), place, LocalDate.parse(date), time, minutes, null, null, repeat)

    private suspend fun all(): List<Lesson> =
        ScheduleRepository(t.db).between("2026-01-01T00:00:00.000Z", "2027-12-31T00:00:00.000Z").first()

    private suspend fun one(id: String?): Lesson = all().first { it.id == id }

    private suspend fun schedule(d: LessonDraft = draft()): String? = (t.schedule.scheduleLesson(d) as ScheduleResult.Ok).lessonId

    private suspend fun cancel(
        id: String?,
        reason: CancelReason = CancelReason.STUDENT,
        charge: Charge? = null,
    ) = t.lessons.cancel(id ?: "", Cancellation(reason, "viagem", charge))

    @Test
    fun `AC-006-4 marking a series class done changes only it, and filling never overwrites it`() =
        runTest {
            val first = schedule(draft(repeat = Repeat.WEEKLY))
            t.lessons.markDone(first ?: "", 90)
            t.now = "2026-11-02T12:00:00.000Z"
            t.schedule.fillSchedule()
            val lessons = all()
            assertEquals(LessonStatus.DONE, one(first).status)
            assertEquals(90, one(first).plannedMin)
            assertEquals(18_000L, (one(first).worth() as Worth.Set).cents)
            assertTrue(lessons.filter { it.id != first }.all { it.status == LessonStatus.SCHEDULED && it.plannedMin == 60 })
        }

    @Test
    fun `AC-006-6 Registrar refuses a later date, a start still ahead and an unlinked place`() =
        runTest {
            val salinha =
                (
                    t.catalog.savePlace(
                        builtinPlace(BuiltinPlace.ONLINE).copy(id = "", builtin = null, name = "Salinha", kind = PlaceKind.SALINHA),
                    ) as Result.Ok
                ).value
            assertEquals(ScheduleResult.Err(ScheduleError.FUTURE_DATE), t.lessons.logLesson(draft(date = "2026-10-27")))
            assertEquals(ScheduleResult.Err(ScheduleError.NOT_STARTED), t.lessons.logLesson(draft(time = LocalTime(10, 0))))
            assertEquals(ScheduleResult.Err(ScheduleError.PLACE_NOT_LINKED), t.lessons.logLesson(draft(place = salinha.id)))
        }

    @Test
    fun `AC-006-8 a logged lesson is done on its date with its value`() =
        runTest {
            val r = t.lessons.logLesson(draft(date = "2026-10-25", time = LocalTime(14, 0), minutes = 90)) as ScheduleResult.Ok
            val l = one(r.lessonId)
            assertEquals(LessonStatus.DONE, l.status)
            assertEquals("2026-10-25T14:00", localOf(l.startAt, zone).toString())
            assertEquals(18_000L, (l.worth() as Worth.Set).cents)
        }

    @Test
    fun `AC-006-10 cancelling a series class changes only it, and filling never brings it back`() =
        runTest {
            val first = schedule(draft(date = "2026-10-27", time = LocalTime(14, 0), repeat = Repeat.WEEKLY))
            cancel(first)
            t.now = "2026-11-02T12:00:00.000Z"
            t.schedule.fillSchedule()
            assertEquals(LessonStatus.CANCELLED, one(first).status)
            assertTrue(all().filter { it.id != first }.all { it.status == LessonStatus.SCHEDULED })
        }

    @Test
    fun `AC-006-11 Aluno faltou and Marcar como dada wait for the start, cancelling does not`() =
        runTest {
            val ahead = schedule(draft(time = LocalTime(14, 0)))
            assertEquals(ScheduleResult.Err(ScheduleError.NOT_STARTED), cancel(ahead, CancelReason.NO_SHOW))
            assertEquals(ScheduleResult.Err(ScheduleError.NOT_STARTED), t.lessons.markDone(ahead ?: "", 60))
            assertEquals(ScheduleResult.Ok(ahead), cancel(ahead, CancelReason.TEACHER))
        }

    @Test
    fun `AC-006-13 Desfazer puts a class back to Agendada, following the current price, with no reason or charge`() =
        runTest {
            val done = schedule(draft(place = home))
            val cancelled = schedule(draft(place = home, time = LocalTime(7, 0)))
            t.lessons.markDone(done ?: "", 60)
            cancel(cancelled, charge = Charge.Percent(10_000))
            t.catalog.savePlace(builtinPlace(BuiltinPlace.STUDENT_HOME).copy(rateCents = 20_000))
            assertEquals(10_000L, (one(done).worth() as Worth.Set).cents)
            t.lessons.undo(done ?: "")
            t.lessons.undo(cancelled ?: "")
            assertEquals(Worth.Set(20_000, PriceSource.PLACE), one(done).worth())
            assertEquals(LessonStatus.SCHEDULED, one(cancelled).status)
            assertEquals(null, one(cancelled).feeCents)
            assertEquals(null, one(cancelled).cancelReason)
        }

    @Test
    fun `AC-006-17 the next class skips cancelled ones`() =
        runTest {
            val a = schedule(draft(time = LocalTime(14, 0)))
            val b = schedule(draft(time = LocalTime(16, 0)))
            cancel(a)
            assertEquals(b, ScheduleRepository(t.db).firstAfter(t.now).first()?.id)
        }

    @Test
    fun `AC-006-22 confirming saves the status, the reason and the charge, which stays when the price changes`() =
        runTest {
            val half = schedule()
            val missed = schedule(draft(time = LocalTime(7, 0)))
            val mine = schedule(draft(time = LocalTime(6, 0)))
            cancel(half, charge = Charge.Percent(5_000))
            cancel(missed, CancelReason.NO_SHOW, Charge.Amount(4_000))
            cancel(mine, CancelReason.TEACHER, Charge.Percent(10_000))
            t.catalog.saveStudent(ana, listOf(PlaceLink(online, true, 30_000)))
            assertEquals(LessonStatus.CANCELLED, one(half).status)
            assertEquals(CancelReason.STUDENT, one(half).cancelReason)
            assertEquals("viagem", one(half).cancelNote)
            assertEquals(6_000L, one(half).feeCents)
            assertEquals(LessonStatus.MISSED, one(missed).status)
            assertEquals(4_000L, one(missed).feeCents)
            // "Eu cancelei" is never charged (AC-006-20).
            assertEquals(null, one(mine).feeCents)
        }
}
