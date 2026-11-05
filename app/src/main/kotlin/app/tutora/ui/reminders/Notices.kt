package app.tutora.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.components.BadgeTone
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.EmptyState
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconButton
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.SectionHeader
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Notice
import app.tutora.domain.NoticeKind
import app.tutora.domain.dateOf
import app.tutora.domain.instantOf
import app.tutora.domain.localOf
import app.tutora.state.LocalServices
import app.tutora.ui.ConfirmClasses
import app.tutora.ui.LessonDetail
import app.tutora.ui.RegistryGraph
import app.tutora.ui.RegistryHome
import app.tutora.ui.RemindersHome
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.clockTime
import app.tutora.ui.openTab
import app.tutora.ui.shortDate
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus

/**
 * Notificações, behind the bell: "Agora" with what needs the teacher now, and "Recentes" with
 * what Tutora sent in the last 30 days; the cogwheel opens Lembretes (AC-014-39, 40).
 */
@Composable
fun NoticesScreen(nav: NavHostController) {
    val services = LocalServices.current
    val recent by remember { services.notices.recent() }.collectAsState(initial = null)
    val toConfirm by remember { services.toConfirm() }.collectAsState(initial = emptyList())
    val balances by remember { services.paymentRepo.balances() }.collectAsState(initial = emptyMap())
    val backup by remember { services.backups.observe() }.collectAsState(initial = null)
    val backupDays =
        backup?.let {
            app.tutora.ui.backup
                .warningDays(it)
        }
    // Seen once the page is open; what was new keeps its mark for this visit: read first, then marked.
    var unseenNow by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(Unit) {
        unseenNow =
            services.notices
                .recent()
                .first()
                .filter { !it.seen }
                .map { it.id }
                .toSet()
        services.notices.markSeen()
    }
    val nothingNow = toConfirm.isEmpty() && balances.values.none { it > 0 } && backupDays == null
    ScreenScaffold(
        stringResource(R.string.ntf_title),
        "notices-screen",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
        trailing = {
            IconButton(
                IconName.GEAR,
                stringResource(R.string.set_reminders),
                { nav.navigate(RemindersHome) },
                Modifier.testTag("notices-settings"),
                variant = IconButtonVariant.SUNKEN,
                size = ControlSize.SM,
            )
        },
    ) {
        val list = recent ?: return@ScreenScaffold
        if (nothingNow && list.isEmpty()) {
            EmptyState(stringResource(R.string.ntf_emptyTitle), icon = IconName.BELL, body = stringResource(R.string.ntf_emptyBody))
            return@ScreenScaffold
        }
        if (!nothingNow) {
            Column(Modifier.testTag("notices-now"), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                SectionHeader(stringResource(R.string.ntf_now))
                app.tutora.ui.confirm
                    .ConfirmAlert(nav)
                app.tutora.ui.payments
                    .PendingAlert(nav)
                app.tutora.ui.backup
                    .BackupAlert(nav)
            }
        }
        if (list.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                SectionHeader(stringResource(R.string.ntf_recent), aside = stringResource(R.string.ntf_recentHint))
                ListGroup(
                    list.map<Notice, @Composable () -> Unit> { n -> { NoticeRow(n, n.id in unseenNow, nav) } },
                    Modifier.testTag("notices-recent"),
                )
            }
        }
    }
}

/** One notification: what it said, when, a dot while new; it opens its page. */
@Composable
private fun NoticeRow(
    n: Notice,
    new: Boolean,
    nav: NavHostController,
) {
    val language = currentLanguage()
    val title =
        when (n.kind) {
            NoticeKind.CLASS -> pluralStringResource(R.plurals.ntf_class, n.minutes.coerceAtLeast(1), n.minutes.coerceAtLeast(1))
            NoticeKind.OWED -> {
                val money = formatMoney(n.cents, language)
                if (n.onlyStudents) {
                    pluralStringResource(R.plurals.remind_owed, n.count, n.count, money)
                } else {
                    pluralStringResource(R.plurals.remind_owedMixed, n.count, n.count, money)
                }
            }
            NoticeKind.CONFIRM -> pluralStringResource(R.plurals.confirm_count, n.count, n.count)
        }
    ListRow(
        title,
        subtitle = whenText(n.at),
        icon =
            when (n.kind) {
                NoticeKind.CLASS -> IconName.CLOCK
                NoticeKind.OWED -> IconName.WALLET
                NoticeKind.CONFIRM -> IconName.CALENDAR_CHECK
            },
        badge = if (new) BadgeTone.DANGER to stringResource(R.string.ntf_new) else null,
        onClick = {
            when (n.kind) {
                NoticeKind.CLASS -> n.lessonId?.let { nav.navigate(LessonDetail(it)) }
                NoticeKind.CONFIRM -> nav.navigate(ConfirmClasses)
                NoticeKind.OWED -> {
                    nav.openTab(RegistryGraph)
                    nav.navigate(RegistryHome(owing = true)) { launchSingleTop = true }
                }
            }
        },
    )
}

/** "Hoje, 20:00", "Ontem, 08:00" or "Seg, 2 nov, 08:00". */
@Composable
private fun whenText(at: String): String {
    val services = LocalServices.current
    val language = currentLanguage()
    val local = localOf(at, services.zone)
    val today = dateOf(instantOf(services.now()), services.zone)
    val time = clockTime(local.time, language)
    return when (local.date) {
        today -> stringResource(R.string.ntf_today, time)
        today.minus(DatePeriod(days = 1)) -> stringResource(R.string.ntf_yesterday, time)
        else -> "${shortDate(local.date, language)}, $time"
    }
}
