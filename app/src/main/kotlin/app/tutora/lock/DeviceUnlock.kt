package app.tutora.lock

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.tutora.R
import java.lang.ref.WeakReference

/** The phone's own unlock: fingerprint or face first, its PIN, pattern or password as the fallback (spec 015). */
interface DeviceUnlock {
    /** Whether the phone has a screen lock to ask for (AC-015-2). */
    fun available(): Boolean

    /** Asks for the phone's unlock; true once it was given. */
    fun ask(onResult: (Boolean) -> Unit)

    /** Opens the phone's security settings, to set a screen lock (AC-015-2). */
    fun openSecuritySettings()

    /** Before the activity is there: nothing to ask with. */
    object None : DeviceUnlock {
        override fun available() = false

        override fun ask(onResult: (Boolean) -> Unit) = onResult(false)

        override fun openSecuritySettings() = Unit
    }
}

/** BiometricPrompt over the activity, allowing weak biometrics or the device credential (AC-015-6). */
class AndroidDeviceUnlock(
    activity: FragmentActivity,
) : DeviceUnlock {
    private val ref = WeakReference(activity)
    private val context: Context = activity.applicationContext

    override fun available(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            // Before Android 11 that pair can't be checked; a secure keyguard means a lock is set.
            context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
        }

    override fun ask(onResult: (Boolean) -> Unit) {
        val activity = ref.get() ?: return onResult(false)
        val prompt =
            BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) = onResult(false)
                },
            )
        val info =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(activity.getString(R.string.lock_promptTitle))
                .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                .build()
        prompt.authenticate(info)
    }

    override fun openSecuritySettings() {
        val activity = ref.get() ?: return
        runCatching { activity.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
    }
}
