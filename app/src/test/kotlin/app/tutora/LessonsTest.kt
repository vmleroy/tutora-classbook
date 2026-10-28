package app.tutora

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.data.ScheduleRepository
import app.tutora.data.SettingsRepository
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.FeeAmount
import app.tutora.domain.FeeRule
import app.tutora.domain.Language
import app.tutora.domain.Lesson
import app.tutora.domain.LessonStatus
import app.tutora.domain.PlaceKind
import app.tutora.domain.PlaceLink
import app.tutora.domain.PriceSource
import app.tutora.domain.RateType
import app.tutora.domain.Repeat
import app.tutora.domain.Settings
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.Worth
import app.tutora.domain.builtinPlace
import app.tutora.domain.localOf
import app.tutora.domain.worth
import app.tutora.usecases.LessonChange
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
import app.tutora.usecases.ScheduleError
import app.tutora.usecases.ScheduleResult
import app.tutora.usecases.Scope
import app.tutora.usecases.StatusChange
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

    private suspend fun setRule(rule: FeeRule) = SettingsRepository(t.db).save(Settings(Language.PT_BR, feeRule = rule), t.now)

    private suspend fun status(
        id: String?,
        change: StatusChange,
        scope: Scope = Scope.ONE,
        other: LessonChange = LessonChange(),
    ) = t.schedule.editLesson(id ?: "", other.copy(status = change), scope)

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
    fun `AC-006-10 a status change stays on its class even with Esta e as proximas`() =
        runTest {
            val first = schedule(draft(date = "2026-10-27", time = LocalTime(14, 0), repeat = Repeat.WEEKLY))
            val r =
                status(
                    first,
                    StatusChange(LessonStatus.CANCELLED),
                    Scope.FOLLOWING,
                    LessonChange(time = LocalTime(15, 0)),
                ) as ScheduleResult.Ok
            val lessons = all()
            assertEquals(LessonStatus.CANCELLED, one(r.lessonId).status)
            assertTrue(lessons.filter { it.id != r.lessonId }.all { it.status == LessonStatus.SCHEDULED })
            assertTrue(lessons.all { localOf(it.startAt, zone).time == LocalTime(15, 0) })
        }

    @Test
    fun `AC-006-11 Dada and Faltou wait for the start, Cancelada does not`() =
        runTest {
            val ahead = schedule(draft(time = LocalTime(14, 0)))
            assertEquals(ScheduleResult.Err(ScheduleError.NOT_STARTED), status(ahead, StatusChange(LessonStatus.DONE)))
            assertEquals(ScheduleResult.Err(ScheduleError.NOT_STARTED), status(ahead, StatusChange(LessonStatus.MISSED)))
            assertEquals(ScheduleResult.Ok(ahead), status(ahead, StatusChange(LessonStatus.CANCELLED)))
        }

    @Test
    fun `AC-006-13 back to Agendada the value follows the current price again`() =
        runTest {
            val id = schedule(draft(place = home))
            status(id, StatusChange(LessonStatus.DONE))
            t.catalog.savePlace(builtinPlace(BuiltinPlace.STUDENT_HOME).copy(rateCents = 20_000))
            assertEquals(10_000L, (one(id).worth() as Worth.Set).cents)
            status(id, StatusChange(LessonStatus.SCHEDULED))
            assertEquals(Worth.Set(20_000, PriceSource.PLACE), one(id).worth())
        }

    @Test
    fun `AC-006-17 the next class skips cancelled ones`() =
        runTest {
            val a = schedule(draft(time = LocalTime(14, 0)))
            val b = schedule(draft(time = LocalTime(16, 0)))
            status(a, StatusChange(LessonStatus.CANCELLED))
            assertEquals(b, ScheduleRepository(t.db).firstAfter(t.now).first()?.id)
        }

    @Test
    fun `AC-006-22 a cancellation is late when notice came less than the window before the start`() =
        runTest {
            setRule(FeeRule(lateOn = true, windowH = 24, late = FeeAmount.Percent(5_000)))
            val late = schedule(draft(date = "2026-10-27", time = LocalTime(14, 0)))
            val early = schedule(draft(date = "2026-10-28", time = LocalTime(14, 0)))
            status(late, StatusChange(LessonStatus.CANCELLED, noticeAt = "2026-10-27T14:00:00.000Z"))
            status(early, StatusChange(LessonStatus.CANCELLED, noticeAt = "2026-10-26T12:00:00.000Z"))
            assertEquals(6_000L, one(late).feeCents)
            assertEquals(null, one(early).feeCents)
        }

    @Test
    fun `AC-006-23 the fee is kept, or turned off and waived`() =
        runTest {
            setRule(FeeRule(missOn = true))
            val kept = schedule()
            val waived = schedule(draft(time = LocalTime(7, 0)))
            status(kept, StatusChange(LessonStatus.MISSED))
            status(waived, StatusChange(LessonStatus.MISSED, keepFee = false))
            assertEquals(12_000L, one(kept).feeCents)
            assertEquals(null, one(waived).feeCents)
            assertTrue(one(waived).feeWaived)
        }

    @Test
    fun `AC-006-27 a kept fee stays when the rule or the price changes later`() =
        runTest {
            setRule(FeeRule(missOn = true, miss = FeeAmount.Percent(5_000)))
            val id = schedule()
            status(id, StatusChange(LessonStatus.MISSED))
            setRule(FeeRule(missOn = true))
            t.catalog.saveStudent(ana, listOf(PlaceLink(online, true, 30_000)))
            t.schedule.editLesson(id ?: "", LessonChange(notes = "avisou depois"), Scope.ONE)
            assertEquals(6_000L, one(id).feeCents)
        }
}
