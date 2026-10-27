package com.vmleroy.tutora.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.vmleroy.tutora.app.AppInfo
import com.vmleroy.tutora.app.R
import com.vmleroy.tutora.app.state.LocalServices
import com.vmleroy.tutora.designsystem.TutoraTheme
import com.vmleroy.tutora.designsystem.components.Card
import com.vmleroy.tutora.designsystem.components.ControlSize
import com.vmleroy.tutora.designsystem.components.IconName
import com.vmleroy.tutora.designsystem.components.ListGroup
import com.vmleroy.tutora.designsystem.components.ListRow
import com.vmleroy.tutora.designsystem.components.RowTrailing
import com.vmleroy.tutora.designsystem.components.SegmentedControl
import com.vmleroy.tutora.designsystem.components.formatMoney
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Spacing
import com.vmleroy.tutora.domain.Language
import com.vmleroy.tutora.domain.Settings
import kotlinx.coroutines.launch

/** A string in a given language, whatever the app's (for the language samples). */
@Composable
private fun stringIn(
    language: Language,
    id: Int,
    vararg args: Any,
): String {
    val context = LocalContext.current
    val config = Configuration(LocalConfiguration.current).apply { setLocale(localeOf(language)) }
    return context.createConfigurationContext(config).resources.getString(id, *args)
}

/** Ajustes: Geral (language, default places) and Sobre (AC-001-22, AC-003-11, AC-004-29). */
@Composable
fun SettingsScreen(
    appInfo: AppInfo,
    schemaVersion: Int,
) {
    val services = LocalServices.current
    val settings by services.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val s = settings ?: return

    fun save(next: Settings) = scope.launch { services.saveSettings(next) }

    ScreenScaffold(stringResource(R.string.tab_settings), "settings-screen") {
        ListGroup(
            title = stringResource(R.string.settings_general),
            rows =
                listOf(
                    { LanguageRow(s.language) { save(s.copy(language = it)) } },
                    {
                        ListRow(
                            stringResource(R.string.settings_showDefaultPlaces),
                            subtitle = stringResource(R.string.settings_showDefaultPlacesHint),
                            icon = IconName.PIN,
                            trailing = RowTrailing.SWITCH,
                            switchValue = s.showDefaultPlaces,
                            onSwitchChange = { save(s.copy(showDefaultPlaces = it)) },
                        )
                    },
                ),
        )
        Card(label = stringResource(R.string.settings_about)) {
            Text(stringResource(R.string.settings_about), style = TutoraTheme.type.rowTitle, color = TutoraTheme.colors.ink)
            Text(
                stringResource(R.string.settings_about_value, appInfo.versionName, appInfo.build, schemaVersion),
                style = TutoraTheme.type.caption.copy(fontFeatureSettings = "tnum"),
                color = TutoraTheme.colors.inkMuted,
            )
        }
    }
}

// A fixed sample, so each option shows how its dates and amounts are written.
private const val SAMPLE_CENTS = 123_456L

/** "Idioma do app": Português / English, each with a sample in its own language (AC-003-11). */
@Composable
private fun LanguageRow(
    language: Language,
    onChange: (Language) -> Unit,
) {
    val c = TutoraTheme.colors
    val languages = listOf(Language.PT_BR, Language.EN)
    Column(Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2_5), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Column {
            Text(stringResource(R.string.settings_language), style = TutoraTheme.type.bodyStrong, color = c.ink)
            Text(stringResource(R.string.settings_languageHint), style = TutoraTheme.type.meta, color = c.inkMuted)
        }
        SegmentedControl(
            languages.map { it to stringResource(if (it == Language.PT_BR) R.string.languages_pt_BR else R.string.languages_en) },
            language,
            onChange,
            stringResource(R.string.settings_language),
            size = ControlSize.SM,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            languages.forEach { l ->
                val day = if (l == Language.PT_BR) "26/10" else "10/26"
                Text(
                    stringIn(l, R.string.settings_languageSample, day, formatMoney(SAMPLE_CENTS, l)),
                    style = TutoraTheme.type.meta.copy(fontFeatureSettings = "tnum"),
                    color = c.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
