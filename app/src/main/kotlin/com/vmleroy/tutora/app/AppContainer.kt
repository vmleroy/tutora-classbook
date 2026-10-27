package com.vmleroy.tutora.app

import android.content.Context
import androidx.core.app.LocaleManagerCompat
import com.vmleroy.tutora.app.usecases.StartApp
import com.vmleroy.tutora.data.openTutoraDatabase
import java.time.Instant

/** Hand-written wiring: every dependency the app needs, built once (architecture doc). */
class AppContainer(
    private val context: Context,
) {
    val appInfo =
        AppInfo(
            versionName = BuildConfig.VERSION_NAME,
            build = BuildConfig.VERSION_CODE.toString(),
        )

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
            now = { Instant.now().toString() },
        )
}

data class AppInfo(
    val versionName: String,
    val build: String,
)
