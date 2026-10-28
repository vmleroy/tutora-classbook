package app.tutora

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.data.ScheduleRepository
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.Lesson
import app.tutora.domain.Payer
import app.tutora.domain.PlaceKind
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Repeat
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.builtinPlace
import app.tutora.domain.localOf
import app.tutora.usecases.LessonChange
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
import app.tutora.usecases.ScheduleError
import app.tutora.usecases.ScheduleResult
import app.tutora.usecases.Scope
import app.tutora.usecases.WhoId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var t: TestDb
    private lateinit var ana: String
    private val online = BuiltinPlace.ONLINE.id
    private val zone = "America/Sao_Paulo"

    @Before
    fun setUp() =
        runTest {
            t = TestDb(tmp.root)
            t.catalog.ensureBuiltinPlaces()
            ana =
                (
                    t.catalog.saveStudent(
                        Student("", "Ana Souza", "Matemática", null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(online, true, 12_000), PlaceLink(BuiltinPlace.STUDENT_HOME.id, false, null)),
                    ) as Result.Ok
                ).value.id
        }

    private fun draft(
        date: String = "2026-10-26",
        time: LocalTime = LocalTime(14, 0),
        repeat: Repeat? = Repeat.WEEKLY,
        place: String = online,
        minutes: Int = 60,
        travel: Long? = null,
    ) = LessonDraft(WhoId.OfStudent(ana), place, LocalDate.parse(date), time, minutes, null, travel, repeat)

    private suspend fun all(): List<Lesson> =
        ScheduleRepository(t.db).between("2026-01-01T00:00:00.000Z", "2027-12-31T00:00:00.000Z").first()

    private suspend fun dates() = all().map { localOf(it.startAt, zone).date.toString() }

    private fun later(days: Int) {
        t.now =
            kotlin.time.Instant
                .parse("2026-10-26T12:00:00.000Z")
                .plus(kotlin.time.Duration.parse("${days}d"))
                .toString()
    }

    private suspend fun series(): List<Lesson> {
        t.schedule.scheduleLesson(draft())
        return all()
    }

    @Test
    fun `AC-005-2 only the student's linked places, or the group's place, are allowed`() =
        runTest {
            val p =
                (
                    t.catalog.savePlace(
                        builtinPlace(
                            BuiltinPlace.ONLINE,
                        ).copy(id = "", builtin = null, name = "Salinha", kind = PlaceKind.SALINHA, payer = Payer.PLACE),
                    ) as Result.Ok
                ).value
            assertEquals(ScheduleResult.Err(ScheduleError.PLACE_NOT_LINKED), t.schedule.scheduleLesson(draft(place = p.id)))
        }

    @Test
    fun `AC-005-3 a class keeps its date, start time and duration`() =
        runTest {
            t.schedule.scheduleLesson(draft(repeat = null, time = LocalTime(16, 30), minutes = 90))
            val l = all().single()
            assertEquals("2026-10-26T16:30", localOf(l.startAt, zone).toString())
            assertEquals(90, l.plannedMin)
        }

    @Test
    fun `AC-005-5 Casa do Aluno with travel varies suggests the last fee for that student`() =
        runTest {
            t.schedule.scheduleLesson(draft(place = BuiltinPlace.STUDENT_HOME.id, repeat = null, travel = 2500))
            assertEquals(2500L, t.schedule.suggestedTravel(ana, BuiltinPlace.STUDENT_HOME.id))
        }

    @Test
    fun `AC-005-8 filling keeps 8 weeks ahead and never duplicates`() =
        runTest {
            t.schedule.scheduleLesson(draft())
            assertEquals(9, all().size) // 26/10 to 21/12
            t.schedule.fillSchedule()
            t.schedule.fillSchedule()
            assertEquals(9, all().size)
            later(7)
            t.schedule.fillSchedule()
            assertEquals("2026-12-28", dates().last())
            assertEquals(10, all().size)
        }

    @Test
    fun `AC-005-10 a class opens with its date, time, duration, value, notes and series`() =
        runTest {
            val l = series().first()
            assertEquals(Repeat.WEEKLY, l.repeat)
            assertEquals(60, l.plannedMin)
            assertEquals(null, l.valueCents)
            assertEquals(null, l.notes)
        }

    @Test
    fun `AC-005-11 a recurring class can change only itself or itself and the following`() =
        runTest {
            val ls = series()
            assertTrue(t.schedule.editLesson(ls[1].id, LessonChange(plannedMin = 90), Scope.ONE) is ScheduleResult.Ok)
            assertTrue(t.schedule.editLesson(ls[2].id, LessonChange(plannedMin = 45), Scope.FOLLOWING) is ScheduleResult.Ok)
        }

    @Test
    fun `AC-005-12 only this class changes, and filling never overwrites it`() =
        runTest {
            val ls = series()
            t.schedule.editLesson(ls[1].id, LessonChange(date = LocalDate.parse("2026-11-03"), time = LocalTime(10, 0)), Scope.ONE)
            later(14)
            t.schedule.fillSchedule()
            val moved = all().first { it.id == ls[1].id }
            assertEquals("2026-11-03T10:00", localOf(moved.startAt, zone).toString())
            assertTrue(moved.isDetached)
            assertFalse("2026-11-02" in dates())
        }

    @Test
    fun `AC-005-13 this and following moves later classes and keeps hand-changed ones`() =
        runTest {
            val ls = series()
            t.schedule.editLesson(ls[4].id, LessonChange(valueCents = 9000), Scope.ONE) // 23/11 changed on its own
            t.schedule.editLesson(ls[2].id, LessonChange(date = LocalDate.parse("2026-11-10"), time = LocalTime(15, 0)), Scope.FOLLOWING)
            val after = all().map { localOf(it.startAt, zone) to it.valueCents }
            assertEquals(listOf("2026-10-26", "2026-11-02"), after.take(2).map { it.first.date.toString() })
            assertTrue(after.any { it.first.toString() == "2026-11-10T15:00" })
            assertFalse(after.any { it.first.toString() == "2026-11-09T14:00" })
            assertEquals(9000L, after.first { it.first.date.toString() == "2026-11-23" }.second)
        }

    @Test
    fun `AC-005-14 removing this and following ends the series`() =
        runTest {
            val ls = series()
            t.schedule.removeLesson(ls[1].id, Scope.ONE)
            later(1)
            t.schedule.fillSchedule()
            assertFalse("2026-11-02" in dates())
            t.schedule.removeLesson(ls[3].id, Scope.FOLLOWING)
            later(30)
            t.schedule.fillSchedule()
            assertEquals(listOf("2026-10-26", "2026-11-09"), dates())
        }

    @Test
    fun `AC-005-15 past classes can change on their own, and series changes never reach them`() =
        runTest {
            val ls = series()
            later(8) // past the 26/10 and 02/11 classes
            assertTrue(t.schedule.editLesson(ls[0].id, LessonChange(plannedMin = 30), Scope.ONE) is ScheduleResult.Ok)
            assertEquals(
                ScheduleResult.Err(ScheduleError.STARTED),
                t.schedule.editLesson(ls[0].id, LessonChange(plannedMin = 30), Scope.FOLLOWING),
            )
            t.schedule.editLesson(ls[2].id, LessonChange(time = LocalTime(18, 0)), Scope.FOLLOWING)
            val second = all().first { it.id == ls[1].id }
            assertEquals("14:00", localOf(second.startAt, zone).time.toString())
            assertTrue(t.schedule.removeLesson(ls[1].id, Scope.ONE) is ScheduleResult.Ok)
        }
}
