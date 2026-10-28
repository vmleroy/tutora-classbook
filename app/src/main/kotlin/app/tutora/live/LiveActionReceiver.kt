package app.tutora.live

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.tutora.TutoraApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Pausar and Retomar from the notification, through the same use cases as the screen (architecture, rule 5). */
class LiveActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val id = intent.getStringExtra(AndroidLiveNotifier.EXTRA_LESSON) ?: return
        val live = (context.applicationContext as TutoraApplication).container.live
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    AndroidLiveNotifier.ACTION_PAUSE -> live.pause(id)
                    AndroidLiveNotifier.ACTION_RESUME -> live.resume(id)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
