package app.tutora

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.ui.RemindersHome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class FieldsTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
    }

    @Test
    fun `AC-014-37 each value has its field - a price typed as money, a reminder's hour picked as a time`() {
        // The setup's prices are money fields, filling from the right: R$ 100,00 then 5 is R$ 1.000,05.
        runBlocking { h.services.saveSettings(h.services.currentSettings().copy(setupDone = false)) }
        h.show(compose)
        repeat(2) {
            compose.onNodeWithTag("setup-next").performClick()
            h.settle(compose)
        }
        compose.onNode(hasSetTextAction() and hasContentDescription("Online / Remoto")).performTextReplacement("R$ 100,005")
        h.settle(compose)
        compose.onNodeWithTag("setup-skip").performClick()
        h.settle(compose)
        compose.onNodeWithText("Ir para Hoje").performClick()
        h.awaitDb(compose) {
            h.t.db
                .places()
                .get(BuiltinPlace.ONLINE.id)
                ?.rateCents == 100_005L
        }
        assertEquals(
            100_005L,
            runBlocking {
                h.t.db
                    .places()
                    .get(BuiltinPlace.ONLINE.id)
                    ?.rateCents
            },
        )
        // Lembretes' hours are clock times, opened in the time picker.
        h.go(compose, RemindersHome)
        compose.onNode(hasContentDescription("Horário: 08:00") and hasClickAction()).assertExists()
    }
}
