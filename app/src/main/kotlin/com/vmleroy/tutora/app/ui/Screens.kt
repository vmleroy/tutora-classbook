package com.vmleroy.tutora.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.vmleroy.tutora.app.AppInfo
import com.vmleroy.tutora.app.R
import com.vmleroy.tutora.designsystem.TutoraTheme
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Radius
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Spacing

/** A tab's title over its empty state; every tab starts here in spec 001. */
@Composable
fun EmptyTab(
    @StringRes title: Int,
    @StringRes emptyTitle: Int,
    @StringRes emptyBody: Int,
    tag: String,
) {
    TabScreen(title, tag) {
        Text(stringResource(emptyTitle), style = TutoraTheme.type.titleMd, color = TutoraTheme.colors.ink)
        Text(stringResource(emptyBody), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
    }
}

@Composable
private fun TabScreen(
    @StringRes title: Int,
    tag: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(TutoraTheme.colors.background)
                .safeDrawingPadding()
                .padding(horizontal = Spacing.s5, vertical = Spacing.s4)
                .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        Text(stringResource(title), style = TutoraTheme.type.titleXl, color = TutoraTheme.colors.ink)
        content()
    }
}

/** Ajustes: only Sobre for now (AC-001-3, AC-001-22). */
@Composable
fun SettingsScreen(
    appInfo: AppInfo,
    schemaVersion: Int,
) {
    TabScreen(R.string.tab_settings, "settings-screen") {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(TutoraTheme.colors.surface, RoundedCornerShape(Radius.xl))
                    .padding(Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s1),
        ) {
            Text(stringResource(R.string.settings_about), style = TutoraTheme.type.rowTitle, color = TutoraTheme.colors.ink)
            Text(
                stringResource(R.string.settings_about_value, appInfo.versionName, appInfo.build, schemaVersion),
                style = TutoraTheme.type.caption.copy(fontFeatureSettings = "tnum"),
                color = TutoraTheme.colors.inkMuted,
            )
        }
    }
}

/** A failed migration: say so in the app language, with where the copy is (AC-001-14). */
@Composable
fun StartupError(
    backupPath: String?,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(TutoraTheme.colors.background)
                .safeDrawingPadding()
                .padding(Spacing.s5)
                .testTag("startup-error"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        Text(stringResource(R.string.startup_error_title), style = TutoraTheme.type.titleLg, color = TutoraTheme.colors.dangerInk)
        Text(stringResource(R.string.startup_error_body), style = TutoraTheme.type.body, color = TutoraTheme.colors.ink)
        Text(
            if (backupPath != null) {
                stringResource(R.string.startup_error_backup, backupPath)
            } else {
                stringResource(R.string.startup_error_no_backup)
            },
            style = TutoraTheme.type.caption,
            color = TutoraTheme.colors.inkMuted,
        )
        Text(stringResource(R.string.startup_error_detail, message), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
    }
}
