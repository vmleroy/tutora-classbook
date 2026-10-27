package com.vmleroy.tutora.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.vmleroy.tutora.app.R
import com.vmleroy.tutora.designsystem.TutoraTheme
import com.vmleroy.tutora.designsystem.components.EmptyState
import com.vmleroy.tutora.designsystem.components.HeaderAction
import com.vmleroy.tutora.designsystem.components.HeaderVariant
import com.vmleroy.tutora.designsystem.components.IconName
import com.vmleroy.tutora.designsystem.components.ScreenHeader
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Spacing

/** A screen: its header, then a scrolling body on the background with the 20 dp gutter. */
@Composable
fun ScreenScaffold(
    title: String,
    tag: String,
    modifier: Modifier = Modifier,
    variant: HeaderVariant = HeaderVariant.TAB,
    eyebrow: String? = null,
    actions: List<HeaderAction> = emptyList(),
    onBack: (() -> Unit)? = null,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(TutoraTheme.colors.background)
            .safeDrawingPadding()
            .testTag(tag),
    ) {
        ScreenHeader(title, variant = variant, eyebrow = eyebrow, actions = actions, onBack = onBack)
        Column(
            Modifier
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = Spacing.s5, vertical = Spacing.s2),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            content = content,
        )
    }
}

@Composable
fun EarningsScreen() {
    ScreenScaffold(stringResource(R.string.tab_earnings), "earnings-screen") {
        EmptyState(
            stringResource(R.string.empty_earnings_title),
            icon = IconName.WALLET,
            body = stringResource(R.string.empty_earnings_body),
        )
    }
}

/** A failed migration: say so in the app language, with where the copy is (AC-001-14). */
@Composable
fun StartupError(
    backupPath: String?,
    message: String,
    modifier: Modifier = Modifier,
) {
    val c = TutoraTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .safeDrawingPadding()
            .padding(Spacing.s5)
            .testTag("startup-error"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        Text(stringResource(R.string.startup_error_title), style = TutoraTheme.type.titleLg, color = c.dangerInk)
        Text(stringResource(R.string.startup_error_body), style = TutoraTheme.type.body, color = c.ink)
        Text(
            if (backupPath !=
                null
            ) {
                stringResource(R.string.startup_error_backup, backupPath)
            } else {
                stringResource(R.string.startup_error_no_backup)
            },
            style = TutoraTheme.type.caption,
            color = c.inkMuted,
        )
        Text(stringResource(R.string.startup_error_detail, message), style = TutoraTheme.type.meta, color = c.inkMuted)
    }
}
