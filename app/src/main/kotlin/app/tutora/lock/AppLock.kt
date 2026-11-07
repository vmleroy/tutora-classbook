package app.tutora.lock

import android.os.SystemClock
import app.tutora.domain.Settings
import app.tutora.domain.shouldLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the gate in front of the app shows (spec 015). */
enum class Gate {
    /** The settings are not read yet: nothing shows, so a locked app never flashes its content. */
    UNKNOWN,
    LOCKED,
    OPEN,
}

/**
 * The app lock, one per process. Whether Tutora is locked lives here in memory only: a new
 * process starts locked when the lock is on (AC-015-6). [clock] is the time since boot, so
 * changing the phone's clock never skips the lock.
 */
class AppLock(
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    /** The phone's unlock, set by the activity once it exists. */
    var device: DeviceUnlock = DeviceUnlock.None

    private val state = MutableStateFlow(Gate.UNKNOWN)
    val gate: StateFlow<Gate> = state.asStateFlow()

    private var settings: Settings? = null
    private var leftAt: Long? = null
    private var asking = false

    /**
     * On only while the phone has a screen lock: a lock restored from another phone's backup
     * never shuts the teacher out (plan, Risks).
     */
    fun active(s: Settings? = settings): Boolean = s != null && s.appLock && device.available()

    /** Follows the saved settings: the first ones decide the start; turning it off opens at once. */
    fun follow(s: Settings) {
        val first = settings == null
        settings = s
        if (first) {
            state.value = if (active(s)) Gate.LOCKED else Gate.OPEN
        } else if (!active(s)) {
            state.value = Gate.OPEN
        }
    }

    /** Tutora left the screen. The unlock prompt itself never counts (the PIN screen of older phones). */
    fun left() {
        if (!asking) leftAt = clock()
    }

    /** Tutora is back: locks if it was away longer than Bloquear após (AC-015-8). */
    fun back() {
        val s = settings ?: return
        val at = leftAt
        leftAt = null
        if (shouldLock(active(s), s.lockAfter, at, clock())) state.value = Gate.LOCKED
    }

    /** Desbloquear (AC-015-6, 9). */
    fun unlock() =
        ask { ok ->
            if (ok) state.value = Gate.OPEN
        }

    /** Asks for the phone's unlock once, for turning the lock on or off (AC-015-1, 3). */
    fun ask(onResult: (Boolean) -> Unit) {
        if (asking) return
        asking = true
        device.ask { ok ->
            asking = false
            onResult(ok)
        }
    }
}

/**
 * With the lock on, Tutora's card in the recent-apps screen is blank where the phone allows it
 * without blocking screenshots (Android 13+). Screenshots are always allowed: FLAG_SECURE is
 * never set (AC-015-14; Victor, 7 Nov 2026).
 */
fun applyPrivacy(
    activity: android.app.Activity,
    lockOn: Boolean,
) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        activity.setRecentsScreenshotEnabled(!lockOn)
    }
}
