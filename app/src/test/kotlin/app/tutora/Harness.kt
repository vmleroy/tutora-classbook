package app.tutora

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    lateinit var nav: NavHostController

    init {
        runBlocking {
            t.catalog.ensureBuiltinPlaces()
            services.saveSettings(Settings(language = Language.PT_BR))
        }
    }

    @Composable
    fun Content() {
        nav = rememberNavController()
        val settings by services.settings.collectAsState(initial = Settings(language = Language.PT_BR))
        TutoraTheme(dark = false, largeText = settings.textScale == "large") {
            CompositionLocalProvider(
                LocalServices provides services,
                app.tutora.adapters.export.LocalExports provides exports,
                app.tutora.ui.backup.LocalBackupPicker provides picker,
                app.tutora.ui.calendar.LocalCalendarPermission provides calendarPermission,
            ) { TutoraNav(AppInfo("1.1.0", "7"), services.schemaVersion, nav = nav) }
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

class FakeCalendarPermission : app.tutora.ui.calendar.CalendarPermission {
    var answer = true
    var settingsOpened = false

    override fun request(onResult: (Boolean) -> Unit) = onResult(answer)

    override fun openSettings() {
        settingsOpened = true
    }
}
