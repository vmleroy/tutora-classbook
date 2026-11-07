package app.tutora

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.Language
import app.tutora.domain.LockAfter
import app.tutora.domain.Settings
import app.tutora.domain.startTimer
import app.tutora.live.AndroidLiveNotifier
import app.tutora.lock.Gate
import app.tutora.lock.applyPrivacy
import app.tutora.reminders.ReminderNotifier
import app.tutora.ui.BackupHome
import app.tutora.ui.PersonalData
import app.tutora.ui.RegistryGraph
import app.tutora.ui.SettingsGraph
import app.tutora.usecases.LiveNote
import app.tutora.usecases.ReminderNotice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class AppLockTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness
    private val minute = 60_000L

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
    }

    private fun settings() = runBlocking { h.services.currentSettings() }

    private fun save(change: (Settings) -> Settings) = runBlocking { h.services.saveSettings(change(settings())) }

    /** Opens Tutora with the lock on; the first unlock answers [answer]. */
    private fun lockedStart(answer: Boolean = false) {
        save { it.copy(appLock = true) }
        h.unlock.answer = answer
        h.show(compose)
    }

    /** Leaves Tutora for [ms] and comes back, as the activity's onStop and onStart do. */
    private fun away(ms: Long) {
        compose.runOnIdle { h.lock.left() }
        h.clockMs += ms
        compose.runOnIdle { h.lock.back() }
        h.settle(compose)
    }

    private fun personal() {
        h.go(compose, PersonalData)
    }

    @Test
    fun `AC-015-1 Bloquear o Tutora is off at first, asks the phone's unlock once to turn on, and Ajustes then says so`() {
        h.show(compose)
        personal()
        compose.onNodeWithText("Privacidade").performScrollTo().assertExists()
        assertFalse(settings().appLock)
        h.unlock.answer = false
        compose.onNodeWithContentDescription("Bloquear o Tutora").performScrollTo().performClick()
        h.settle(compose)
        assertEquals(1, h.unlock.asks)
        assertFalse(settings().appLock)
        h.unlock.answer = true
        compose.onNodeWithContentDescription("Bloquear o Tutora").performScrollTo().performClick()
        h.awaitDb(compose) { h.services.currentSettings().appLock }
        assertEquals(2, h.unlock.asks)
        // It takes effect without locking the page in use.
        assertEquals(Gate.OPEN, h.lock.gate.value)
        compose.runOnIdle { h.nav.popBackStack() }
        h.go(compose, SettingsGraph)
        compose.onNodeWithText("Bloqueio ligado", substring = true).assertExists()
    }

    @Test
    fun `AC-015-2 with no screen lock on the phone the switch can't be turned on and offers the phone's settings`() {
        h.unlock.available = false
        h.show(compose)
        personal()
        compose.onNodeWithText("Defina um bloqueio de tela no celular para usar").performScrollTo().assertExists()
        compose.onNodeWithContentDescription("Bloquear o Tutora").assertDoesNotExist()
        compose.onNodeWithText("Abrir configurações").performScrollTo().performClick()
        assertTrue(h.unlock.settingsOpened)
        assertEquals(0, h.unlock.asks)
    }

    @Test
    fun `AC-015-3 turning the lock off asks the phone's unlock first, then it never locks`() {
        lockedStart(answer = true)
        personal()
        val asks = h.unlock.asks
        h.unlock.answer = false
        compose.onNodeWithContentDescription("Bloquear o Tutora").performScrollTo().performClick()
        h.settle(compose)
        assertTrue(settings().appLock)
        h.unlock.answer = true
        compose.onNodeWithContentDescription("Bloquear o Tutora").performScrollTo().performClick()
        h.awaitDb(compose) { !h.services.currentSettings().appLock }
        assertEquals(asks + 2, h.unlock.asks)
        away(60 * minute)
        assertEquals(Gate.OPEN, h.lock.gate.value)
    }

    @Test
    fun `AC-015-4 Bloquear apos shows under the switch once on, 5 minutos at first`() {
        lockedStart(answer = true)
        personal()
        compose.onNodeWithText("Bloquear após").performScrollTo().assertExists()
        listOf("Imediatamente", "1 minuto", "5 minutos").forEach { compose.onNodeWithText(it).performScrollTo().assertExists() }
        assertEquals(LockAfter.FIVE_MINUTES, settings().lockAfter)
        compose.onNodeWithText("Imediatamente").performClick()
        h.awaitDb(compose) { h.services.currentSettings().lockAfter == LockAfter.IMMEDIATELY }
        assertEquals(LockAfter.IMMEDIATELY, settings().lockAfter)
    }

    @Test
    fun `AC-015-5 saving the Pix key the first time offers the lock once`() {
        h.show(compose)
        personal()
        val pix = hasSetTextAction() and hasAnyAncestor(hasTestTag("pix-key"))
        compose.onNode(pix).performScrollTo().performTextInput("11987659876")
        h.settle(compose)
        // Leaving the field makes the offer.
        compose.onAllNodes(hasSetTextAction())[0].performClick()
        h.settle(compose)
        compose.onNodeWithText("Sua chave Pix fica no app. Quer proteger o Tutora com o bloqueio do celular?").assertExists()
        compose.onNodeWithText("Ativar").performClick()
        h.awaitDb(compose) { h.services.currentSettings().appLock }
        assertTrue(settings().appLock)
        assertTrue(settings().lockOffered)
        assertEquals(1, h.unlock.asks)
    }

    @Test
    fun `AC-015-6 opening Tutora with the lock on shows the lock screen first and asks for the phone's unlock`() {
        lockedStart(answer = false)
        compose.onNodeWithTag("lock-screen").assertExists()
        compose.onNodeWithText("Tutora").assertExists()
        compose.onNodeWithText("Desbloquear").assertExists()
        // It asked at once; nothing of Hoje is there to read.
        assertEquals(1, h.unlock.asks)
        compose.onNodeWithTag("today-screen").assertDoesNotExist()
        h.unlock.answer = true
        compose.onNodeWithText("Desbloquear").performClick()
        h.settle(compose)
        compose.onNodeWithTag("lock-screen").assertDoesNotExist()
        compose.onNodeWithTag("today-screen").assertExists()
    }

    @Test
    fun `AC-015-7 once unlocked nothing asks again while Tutora is in use`() {
        lockedStart(answer = true)
        val asks = h.unlock.asks
        h.go(compose, RegistryGraph)
        h.go(compose, SettingsGraph)
        personal()
        h.go(compose, BackupHome)
        assertEquals(asks, h.unlock.asks)
        assertEquals(Gate.OPEN, h.lock.gate.value)
    }

    @Test
    fun `AC-015-8 it locks again only after Bloquear apos away, never for turning the phone`() {
        lockedStart(answer = true)
        h.unlock.answer = false
        away(4 * minute)
        assertEquals(Gate.OPEN, h.lock.gate.value)
        h.landscape = true
        h.settle(compose)
        h.landscape = false
        h.settle(compose)
        assertEquals(Gate.OPEN, h.lock.gate.value)
        away(5 * minute)
        assertEquals(Gate.LOCKED, h.lock.gate.value)
        compose.onNodeWithTag("lock-screen").assertExists()
    }

    @Test
    fun `AC-015-9 cancelled, the lock screen stays and nothing behind it shows, not even an open sheet`() {
        runBlocking { h.services.backups.setFolder("content://tree/downloads", "Download") }
        lockedStart(answer = true)
        h.go(compose, BackupHome)
        compose.onNodeWithText("Backup completo (.json)").performScrollTo().performClick()
        h.settle(compose)
        compose.onNodeWithText("Salvar o backup em Download?").assertExists()
        h.unlock.answer = false
        away(10 * minute)
        compose.onNodeWithTag("lock-screen").assertExists()
        compose.onNodeWithText("Salvar o backup em Download?").assertDoesNotExist()
        compose.onNodeWithText("Backup completo (.json)").assertDoesNotExist()
        compose.onNodeWithText("Desbloquear").performClick()
        h.settle(compose)
        compose.onNodeWithTag("lock-screen").assertExists()
    }

    @Test
    fun `AC-015-10 once unlocked Tutora is where it was, with what was typed and an open sheet, or where a notification led`() {
        runBlocking { h.services.backups.setFolder("content://tree/downloads", "Download") }
        lockedStart(answer = true)
        personal()
        compose.onAllNodes(hasSetTextAction())[0].performScrollTo().performTextInput("Victor")
        h.settle(compose)
        h.unlock.answer = false
        away(10 * minute)
        // A notification opened while locked lands behind the gate.
        compose.runOnIdle { h.nav.navigate(BackupHome) }
        h.settle(compose)
        h.unlock.answer = true
        compose.onNodeWithText("Desbloquear").performClick()
        h.settle(compose)
        compose.onNodeWithTag("lock-screen").assertDoesNotExist()
        compose.onNodeWithText("Backup completo (.json)").performScrollTo().assertExists()
        compose.runOnIdle { h.nav.popBackStack() }
        h.settle(compose)
        compose.onNodeWithText("Victor").assertExists()
        // An open sheet comes back too.
        h.go(compose, BackupHome)
        compose.onNodeWithText("Backup completo (.json)").performScrollTo().performClick()
        h.settle(compose)
        h.unlock.answer = false
        away(10 * minute)
        h.unlock.answer = true
        compose.onNodeWithText("Desbloquear").performClick()
        h.settle(compose)
        compose.onNodeWithText("Salvar o backup em Download?").assertExists()
    }

    @Test
    @Config(qualifiers = "pt-rBR-w800dp-h360dp-land")
    fun `AC-015-11 the lock screen fits sideways on a 360 dp phone with Muito grande text`() {
        save { it.copy(textScale = "xlarge", uiScale = "large") }
        lockedStart(answer = false)
        compose.onNodeWithText("Desbloquear").assertExists()
        compose.assertFitsSideways("Lock screen")
    }

    @Test
    fun `AC-015-12 Pausar, Retomar and Marcar todas work from the notification without opening Tutora, so without the unlock`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val timer = startTimer("l", "2026-10-26T12:00:00.000Z")
        AndroidLiveNotifier(app).show(LiveNote("l", "Ana Souza", timer, "2026-10-26T12:10:00.000Z"))
        ReminderNotifier(app).post(ReminderNotice.Confirm(3), Language.PT_BR, 15, "America/Sao_Paulo")
        val actions = Shadows.shadowOf(manager).allNotifications.flatMap { it.actions.toList() }
        val pause = actions.single { it.title.toString() == "Pausar" }
        val all = actions.single { it.title.toString() == "Marcar todas como dadas" }
        // Broadcasts run in the background: the lock, which stands in front of the screens, never sees them.
        assertTrue(pause.actionIntent.isBroadcast)
        assertTrue(all.actionIntent.isBroadcast)
        // The notifications stay as they are, names included.
        assertTrue(Shadows.shadowOf(manager).allNotifications.any { it.extras.getString("android.title") == "Ana Souza" })
    }

    @Test
    fun `AC-015-14 with the lock on, screenshots are still allowed`() {
        val activity =
            Robolectric
                .buildActivity(androidx.activity.ComponentActivity::class.java)
                .setup()
                .get()
        applyPrivacy(activity, lockOn = true)
        assertEquals(0, activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Test
    fun `AC-015-16 the Pix key shows masked in Ajustes and Seus dados until the eye shows it`() {
        save { it.copy(pixKey = "victor@gmail.com", teacherName = "Victor") }
        h.show(compose)
        h.go(compose, SettingsGraph)
        compose.onNodeWithText("Victor · vi•••@gmail.com").assertExists()
        compose.onAllNodesWithText("victor@gmail.com", substring = true).fetchSemanticsNodes().let { assertTrue(it.isEmpty()) }
        personal()
        compose.onNodeWithText("vi•••@gmail.com").performScrollTo().assertExists()
        compose.onNodeWithText("victor@gmail.com").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mostrar").performScrollTo().performClick()
        h.settle(compose)
        compose.onNodeWithText("victor@gmail.com").assertExists()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("pix-key"))).assertExists()
    }

    @Test
    @Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-015-17 the lock screen and the Privacy rows follow the app language`() {
        save { it.copy(language = Language.EN) }
        lockedStart(answer = false)
        compose.onNodeWithText("Unlock").assertExists()
        compose.onNodeWithText("Unlock to continue").assertExists()
        h.unlock.answer = true
        compose.onNodeWithText("Unlock").performClick()
        h.settle(compose)
        personal()
        listOf("Privacy", "Lock after", "Immediately", "5 minutes").forEach { compose.onNodeWithText(it).performScrollTo().assertExists() }
        compose.onNodeWithContentDescription("Lock Tutora").assertExists()
    }
}
