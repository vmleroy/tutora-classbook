package app.tutora.live

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.tutora.MainActivity
import app.tutora.R
import app.tutora.domain.elapsedMs
import app.tutora.domain.instantOf
import app.tutora.ui.clockText
import app.tutora.usecases.LiveNote
import app.tutora.usecases.LiveNotifier

/**
 * The live class's ongoing notification (AC-007-15, 16). Running, the system ticks its
 * chronometer from the moment the class started minus its pauses, so no app code runs in the
 * background; paused, it shows the frozen time. Pausar / Retomar go to [LiveActionReceiver],
 * Encerrar and a tap open the app.
 */
class AndroidLiveNotifier(
    private val context: Context,
) : LiveNotifier {
    private val manager get() = NotificationManagerCompat.from(context)

    private fun channel() {
        val c =
            NotificationChannel(CHANNEL, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.notif_channelDesc)
                setShowBadge(false)
            }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(c)
    }

    private fun activity(
        lessonId: String,
        end: Boolean,
        code: Int,
    ): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_LESSON, lessonId)
                .putExtra(EXTRA_END, end)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun broadcast(
        action: String,
        lessonId: String,
    ): PendingIntent {
        val intent = Intent(context, LiveActionReceiver::class.java).setAction(action).putExtra(EXTRA_LESSON, lessonId)
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun show(note: LiveNote) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            // Without the permission the timer still works in the app (AC-007-16).
            return
        }
        channel()
        val elapsed = elapsedMs(note.timer, note.now)
        val running = note.timer.running
        val builder =
            NotificationCompat
                .Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(note.title)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setContentIntent(activity(note.lessonId, end = false, code = 1))
                .addAction(
                    0,
                    context.getString(if (running) R.string.live_pause else R.string.live_resume),
                    broadcast(if (running) ACTION_PAUSE else ACTION_RESUME, note.lessonId),
                ).addAction(0, context.getString(R.string.live_end), activity(note.lessonId, end = true, code = 2))
        if (running) {
            builder
                .setContentText(context.getString(R.string.status_live))
                .setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(instantOf(note.now).toEpochMilliseconds() - elapsed)
        } else {
            builder.setContentText(context.getString(R.string.notif_paused, clockText(elapsed))).setShowWhen(false)
        }
        manager.notify(ID, builder.build())
    }

    override fun hide() = manager.cancel(ID)

    companion object {
        const val CHANNEL = "live_lesson"
        const val ID = 7
        const val EXTRA_LESSON = "live_lesson"
        const val EXTRA_END = "live_end"
        const val ACTION_PAUSE = "app.tutora.live.PAUSE"
        const val ACTION_RESUME = "app.tutora.live.RESUME"
    }
}
