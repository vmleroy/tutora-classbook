package com.vmleroy.tutora.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vmleroy.tutora.app.ui.LessonNew
import com.vmleroy.tutora.app.ui.PlaceDetail
import com.vmleroy.tutora.app.ui.PlaceEdit
import com.vmleroy.tutora.app.ui.RegistryHome
import com.vmleroy.tutora.app.ui.ScheduleGraph
import com.vmleroy.tutora.app.ui.SettingsGraph
import com.vmleroy.tutora.app.ui.StudentDetail
import com.vmleroy.tutora.app.ui.StudentEdit
import com.vmleroy.tutora.app.usecases.LessonDraft
import com.vmleroy.tutora.app.usecases.Result
import com.vmleroy.tutora.app.usecases.WhoId
import com.vmleroy.tutora.domain.BuiltinPlace
import com.vmleroy.tutora.domain.Group
import com.vmleroy.tutora.domain.Language
import com.vmleroy.tutora.domain.PayCycle
import com.vmleroy.tutora.domain.Payer
import com.vmleroy.tutora.domain.PlaceKind
import com.vmleroy.tutora.domain.PlaceLink
import com.vmleroy.tutora.domain.RateType
import com.vmleroy.tutora.domain.Repeat
import com.vmleroy.tutora.domain.Student
import com.vmleroy.tutora.domain.TagColor
import com.vmleroy.tutora.domain.builtinPlace
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class FeatureScreensTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness
    private val online = BuiltinPlace.ONLINE.id
    private val home = BuiltinPlace.STUDENT_HOME.id

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
    }

    private fun student(
        name: String,
        subject: String,
        links: List<PlaceLink>,
        color: TagColor = TagColor.AMBER,
    ) = runBlocking {
        (h.t.catalog.saveStudent(Student("", name, subject, null, null, color, RateType.HOUR, null), links) as Result.Ok).value.id
    }

    private fun salinha() =
        runBlocking {
            val p =
                builtinPlace(BuiltinPlace.ONLINE).copy(
                    id = "",
                    builtin = null,
                    name = "Salinha Aprender+",
                    address = "Rua das Flores, 120",
                    kind = PlaceKind.SALINHA,
                    payer = Payer.PLACE,
                    rateCents = 6000,
                    payCycle = PayCycle.MONTHLY,
                )
            (h.t.catalog.savePlace(p) as Result.Ok).value.id
        }

    private fun plain(s: String) = s.replace(' ', ' ').replace(' ', ' ')

    private fun hasPlainText(text: String) =
        SemanticsMatcher("text is $text") { n ->
            n.config.getOrNull(SemanticsProperties.Text)?.any {
                plain(it.text) ==
                    text
            } ==
                true
        }

    // Ajustes (spec 003, 004)

    @Test
    fun `AC-003-11 Ajustes opens with Geral, whose first row is Idioma do app with both languages and samples`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        compose.onNodeWithContentDescription("Geral").assertExists()
        compose.onNodeWithText("Idioma do app", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Português", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("English", useUnmergedTree = true).assertExists()
        compose.onNode(hasPlainText("Datas 26/10 · R$ 1.234,56")).assertExists()
        compose.onNode(hasPlainText("Dates 10/26 · R$1,234.56")).assertExists()
    }

    @Test
    fun `AC-003-12 switching language is saved at once`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        compose.onNodeWithText("English").performClick()
        h.awaitDb(compose) { h.services.currentSettings().language == Language.EN }
        assertEquals(Language.EN, runBlocking { h.services.currentSettings().language })
    }

    @Test
    fun `AC-003-13 a saved language survives a restart and beats the phone language`() {
        runBlocking {
            h.services.saveSettings(
                h.services.settings
                    .first()
                    .copy(language = Language.EN),
            )
        }
        val again =
            runBlocking {
                com.vmleroy.tutora.app.usecases.StartApp(
                    openDatabase = { h.t.db to null },
                    phoneTags = { listOf("pt-BR") },
                    now = { h.t.now },
                    newId = {
                        java.util.UUID
                            .randomUUID()
                            .toString()
                    },
                )()
            }
        assertEquals(Language.EN, (again as com.vmleroy.tutora.app.usecases.StartResult.Ready).settings.language)
    }

    @Test
    fun `AC-003-14 no currency is offered anywhere`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        for (word in listOf("Moeda", "Currency", "US$", "Cotação")) compose.onAllNodesWithText(word, substring = true).assertCountEquals(0)
    }

    @Test
    fun `AC-004-29 Mostrar locais padrão is on by default and turning it off hides the default places`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        compose.onNodeWithContentDescription("Mostrar locais padrão").performClick()
        h.awaitDb(compose) {
            !h.services.settings
                .first()
                .showDefaultPlaces
        }
        assertEquals(
            false,
            runBlocking {
                h.services.settings
                    .first()
                    .showDefaultPlaces
            },
        )
        h.go(compose, RegistryHome("places"))
        compose.onAllNodesWithText("Casa do Aluno", useUnmergedTree = true).assertCountEquals(0)
    }

    // Cadastro (spec 004)

    @Test
    fun `AC-004-1 Locais lists the default places under Padrão, then yours, with price, duration and students`() {
        salinha()
        h.show(compose)
        h.go(compose, RegistryHome("places"))
        compose.onNodeWithContentDescription("Padrão · alunos herdam o preço").assertExists()
        compose
            .onNode(
                hasPlainText("Sem preço · 60 min · nenhum aluno · Deslocamento: varia, você informa em cada aula"),
                useUnmergedTree = true,
            ).assertExists()
        compose.onNode(hasPlainText("R$ 60/h pagos pelo local · 60 min · nenhum aluno"), useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText("Nenhum local seu ainda", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `AC-004-17 a place shows who pays, its students with + Ligar aluno, and its groups`() {
        val p = salinha()
        val pedro = student("Pedro Lima", "Física", listOf(PlaceLink(p, true, null)))
        runBlocking { h.t.catalog.saveGroup(Group("", p, "Turma ENEM", "Redação", 18_000, listOf(pedro), null)) }
        h.show(compose)
        h.go(compose, PlaceDetail(p))
        compose.onNodeWithText("Salinha · Rua das Flores, 120", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Quem paga: o local", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Mensal", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Alunos daqui · 1", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("+ Ligar aluno", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Turma ENEM", useUnmergedTree = true).assertExists()
        compose.onNode(hasContentDescription("Pedro Lima", substring = true)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("student-detail").assertExists()
    }

    @Test
    fun `AC-004-2 a built-in place has no name field and no archive button`() {
        h.show(compose)
        h.go(compose, PlaceEdit(online))
        compose.onNodeWithTag("place-edit").assertExists()
        compose.onAllNodesWithText("Nome", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText("Arquivar", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `AC-004-14 Alunos lists students and groups with search and Por local`() {
        student("Ana Souza", "Matemática", listOf(PlaceLink(online, true, 12_000), PlaceLink(home, false, null)))
        student("Lucas Mendes", "Química", listOf(PlaceLink(home, true, null)))
        runBlocking { h.t.catalog.saveGroup(Group("", online, "Turma ENEM", "Redação", 18_000, emptyList(), null)) }
        h.show(compose)
        h.go(compose, RegistryHome())
        compose.onNodeWithText("Matemática · Online / Remoto, Casa do Aluno", useUnmergedTree = true).assertExists()
        compose.onNode(hasPlainText("Redação · Online / Remoto · nenhum aluno · R$ 180/aula")).assertExists()
        compose.onNodeWithText("Todos · 3", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Buscar aluno").performTextInput("lucas")
        compose.waitForIdle()
        compose.onAllNodesWithText("Ana Souza", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithText("Lucas Mendes", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `AC-004-15 a student's detail shows contact, rate and places, which open the place`() {
        val ana = student("Ana Souza", "Matemática", listOf(PlaceLink(online, true, 12_000), PlaceLink(home, false, null)))
        h.show(compose)
        h.go(compose, StudentDetail(ana))
        compose.onNodeWithText("Por hora", useUnmergedTree = true).assertExists()
        compose.onNode(hasPlainText("R$ 120/h"), useUnmergedTree = true).assertExists()
        compose.onNode(hasContentDescription("Casa do Aluno", substring = true)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("place-detail").assertExists()
    }

    @Test
    fun `AC-004-23 Arquivados opens from the archive icon in the Cadastro header`() {
        h.show(compose)
        h.go(compose, RegistryHome())
        compose.onNodeWithContentDescription("Arquivados").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("archived").assertExists()
    }

    @Test
    fun `AC-004-26 built-in names follow the language, typed names never change`() {
        salinha()
        h.show(compose)
        h.go(compose, RegistryHome("places"))
        compose.onNodeWithText("Casa do Aluno", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Salinha Aprender+", useUnmergedTree = true).assertExists()
        // In English the same built-in place reads in English; the typed name stays as typed.
        val context =
            androidx.test.core.app.ApplicationProvider
                .getApplicationContext<android.content.Context>()
        val english =
            context.createConfigurationContext(
                android.content.res
                    .Configuration(context.resources.configuration)
                    .apply { setLocale(java.util.Locale.US) },
            )
        assertEquals("Student's home", english.getString(R.string.places_builtin_student_home))
        assertEquals("Online / Remote", english.getString(R.string.places_builtin_online))
    }

    @Test
    fun `AC-004-28 every student row shows their initials in their color, and a new student starts with a random color`() {
        student("Ana Souza", "Matemática", listOf(PlaceLink(online, true, null)), TagColor.VIOLET)
        h.show(compose)
        h.go(compose, RegistryHome())
        compose.onNodeWithText("AS", useUnmergedTree = true).assertExists()
        h.go(compose, StudentEdit())
        val colors = listOf("Amarelo", "Verde", "Azul", "Roxo", "Laranja", "Vermelho", "Azul-esverdeado")
        compose
            .onNode(
                SemanticsMatcher("a color is chosen") {
                    it.config.getOrNull(SemanticsProperties.Selected) == true &&
                        it.config.getOrNull(SemanticsProperties.ContentDescription)?.any { d -> d in colors } == true
                },
            ).assertExists()
    }

    // Agenda and Hoje (spec 005)

    private fun seedClasses() {
        val ana = student("Ana Souza", "Matemática", listOf(PlaceLink(online, true, 12_000)), TagColor.VIOLET)
        runBlocking {
            h.t.schedule.scheduleLesson(
                LessonDraft(WhoId.OfStudent(ana), online, LocalDate(2026, 10, 26), LocalTime(14, 0), 60, null, null, Repeat.WEEKLY),
            )
            h.t.schedule.scheduleLesson(
                LessonDraft(WhoId.OfStudent(ana), online, LocalDate(2026, 10, 28), LocalTime(16, 30), 60, null, null, null),
            )
        }
    }

    @Test
    fun `AC-005-1 the + on Agenda and Hoje opens Nova aula to schedule`() {
        h.show(compose)
        h.go(compose, ScheduleGraph)
        compose.onNodeWithContentDescription("Nova aula").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("lesson-new").assertExists()
        compose.onNodeWithText("Agendar", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `AC-005-17 the month and a Monday-to-Sunday week strip, moving by week`() {
        seedClasses()
        h.show(compose)
        h.go(compose, ScheduleGraph)
        compose.onNodeWithText("Outubro 2026", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Seg 26").assertIsSelected()
        compose.onNodeWithContentDescription("Dom 1").assertExists()
        compose.onNodeWithContentDescription("Próxima semana").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Novembro 2026", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `AC-005-18 Dia shows each class as a block, the next one marked, with the day header`() {
        seedClasses()
        h.show(compose)
        h.go(compose, ScheduleGraph)
        compose.onNode(hasContentDescription("Ana Souza, Matemática · Online / Remoto, Próxima")).assertExists()
        compose.onNodeWithText("Hoje · 1 aula", useUnmergedTree = true).assertExists()
        compose.onNode(hasPlainText("Previsto R$ 120")).assertExists()
    }

    @Test
    fun `AC-005-20 Semana shows the seven days side by side`() {
        seedClasses()
        h.show(compose)
        h.go(compose, ScheduleGraph)
        compose.onNodeWithText("Semana").performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("Ana Souza", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun `AC-005-24 Hoje lists today's classes with time, place, duration and value`() {
        seedClasses()
        h.show(compose)
        compose.onNodeWithText("Aulas de hoje", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("1 aula", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Matemática · Online / Remoto · 60 min", useUnmergedTree = true).assertExists()
        compose.onNode(hasPlainText("R$ 120,00"), useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Segunda, 26 de outubro", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Bom dia", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Próxima aula · em 5h", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `AC-005-25 with no classes Hoje and Agenda keep their empty states`() {
        h.show(compose)
        compose.onNodeWithText("Nenhuma aula hoje", useUnmergedTree = true).assertExists()
        h.go(compose, ScheduleGraph)
        compose.onNodeWithText("Agenda vazia", useUnmergedTree = true).assertExists()
    }

    @Test
    @Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-005-26 dates and times follow the language, and times never wrap`() {
        seedClasses()
        h.show(compose)
        // "2:00 PM" has a narrow no-break space before PM.
        val times = compose.onAllNodes(hasPlainText("2:00 PM"), useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(2, times.size)
        compose.onNodeWithText("Monday, October 26", useUnmergedTree = true).assertExists()
        h.go(compose, ScheduleGraph)
        compose.onNodeWithText("October 2026", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Today · 1 lesson", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Mon 26").assertExists()
    }

    @Test
    fun `AC-005-1 Nova aula schedules a weekly class that fills 8 weeks`() {
        student("Ana Souza", "Matemática", listOf(PlaceLink(online, true, 12_000)))
        h.show(compose)
        h.go(compose, LessonNew("2026-10-26"))
        compose.onNodeWithText("Escolher aluno ou turma").performClick()
        compose.waitForIdle()
        compose.onNode(hasContentDescription("Ana Souza", substring = true)).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Agendar").performScrollTo().performClick()
        h.awaitDb(compose) {
            h.services.scheduleRepo
                .between("2026-01-01T00:00:00.000Z", "2027-12-31T00:00:00.000Z")
                .first()
                .isNotEmpty()
        }
        val count =
            runBlocking {
                h.services.scheduleRepo
                    .between("2026-01-01T00:00:00.000Z", "2027-12-31T00:00:00.000Z")
                    .first()
                    .size
            }
        assertEquals(9, count)
    }
}
