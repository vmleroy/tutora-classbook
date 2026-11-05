package app.tutora.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.tutora.MainActivity
import app.tutora.R
import app.tutora.TutoraApplication
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.Language
import app.tutora.domain.Who
import app.tutora.domain.instantOf
import app.tutora.domain.localOf
import app.tutora.ui.clockTime
import app.tutora.usecases.ReminderAlarm
import app.tutora.usecases.ReminderKind
import app.tutora.usecases.ReminderNotice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * One alarm per reminder (spec 014), like the automatic start's (AC-007-22): allowed while idle but
 * not exact, so Android may deliver it a few minutes late. No exact-alarm permission is needed.
 */
class AndroidReminderAlarm(
    private val context: Context,
) : ReminderAlarm {
    private fun intent(
        kind: ReminderKind,
        lessonId: String?,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            kind.ordinal + REQUEST_BASE,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ACTION)
                .putExtra(EXTRA_KIND, kind.key)
                .putExtra(EXTRA_LESSON, lessonId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    override fun set(
        kind: ReminderKind,
        at: String?,
        lessonId: String?,
    ) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        if (at == null) {
            manager.cancel(intent(kind, null))
        } else {
            val ms = instantOf(at).toEpochMilliseconds()
            // Only the class reminder needs the minute; the weekly and daily ones may come a little late (AC-014-27).
            if (kind == ReminderKind.CLASS && exactAllowed(context)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, intent(kind, lessonId))
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, intent(kind, lessonId))
            }
        }
    }

    companion object {
        const val ACTION = "app.tutora.reminders.REMIND"
        const val ACTION_CONFIRM_ALL = "app.tutora.reminders.CONFIRM_ALL"
        const val EXTRA_KIND = "kind"
        const val EXTRA_LESSON = "lesson"

        /** What a tap on a reminder opens: a class, the confirm list or who owes. */
        const val EXTRA_OPEN = "app.tutora.open"
        const val OPEN_LESSON = "lesson"
        const val OPEN_CONFIRM = "confirm"
        const val OPEN_OWED = "owed"
        const val OPEN_TODAY = "today"
        private const val REQUEST_BASE = 100
    }
}

/**
 * Whether exact alarms are allowed: always before Android 12, granted at install with
 * USE_EXACT_ALARM, or once the teacher turns on SCHEDULE_EXACT_ALARM (AC-014-27).
 */
fun exactAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

/** Whether the phone lets Tutora post notifications (AC-014-16). */
fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** Posts a reminder's notification in the app language, on the "Lembretes" channel. */
class ReminderNotifier(
    private val context: Context,
) {
    private fun resources(language: Language): Resources {
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language.tag)) }
        return context.createConfigurationContext(config).resources
    }

    private fun channel(res: Resources) {
        val c =
            NotificationChannel(CHANNEL, res.getString(R.string.remind_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = res.getString(R.string.remind_channelHint)
            }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(c)
    }

    private fun open(
        what: String,
        lessonId: String? = null,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            what.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(AndroidReminderAlarm.EXTRA_OPEN, what)
                .putExtra(AndroidReminderAlarm.EXTRA_LESSON, lessonId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun post(
        notice: ReminderNotice,
        language: Language,
        minutesBefore: Int,
        zone: String,
    ) {
        // Without the permission there is nothing to post; Ajustes › Lembretes says so (AC-014-16).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val res = resources(language)
        channel(res)
        val b =
            NotificationCompat
                .Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
        val id =
            when (notice) {
                is ReminderNotice.Class -> {
                    val l = notice.lesson
                    val who =
                        when (val w = l.who) {
                            is Who.OfStudent -> w.student.name
                            is Who.OfGroup -> w.group.name
                        }
                    val place =
                        when (l.place.builtin) {
                            BuiltinPlace.STUDENT_HOME -> res.getString(R.string.places_builtin_student_home)
                            BuiltinPlace.ONLINE -> res.getString(R.string.places_builtin_online)
                            null -> l.place.name.orEmpty()
                        }
                    // The minutes really left: an inexact alarm can come later than asked.
                    val left =
                        ((instantOf(l.startAt).toEpochMilliseconds() - System.currentTimeMillis()) / MINUTE_MS)
                            .toInt()
                            .coerceIn(1, minutesBefore.coerceAtLeast(1))
                    b
                        .setContentTitle(res.getQuantityString(R.plurals.remind_classIn, left, left))
                        .setContentText(
                            res.getString(R.string.remind_classText, who, clockTime(localOf(l.startAt, zone).time, language), place),
                        ).setContentIntent(open(AndroidReminderAlarm.OPEN_LESSON, l.id))
                    ID_CLASS
                }
                is ReminderNotice.Owed -> {
                    val money =
                        app.tutora.designsystem.components
                            .formatMoney(notice.summary.cents, language)
                    val n = notice.summary.count
                    b
                        .setContentTitle(
                            if (notice.onlyStudents) {
                                res.getQuantityString(R.plurals.remind_owed, n, n, money)
                            } else {
                                res.getQuantityString(R.plurals.remind_owedMixed, n, n, money)
                            },
                        ).setContentText(res.getString(R.string.remind_owedText))
                        // Cadastro lists students; with groups or places owing too, Hoje's alert lists everyone.
                        .setContentIntent(
                            open(if (notice.onlyStudents) AndroidReminderAlarm.OPEN_OWED else AndroidReminderAlarm.OPEN_TODAY),
                        )
                    ID_OWED
                }
                is ReminderNotice.Confirm -> {
                    val all =
                        PendingIntent.getBroadcast(
                            context,
                            ID_CONFIRM,
                            Intent(context, ReminderReceiver::class.java).setAction(AndroidReminderAlarm.ACTION_CONFIRM_ALL),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    b
                        .setContentTitle(res.getQuantityString(R.plurals.confirm_count, notice.count, notice.count))
                        .setContentIntent(open(AndroidReminderAlarm.OPEN_CONFIRM))
                        .addAction(0, res.getString(R.string.remind_markAll), all)
                        .addAction(0, res.getString(R.string.remind_see), open(AndroidReminderAlarm.OPEN_CONFIRM))
                    ID_CONFIRM
                }
            }
        NotificationManagerCompat.from(context).notify(id, b.build())
    }

    fun cancelConfirm() = NotificationManagerCompat.from(context).cancel(ID_CONFIRM)

    companion object {
        const val CHANNEL = "reminders"
        private const val ID_CLASS = 21
        private const val ID_OWED = 22
        private const val ID_CONFIRM = 23
        private const val MINUTE_MS = 60_000L
    }
}

/**
 * A reminder's alarm went off, "Marcar todas como dadas" was tapped, or the phone restarted: post
 * what is due and set the next alarms (AC-014-14, 15).
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val container = (context.applicationContext as TutoraApplication).container
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    AndroidReminderAlarm.ACTION -> {
                        val kind = ReminderKind.fromKey(intent.getStringExtra(AndroidReminderAlarm.EXTRA_KIND)) ?: return@launch
                        val notice = container.reminders.fire(kind, intent.getStringExtra(AndroidReminderAlarm.EXTRA_LESSON))
                        val s = container.settings()
                        notice?.let { container.reminderNotifier.post(it, s.language, s.remindBefore, container.zone()) }
                    }
                    // Marcar todas asks nothing more: the notification said how many (AC-014-14).
                    AndroidReminderAlarm.ACTION_CONFIRM_ALL -> {
                        container.confirming.confirmAll()
                        container.reminderNotifier.cancelConfirm()
                    }
                    // After a restart, or once exact alarms are allowed, the reminders are set again.
                    Intent.ACTION_BOOT_COMPLETED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED ->
                        container.reminders
                            .plan()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
