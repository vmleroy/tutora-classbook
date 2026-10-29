package app.tutora

import android.content.Context
import androidx.core.app.LocaleManagerCompat
import app.tutora.data.openTutoraDatabase
import app.tutora.domain.isoText
import app.tutora.domain.uuidv7
import app.tutora.live.AndroidAutoStartAlarm
import app.tutora.live.AndroidLiveNotifier
import app.tutora.usecases.Backups
import app.tutora.usecases.Ctx
import app.tutora.usecases.Live
import app.tutora.usecases.StartApp
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

    /** The live notification, shared by the screens and the notification's own buttons. */
    val notifier = AndroidLiveNotifier(context)

    /** The alarm that starts a class with the app closed (AC-007-22). */
    val alarm = AndroidAutoStartAlarm(context)

    /** Where automatic backups go, and what wakes the app for them (spec 011). */
    private val backupFolder =
        app.tutora.adapters.backup
            .AndroidBackupFolder(context)
    private val backupScheduler =
        app.tutora.adapters.backup
            .WorkManagerBackupScheduler(context)

    /** The phone's calendars and the event texts (spec 012). */
    private val calendar =
        app.tutora.adapters.calendar
            .AndroidPhoneCalendar(context)
    private val calendarWords =
        app.tutora.adapters.calendar
            .androidCalendarWords(context)

    // Opened once per process, so the notification's buttons use the same database as the screens.
    private val database by lazy { openTutoraDatabase(context) }

    /** The live timer for the notification's buttons, which run without the screens. */
    val live by lazy { Live(ctx) }

    /** Automatic backups for the 20:00 work, which runs without the screens (AC-011-6). */
    val backups by lazy { Backups(ctx) }

    private val ctx by lazy {
        Ctx(
            database.first,
            ::now,
            ::newId,
            live = notifier,
            alarm = alarm,
            backupFolder = backupFolder,
            backupScheduler = backupScheduler,
            privateDir = context.filesDir,
            calendar = calendar,
            calendarWords = calendarWords,
        )
    }

    val startApp =
        StartApp(
            openDatabase = { database },
            phoneTags = {
                LocaleManagerCompat
                    .getSystemLocales(context)
                    .toLanguageTags()
                    .split(',')
                    .filter { it.isNotBlank() }
            },
            now = ::now,
            newId = ::newId,
            notifier = notifier,
            alarm = alarm,
            backupFolder = backupFolder,
            backupScheduler = backupScheduler,
            privateDir = context.filesDir,
            calendar = calendar,
            calendarWords = calendarWords,
        )
}

data class AppInfo(
    val versionName: String,
    val build: String,
)
