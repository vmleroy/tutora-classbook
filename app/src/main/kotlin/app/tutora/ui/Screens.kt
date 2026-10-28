package app.tutora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.HeaderAction
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.ScreenHeader
import app.tutora.designsystem.tokens.TutoraTokens.Spacing

/**
 * A screen: its header, then a scrolling body on the background with the 20 dp gutter, and
 * an optional [bottomBar] fixed under it.
 */
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
    bottomBar: (@Composable () -> Unit)? = null,
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
                .then(if (bottomBar != null) Modifier.weight(1f) else Modifier)
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = Spacing.s5, vertical = Spacing.s2),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            content = content,
        )
        // A form's main action stays in reach, above the keyboard, however long the form is.
        if (bottomBar != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(TutoraTheme.colors.surface)
                    .padding(horizontal = Spacing.s5, vertical = Spacing.s3),
            ) { bottomBar() }
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
