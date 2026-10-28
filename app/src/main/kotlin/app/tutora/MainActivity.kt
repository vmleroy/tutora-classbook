package app.tutora

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.tutora.domain.Language
import app.tutora.live.AndroidLiveNotifier
import app.tutora.ui.LiveRequest
import app.tutora.ui.TutoraApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// How often the open app looks for a class whose time has come.
private const val AUTO_START_CHECK_MS = 30_000L

class MainActivity : AppCompatActivity() {
    private val container get() = (application as TutoraApplication).container

    private val app: AppViewModel by viewModels {
        viewModelFactory { initializer { AppViewModel(container.startApp) } }
    }

    /** A tap on the live notification, waiting for the tabs to open it (AC-007-15). */
    private val liveRequest = MutableStateFlow<LiveRequest?>(null)

    private fun takeLiveRequest(intent: Intent?) {
        val id = intent?.getStringExtra(AndroidLiveNotifier.EXTRA_LESSON) ?: return
        liveRequest.value = LiveRequest(id, intent.getBooleanExtra(AndroidLiveNotifier.EXTRA_END, false))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeLiveRequest(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) takeLiveRequest(intent)
        watchAutoStart()
        // The splash covers everything until settings and migrations are ready (AC-001-4).
        installSplashScreen().setKeepOnScreenCondition { app.state.value == AppState.Loading }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by app.state.collectAsStateWithLifecycle()
            when (val s = state) {
                is AppState.Ready -> {
                    // The saved language rules, whatever the phone's; switching recreates the
                    // screen in the new language with no app restart (AC-001-6, AC-003-12, 13).
                    val services = app.services
                    if (services != null) {
                        LaunchedEffect(services) { services.settings.collect { applyLanguage(it.language) } }
                    }
                }
                is AppState.Failed -> applyLanguage(s.result.language)
                AppState.Loading -> Unit
            }
            val request by liveRequest.collectAsStateWithLifecycle()
            TutoraApp(state, container.appInfo, app.services, request) { liveRequest.value = null }
        }
    }

    /** While Tutora is on screen, a class whose time comes starts by itself (AC-007-21). */
    private fun watchAutoStart() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    app.services?.live?.autoStart()
                    delay(AUTO_START_CHECK_MS)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        app.onResume()
    }

    private fun applyLanguage(language: Language) {
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (current != language.tag) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        }
    }
}
