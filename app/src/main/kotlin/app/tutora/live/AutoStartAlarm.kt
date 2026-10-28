package app.tutora.live

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.tutora.TutoraApplication
import app.tutora.domain.instantOf
import app.tutora.usecases.AutoStartAlarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * One alarm at the next class's start time, for "Também com o app fechado" (AC-007-22). It is
 * allowed while idle but not exact, so Android may deliver it a few minutes late; the class still
 * counts from its planned start. No exact-alarm permission is needed.
 */
class AndroidAutoStartAlarm(
    private val context: Context,
) : AutoStartAlarm {
    private fun intent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AutoStartReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    override fun set(at: String?) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        if (at == null) {
            manager.cancel(intent())
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, instantOf(at).toEpochMilliseconds(), intent())
        }
    }

    companion object {
        const val ACTION = "app.tutora.live.AUTO_START"
    }
}

/** The alarm went off, or the phone restarted: start what is due and set the next alarm (AC-007-22). */
class AutoStartReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != AndroidAutoStartAlarm.ACTION && intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val live = (context.applicationContext as TutoraApplication).container.live
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                live.autoStart()
            } finally {
                pending.finish()
            }
        }
    }
}
