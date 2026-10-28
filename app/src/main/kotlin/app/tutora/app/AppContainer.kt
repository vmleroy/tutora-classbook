package app.tutora.app

import android.content.Context
import androidx.core.app.LocaleManagerCompat
import app.tutora.app.usecases.StartApp
import app.tutora.data.openTutoraDatabase
import app.tutora.domain.isoText
import app.tutora.domain.uuidv7
import java.security.SecureRandom
import kotlin.time.Clock

/** Hand-written wiring: every dependency the app needs, built once (architecture doc). */
class AppContainer(
    private val context: Context,
) {
    val appInfo =
        AppInfo(
            versionName = BuildConfig.VERSION_NAME,
            build = BuildConfig.VERSION_CODE.toString(),
        )

    private val random = SecureRandom()

    fun now(): String = Clock.System.now().isoText()

    fun newId(): String = uuidv7(System.currentTimeMillis(), ByteArray(10).also(random::nextBytes))

    val startApp =
        StartApp(
            openDatabase = { openTutoraDatabase(context) },
            phoneTags = {
                LocaleManagerCompat
                    .getSystemLocales(context)
                    .toLanguageTags()
                    .split(',')
                    .filter { it.isNotBlank() }
            },
            now = ::now,
            newId = ::newId,
        )
}

data class AppInfo(
    val versionName: String,
    val build: String,
)
