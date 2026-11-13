package app.tutora

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import app.tutora.designsystem.TutoraTheme
import app.tutora.domain.Language
import app.tutora.domain.Settings
import app.tutora.state.AppServices
import app.tutora.state.LocalServices
import app.tutora.ui.TutoraNav
import kotlinx.coroutines.runBlocking

/** The real navigation over a test database, a frozen clock and São Paulo time. */
class Harness(
    val t: TestDb,
) {
    val services = AppServices(t.db, t.ctx, 1)

    /** What reports shared and copied (spec 010). */
    val exports = FakeExports()

    /** What the folder and file pickers answer (spec 011); null is a cancelled picker. */
    val picker = FakeBackupPicker()

    /** What the calendar permission answers (spec 012). */
    val calendarPermission = FakeCalendarPermission()

    /** Whether notifications are allowed, and what asking answers (spec 014). */
    val notificationPermission = FakeNotificationPermission()

    /** Whether exact alarms are allowed, and whether their page was opened (AC-014-27). */
    val exactAlarm = FakeExactAlarm()

    /** What the camera, gallery and file pickers answer for comprovantes (spec 021): file paths. */
    val receiptPicker = FakeReceiptPicker()

    /** The phone's unlock (spec 015): whether a screen lock is set and what asking answers. */
    val unlock = FakeDeviceUnlock()

    /** The time since boot the app lock reads, moved by the tests. */
    var clockMs = 1_000_000L

    /** The app lock over the screens, as in the app (spec 015). */
    val lock =
        app.tutora.lock
            .AppLock { clockMs }
            .also { it.device = unlock }
    lateinit var nav: NavHostController

    /**
     * Turns the "phone" in the middle of a test (spec 017): true sideways, false upright, null
     * the window's own shape. The activity is never recreated on turning (configChanges), so a
     * new window size reaching the same composition is what a real turn does.
     */
    var landscape by androidx.compose.runtime.mutableStateOf<Boolean?>(null)

    init {
        runBlocking {
            t.catalog.ensureBuiltinPlaces()
            services.saveSettings(Settings(language = Language.PT_BR, setupDone = true))
        }
    }

    @Composable
    fun Content() {
        val base = androidx.compose.ui.platform.LocalWindowInfo.current
        val side = landscape
        val turned =
            if (side == null) {
                base
            } else {
                object : androidx.compose.ui.platform.WindowInfo by base {
                    override val containerSize: androidx.compose.ui.unit.IntSize
                        get() {
                            val s = base.containerSize
                            val long = maxOf(s.width, s.height)
                            val short = minOf(s.width, s.height)
                            return if (side) {
                                androidx.compose.ui.unit.IntSize(
                                    long,
                                    short,
                                )
                            } else {
                                androidx.compose.ui.unit
                                    .IntSize(short, long)
                            }
                        }
                }
            }
        CompositionLocalProvider(androidx.compose.ui.platform.LocalWindowInfo provides turned) { Themed() }
    }

    @Composable
    private fun Themed() {
        nav = rememberNavController()
        val settings by services.settings.collectAsState(initial = Settings(language = Language.PT_BR))
        TutoraTheme(
            dark = false,
            textScale =
                app.tutora.domain.TextSize
                    .of(settings.textScale)
                    .factor,
            uiScale =
                app.tutora.domain.UiSize
                    .of(settings.uiScale)
                    .factor,
        ) {
            CompositionLocalProvider(
                LocalServices provides services,
                app.tutora.adapters.export.LocalExports provides exports,
                app.tutora.ui.backup.LocalBackupPicker provides picker,
                app.tutora.ui.calendar.LocalCalendarPermission provides calendarPermission,
                app.tutora.ui.reminders.LocalNotificationPermission provides notificationPermission,
                app.tutora.ui.reminders.LocalExactAlarmPermission provides exactAlarm,
                app.tutora.lock.LocalAppLock provides lock,
                app.tutora.ui.receipts.LocalReceiptPicker provides receiptPicker,
            ) { app.tutora.lock.LockGate { TutoraNav(AppInfo("1.1.0", "7"), services.schemaVersion, nav = nav) } }
        }
    }

    fun show(compose: ComposeContentTestRule) {
        compose.setContent { Content() }
        settle(compose)
    }

    /**
     * Room emits from its own IO thread, which waitForIdle does not wait for: idle Compose and
     * give the database real time, so screens show their data before the test looks.
     */
    fun settle(compose: ComposeContentTestRule) {
        repeat(6) {
            compose.waitForIdle()
            Thread.sleep(40)
        }
        compose.waitForIdle()
    }

    /** Room writes on its own IO thread: idle Compose and give it real time until [done]. */
    fun awaitDb(
        compose: ComposeContentTestRule,
        done: suspend () -> Boolean,
    ) {
        repeat(100) {
            compose.waitForIdle()
            if (runBlocking { done() }) return
            Thread.sleep(50)
        }
    }

    fun go(
        compose: ComposeContentTestRule,
        route: Any,
    ) {
        compose.runOnIdle { nav.navigate(route) }
        settle(compose)
    }
}

class FakeBackupPicker : app.tutora.ui.backup.BackupPicker {
    var folderAnswer: String? = null
    var fileAnswer: String? = null

    override fun folder(onPicked: (String?) -> Unit) = onPicked(folderAnswer)

    override fun file(onPicked: (String?) -> Unit) = onPicked(fileAnswer)
}

/** The three sources of a comprovante answer with [answer], file paths in a temp folder; empty is cancelled. */
class FakeReceiptPicker : app.tutora.ui.receipts.ReceiptPicker {
    var answer: List<String> = emptyList()

    override fun camera(onPicked: (List<String>) -> Unit) = onPicked(answer)

    override fun gallery(onPicked: (List<String>) -> Unit) = onPicked(answer)

    override fun file(onPicked: (List<String>) -> Unit) = onPicked(answer)
}

class FakeDeviceUnlock : app.tutora.lock.DeviceUnlock {
    var available = true
    var answer = true
    var asks = 0
    var settingsOpened = false

    override fun available() = available

    override fun ask(onResult: (Boolean) -> Unit) {
        asks++
        onResult(answer)
    }

    override fun openSecuritySettings() {
        settingsOpened = true
    }
}

class FakeCalendarPermission : app.tutora.ui.calendar.CalendarPermission {
    var answer = true
    var settingsOpened = false

    override fun request(onResult: (Boolean) -> Unit) = onResult(answer)

    override fun openSettings() {
        settingsOpened = true
    }
}

class FakeNotificationPermission : app.tutora.ui.reminders.NotificationPermission {
    var allowed = true
    var answer = true
    var asked = 0
    var settingsOpened = false

    override fun allowed() = allowed

    override fun request(onResult: (Boolean) -> Unit) {
        asked++
        allowed = answer
        onResult(answer)
    }

    override fun openSettings() {
        settingsOpened = true
    }
}

class FakeExactAlarm : app.tutora.ui.reminders.ExactAlarmPermission {
    var allowed = true
    var opened = false

    override fun allowed() = allowed

    override fun open() {
        opened = true
    }
}

/**
 * Sideways nothing is cut off (AC-017-3): no text reaches past the window's left or right edge,
 * measured unclipped, unless it sits in a row that scrolls sideways.
 */
fun ComposeContentTestRule.assertFitsSideways(screen: String) {
    val root =
        onAllNodes(
            androidx.compose.ui.test
                .isRoot(),
        ).fetchSemanticsNodes().first().boundsInRoot
    val texts =
        onAllNodes(
            androidx.compose.ui.test.SemanticsMatcher
                .keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.Text),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
    check(texts.isNotEmpty()) { "$screen shows no text" }

    fun scrollsSideways(n: androidx.compose.ui.semantics.SemanticsNode): Boolean =
        generateSequence(n.parent) { it.parent }.any {
            it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.HorizontalScrollAxisRange)
        }
    texts.forEach { n ->
        if (n.size.width <= 0 || scrollsSideways(n)) return@forEach
        val left = n.positionInRoot.x
        val right = left + n.size.width
        val label = n.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)?.joinToString()
        org.junit.Assert.assertTrue("$screen: '$label' ends past the right edge ($right > ${root.right})", right <= root.right + 1f)
        org.junit.Assert.assertTrue("$screen: '$label' starts before the left edge ($left)", left >= root.left - 1f)
    }
}
