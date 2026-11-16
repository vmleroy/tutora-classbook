package app.tutora

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.PayMethod
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.ui.receipts.attachRoute
import app.tutora.usecases.PaymentDraft
import app.tutora.usecases.PaymentResult
import app.tutora.usecases.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class ReceiptScreensTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness
    private lateinit var anaRef: PayerRef

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
        val ana =
            runBlocking {
                (
                    h.t.catalog.saveStudent(
                        Student("", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(BuiltinPlace.ONLINE.id, true, 12_000)),
                    ) as Result.Ok
                ).value
            }
        anaRef = PayerRef(PayerKind.STUDENT, ana.id)
    }

    private fun matching(text: String) =
        SemanticsMatcher("text is $text") { n ->
            n.config.getOrNull(SemanticsProperties.Text)?.any { it.text.replace('\u00A0', ' ').replace('\u202F', ' ') == text } == true
        }

    private fun exists(text: String) {
        h.awaitDb(compose) { compose.onAllNodes(matching(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(matching(text), useUnmergedTree = true).onFirst().assertExists()
    }

    private fun pay(
        day: LocalDate,
        cents: Long,
    ) = runBlocking { (h.services.payments.record(PaymentDraft(anaRef, cents, PayMethod.PIX, day)) as PaymentResult.Ok).paymentId }

    /** A file shared from the bank app, already copied in as MainActivity does. */
    private fun shared(): app.tutora.ui.AttachReceipt {
        val photo = java.io.File(tmp.newFolder(), "pix.jpg").apply { writeBytes(ByteArray(200) { 3 }) }
        return attachRoute(listOf(h.services.receipts.stage(photo.path)))
    }

    @Test
    fun `AC-021-3 Anexar comprovante lists the last 60 days newest first, attaches to one, or opens a new payment with it`() {
        val recent = pay(LocalDate(2026, 10, 20), 24_000)
        pay(LocalDate(2026, 10, 2), 12_000)
        pay(LocalDate(2026, 8, 20), 9_000)
        val route = shared()
        h.show(compose)
        h.go(compose, route)
        exists("Anexar comprovante")
        exists("De qual pagamento ele é?")
        // Newest first; the payment from August is past 60 days.
        exists("20/10 · Pix · R$ 240,00")
        exists("02/10 · Pix · R$ 120,00")
        assertEquals(0, compose.onAllNodes(matching("20/08 · Pix · R$ 90,00"), useUnmergedTree = true).fetchSemanticsNodes().size)
        compose.onAllNodes(matching("20/10 · Pix · R$ 240,00"), useUnmergedTree = true).onFirst().performClick()
        h.awaitDb(compose) {
            h.services.receiptRepo
                .forPayment(recent)
                .first()
                .size == 1
        }
        assertEquals(
            1,
            runBlocking {
                h.services.receiptRepo
                    .forPayment(recent)
                    .first()
                    .size
            },
        )
        // Another file: Registrar novo pagamento › Quem pagou › the payment sheet with it attached.
        h.go(compose, shared())
        compose.onNodeWithTag("attach-new").performScrollTo().performClick()
        h.settle(compose)
        exists("Quem pagou?")
        compose
            .onAllNodes(matching("Ana Souza") and hasAnyAncestor(hasTestTag("who-paid")), useUnmergedTree = true)
            .onFirst()
            .performClick()
        h.settle(compose)
        exists("Registrar pagamento")
        val inSheet = hasTestTag("thumbnail") and hasAnyAncestor(hasTestTag("receipts-field"))
        h.awaitDb(compose) { compose.onAllNodes(inSheet).fetchSemanticsNodes().size == 1 }
        assertEquals(1, compose.onAllNodes(inSheet).fetchSemanticsNodes().size)
    }
}
