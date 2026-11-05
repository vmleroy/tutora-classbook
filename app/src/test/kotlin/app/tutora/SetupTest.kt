package app.tutora

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.data.SettingsRepository
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.Language
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.Teaching
import app.tutora.domain.TravelMode
import app.tutora.usecases.Setup
import app.tutora.usecases.SetupChoices
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var t: TestDb
    private lateinit var setup: Setup

    @Before
    fun setUp() =
        runTest {
            t = TestDb(tmp.root)
            setup = Setup(t.ctx)
            t.catalog.ensureBuiltinPlaces()
            SettingsRepository(t.db).ensureDefaults(Language.PT_BR, t.now)
        }

    private suspend fun place(id: String) =
        t.db
            .places()
            .get(id)!!

    @Test
    fun `AC-014-17 the setup is for a first run with no data, and a teacher with data never sees it`() =
        runTest {
            assertTrue(setup.needed())
            t.catalog.saveStudent(
                Student("", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null),
                listOf(PlaceLink(BuiltinPlace.ONLINE.id, true, 12_000)),
            )
            assertFalse(setup.needed())
            setup.markDoneIfData()
            assertTrue(SettingsRepository(t.db).load().setupDone)
        }

    @Test
    fun `AC-014-21 the prices and travel chosen become the two built-in places' own`() =
        runTest {
            setup.save(SetupChoices(Language.PT_BR, onlineCents = 11_000, homeCents = 13_000, travel = TravelMode.FIXED))
            assertEquals(11_000L, place(BuiltinPlace.ONLINE.id).rateCents)
            assertEquals(13_000L, place(BuiltinPlace.STUDENT_HOME.id).rateCents)
            assertEquals(TravelMode.FIXED.name, place(BuiltinPlace.STUDENT_HOME.id).travelMode)
            assertEquals(2_000L, place(BuiltinPlace.STUDENT_HOME.id).travelFeeCents)
        }

    @Test
    fun `AC-014-23 skipping keeps every step's defaults and ends the setup`() =
        runTest {
            setup.save(SetupChoices(Language.EN))
            val s = SettingsRepository(t.db).observe().first()
            assertEquals(Language.EN, s.language)
            assertEquals(setOf(Teaching.PRIVATE), s.teaching)
            assertEquals(15, s.remindBefore)
            assertTrue(s.setupDone)
            assertEquals(10_000L, place(BuiltinPlace.ONLINE.id).rateCents)
            assertEquals(12_000L, place(BuiltinPlace.STUDENT_HOME.id).rateCents)
            assertEquals(TravelMode.VARIES.name, place(BuiltinPlace.STUDENT_HOME.id).travelMode)
            assertFalse(setup.needed())
        }
}
