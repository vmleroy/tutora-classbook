package com.vmleroy.tutora.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vmleroy.tutora.app.ui.TutoraApp
import com.vmleroy.tutora.domain.Language

class MainActivity : AppCompatActivity() {
    private val container get() = (application as TutoraApplication).container

    private val app: AppViewModel by viewModels {
        viewModelFactory { initializer { AppViewModel(container.startApp) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The splash covers everything until settings and migrations are ready (AC-001-4).
        installSplashScreen().setKeepOnScreenCondition { app.state.value == AppState.Loading }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by app.state.collectAsStateWithLifecycle()
            when (val s = state) {
                is AppState.Ready -> applyLanguage(s.result.settings.language)
                is AppState.Failed -> applyLanguage(s.result.language)
                AppState.Loading -> Unit
            }
            TutoraApp(state, container.appInfo)
        }
    }

    /** The saved language rules, whatever the phone's (AC-001-6); no restart needed. */
    private fun applyLanguage(language: Language) {
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (current != language.tag) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        }
    }
}
