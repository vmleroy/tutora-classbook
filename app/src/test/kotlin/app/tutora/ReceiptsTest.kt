package app.tutora

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.data.PaymentRepository
import app.tutora.data.ReceiptRepository
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.PayMethod
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.usecases.PaymentDraft
import app.tutora.usecases.PaymentResult
import app.tutora.usecases.Payments
import app.tutora.usecases.ReceiptRef
import app.tutora.usecases.Receipts
import app.tutora.usecases.Result
import app.tutora.usecases.StageResult
import app.tutora.usecases.StagedReceipt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

// Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
class ReceiptsTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var t: TestDb
    private lateinit var payments: Payments
    private lateinit var receipts: Receipts
    private lateinit var anaRef: PayerRef
    private val today = LocalDate(2026, 10, 26)

    @Before
    fun setUp() =
        runTest {
            t = TestDb(tmp.newFolder("app"))
            payments = Payments(t.ctx)
            receipts = Receipts(t.ctx)
            t.catalog.ensureBuiltinPlaces()
            val ana =
                (
                    t.catalog.saveStudent(
                        Student("", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(BuiltinPlace.ONLINE.id, true, 12_000)),
                    ) as Result.Ok
                ).value
            anaRef = PayerRef(PayerKind.STUDENT, ana.id)
        }

    /** A file in another app's folder (the gallery, WhatsApp), staged as the sheet does when it is picked. */
    private fun picked(name: String): Pair<File, StagedReceipt> {
        val original = File(tmp.newFolder(), name).apply { writeBytes(ByteArray(1_000) { it.toByte() }) }
        return original to (receipts.stage(original.path) as StageResult.Ok).staged
    }

    private suspend fun rows(paymentId: String) = ReceiptRepository(t.db).forPayment(paymentId).first()

    private fun draft(refs: List<ReceiptRef>) = PaymentDraft(anaRef, 24_000, PayMethod.PIX, today, receipts = refs)

    @Test
    fun `AC-021-1 a payment holds as many comprovantes as the teacher adds, and editing adds and takes them off`() =
        runTest {
            val five = (1..5).map { picked(if (it == 3) "banco.pdf" else "print$it.jpg").second }
            val id = (payments.record(draft(five.map { ReceiptRef.New(it) })) as PaymentResult.Ok).paymentId
            val kept = rows(id)
            assertEquals(5, kept.size)
            assertEquals(
                5,
                PaymentRepository(t.db)
                    .ledger(anaRef)
                    .first()
                    .payments
                    .single()
                    .receipts,
            )
            // Editar: the second one taken off, a sixth added at the end.
            val sixth = picked("print6.png").second
            val refs = kept.filter { it.position != 1 }.map { ReceiptRef.Existing(it.id) } + ReceiptRef.New(sixth)
            payments.edit(id, draft(refs))
            val after = rows(id)
            assertEquals(5, after.size)
            assertFalse(after.any { it.id == kept[1].id })
            assertFalse(receipts.store.file(kept[1].file).exists())
            assertEquals("image/png", after.last().mime)
        }

    @Test
    fun `AC-021-2 Tutora keeps its own copy, so deleting the original loses nothing`() =
        runTest {
            val (original, staged) = picked("comprovante.jpg")
            val id = (payments.record(draft(listOf(ReceiptRef.New(staged)))) as PaymentResult.Ok).paymentId
            original.delete()
            val row = rows(id).single()
            assertTrue(receipts.store.file(row.file).exists())
            assertEquals(1_000L, row.bytes)
        }

    @Test
    fun `AC-021-6 removing a payment removes its comprovantes, and Desfazer brings both back`() =
        runTest {
            val id = (payments.record(draft(listOf(ReceiptRef.New(picked("pix.jpg").second)))) as PaymentResult.Ok).paymentId
            val row = rows(id).single()
            val deleted = checkNotNull(payments.delete(id))
            assertEquals(emptyList<Any>(), rows(id))
            payments.restore(deleted)
            assertEquals(listOf(row), rows(id))
            assertTrue(receipts.store.file(row.file).exists())
            // Removed and not brought back: the start-up sweep lets the file go.
            payments.delete(id)
            receipts.sweep()
            assertFalse(receipts.store.file(row.file).exists())
        }
}
