package app.tutora

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.ui.EarningsGraph
import app.tutora.ui.StudentDetail
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
import app.tutora.usecases.WhoId
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class InvoicesScreenTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness
    private lateinit var ana: Student

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
        val online = BuiltinPlace.ONLINE.id
        ana =
            runBlocking {
                (
                    h.t.catalog.saveStudent(
                        Student("", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null, guardian = "Maria Souza"),
                        listOf(PlaceLink(online, true, 12_000)),
                    ) as Result.Ok
                ).value
            }
        runBlocking {
            h.t.lessons.logLesson(
                LessonDraft(WhoId.OfStudent(ana.id), online, LocalDate(2026, 10, 22), LocalTime(8, 0), 60, null, null, null),
            )
        }
    }

    private fun plain(s: String) = s.replace('\u00A0', ' ').replace('\u202F', ' ')

    private fun openInvoices() {
        h.show(compose)
        h.go(compose, EarningsGraph)
        compose.onNodeWithTag("open-invoices").performClick()
        h.settle(compose)
    }

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        h.settle(compose)
        h.awaitDb(compose) { h.exports.shared.isNotEmpty() || h.exports.copied.isNotEmpty() }
    }

    @Test
    fun `AC-020-24 each row shares its Fatura or copies its text, and shows a check once sent`() {
        openInvoices()
        compose.onNodeWithTag("invoices-screen").assertExists()
        compose.onNodeWithTag("due-Ana Souza").assertExists()
        tap("due-pdf-Ana Souza")
        assertEquals(
            "Ana Souza - fatura - outubro 2026.pdf",
            h.exports.shared
                .single()
                .first,
        )
        compose.onNodeWithContentDescription("Enviada").assertExists()
        tap("due-text-Ana Souza")
        h.awaitDb(compose) { h.exports.copied.isNotEmpty() }
        assertTrue(plain(h.exports.copied.single()).contains("Total a pagar: R$ 120,00"))
    }

    @Test
    fun `AC-020-25 a Vencimento set at the top goes on every Fatura sent from the list`() {
        openInvoices()
        compose.onNodeWithTag("due-date").performClick()
        h.settle(compose)
        compose
            .onAllNodes(
                SemanticsMatcher("OK") { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "OK" } == true },
                true,
            ).onFirst()
            .performClick()
        h.settle(compose)
        tap("due-text-Ana Souza")
        h.awaitDb(compose) { h.exports.copied.isNotEmpty() }
        assertTrue(
            h.exports.copied.single(),
            h.exports.copied
                .single()
                .contains("(até 26/10)"),
        )
    }

    @Test
    fun `AC-020-17 the student's detail shows the Responsavel`() {
        h.show(compose)
        h.go(compose, StudentDetail(ana.id))
        compose
            .onAllNodes(
                SemanticsMatcher("guardian") { n ->
                    n.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "Responsável: Maria Souza" } == true
                },
                true,
            ).onFirst()
            .assertExists()
    }
}
