package app.tutora.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.app.usecases.Kind
import app.tutora.app.usecases.Result
import app.tutora.data.CatalogRepository
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.CatalogError
import app.tutora.domain.Group
import app.tutora.domain.Payer
import app.tutora.domain.Place
import app.tutora.domain.PlaceKind
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.TravelMode
import app.tutora.domain.builtinPlace
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var t: TestDb
    private val repo get() = CatalogRepository(t.db)
    private val online = BuiltinPlace.ONLINE.id
    private val home = BuiltinPlace.STUDENT_HOME.id
    private val ana = Student("", "Ana Souza", "Matemática", "(11) 90000-0000", null, TagColor.AMBER, RateType.HOUR, null)

    @Before
    fun setUp() =
        runTest {
            t = TestDb(tmp.root)
            t.catalog.ensureBuiltinPlaces()
        }

    private fun salinha(
        payer: Payer = Payer.STUDENT,
        rate: Long? = 10_000,
        kind: PlaceKind = PlaceKind.SALINHA,
        name: String = "Salinha Aprender+",
    ): Place = builtinPlace(BuiltinPlace.ONLINE).copy(id = "", builtin = null, name = name, kind = kind, payer = payer, rateCents = rate)

    private suspend fun placeId(p: Place = salinha()) = (t.catalog.savePlace(p) as Result.Ok).value.id

    private suspend fun studentId(
        links: List<PlaceLink>,
        s: Student = ana,
    ) = (t.catalog.saveStudent(s, links) as Result.Ok).value.id

    @Test
    fun `AC-004-2 built-ins start with no price, can be priced, never renamed or archived`() =
        runTest {
            val p = repo.place(online).first()!!
            assertNull(p.rateCents)
            t.catalog.savePlace(p.copy(rateCents = 10_000, defaultDurationMin = 50))
            assertEquals(10_000L, repo.place(online).first()?.rateCents)
            assertEquals(Result.Err(CatalogError.BUILTIN_FIXED), t.catalog.savePlace(p.copy(name = "Zoom")))
            assertEquals(Result.Err(CatalogError.BUILTIN_FIXED), t.catalog.archive(Kind.PLACE, online))
        }

    @Test
    fun `AC-004-3 a place needs a name`() =
        runTest {
            assertEquals(Result.Err(CatalogError.NAME_REQUIRED), t.catalog.savePlace(salinha(name = "  ")))
        }

    @Test
    fun `AC-004-9 Casa do Aluno starts with travel varies`() =
        runTest { assertEquals(TravelMode.VARIES, repo.place(home).first()?.travelMode) }

    @Test
    fun `AC-004-10 a student is saved with name, subject, phone, charging, color and notes`() =
        runTest {
            assertEquals(Result.Err(CatalogError.NAME_REQUIRED), t.catalog.saveStudent(ana.copy(name = ""), emptyList()))
            assertEquals(Result.Err(CatalogError.PLACE_REQUIRED), t.catalog.saveStudent(ana, emptyList()))
            val id = studentId(listOf(PlaceLink(online, false, null)), ana.copy(notes = "ENEM"))
            assertEquals(ana.copy(id = id, notes = "ENEM"), repo.student(id).first()?.student)
        }

    @Test
    fun `AC-004-12 at each place the student pays the place's price or their own`() =
        runTest {
            val id = studentId(listOf(PlaceLink(online, false, 12_000), PlaceLink(home, true, null)))
            assertEquals(listOf(PlaceLink(home, true, null), PlaceLink(online, false, 12_000)), repo.student(id).first()?.links)
        }

    @Test
    fun `AC-004-13 at a place that pays, the student keeps no price of their own`() =
        runTest {
            val paying = placeId(salinha(payer = Payer.PLACE, rate = 6000))
            val id = studentId(listOf(PlaceLink(paying, true, 9000)))
            assertNull(
                repo
                    .student(id)
                    .first()
                    ?.links
                    ?.first()
                    ?.rateCents,
            )
        }

    @Test
    fun `AC-004-16 a group keeps only members linked to its place, and at an Escola it has no price`() =
        runTest {
            val p = placeId()
            val s = studentId(listOf(PlaceLink(p, true, null)))
            val other = studentId(listOf(PlaceLink(online, true, null)), ana.copy(name = "Bia"))
            val g = (t.catalog.saveGroup(Group("", p, "Turma ENEM", "Redação", 18_000, listOf(s, other), null)) as Result.Ok).value
            assertEquals(
                listOf(s),
                t.catalog.let {
                    repo
                        .groups()
                        .first()
                        .first { it.id == g.id }
                        .memberIds
                },
            )
            val school = placeId(salinha(kind = PlaceKind.SCHOOL, name = "Colégio Santa Clara"))
            assertNull((t.catalog.saveGroup(Group("", school, "9º B", null, 5000, emptyList(), null)) as Result.Ok).value.rateCents)
        }

    @Test
    fun `AC-004-17 + Ligar aluno links a student to a place with its price`() =
        runTest {
            val p = placeId()
            val s = studentId(listOf(PlaceLink(online, true, null)))
            t.catalog.linkStudentToPlace(s, p)
            assertEquals(
                PlaceLink(p, false, null),
                repo
                    .student(s)
                    .first()
                    ?.links
                    ?.first { it.placeId == p },
            )
            assertEquals(
                1,
                repo
                    .places()
                    .first()
                    .first { it.place.id == p }
                    .studentCount,
            )
        }

    @Test
    fun `AC-004-23 archiving hides an item and keeps it, and restoring brings it back`() =
        runTest {
            val s = studentId(listOf(PlaceLink(online, true, null)))
            t.catalog.archive(Kind.STUDENT, s)
            assertEquals(emptyList<Any>(), repo.students().first())
            assertEquals(listOf("Ana Souza"), repo.students(archived = true).first().map { it.student.name })
            t.catalog.restore(Kind.STUDENT, s)
            assertEquals(
                1,
                repo
                    .students()
                    .first()
                    .single()
                    .links.size,
            )
        }

    @Test
    fun `AC-004-24 only an archived item is removed, and a place takes its links and groups, not students`() =
        runTest {
            val p = placeId()
            val s = studentId(listOf(PlaceLink(p, true, null)))
            t.catalog.saveGroup(Group("", p, "Turma", null, null, emptyList(), null))
            assertEquals(Result.Err(CatalogError.NOT_ARCHIVED), t.catalog.removePermanently(Kind.PLACE, p))
            t.catalog.archive(Kind.PLACE, p)
            assertEquals(Result.Ok(Unit), t.catalog.removePermanently(Kind.PLACE, p))
            assertNull(repo.place(p).first())
            assertEquals(emptyList<Group>(), repo.groups(archived = true).first())
            assertEquals(emptyList<PlaceLink>(), repo.student(s).first()?.links)
        }

    @Test
    fun `AC-004-25 archiving a place keeps its students and hides their links until restored`() =
        runTest {
            val p = placeId()
            val s = studentId(listOf(PlaceLink(p, true, 9000)))
            t.catalog.archive(Kind.PLACE, p)
            assertEquals(emptyList<PlaceLink>(), repo.student(s).first()?.links)
            t.catalog.restore(Kind.PLACE, p)
            assertEquals(listOf(PlaceLink(p, true, 9000)), repo.student(s).first()?.links)
        }
}
