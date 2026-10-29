package app.tutora

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BackupEvery
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.ui.BackupHome
import app.tutora.ui.SettingsGraph
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
import app.tutora.usecases.WhoId
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
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
class BackupScreensTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness
    private lateinit var ana: Student

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
        runBlocking {
            app.tutora.data
                .BackupStore(h.t.db)
                .ensureDefaults(h.t.now)
            ana =
                (
                    h.t.catalog.saveStudent(
                        Student("", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(BuiltinPlace.ONLINE.id, true, 12_000)),
                    ) as Result.Ok
                ).value
            h.t.lessons.logLesson(
                LessonDraft(
                    WhoId.OfStudent(ana.id),
                    BuiltinPlace.ONLINE.id,
                    LocalDate(2026, 10, 22),
                    LocalTime(8, 0),
                    60,
                    null,
                    null,
                    null,
                ),
                null,
            )
        }
    }

    private fun exists(text: String) =
        compose
            .onNodeWithText(text, substring = true)
            .performScrollTo()
            .assertExists()

    private fun click(text: String) {
        compose
            .onNodeWithText(text, substring = true)
            .performScrollTo()
            .performClick()
        h.settle(compose)
    }

    @Test
    fun `AC-011-1 Ajustes has Backup e planilha in Seus dados, and its page has the three parts`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        exists("Nenhum backup ainda")
        click("Backup e planilha")
        listOf("Backup automático", "Fazer agora", "Backup completo (.json)", "Planilha .xlsx", "Restaurar backup").forEach(::exists)
    }

    @Test
    fun `AC-011-5 turning automatic backup on asks for a folder, and cancelling leaves it off`() {
        h.show(compose)
        h.go(compose, BackupHome)
        compose.onNodeWithContentDescription("Backup automático").performClick()
        h.settle(compose)
        exists("Desligado · só backup manual")
        h.picker.folderAnswer = "content://tree/tutora"
        compose.onNodeWithContentDescription("Backup automático").performClick()
        h.awaitDb(compose) {
            h.services.backups
                .state()
                .auto
        }
        h.settle(compose)
        exists("Salvar em Tutora backups")
    }

    @Test
    fun `AC-011-6 the switch's line says when the next backup runs`() {
        runBlocking { h.services.backups.setAuto(true, BackupEvery.WEEK, 5, "content://tree/tutora", "Tutora backups") }
        h.show(compose)
        h.go(compose, BackupHome)
        exists("Próximo: dom, 1 nov, 20:00")
    }

    @Test
    fun `AC-011-10 after 14 days without a backup, Ajustes and Hoje warn`() {
        h.t.now = "2026-11-12T12:00:00.000Z" // 17 days after the first class was added
        h.show(compose)
        exists("Sem backup há 17 dias")
        click("Sem backup há 17 dias")
        compose.onNodeWithTag("backup-screen").assertExists()
        h.go(compose, SettingsGraph)
        exists("Último backup há 17 dias")
        runBlocking { h.services.backups.makeBackup() }
        h.settle(compose)
        compose.onNodeWithText("Último backup há", substring = true).assertDoesNotExist()
    }

    @Test
    fun `AC-011-11 Recentes lists the backups, how they were made, and a failed run in red`() {
        runBlocking {
            h.services.backups.makeBackup()
            h.services.backups.setAuto(true, BackupEvery.DAY, 5, "content://tree/tutora", "Google Drive")
            h.t.folder.failing = true
            h.services.backups.runAuto()
        }
        h.show(compose)
        h.go(compose, BackupHome)
        exists("Recentes")
        exists("Manual · compartilhado")
        exists("Automático · Google Drive")
        exists("Falhou")
        compose.onAllNodesWithTextExact("Seg, 26 out · 09:00").onFirst().assertExists()
    }

    @Test
    fun `AC-011-11 tapping an automatic backup in Recentes asks to restore it`() {
        runBlocking {
            h.services.backups.setAuto(true, BackupEvery.DAY, 5, "content://tree/tutora", "Google Drive")
            h.services.backups.runAuto()
        }
        h.show(compose)
        h.go(compose, BackupHome)
        click("Automático · Google Drive")
        compose.onNodeWithText("Restaurar o backup de 26/10/2026, 09:00?").assertExists()
    }

    @Test
    fun `AC-011-13 picking a backup asks first, saying what it holds and what it replaces`() {
        h.t.folder.picked["content://file/b"] =
            runBlocking {
                h.services.backups
                    .makeBackup()
                    .second
            }
        h.picker.fileAnswer = "content://file/b"
        h.show(compose)
        h.go(compose, BackupHome)
        click("Restaurar backup")
        compose.onNodeWithText("Restaurar o backup de 26/10/2026, 09:00?").assertExists()
        compose.onNodeWithText("1 aluno, 1 aula, nenhum pagamento. Os dados atuais deste celular serão substituídos.").assertExists()
    }

    @Test
    fun `AC-011-14 after restoring, the app shows Hoje with the restored data`() {
        h.t.folder.picked["content://file/b"] =
            runBlocking {
                h.services.backups
                    .makeBackup()
                    .second
            }
        h.picker.fileAnswer = "content://file/b"
        h.show(compose)
        h.go(compose, BackupHome)
        click("Restaurar backup")
        compose.onAllNodesWithTextExact("Restaurar").onFirst().performClick()
        h.awaitDb(compose) {
            h.services.backups
                .state()
                .restoreUndo
        }
        h.settle(compose)
        compose.onNodeWithTag("today-screen").assertExists()
    }

    @Test
    fun `AC-011-12 a file that is not a backup says so and changes nothing`() {
        h.t.folder.picked["content://file/x"] = "olá".encodeToByteArray()
        h.picker.fileAnswer = "content://file/x"
        h.show(compose)
        h.go(compose, BackupHome)
        click("Restaurar backup")
        exists("Este arquivo não é um backup do Tutora")
        assertTrue(
            runBlocking {
                !h.services.backups
                    .state()
                    .restoreUndo
            },
        )
    }

    @Test
    @Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-011-21 the page follows the app language`() {
        h.show(compose)
        h.go(compose, BackupHome)
        listOf("Automatic backup", "Off · manual backups only", "Full backup (.json)", "Restore backup").forEach(::exists)
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextExact(text: String) =
    onAllNodes(
        androidx.compose.ui.test
            .hasText(text, substring = false),
    )
