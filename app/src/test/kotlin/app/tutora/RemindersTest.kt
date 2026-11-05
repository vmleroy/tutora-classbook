package app.tutora

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.data.SettingsRepository
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.CancelReason
import app.tutora.domain.OwedSummary
import app.tutora.domain.PastClasses
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Settings
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.ui.RemindersHome
import app.tutora.usecases.Cancellation
import app.tutora.usecases.Confirming
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.ReminderKind
import app.tutora.usecases.ReminderNotice
import app.tutora.usecases.Reminders
import app.tutora.usecases.Result
import app.tutora.usecases.ScheduleResult
import app.tutora.usecases.WhoId
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class RemindersTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var t: TestDb
    private lateinit var reminders: Reminders
    private lateinit var ana: String
    private val online = BuiltinPlace.ONLINE.id

    @Before
    fun setUp() =
        runTest {
            t = TestDb(tmp.root)
            reminders = Reminders(t.ctx)
            t.catalog.ensureBuiltinPlaces()
            ana =
                (
                    t.catalog.saveStudent(
                        Student("", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(online, true, 12_000)),
                    ) as Result.Ok
                ).value.id
        }

    private suspend fun settings(change: (Settings) -> Settings) {
        val repo = SettingsRepository(t.db)
        repo.save(change(repo.load()), t.now)
    }

    private suspend fun lesson(
        hour: Int,
        date: String = "2026-10-26",
    ): String =
        (
            t.schedule.scheduleLesson(
                LessonDraft(WhoId.OfStudent(ana), online, LocalDate.parse(date), LocalTime(hour, 0), 60, null, null, null),
            ) as ScheduleResult.Ok
        ).lessonId ?: ""

    @Test
    fun `AC-014-12 the reminder is set the chosen minutes before the next class and tells who, when and where`() =
        runTest {
            settings { it.copy(remindBefore = 15) }
            val a = lesson(14)
            reminders.plan()
            // 14:00 in São Paulo is 17:00 UTC; 15 minutes before.
            assertEquals("2026-10-26T16:45:00.000Z" to a, t.reminders.set[ReminderKind.CLASS])
            val notice = reminders.fire(ReminderKind.CLASS, a) as ReminderNotice.Class
            assertEquals(a, notice.lesson.id)
            settings { it.copy(remindBefore = 0) }
            reminders.plan()
            assertNull(t.reminders.set[ReminderKind.CLASS])
        }

    @Test
    fun `AC-014-13 who owes is set weekly on Monday at 08 00, says how many and how much, and says nothing when no one owes`() =
        runTest {
            reminders.plan()
            assertEquals("2026-11-02T11:00:00.000Z", t.reminders.set[ReminderKind.OWED]?.first)
            assertNull(reminders.fire(ReminderKind.OWED, null))
            val a = lesson(7, "2026-10-23")
            t.lessons.markDone(a, 60)
            assertEquals(ReminderNotice.Owed(OwedSummary(1, 12_000)), reminders.fire(ReminderKind.OWED, null))
            settings { it.copy(owedWeekly = false) }
            reminders.plan()
            assertNull(t.reminders.set[ReminderKind.OWED])
        }

    @Test
    fun `AC-014-14 to confirm comes daily at 20 00 with the count, and never with Marcar como dadas sozinhas`() =
        runTest {
            lesson(7, "2026-10-23")
            lesson(8, "2026-10-23")
            reminders.plan()
            assertEquals("2026-10-26T23:00:00.000Z", t.reminders.set[ReminderKind.CONFIRM]?.first)
            assertEquals(ReminderNotice.Confirm(2), reminders.fire(ReminderKind.CONFIRM, null))
            // Marcar todas from the notification asks nothing more.
            assertEquals(2, Confirming(t.ctx).confirmAll().size)
            assertNull(reminders.fire(ReminderKind.CONFIRM, null))
            lesson(7, "2026-10-24")
            settings { it.copy(pastClasses = PastClasses.AUTO) }
            assertNull(reminders.fire(ReminderKind.CONFIRM, null))
        }

    @Test
    fun `AC-014-15 reminders follow every change - a class cancelled or added moves the next reminder`() =
        runTest {
            settings { it.copy(remindBefore = 15) }
            val a = lesson(14)
            reminders.plan()
            assertEquals(a, t.reminders.set[ReminderKind.CLASS]?.second)
            val b = lesson(11)
            reminders.plan()
            assertEquals(b, t.reminders.set[ReminderKind.CLASS]?.second)
            t.lessons.cancel(b, Cancellation(CancelReason.TEACHER))
            reminders.plan()
            assertEquals(a, t.reminders.set[ReminderKind.CLASS]?.second)
            // A cancelled class's alarm, already set, posts nothing.
            assertNull(reminders.fire(ReminderKind.CLASS, b))
        }

    @Test
    fun `AC-014-16 turning a reminder on asks for notifications once, and refused the page says so with the phone's settings`() {
        val h = Harness(t)
        h.notificationPermission.allowed = false
        h.notificationPermission.answer = false
        h.show(compose)
        h.go(compose, RemindersHome)
        compose.onNodeWithText("15 min antes").performClick()
        h.settle(compose)
        assertEquals(1, h.notificationPermission.asked)
        compose.onNodeWithTag("notifications-off").assertExists().performClick()
        assertTrue(h.notificationPermission.settingsOpened)
        assertEquals(15, runBlocking { h.services.currentSettings().remindBefore })
    }
}
