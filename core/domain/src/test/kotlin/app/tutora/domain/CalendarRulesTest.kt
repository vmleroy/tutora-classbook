package app.tutora.domain

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarRulesTest {
    private val zone = "America/Sao_Paulo"
    private val ana = Student("s", "Ana Souza", "Matemática", null, null, TagColor.AMBER, RateType.HOUR, null)
    private val online = builtinPlace(BuiltinPlace.ONLINE).copy(rateCents = 12_000)
    private val salinha =
        online.copy(id = "p", builtin = null, name = "Salinha da Rua", address = "Rua A, 10", kind = PlaceKind.SALINHA)

    private fun lesson(
        id: String = "l1",
        start: String = "2026-10-27T11:00:00.000Z", // Tue 08:00 in São Paulo
        status: LessonStatus = LessonStatus.SCHEDULED,
        place: Place = online,
        notes: String? = null,
    ) = Lesson(id, null, null, null, start, 60, null, null, false, notes, Who.OfStudent(ana, null), place, status)

    private fun words(english: Boolean = false) =
        CalendarWords(
            cancelledPrefix = if (english) "Cancelled · " else "Cancelada · ",
            missedPrefix = if (english) "No-show · " else "Faltou · ",
            value = if (english) "Value: %1\$s" else "Valor: %1\$s",
            footer = if (english) "Made by Tutora" else "Criado pelo Tutora",
            builtinName = { if (english) "Online / Remote" else "Online / Remoto" },
            money = { "R$ ${it / 100},00" },
        )

    @Test
    fun `AC-012-5 every event ends with Tutora's line`() {
        assertTrue(eventFor(lesson(notes = "Levar o livro"), words(), false).description.endsWith("\nCriado pelo Tutora"))
    }

    @Test
    fun `AC-012-6 the title is who and the subject, the place and notes are the class's, and cancelled or missed say so`() {
        val e = eventFor(lesson(place = salinha, notes = "Levar o livro"), words(), false)
        assertEquals("Ana Souza · Matemática", e.title)
        assertEquals("Salinha da Rua, Rua A, 10", e.location)
        assertEquals("Levar o livro\nCriado pelo Tutora", e.description)
        assertEquals(60 * 60_000L, e.endMs - e.startMs)
        assertEquals("Cancelada · Ana Souza · Matemática", eventFor(lesson(status = LessonStatus.CANCELLED), words(), false).title)
        assertEquals("Faltou · Ana Souza · Matemática", eventFor(lesson(status = LessonStatus.MISSED), words(), false).title)
    }

    @Test
    fun `AC-012-7 the value is in the description only when asked for`() {
        assertEquals("Criado pelo Tutora", eventFor(lesson(), words(), false).description)
        assertEquals("Valor: R$ 120,00\nCriado pelo Tutora", eventFor(lesson(), words(), true).description)
    }

    @Test
    fun `AC-012-8 a moved class updates its event, a deleted one removes it, a new one is created`() {
        val moved = eventFor(lesson("a", start = "2026-10-28T11:00:00.000Z"), words(), false)
        val added = eventFor(lesson("c"), words(), false)
        val existing =
            listOf(
                MarkedEvent(1, eventFor(lesson("a"), words(), false)),
                MarkedEvent(2, eventFor(lesson("b"), words(), false)),
            )
        val diff = diffEvents(listOf(moved, added), existing, 0)
        assertEquals(EventDiff(listOf(added), listOf(1L to moved), listOf(2L)), diff)
    }

    @Test
    fun `AC-012-9 the window runs from today to the 8-week horizon, and past events stay`() {
        val (from, to) = syncWindow(LocalDate(2026, 10, 26), zone)
        assertEquals("2026-10-26T03:00:00.000Z", from)
        assertEquals("2026-12-22T03:00:00.000Z", to)
        val past = MarkedEvent(9, eventFor(lesson("old", start = "2026-10-20T11:00:00.000Z"), words(), false))
        assertTrue(diffEvents(emptyList(), listOf(past), instantOf(from).toEpochMilliseconds()).isEmpty)
    }

    @Test
    fun `AC-012-11 an event edited in the calendar app is put back as Tutora has it`() {
        val e = eventFor(lesson(), words(), false)
        val edited = MarkedEvent(1, e.copy(title = "Mudei aqui"))
        assertEquals(listOf(1L to e), diffEvents(listOf(e), listOf(edited), 0).update)
    }

    @Test
    fun `AC-012-18 an all-day event is a band on its day, a timed one a block`() {
        val day = LocalDate(2026, 10, 27)
        val holiday = BusyEvent("Feriado", 1_793_059_200_000, 1_793_145_600_000, allDay = true) // 27 Oct UTC
        val dentist =
            BusyEvent(
                "Dentista",
                instantOf("2026-10-27T17:00:00.000Z").toEpochMilliseconds(),
                instantOf("2026-10-27T18:00:00.000Z").toEpochMilliseconds(),
                false,
            )
        val busy = busyFor(listOf(holiday, dentist), day, zone)
        assertEquals(listOf("Feriado"), busy.bands)
        assertEquals(listOf(Triple("Dentista", 14 * 60, 15 * 60)), busy.blocks)
    }

    @Test
    fun `AC-012-19 Tutora's own events are never busy time`() {
        val mine =
            BusyEvent(
                "Ana Souza · Matemática",
                instantOf("2026-10-27T11:00:00.000Z").toEpochMilliseconds(),
                instantOf("2026-10-27T12:00:00.000Z").toEpochMilliseconds(),
                false,
                marked = true,
            )
        assertTrue(busyFor(listOf(mine), LocalDate(2026, 10, 27), zone).blocks.isEmpty())
    }

    @Test
    fun `AC-012-20 the ics file holds one event per class in UTC`() {
        val ics = icsFile(listOf(eventFor(lesson(place = salinha), words(), false)), 0)
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0"))
        listOf(
            "DTSTART:20261027T110000Z",
            "DTEND:20261027T120000Z",
            "SUMMARY:Ana Souza · Matemática",
            "LOCATION:Salinha da Rua\\, Rua A\\, 10",
        ).forEach {
            assertTrue(it, ics.contains(it))
        }
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"))
    }

    @Test
    fun `AC-012-21 the same class keeps its UID, and a later export has a higher sequence`() {
        val e = listOf(eventFor(lesson(), words(), false))
        val first = icsFile(e, 1_000_000)
        val later = icsFile(e, 2_000_000)
        assertTrue(first.contains("UID:l1@tutora.app") && later.contains("UID:l1@tutora.app"))
        assertTrue(first.contains("SEQUENCE:1000") && later.contains("SEQUENCE:2000"))
    }

    @Test
    fun `AC-012-22 the event texts follow the app language`() {
        val e = eventFor(lesson(status = LessonStatus.CANCELLED), words(english = true), true)
        assertEquals("Cancelled · Ana Souza · Matemática", e.title)
        assertEquals("Online / Remote", e.location)
        assertEquals("Value: R$ 120,00\nMade by Tutora", e.description)
    }
}
