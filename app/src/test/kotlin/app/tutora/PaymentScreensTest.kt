package app.tutora

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.Group
import app.tutora.domain.PayMethod
import app.tutora.domain.Payer
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.PlaceKind
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.builtinPlace
import app.tutora.ui.LessonDetail
import app.tutora.ui.LiveLesson
import app.tutora.ui.PlaceDetail
import app.tutora.ui.RegistryHome
import app.tutora.ui.StudentDetail
import app.tutora.ui.StudentEdit
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.PaymentDraft
import app.tutora.usecases.Result
import app.tutora.usecases.ScheduleResult
import app.tutora.usecases.WhoId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class PaymentScreensTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness
    private lateinit var ana: Student
    private val online = BuiltinPlace.ONLINE.id
    private val today = LocalDate(2026, 10, 26)

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
        ana =
            runBlocking {
                (
                    h.t.catalog.saveStudent(
                        Student("", "Ana Souza", "Matemática", null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(online, true, 12_000)),
                    ) as Result.Ok
                ).value
            }
    }

    private val anaRef get() = PayerRef(PayerKind.STUDENT, ana.id)

    /** A class Ana was given on [day] of October at 08:00, R$ 120,00. */
    private fun given(day: Int): String =
        runBlocking {
            val r =
                h.t.lessons.logLesson(
                    LessonDraft(WhoId.OfStudent(ana.id), online, LocalDate(2026, 10, day), LocalTime(8, 0), 60, null, null, null),
                )
            (r as ScheduleResult.Ok).lessonId ?: ""
        }

    private fun pay(cents: Long) = runBlocking { h.services.payments.record(PaymentDraft(anaRef, cents, PayMethod.PIX, today)) }

    private fun plain(s: String) = s.replace(' ', ' ').replace(' ', ' ')

    /** A node whose text is [text], or contains it when [part]. */
    private fun matching(
        text: String,
        part: Boolean = false,
    ) = SemanticsMatcher("text is $text") { n ->
        n.config.getOrNull(SemanticsProperties.Text)?.any { if (part) plain(it.text).contains(text) else plain(it.text) == text } == true
    }

    private fun exists(
        text: String,
        part: Boolean = false,
    ) {
        compose.onAllNodes(matching(text, part), useUnmergedTree = true).onFirst().assertExists()
    }

    /** Taps [text]; inside a sheet there is nothing to scroll. */
    private fun click(
        text: String,
        scroll: Boolean = true,
    ) {
        val node = compose.onAllNodes(matching(text), useUnmergedTree = true).onFirst()
        if (scroll) node.performScrollTo()
        node.performClick()
        h.settle(compose)
    }

    private fun openAna() {
        h.show(compose)
        h.go(compose, StudentDetail(ana.id))
    }

    @Test
    fun `AC-008-5 the payment sheet starts at the balance, with the four methods, the date and the save amount`() {
        given(19)
        given(22)
        openAna()
        click("Registrar pagamento")
        listOf("Pix", "Dinheiro", "Transferência", "Cartão", "Observação", "Registrar · R$ 240,00").forEach { exists(it) }
    }

    @Test
    fun `AC-008-6 the sheet says which classes the payment pays, oldest first`() {
        given(19)
        given(22)
        openAna()
        click("Registrar pagamento")
        exists("Paga 2 aulas: 19/10 e 22/10")
    }

    @Test
    fun `AC-008-7 Escolher aulas lists the owed classes and ticking sets the amount`() {
        given(19)
        given(22)
        openAna()
        click("Registrar pagamento")
        click("Escolher aulas", scroll = false)
        compose.onAllNodes(matching("22/10 · 60 min · falta R$ 120,00"), useUnmergedTree = true).onFirst().performClick()
        h.settle(compose)
        exists("Registrar · R$ 120,00")
        exists("Paga 1 aula: 22/10")
    }

    @Test
    fun `AC-008-9 a class paid in full shows Pago and one paid in part what is left`() {
        given(19)
        given(22)
        pay(18_000)
        openAna()
        exists("Pago")
        exists("A receber · falta R$ 60,00")
    }

    @Test
    fun `AC-008-11 the detail shows the balance and Pagamentos, newest first, or none yet`() {
        given(19)
        given(22)
        openAna()
        exists("Deve")
        exists("Nenhum pagamento ainda")
        pay(24_000)
        h.settle(compose)
        exists("+ R$ 240,00")
        exists("26/10 · Pix · 2 aulas")
        exists("Em dia")
    }

    @Test
    fun `AC-008-13 Alunos rows show the balance and Devendo lists only who owes`() {
        given(22)
        runBlocking {
            h.t.catalog.saveStudent(
                Student("", "Pedro Lima", null, null, null, TagColor.BLUE, RateType.HOUR, null),
                listOf(PlaceLink(online, true, 12_000)),
            )
        }
        h.show(compose)
        h.go(compose, RegistryHome())
        exists("Deve R$ 120")
        exists("Em dia")
        click("Devendo · 1")
        exists("Ana Souza")
        compose.onAllNodes(matching("Pedro Lima")).assertCountEquals(0)
    }

    @Test
    fun `AC-008-14 a place that pays shows what it owes on Locais and Receber da salinha on its detail`() {
        val salinha =
            runBlocking {
                (
                    h.t.catalog.savePlace(
                        builtinPlace(BuiltinPlace.ONLINE).copy(
                            id = "",
                            builtin = null,
                            name = "Salinha Aprender+",
                            kind = PlaceKind.SALINHA,
                            payer = Payer.PLACE,
                            rateCents = 12_000,
                        ),
                    ) as Result.Ok
                ).value.id
            }
        runBlocking {
            h.t.catalog.linkStudents(listOf(ana.id to salinha))
            h.t.lessons.logLesson(
                LessonDraft(WhoId.OfStudent(ana.id), salinha, LocalDate(2026, 10, 22), LocalTime(8, 0), 60, null, null, null),
            )
        }
        h.show(compose)
        h.go(compose, RegistryHome("places"))
        exists("Deve R$ 120")
        h.go(compose, PlaceDetail(salinha))
        exists("Me deve")
        click("Receber da salinha")
        exists("Registrar · R$ 120,00")
    }

    @Test
    fun `AC-008-15 Hoje shows who has a pending payment and opens whoever owes`() {
        given(22)
        val pedro =
            runBlocking {
                (
                    h.t.catalog.saveStudent(
                        Student("", "Pedro Lima", null, null, null, TagColor.BLUE, RateType.HOUR, null),
                        listOf(PlaceLink(online, true, 12_000)),
                    ) as Result.Ok
                ).value.id
            }
        runBlocking {
            h.t.lessons.logLesson(
                LessonDraft(WhoId.OfStudent(pedro), online, LocalDate(2026, 10, 22), LocalTime(10, 0), 60, null, null, null),
            )
        }
        h.show(compose)
        exists("2 alunos com pagamento pendente")
        compose.onNodeWithTag("pending-alert").performClick()
        h.settle(compose)
        compose.onNode(matching("Devendo · 2"), useUnmergedTree = true).assertExists()
        // Paid up, Pedro leaves the alert, which now opens Ana herself.
        runBlocking { h.services.payments.record(PaymentDraft(PayerRef(PayerKind.STUDENT, pedro), 12_000, PayMethod.PIX, today)) }
        h.go(compose, app.tutora.ui.TodayHome)
        exists("1 aluno com pagamento pendente")
        compose.onNodeWithTag("pending-alert").performClick()
        h.settle(compose)
        compose.onNodeWithTag("student-detail").assertExists()
    }

    @Test
    fun `AC-008-16 a lesson's detail shows A receber and the payer's balance`() {
        val id = given(22)
        h.show(compose)
        h.go(compose, LessonDetail(id))
        exists("A receber")
        exists("Saldo de Ana Souza")
        exists("1 aula a receber")
    }

    @Test
    fun `AC-008-18 Aula concluida shows the balance it was added to and Registrar pagamento with this class`() {
        val id =
            runBlocking {
                val r =
                    h.t.schedule.scheduleLesson(
                        LessonDraft(WhoId.OfStudent(ana.id), online, LocalDate(2026, 10, 26), LocalTime(8, 0), 60, null, null, null),
                    )
                val lesson = (r as ScheduleResult.Ok).lessonId ?: ""
                h.t.live.start(lesson)
                h.t.live.end(lesson, 60)
                lesson
            }
        given(19)
        h.show(compose)
        h.go(compose, LiveLesson(id))
        exists("Adicionado ao saldo de Ana Souza. Saldo pendente agora: R$ 240,00")
        click("Registrar pagamento")
        exists("Registrar · R$ 120,00")
        exists("Paga 1 aula: 26/10")
    }

    @Test
    fun `AC-008-19 the student form offers Pacote with the number of classes, 10 at first, and its price`() {
        h.show(compose)
        h.go(compose, StudentEdit(ana.id))
        click("Pacote")
        compose.onNodeWithTag("package-terms").assertExists()
        exists("Aulas no pacote")
        exists("10")
        exists("Preço do pacote")
    }

    @Test
    fun `AC-008-21 a package's use shows on the detail and the row, with a warning when one class is left`() {
        runBlocking {
            h.t.catalog.saveStudent(
                ana.copy(rateType = RateType.PACKAGE, packageLessons = 2, packagePriceCents = 20_000),
                listOf(PlaceLink(online, true, null)),
            )
            h.services.payments.buyPackage(ana.id, PayMethod.PIX, today)
        }
        given(22)
        openAna()
        exists("1 de 2 usadas")
        exists("Resta 1 aula no pacote")
        h.go(compose, RegistryHome())
        exists("1 de 2 usadas", part = true)
    }

    @Test
    @Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-008-24 balances and payments follow the language and keep their amounts`() {
        given(19)
        given(22)
        pay(12_000)
        openAna()
        listOf("Owes", "R$120.00", "+ R$120.00", "Payments · 1", "Record payment").forEach { exists(it) }
        exists("10/26 · Pix · 1 lesson")
    }

    private fun salinha(): String =
        runBlocking {
            val id =
                (
                    h.t.catalog.savePlace(
                        builtinPlace(BuiltinPlace.ONLINE).copy(
                            id = "",
                            builtin = null,
                            name = "Ventura",
                            kind = PlaceKind.SALINHA,
                            payer = Payer.PLACE,
                            rateCents = 12_000,
                        ),
                    ) as Result.Ok
                ).value.id
            h.t.catalog.linkStudents(listOf(ana.id to id))
            id
        }

    @Test
    fun `AC-008-25 Marcar como paga on a lesson opens the sheet for whoever owes it, with the class ticked`() {
        val place = salinha()
        val id =
            runBlocking {
                val r =
                    h.t.lessons.logLesson(
                        LessonDraft(WhoId.OfStudent(ana.id), place, LocalDate(2026, 10, 22), LocalTime(8, 0), 60, null, null, null),
                    )
                (r as ScheduleResult.Ok).lessonId ?: ""
            }
        given(19)
        h.show(compose)
        h.go(compose, LessonDetail(id))
        click("Marcar como paga")
        exists("Registrar · R$ 120,00")
        exists("Paga 1 aula: 22/10")
        compose.onAllNodes(matching("Registrar · R$ 120,00"), useUnmergedTree = true).onFirst().performClick()
        h.awaitDb(compose) {
            h.services.paymentRepo
                .ledgerNow(PayerRef(PayerKind.PLACE, place))
                .payments.size == 1
        }
        h.settle(compose)
        exists("Pago")
    }

    @Test
    fun `AC-008-26 a student's detail shows their classes a place pays for, and Escolher aulas says when there is none`() {
        val place = salinha()
        runBlocking {
            h.t.lessons.logLesson(
                LessonDraft(WhoId.OfStudent(ana.id), place, LocalDate(2026, 10, 22), LocalTime(8, 0), 60, null, null, null),
            )
        }
        val pedro =
            runBlocking {
                (
                    h.t.catalog.saveStudent(
                        Student("", "Pedro Lima", null, null, null, TagColor.BLUE, RateType.HOUR, null),
                        listOf(PlaceLink(online, true, 12_000)),
                    ) as Result.Ok
                ).value.id
            }
        openAna()
        exists("o local te paga · R$ 120,00 a receber", part = true)
        exists("22/10 · Ventura")
        exists("60 min · R$ 120,00 · o local te paga")
        h.go(compose, StudentDetail(pedro))
        click("Registrar pagamento")
        click("Escolher aulas", scroll = false)
        exists("Nenhuma aula a receber")
    }

    @Test
    fun `AC-008-27 a student's sheet lists their classes a place pays for and ticking one sets the amount`() {
        val place = salinha()
        runBlocking {
            h.t.lessons.logLesson(
                LessonDraft(WhoId.OfStudent(ana.id), place, LocalDate(2026, 10, 22), LocalTime(8, 0), 60, null, null, null),
            )
        }
        openAna()
        click("Registrar pagamento")
        click("Escolher aulas", scroll = false)
        compose.onAllNodes(matching("22/10 · 60 min · Ventura paga · falta R$ 120,00"), useUnmergedTree = true).onFirst().performClick()
        h.settle(compose)
        exists("Registrar · R$ 120,00")
        exists("Paga 1 aula: 22/10")
    }

    private fun creditsOn() = runBlocking { h.services.saveSettings(h.services.currentSettings().copy(credits = true)) }

    // AC-008-28 and AC-008-29 (refusing overpayment, credit waiting) are replaced by AC-016-24 (Victor, 29 Oct 2026).
    @Test
    fun `AC-016-24 paying more than is owed only cautions, and a payment that pays no class says so`() {
        given(22)
        openAna()
        click("Registrar pagamento")
        val field = compose.onAllNodes(hasSetTextAction(), useUnmergedTree = true).onFirst()
        field.performTextReplacement("R$ 150,00")
        h.settle(compose)
        exists("R$ 30,00 a mais do que o devido")
        compose.onAllNodes(matching("Registrar · R$ 150,00")).onFirst().assertIsEnabled()
    }

    @Test
    fun `AC-016-25 the payment sheet says Em dia or Deve with how much, and Escolher aulas is a switch`() {
        given(22)
        openAna()
        click("Registrar pagamento")
        exists("Deve R$ 120,00")
        compose.onNode(isToggleable() and hasContentDescription("Escolher aulas"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun `AC-008-32 the Aulas section shows the last 3 classes and Ver todas opens them all with filters`() {
        listOf(15, 19, 20, 22).forEach { given(it) }
        pay(12_000)
        openAna()
        exists("Aulas · 4")
        compose.onAllNodes(matching("15/10 ·", part = true), useUnmergedTree = true).assertCountEquals(0)
        click("Ver todas")
        compose.onNodeWithTag("payer-lessons").assertExists()
        exists("15/10 ·", part = true)
        click("Pagas")
        exists("15/10 ·", part = true)
        compose.onAllNodes(matching("22/10 ·", part = true), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `AC-008-33 a place's classes say whose they are, a student's say where`() {
        val place = salinha()
        runBlocking {
            h.t.lessons.logLesson(
                LessonDraft(WhoId.OfStudent(ana.id), place, LocalDate(2026, 10, 22), LocalTime(8, 0), 60, null, null, null),
            )
        }
        h.show(compose)
        h.go(compose, PlaceDetail(place))
        exists("22/10 · Ana Souza")
        given(19)
        h.go(compose, StudentDetail(ana.id))
        exists("19/10 · Online / Remoto")
    }

    @Test
    fun `AC-008-34 Nova aula's who list can be searched and filtered`() {
        runBlocking {
            h.t.catalog.saveStudent(
                Student("", "Pedro Lima", null, null, null, TagColor.BLUE, RateType.HOUR, null),
                listOf(PlaceLink(online, true, 12_000)),
            )
        }
        h.show(compose)
        h.go(compose, app.tutora.ui.LessonNew())
        click("Escolher aluno ou turma")
        exists("Pedro Lima")
        compose.onAllNodes(hasSetTextAction(), useUnmergedTree = true).onLast().performTextReplacement("ana")
        h.settle(compose)
        exists("Ana Souza")
        compose.onAllNodes(matching("Pedro Lima"), useUnmergedTree = true).assertCountEquals(0)
        click("Turmas", scroll = false)
        compose.onAllNodes(matching("Ana Souza"), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `AC-008-35 Turmas filters by Devendo and Locais searches and filters by Devendo`() {
        val place = salinha()
        runBlocking {
            h.t.catalog.saveGroup(Group("", place, "ENEM", null, 18_000, emptyList(), null))
            h.t.catalog.saveGroup(Group("", online, "Redação", null, 15_000, emptyList(), null))
            val redacao =
                h.services.catalogRepo
                    .groups()
                    .first()
                    .first { it.name == "Redação" }
            h.t.lessons.logLesson(
                LessonDraft(WhoId.OfGroup(redacao.id), online, LocalDate(2026, 10, 22), LocalTime(8, 0), 60, null, null, null),
            )
        }
        h.show(compose)
        h.go(compose, RegistryHome("groups"))
        click("Devendo · 1")
        exists("Redação")
        compose.onAllNodes(matching("ENEM"), useUnmergedTree = true).assertCountEquals(0)
        h.go(compose, RegistryHome("places"))
        compose.onAllNodes(hasSetTextAction(), useUnmergedTree = true).onFirst().performTextReplacement("vent")
        h.settle(compose)
        exists("Ventura")
        compose.onAllNodes(matching("Online / Remoto"), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `AC-008-36 a place where students pay shows what is owed for its classes`() {
        given(22)
        h.show(compose)
        h.go(compose, RegistryHome("places"))
        exists("A receber R$ 120")
        pay(12_000)
        h.settle(compose)
        exists("Em dia")
    }

    @Test
    fun `AC-008-31 Dar bonus from the student's page is money or minutes`() {
        creditsOn()
        openAna()
        compose.onNodeWithContentDescription("Dar bônus").performScrollTo().performClick()
        h.settle(compose)
        click("Minutos", scroll = false)
        repeat(2) { compose.onNodeWithContentDescription("Aumentar").performClick() }
        h.settle(compose)
        compose.onAllNodes(matching("Dar bônus"), useUnmergedTree = true).onLast().performClick()
        h.awaitDb(compose) {
            h.services.paymentRepo
                .ledgerNow(anaRef)
                .creditMinutes == 30
        }
        val ledger = runBlocking { h.services.paymentRepo.ledgerNow(anaRef) }
        org.junit.Assert.assertEquals(0L, ledger.creditCents)
        org.junit.Assert.assertEquals(30, ledger.creditMinutes)
        h.settle(compose)
        exists("Bônus: 30 min")
    }

    @Test
    fun `AC-008-37 Creditos e bonus is its own section, and a bonus there can be removed`() {
        creditsOn()
        runBlocking { h.services.payments.giveBonus(anaRef, 5_000, 0, "aniversário") }
        openAna()
        compose.onNodeWithTag("credits-section").assertExists()
        exists("Bônus · R$ 50,00")
        click("Bônus · R$ 50,00")
        click("Remover bônus", scroll = false)
        h.awaitDb(compose) {
            h.services.paymentRepo
                .ledgerNow(anaRef)
                .creditCents == 0L
        }
        h.settle(compose)
        exists("Sem crédito nem bônus")
    }

    @Test
    fun `AC-008-38 turning Creditos e bonus off with a bonus given warns, and confirming deletes the bonuses`() {
        creditsOn()
        runBlocking { h.services.payments.giveBonus(anaRef, 5_000, 0) }
        h.show(compose)
        h.go(compose, app.tutora.ui.SettingsGraph)
        compose.onNodeWithContentDescription("Créditos e bônus").performScrollTo().performClick()
        h.settle(compose)
        exists("1 aluno tem bônus")
        click("Desligar e apagar", scroll = false)
        h.awaitDb(compose) { !h.services.currentSettings().credits }
        org.junit.Assert.assertEquals(
            0,
            runBlocking {
                h.services.paymentRepo
                    .ledgerNow(anaRef)
                    .bonuses.size
            },
        )
    }

    @Test
    fun `AC-008-39 marking a class done offers the bonus, and the minutes chosen come off the class`() {
        creditsOn()
        runBlocking { h.services.payments.giveBonus(anaRef, 0, 30) }
        val id =
            runBlocking {
                val r =
                    h.t.schedule.scheduleLesson(
                        LessonDraft(WhoId.OfStudent(ana.id), online, LocalDate(2026, 10, 26), LocalTime(8, 0), 60, null, null, null),
                    )
                (r as ScheduleResult.Ok).lessonId ?: ""
            }
        h.show(compose)
        h.go(compose, LessonDetail(id))
        click("Marcar como dada", scroll = false)
        click("Usar bônus nesta aula? · 30 min", scroll = false)
        compose.onAllNodes(matching("Confirmar · R$ 120,00"), useUnmergedTree = true).onFirst().performClick()
        h.awaitDb(compose) {
            h.services.paymentRepo
                .ledgerNow(anaRef)
                .creditMinutes == 0
        }
        org.junit.Assert.assertEquals(
            6_000L,
            runBlocking {
                h.services.paymentRepo
                    .ledgerNow(anaRef)
                    .owedCents
            },
        )
    }
}
