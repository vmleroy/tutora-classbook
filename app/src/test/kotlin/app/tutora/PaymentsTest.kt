package app.tutora

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.data.PaymentRepository
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.PayMethod
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.PaymentDraft
import app.tutora.usecases.PaymentError
import app.tutora.usecases.PaymentResult
import app.tutora.usecases.Payments
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

// Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
class PaymentsTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var t: TestDb
    private lateinit var payments: Payments
    private lateinit var repo: PaymentRepository
    private lateinit var ana: Student
    private lateinit var anaRef: PayerRef
    private val online = BuiltinPlace.ONLINE.id
    private val today = LocalDate(2026, 10, 26)

    @Before
    fun setUp() =
        runTest {
            t = TestDb(tmp.root)
            payments = Payments(t.ctx)
            repo = PaymentRepository(t.db)
            t.catalog.ensureBuiltinPlaces()
            ana =
                (
                    t.catalog.saveStudent(
                        Student("", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(online, true, 12_000)),
                    ) as Result.Ok
                ).value
            anaRef = PayerRef(PayerKind.STUDENT, ana.id)
        }

    /** A class given on [day] of October at 08:00 (São Paulo), logged as done. */
    private suspend fun given(
        day: Int,
        paidWith: PayMethod? = null,
    ): String =
        (
            t.lessons.logLesson(
                LessonDraft(WhoId.OfStudent(ana.id), online, LocalDate(2026, 10, day), LocalTime(8, 0), 60, null, null, null),
                paidWith,
            ) as ScheduleResult.Ok
        ).lessonId ?: error("no class")

    private suspend fun balance(): Long = repo.balances().first()[anaRef] ?: 0

    private suspend fun pay(
        cents: Long,
        chosen: List<String> = emptyList(),
    ) = (payments.record(PaymentDraft(anaRef, cents, PayMethod.PIX, today, chosen = chosen)) as PaymentResult.Ok).paymentId

    @Test
    fun `AC-008-3 undoing a done class takes it off the balance, and money paid for it pays the next class`() =
        runTest {
            val first = given(19)
            val second = given(22)
            pay(12_000, chosen = listOf(first))
            assertEquals(12_000L, balance())
            t.lessons.undo(first)
            assertEquals(0L, balance())
            assertTrue(repo.ledgerNow(anaRef).allocation.isPaid(second))
        }

    @Test
    fun `AC-008-10 saving a payment updates the balance at once, and a future date is refused`() =
        runTest {
            given(22)
            assertEquals(12_000L, balance())
            pay(5_000)
            assertEquals(7_000L, balance())
            val future = payments.record(PaymentDraft(anaRef, 1_000, PayMethod.PIX, LocalDate(2026, 10, 27)))
            assertEquals(PaymentResult.Err(PaymentError.FUTURE_DATE), future)
        }

    @Test
    fun `AC-008-12 editing a payment moves the balance, and deleting it puts its classes back to owing`() =
        runTest {
            val lesson = given(22)
            val id = pay(12_000)
            payments.edit(id, PaymentDraft(anaRef, 4_000, PayMethod.CASH, today))
            assertEquals(8_000L, balance())
            payments.delete(id)
            assertEquals(12_000L, balance())
            assertEquals(12_000L, repo.ledgerNow(anaRef).allocation.left[lesson])
        }

    private suspend fun packageStudent() {
        t.catalog.saveStudent(
            ana.copy(rateType = RateType.PACKAGE, packageLessons = 2, packagePriceCents = 20_000),
            listOf(PlaceLink(online, true, null)),
        )
    }

    @Test
    fun `AC-008-20 a package's classes use it instead of owing, and once used up they owe the price per class`() =
        runTest {
            packageStudent()
            payments.buyPackage(ana.id, PayMethod.PIX, today)
            given(19)
            given(20)
            assertEquals(0L, balance())
            given(22)
            assertEquals(10_000L, balance())
            assertEquals(2, repo.lastPackage(ana.id).first()?.used)
        }

    @Test
    fun `AC-008-22 undoing a class that used the package gives it back`() =
        runTest {
            packageStudent()
            payments.buyPackage(ana.id, PayMethod.PIX, today)
            val lesson = given(22)
            assertEquals(1, repo.lastPackage(ana.id).first()?.left)
            t.lessons.undo(lesson)
            assertEquals(2, repo.lastPackage(ana.id).first()?.left)
        }

    @Test
    fun `AC-008-17 logging a class as already paid records its payment and leaves nothing owed`() =
        runTest {
            val lesson = given(22, paidWith = PayMethod.CASH)
            assertEquals(0L, balance())
            val ledger = repo.ledgerNow(anaRef)
            assertEquals(PayMethod.CASH, ledger.payments.single().method)
            assertEquals(listOf(lesson), ledger.allocation.paidBy[ledger.payments.single().id])
        }
}
