package app.tutora.ui.live

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.AlertBanner
import app.tutora.designsystem.components.AlertTone
import app.tutora.designsystem.components.Avatar
import app.tutora.designsystem.components.Badge
import app.tutora.designsystem.components.BadgeTone
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.Money
import app.tutora.designsystem.components.MoneySize
import app.tutora.designsystem.components.MoneyTone
import app.tutora.designsystem.components.TextField
import app.tutora.designsystem.components.TimerRing
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Lesson
import app.tutora.domain.LessonStatus
import app.tutora.domain.LessonTimer
import app.tutora.domain.RateType
import app.tutora.domain.Worth
import app.tutora.domain.currentPrice
import app.tutora.domain.earnedSoFar
import app.tutora.domain.elapsedMs
import app.tutora.domain.initials
import app.tutora.domain.liveMinutes
import app.tutora.domain.localOf
import app.tutora.domain.multiplyRate
import app.tutora.domain.worth
import app.tutora.state.AppServices
import app.tutora.state.LocalServices
import app.tutora.ui.LiveLesson
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.TodayGraph
import app.tutora.ui.clockText
import app.tutora.ui.clockTime
import app.tutora.ui.durationText
import app.tutora.ui.payments.DoneMoney
import app.tutora.ui.schedule.DoneSheet
import app.tutora.ui.schedule.lessonSubtitle
import app.tutora.ui.schedule.whoName
import app.tutora.ui.schedule.whoTag
import app.tutora.ui.schedule.worthText
import app.tutora.usecases.LiveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime

/**
 * The clock's "now", read again every second. The loop runs off the main dispatcher, so only
 * the composables that read it recompose, and UI tests are not kept waiting on it.
 */
@Composable
fun rememberNow(services: AppServices): State<String> {
    val now = remember { mutableStateOf(services.now()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            while (true) {
                delay(TICK_MS)
                now.value = services.now()
            }
        }
    }
    return now
}

private const val TICK_MS = 1_000L

/**
 * Iniciar aula: asks once for notifications (the timer works without them), starts the class and
 * opens the live screen; if another class is live it says so and offers to open that one (AC-007-1, 2).
 */
@Composable
fun rememberStartLesson(nav: NavHostController): (String) -> Unit {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var alreadyLive by remember { mutableStateOf<String?>(null) }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) scope.launch { services.live.restoreNotification() }
        }
    alreadyLive?.let { other ->
        BottomSheet(
            stringResource(R.string.live_alreadyTitle),
            { alreadyLive = null },
            subtitle = stringResource(R.string.live_alreadyBody),
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.catalog_cancel), { alreadyLive = null }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.live_open), {
                            alreadyLive = null
                            nav.navigate(LiveLesson(other))
                        }, m)
                    },
                ),
        )
    }
    return { id ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        scope.launch {
            when (val r = services.live.start(id)) {
                LiveResult.Ok -> nav.navigate(LiveLesson(id))
                is LiveResult.AlreadyLive -> alreadyLive = r.lessonId
                is LiveResult.Err -> Unit
            }
        }
    }
}

/** Aula ao vivo: the clock, what it has earned, a quick note, Pausar, Encerrar and Descartar (AC-007-4 to 12). */
@Composable
fun LiveScreen(
    nav: NavHostController,
    id: String,
    openEnd: Boolean,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val lesson by remember(id) { services.scheduleRepo.lesson(id) }.collectAsState(initial = null)
    val timer by remember(id) { services.scheduleRepo.timer(id) }.collectAsState(initial = null)
    var ending by rememberSaveable { mutableStateOf(openEnd) }
    var discarding by remember { mutableStateOf(false) }
    val l = lesson ?: return
    val t = timer
    // Discarded, or undone elsewhere: nothing live to show.
    if (l.status != LessonStatus.LIVE && l.status != LessonStatus.DONE || t == null) {
        LaunchedEffect(l.status) { nav.popBackStack() }
        return
    }
    if (l.status == LessonStatus.DONE) {
        Done(nav, l)
        return
    }
    ScreenScaffold(
        stringResource(R.string.live_title),
        "live-screen",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
        bottomBar = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                Button(
                    stringResource(if (t.running) R.string.live_pause else R.string.live_resume),
                    { scope.launch { if (t.running) services.live.pause(id) else services.live.resume(id) } },
                    Modifier.weight(1f),
                    variant = ButtonVariant.OUTLINE,
                )
                Button(stringResource(R.string.live_end), { ending = true }, Modifier.weight(1f))
            }
        },
    ) {
        Badge(
            stringResource(if (t.running) R.string.status_live else R.string.live_paused),
            tone = if (t.running) BadgeTone.ACCENT else BadgeTone.NEUTRAL,
        )
        Who(l, services)
        Clock(l, t, services)
        var note by remember(id) { mutableStateOf(t.note.orEmpty()) }
        TextField(stringResource(R.string.live_note), note, {
            note = it
            scope.launch { services.live.setNote(id, it) }
        }, imeAction = ImeAction.Done)
        Button(stringResource(R.string.live_discard), { discarding = true }, variant = ButtonVariant.GHOST)
    }
    if (ending) {
        val counted = elapsedMs(t, services.now())
        DoneSheet(l, liveMinutes(counted), stringResource(R.string.live_end), {
            ending = false
        }, stringResource(R.string.live_continue)) { minutes ->
            ending = false
            scope.launch { services.live.end(id, minutes) }
        }
    }
    if (discarding) {
        BottomSheet(
            stringResource(R.string.live_discardTitle),
            { discarding = false },
            subtitle = stringResource(R.string.live_discardBody),
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.catalog_cancel), { discarding = false }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.live_discard), {
                            discarding = false
                            scope.launch { services.live.discard(id) }
                        }, m, variant = ButtonVariant.DANGER)
                    },
                ),
        )
    }
}

/** The student or group, subject, place and planned time ("14:00–15:00"). */
@Composable
private fun Who(
    l: Lesson,
    services: AppServices,
) {
    val language = currentLanguage()
    val local = localOf(l.startAt, services.zone)
    val endMin = local.hour * 60 + local.minute + l.plannedMin
    val end = LocalTime((endMin / 60) % 24, endMin % 60)
    Card {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalAlignment = Alignment.CenterVertically) {
            Avatar(initials(l.whoName), l.whoTag)
            Column(Modifier.weight(1f)) {
                Text(l.whoName, style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.ink)
                Text(
                    "${lessonSubtitle(l)} · ${clockTime(local.time, language)}–${clockTime(end, language)}",
                    style = TutoraTheme.type.meta,
                    color = TutoraTheme.colors.inkMuted,
                )
            }
        }
    }
}

/** The ticking part: only this recomposes every second. */
@Composable
private fun Clock(
    l: Lesson,
    t: LessonTimer,
    services: AppServices,
) {
    val language = currentLanguage()
    val now by rememberNow(services)
    val ms = elapsedMs(t, now)
    val c = TutoraTheme.colors
    // The ring is drawn for the header ground (its guideline): white digits and an accent ring on
    // the main color, as in the Live mockup, so the clock keeps its contrast in every theme.
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(c.header)
            .padding(vertical = Spacing.s5, horizontal = Spacing.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        TimerRing(
            clockText(ms),
            ms / (l.plannedMin * MINUTE_MS).toFloat(),
            caption = stringResource(R.string.live_planned, l.plannedMin),
            label = "${stringResource(R.string.live_time)} ${clockText(ms)}",
        )
        val price = currentPrice(l.place, l.who)
        if (price != null && price.rateType == RateType.HOUR) {
            Text(stringResource(R.string.live_earned), style = TutoraTheme.type.caption, color = c.onHeaderMuted)
            Money(earnedSoFar(price, ms) ?: 0, size = MoneySize.XL, tone = MoneyTone.ON_HEADER)
            Text(
                stringResource(
                    R.string.live_rateLine,
                    formatMoney(price.cents, language),
                    formatMoney(multiplyRate(price.cents, 1, 60), language),
                ),
                style = TutoraTheme.type.meta,
                color = c.onHeaderMuted,
            )
        } else {
            // A fixed price, a salary or no price: the value doesn't tick (AC-007-5).
            Text(stringResource(R.string.lesson_value), style = TutoraTheme.type.caption, color = c.onHeaderMuted)
            val worth = l.worth()
            if (worth is Worth.Set) {
                Money(worth.cents, size = MoneySize.XL, tone = MoneyTone.ON_HEADER)
            } else {
                Text(worthText(worth), style = TutoraTheme.type.titleLg, color = c.onHeader)
            }
        }
    }
}

private const val MINUTE_MS = 60_000L

/** Aula concluída: who, how long and the value, with Desfazer and Voltar para Hoje (AC-007-10, 11). */
@Composable
private fun Done(
    nav: NavHostController,
    l: Lesson,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    ScreenScaffold(stringResource(R.string.live_doneTitle), "live-done", variant = HeaderVariant.PUSH, onBack = {
        nav.popBackStack()
    }, bottomBar = {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            Button(
                stringResource(R.string.lesson_undo),
                { scope.launch { services.live.undoEnd(l.id) } },
                Modifier.weight(1f),
                variant = ButtonVariant.OUTLINE,
            )
            Button(stringResource(R.string.live_backToday), {
                nav.navigate(TodayGraph) { popUpTo(nav.graph.findStartDestination().id) { inclusive = false } }
            }, Modifier.weight(1f))
        }
    }) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            Text(stringResource(R.string.live_doneTitle), style = TutoraTheme.type.titleXl, color = TutoraTheme.colors.ink)
            Text(
                stringResource(R.string.live_doneWho, l.whoName, durationText(l.plannedMin)),
                style = TutoraTheme.type.body,
                color = TutoraTheme.colors.inkMuted,
            )
            val worth = l.worth()
            if (worth is Worth.Set) {
                Money(worth.cents, size = MoneySize.XL, tone = MoneyTone.EARNINGS)
            } else {
                Text(worthText(worth), style = TutoraTheme.type.titleLg, color = TutoraTheme.colors.ink)
            }
        }
        // The balance it was added to, Registrar pagamento, and a package's last class (AC-008-18, 21).
        DoneMoney(l)
    }
}

/** Hoje's card for the class running now, ticking; it opens the live screen (AC-007-13). */
@Composable
fun LiveBanner(nav: NavHostController) {
    val services = LocalServices.current
    val live by remember { services.scheduleRepo.live() }.collectAsState(initial = null)
    val l = live ?: return
    val timer by remember(l.id) { services.scheduleRepo.timer(l.id) }.collectAsState(initial = null)
    val t = timer ?: return
    val now by rememberNow(services)
    AlertBanner(
        stringResource(R.string.live_banner, l.whoName, clockText(elapsedMs(t, now))),
        tone = AlertTone.ACCENT,
        icon = IconName.PLAY,
        onClick = { nav.navigate(LiveLesson(l.id)) },
    )
}
