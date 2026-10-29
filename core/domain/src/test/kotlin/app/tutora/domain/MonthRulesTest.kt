package app.tutora.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthRulesTest {
    private val ana = Student("s", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null)
    private val enem = Group("g", "sal", "Turma ENEM", null, 18_000, emptyList(), null)
    private val online = builtinPlace(BuiltinPlace.ONLINE)
    private val school = online.copy(id = "col", builtin = null, name = "Colégio", kind = PlaceKind.SCHOOL)
    private val today = LocalDate(2026, 10, 26)

    private fun lesson(
        status: LessonStatus = LessonStatus.SCHEDULED,
        who: Who = Who.OfStudent(ana, null),
        place: Place = online,
    ) = Lesson("l", null, null, null, "2026-10-26T17:00:00.000Z", 60, null, null, false, null, who, place, status)

    @Test
    fun `AC-016-16 a month is whole weeks from Monday, with the neighbors' days marked`() {
        val cells = monthCells(YearMonth(2026, Month.OCTOBER))
        assertEquals(35, cells.size)
        assertEquals(LocalDate(2026, 9, 28), cells.first().date)
        assertEquals(LocalDate(2026, 11, 1), cells.last().date)
        assertEquals(31, cells.count { it.inMonth })
        assertFalse(cells.first().inMonth)
    }

    @Test
    fun `AC-016-17 each class puts a dot of its kind under its day, cancelled and missed none`() {
        assertEquals(DayKind.PRIVATE, lesson().dayKind())
        assertEquals(DayKind.SCHOOL, lesson(place = school).dayKind())
        assertEquals(DayKind.GROUP, lesson(who = Who.OfGroup(enem)).dayKind())
        assertEquals(DayKind.DONE, lesson(LessonStatus.DONE, place = school).dayKind())
        assertEquals(null, lesson(LessonStatus.CANCELLED).dayKind())
        assertEquals(null, lesson(LessonStatus.MISSED).dayKind())
        val dots =
            dayDots(
                mapOf(
                    today to listOf(lesson(), lesson(LessonStatus.CANCELLED)),
                    LocalDate(2026, 10, 27) to listOf(lesson(LessonStatus.MISSED)),
                ),
                today,
            )
        assertEquals(mapOf(today to listOf(DayKind.PRIVATE)), dots)
    }

    @Test
    fun `AC-016-23 the Agenda looks 8 weeks ahead and no further, any month back`() {
        assertEquals(LocalDate(2026, 12, 21), agendaHorizon(today))
        assertTrue(canShowMonth(YearMonth(2026, Month.DECEMBER), today))
        assertFalse(canShowMonth(YearMonth(2027, Month.JANUARY), today))
        assertTrue(canShowMonth(YearMonth(2020, Month.MARCH), today))
        val late = LocalDate(2026, 12, 28)
        assertEquals(emptyMap<LocalDate, List<DayKind>>(), dayDots(mapOf(late to listOf(lesson())), today))
    }
}
