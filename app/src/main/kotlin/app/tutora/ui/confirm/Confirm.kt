package app.tutora.ui.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.AlertBanner
import app.tutora.designsystem.components.AlertTone
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.EmptyState
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.LessonRow
import app.tutora.designsystem.components.UndoBar
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.CancelReason
import app.tutora.domain.Lesson
import app.tutora.domain.Worth
import app.tutora.domain.localOf
import app.tutora.domain.worth
import app.tutora.state.LocalServices
import app.tutora.ui.ConfirmClasses
import app.tutora.ui.LessonDetail
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.clockTime
import app.tutora.ui.schedule.CancelSheet
import app.tutora.ui.schedule.lessonSubtitle
import app.tutora.ui.schedule.whoName
import app.tutora.ui.schedule.whoTag
import app.tutora.ui.schedule.worthText
import app.tutora.ui.shortDate
import kotlinx.coroutines.launch

/** Hoje: "3 aulas para confirmar" while any class is to confirm; it opens the list (AC-014-4). */
@Composable
fun ConfirmAlert(nav: NavHostController) {
    val services = LocalServices.current
    val list by remember { services.toConfirm() }.collectAsState(initial = emptyList())
    if (list.isEmpty()) return
    AlertBanner(
        pluralStringResource(R.plurals.confirm_count, list.size, list.size),
        Modifier.testTag("confirm-alert"),
        tone = AlertTone.SECONDARY,
        icon = IconName.CALENDAR_CHECK,
        onClick = { nav.navigate(ConfirmClasses) },
    )
}

/**
 * Aulas para confirmar: every class to confirm, oldest first, with Dada, Cancelada and Faltou,
 * and "Confirmar todas como dadas" with Desfazer for a few seconds (AC-014-6 to 9).
 */
@Composable
fun ConfirmClassesScreen(nav: NavHostController) {
    val services = LocalServices.current
    val language = currentLanguage()
    val zone = services.zone
    val scope = rememberCoroutineScope()
    val list by remember { services.toConfirm() }.collectAsState(initial = emptyList())
    var cancelling by remember { mutableStateOf<Pair<Lesson, CancelReason>?>(null) }
    var asking by remember { mutableStateOf(false) }
    var undo by remember { mutableStateOf<List<String>>(emptyList()) }
    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(
            stringResource(R.string.confirm_title),
            "confirm-screen",
            variant = HeaderVariant.PUSH,
            onBack = { nav.popBackStack() },
            bottomBar =
                if (list.isNotEmpty()) {
                    {
                        Button(
                            stringResource(R.string.confirm_all),
                            { asking = true },
                            fullWidth = true,
                            icon = IconName.CHECK,
                        )
                    }
                } else {
                    null
                },
        ) {
            if (list.isEmpty()) {
                EmptyState(
                    stringResource(R.string.confirm_emptyTitle),
                    icon = IconName.CALENDAR_CHECK,
                    body = stringResource(R.string.confirm_emptyBody),
                )
            } else {
                androidx.compose.material3.Text(
                    stringResource(R.string.confirm_hint),
                    style = TutoraTheme.type.meta,
                    color = TutoraTheme.colors.inkMuted,
                )
                list.forEach { l ->
                    val at = localOf(l.startAt, zone)
                    Column(Modifier.testTag("confirm-row"), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                        LessonRow(
                            clockTime(at.time, language),
                            l.whoTag,
                            l.whoName,
                            subtitle = "${shortDate(at.date, language)} · ${lessonSubtitle(l)}",
                            trailing = worthText(l.worth()),
                            onClick = { nav.navigate(LessonDetail(l.id)) },
                            card = true,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                            Button(
                                stringResource(R.string.confirm_done),
                                { scope.launch { services.confirming.confirm(l.id) } },
                                Modifier.weight(1f),
                                variant = ButtonVariant.OUTLINE,
                                size = ControlSize.SM,
                                icon = IconName.CHECK,
                            )
                            Button(
                                stringResource(R.string.confirm_cancelled),
                                { cancelling = l to CancelReason.STUDENT },
                                Modifier.weight(1f),
                                variant = ButtonVariant.OUTLINE,
                                size = ControlSize.SM,
                            )
                            Button(
                                stringResource(R.string.confirm_missed),
                                { cancelling = l to CancelReason.NO_SHOW },
                                Modifier.weight(1f),
                                variant = ButtonVariant.OUTLINE,
                                size = ControlSize.SM,
                            )
                        }
                    }
                }
            }
        }
        if (undo.isNotEmpty()) {
            val ids = undo
            UndoBar(
                pluralStringResource(R.plurals.confirm_allDone, ids.size, ids.size),
                stringResource(R.string.confirm_undo),
                {
                    undo = emptyList()
                    scope.launch { services.confirming.undoAll(ids) }
                },
                { undo = emptyList() },
                Modifier.align(Alignment.BottomCenter),
            )
        }
    }
    cancelling?.let { (l, reason) -> CancelSheet(l, started = true, onDismiss = { cancelling = null }, initial = reason) }
    if (asking) {
        val total = list.sumOf { (it.worth() as? Worth.Set)?.cents ?: 0L }
        BottomSheet(
            pluralStringResource(R.plurals.confirm_allAsk, list.size, list.size),
            { asking = false },
            subtitle = stringResource(R.string.confirm_allEarns, formatMoney(total, language)),
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.cancel_back), { asking = false }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.confirm_allYes), {
                            asking = false
                            scope.launch { undo = services.confirming.confirmAll() }
                        }, m)
                    },
                ),
        )
    }
}
