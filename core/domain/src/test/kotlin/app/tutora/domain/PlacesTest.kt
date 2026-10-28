package app.tutora.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacesTest {
    private fun place(
        kind: PlaceKind = PlaceKind.SALINHA,
        payer: Payer = Payer.STUDENT,
        rate: Cents? = 10_000,
        commission: Int = 0,
    ) = builtinPlace(
        BuiltinPlace.ONLINE,
    ).copy(
        id = "p",
        builtin = null,
        name = "Salinha Aprender+",
        kind = kind,
        payer = payer,
        rateCents = rate,
        commissionBp = commission,
    )

    private val ana = Student("s", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null)

    @Test
    fun `AC-004-4 Escola and Cursinho count as job, the rest as private lessons`() {
        assertEquals(EarningsType.JOB, earningsType(PlaceKind.SCHOOL))
        assertEquals(EarningsType.JOB, earningsType(PlaceKind.COURSE))
        for (k in listOf(
            PlaceKind.SALINHA,
            PlaceKind.PRIVATE,
            PlaceKind.STUDENT_HOME,
            PlaceKind.ONLINE,
        )) {
            assertEquals(EarningsType.PRIVATE, earningsType(k))
        }
    }

    @Test
    fun `AC-004-5 Salinha, Cursinho and Particular ask who pays`() {
        for (k in listOf(PlaceKind.SALINHA, PlaceKind.COURSE, PlaceKind.PRIVATE)) assertEquals(PayMode.CHOOSE, payMode(k))
        assertEquals(PayMode.STUDENT, payMode(PlaceKind.ONLINE))
    }

    @Test
    fun `AC-004-6 an Escola is paid by a monthly salary`() = assertEquals(PayMode.SALARY, payMode(PlaceKind.SCHOOL))

    @Test
    fun `AC-004-8 places default to 60 minutes`() = assertEquals(60, builtinPlace(BuiltinPlace.ONLINE).defaultDurationMin)

    @Test
    fun `AC-004-11 the first place is the default and the star moves when it is removed`() {
        val links = withDefault(listOf(PlaceLink("a", false, null), PlaceLink("b", false, null)))
        assertEquals(listOf(true, false), links.map { it.isDefault })
        assertEquals(listOf(PlaceLink("b", true, null)), unlink(links, "a"))
    }

    @Test
    fun `AC-004-28 a new student gets any of the color tags at random`() {
        val picked = listOf(0.0, 0.15, 0.3, 0.45, 0.6, 0.75, 0.99).map(::randomTag).toSet()
        assertEquals(TagColor.entries.toSet(), picked)
        assertEquals("AS", initials("Ana Souza"))
    }

    @Test
    fun `AC-004-7 the preview shows what a 60-minute lesson earns`() {
        assertEquals(Preview.PlacePays(6000), previewLesson(place(payer = Payer.PLACE, rate = 6000), 60))
        assertEquals(Preview.StudentPays(10_000, 7000), previewLesson(place(commission = 3000), 60))
    }

    @Test
    fun `AC-004-18 the student's own price wins unless the place pays`() {
        assertEquals(12_000L, (resolvePrice(place(), ana, PlaceLink("p", true, 12_000)) as Price.Set).cents)
        assertEquals(10_000L, (resolvePrice(place(), ana, PlaceLink("p", true, null)) as Price.Set).cents)
        assertEquals(6000L, (resolvePrice(place(payer = Payer.PLACE, rate = 6000), ana, PlaceLink("p", true, 12_000)) as Price.Set).cents)
    }

    @Test
    fun `AC-004-19 a group lesson is the group's price and at an Escola the salary covers it`() {
        val group = Group("g", "p", "Turma", null, 18_000, emptyList(), null)
        assertEquals(Price.Set(18_000, RateType.LESSON, PriceSource.GROUP), resolvePrice(place(), group = group))
        assertEquals(Price.Salary, resolvePrice(place(kind = PlaceKind.SCHOOL), group = group))
    }

    @Test
    fun `AC-004-20 the price says where it came from`() {
        assertEquals(PriceSource.PLACE, (resolvePrice(place(), ana) as Price.Set).source)
        assertEquals(PriceSource.STUDENT, (resolvePrice(place(), ana, PlaceLink("p", true, 1)) as Price.Set).source)
    }

    @Test
    fun `AC-004-21 no price anywhere is unset, never zero`() =
        assertEquals(Price.Unset, resolvePrice(builtinPlace(BuiltinPlace.ONLINE), ana))

    @Test
    fun `AC-004-22 a place's new price reaches Do local students only`() {
        val raised = place(rate = 15_000)
        assertEquals(15_000L, (resolvePrice(raised, ana, PlaceLink("p", true, null)) as Price.Set).cents)
        assertEquals(12_000L, (resolvePrice(raised, ana, PlaceLink("p", true, 12_000)) as Price.Set).cents)
    }

    @Test
    fun `uuidv7 is a version 7 UUID that sorts by creation time`() {
        val a = uuidv7(1_761_566_400_000, ByteArray(10) { -1 })
        val b = uuidv7(1_761_566_400_001, ByteArray(10))
        assertTrue(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").matches(a))
        assertTrue(a < b)
    }
}
