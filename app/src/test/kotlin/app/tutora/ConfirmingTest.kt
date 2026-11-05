package app.tutora

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.data.EarningsRepository
import app.tutora.data.PaymentRepository
import app.tutora.data.ScheduleRepository
import app.tutora.data.SettingsRepository
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.CancelReason
import app.tutora.domain.ConfirmWhen
import app.tutora.domain.Lesson
import app.tutora.domain.LessonStatus
import app.tutora.domain.PastClasses
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Repeat
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.instantOf
import app.tutora.domain.toConfirm
import app.tutora.usecases.Cancellation
import app.tutora.usecases.Confirming
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
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

// Classes on Monday 26 Oct 2026 in São Paulo; now moves past them.
@RunWith(AndroidJUnit4::class)
class ConfirmingTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var t: TestDb
    private lateinit var confirming: Confirming
    private lateinit var ana: Student
    private val online = BuiltinPlace.ONLINE.id
    private val zone = "America/Sao_Paulo"

    @Before
    fun setUp() =
        runTest {
            t = TestDb(tmp.root)
            confirming = Confirming(t.ctx)
            t.catalog.ensureBuiltinPlaces()
            ana =
                (
                    t.catalog.saveStudent(
                        Student("", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(online, true, 12_000)),
                    ) as Result.Ok
                ).value
        }

    private suspend fun schedule(
        hour: Int,
        repeat: Repeat? = null,
    ): String =
        (
            t.schedule.scheduleLesson(
                LessonDraft(WhoId.OfStudent(ana.id), online, LocalDate.parse("2026-10-26"), LocalTime(hour, 0), 60, null, null, repeat),
            ) as ScheduleResult.Ok
        ).lessonId ?: ""

    private suspend fun all(): List<Lesson> =
        ScheduleRepository(t.db).between("2026-10-01T00:00:00.000Z", "2027-01-31T00:00:00.000Z").first()

    private suspend fun one(id: String) = all().first { it.id == id }

    private suspend fun toConfirm(): List<String> = all().filter { it.toConfirm(instantOf(t.now), zone, ConfirmWhen.END) }.map { it.id }

    private suspend fun settings(change: (app.tutora.domain.Settings) -> app.tutora.domain.Settings) {
        val repo = SettingsRepository(t.db)
        repo.save(change(repo.load()), t.now)
    }

    private val evening = "2026-10-26T23:00:00.000Z" // 20:00 in São Paulo

    @Test
    fun `AC-014-2 with Marcar como dadas sozinhas a class due becomes Dada sozinha with its planned duration, and can still be changed`() =
        runTest {
            settings { it.copy(pastClasses = PastClasses.AUTO) }
            val a = schedule(14)
            t.now = evening
            assertEquals(listOf(a), confirming.autoConfirm())
            assertEquals(LessonStatus.DONE, one(a).status)
            assertEquals(60, one(a).plannedMin)
            assertTrue(one(a).autoDone)
            t.lessons.cancel(a, Cancellation(CancelReason.STUDENT))
            assertEquals(LessonStatus.CANCELLED, one(a).status)
            assertEquals(false, one(a).autoDone)
        }

    @Test
    fun `AC-014-7 Dada confirms with the planned duration and value, as Marcar como dada, and leaves the list`() =
        runTest {
            val a = schedule(14)
            t.now = evening
            assertEquals(listOf(a), toConfirm())
            confirming.confirm(a)
            assertEquals(LessonStatus.DONE, one(a).status)
            assertEquals(60, one(a).plannedMin)
            assertEquals(emptyList<String>(), toConfirm())
        }

    @Test
    fun `AC-014-8 Cancelada and Faltou from the list record the reason and charge and remove the class`() =
        runTest {
            val a = schedule(14)
            val b = schedule(16)
            t.now = evening
            t.lessons.cancel(a, Cancellation(CancelReason.STUDENT, "viagem"))
            t.lessons.cancel(b, Cancellation(CancelReason.NO_SHOW))
            assertEquals(LessonStatus.CANCELLED, one(a).status)
            assertEquals("viagem", one(a).cancelNote)
            assertEquals(LessonStatus.MISSED, one(b).status)
            assertEquals(emptyList<String>(), toConfirm())
        }

    @Test
    fun `AC-014-9 Confirmar todas marks every class to confirm as Dada and Desfazer puts them back`() =
        runTest {
            val ids = listOf(schedule(10), schedule(14, Repeat.WEEKLY), schedule(16))
            t.now = evening
            val confirmed = confirming.confirmAll()
            assertEquals(ids, confirmed)
            assertTrue(confirmed.all { one(it).status == LessonStatus.DONE })
            confirming.undoAll(confirmed)
            assertTrue(confirmed.all { one(it).status == LessonStatus.SCHEDULED })
            assertEquals(ids, toConfirm())
        }

    @Test
    fun `AC-014-10 a confirmation counts at once in earnings and balances, on the class's own day`() =
        runTest {
            val a = schedule(14)
            t.now = "2026-10-28T12:00:00.000Z"
            val day = LocalDate.parse("2026-10-26")
            val earnings = EarningsRepository(t.db)
            assertEquals(0L, earnings.earnedBetween(day, day, zone).first())
            confirming.confirm(a)
            assertEquals(12_000L, earnings.earnedBetween(day, day, zone).first())
            assertEquals(12_000L, PaymentRepository(t.db).balances().first()[PayerRef(PayerKind.STUDENT, ana.id)])
        }

    @Test
    fun `AC-014-11 a class confirmed from its own page leaves the list`() =
        runTest {
            val a = schedule(14)
            t.now = evening
            t.lessons.markDone(a, 75)
            assertEquals(emptyList<String>(), toConfirm())
            assertEquals(75, one(a).plannedMin)
        }
}
