package app.tutora

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.data.CalendarStore
import app.tutora.domain.BusyEvent
import app.tutora.domain.instantOf
import app.tutora.ui.CalendarHome
import app.tutora.ui.ScheduleGraph
import app.tutora.ui.SettingsGraph
import kotlinx.coroutines.runBlocking
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
class CalendarScreensTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
        runBlocking { CalendarStore(h.t.db).ensureDefaults(h.t.now) }
    }

    private fun exists(text: String) =
        compose
            .onNodeWithText(text, substring = true)
            .performScrollTo()
            .assertExists()

    private fun page() {
        h.show(compose)
        h.go(compose, CalendarHome)
    }

    @Test
    fun `AC-012-1 Ajustes has Calendario in Geral, and its page has every part`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        exists("Calendário")
        compose.onNodeWithTag("calendar-row").performScrollTo().performClick()
        h.settle(compose)
        listOf(
            "Enviar aulas para o calendário",
            "Em qual calendário?",
            "Só neste aparelho",
            "Mostrar outros compromissos",
            "Incluir valor na descrição",
            "Exportar agenda como .ics",
            "Limpar eventos do Tutora",
        ).forEach(::exists)
    }

    @Test
    fun `AC-012-2 refusing the calendar permission leaves it off and says how to allow it`() {
        h.calendarPermission.answer = false
        page()
        compose.onNodeWithContentDescription("Enviar aulas para o calendário").performClick()
        h.settle(compose)
        exists("Sem acesso ao calendário. Permita nas configurações do celular.")
        assertEquals(false, runBlocking { CalendarStore(h.t.db).load().on })
        compose.onNodeWithTag("calendar-no-access").performClick()
        assertEquals(true, h.calendarPermission.settingsOpened)
    }

    @Test
    fun `AC-012-3 the choices are Tutora's own phone calendar and each account calendar, and Em uso only while on`() {
        page()
        exists("Só neste aparelho")
        exists("Google · victor@gmail.com")
        exists("Outlook · victor@outlook.com")
        compose.onNodeWithText("Em uso").assertDoesNotExist()
        compose.onNodeWithText("Google · victor@gmail.com").performScrollTo().performClick()
        h.awaitDb(compose) { CalendarStore(h.t.db).load().on }
        h.settle(compose)
        assertEquals(10L, runBlocking { CalendarStore(h.t.db).load().calendarId })
        exists("Em uso")
    }

    @Test
    fun `AC-012-17 busy time shows on Dia and Semana, behind the classes, and not on Mes`() {
        runBlocking { h.services.calendar.setOptions(includeValue = false, showBusy = true) }
        h.t.calendar.busy.value =
            listOf(
                BusyEvent(
                    "Dentista",
                    instantOf("2026-10-26T17:00:00.000Z").toEpochMilliseconds(),
                    instantOf("2026-10-26T18:00:00.000Z").toEpochMilliseconds(),
                    false,
                ),
            )
        h.show(compose)
        h.go(compose, ScheduleGraph)
        compose.onNodeWithTag("busy-block").assertExists()
        compose.onNodeWithText("Dentista").assertExists()
        compose.onNodeWithText("Mês").performClick()
        h.settle(compose)
        compose.onNodeWithTag("busy-block").assertDoesNotExist()
    }

    @Test
    fun `AC-012-20 Exportar agenda como ics shares Tutora aulas ics`() {
        page()
        compose.onNodeWithText("Exportar agenda como .ics").performScrollTo().performClick()
        h.awaitDb(compose) { h.exports.shared.isNotEmpty() }
        assertEquals(
            "Tutora aulas.ics",
            h.exports.shared
                .single()
                .first,
        )
    }

    @Test
    @Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-012-22 the page follows the app language`() {
        page()
        listOf("Send classes to the calendar", "Which calendar?", "This phone only", "Export schedule as .ics").forEach(::exists)
    }
}
