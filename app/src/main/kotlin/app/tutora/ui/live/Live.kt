package app.tutora.ui.live

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
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
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconButton
import app.tutora.designsystem.components.IconButtonVariant
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
import app.tutora.ui.minutesText
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
fun rememberNow(
    services: AppServices,
    tickMs: Long = TICK_MS,
): State<String> {
    val now = remember { mutableStateOf(services.now()) }
    LaunchedEffect(tickMs) {
        withContext(Dispatchers.Default) {
            while (true) {
                delay(tickMs)
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
    // The phone doesn't dim or lock while the class is on screen; leaving gives its timeout back
    // (AC-017-13; Victor, 4 Nov 2026).
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
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
    var note by remember(id) { mutableStateOf(t.note.orEmpty()) }
    // The same parts in two whole layouts: upright one column, sideways the clock on the left (L1,
    // Victor 5 Nov 2026). Only this line picks between them.
    val parts =
        LiveParts(
            status = {
                Badge(
                    stringResource(if (t.running) R.string.status_live else R.string.live_paused),
                    tone = if (t.running) BadgeTone.ACCENT else BadgeTone.NEUTRAL,
                )
            },
            who = { Who(l, services) },
            ring = { modifier, fill -> LiveRing(l, t, services, modifier, fill) },
            value = { onHeader -> LiveValue(l, t, services, onHeader) },
            note = {
                TextField(stringResource(R.string.live_note), note, {
                    note = it
                    scope.launch { services.live.setNote(id, it) }
                }, imeAction = ImeAction.Done)
            },
            discard = { compact ->
                if (compact) {
                    IconButton(
                        IconName.TRASH,
                        stringResource(R.string.live_discard),
                        { discarding = true },
                        variant = IconButtonVariant.ON_PRIMARY,
                        size = ControlSize.SM,
                    )
                } else {
                    Button(stringResource(R.string.live_discard), { discarding = true }, variant = ButtonVariant.GHOST)
                }
            },
            controls = {
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
        )
    if (TutoraTheme.landscape) LiveSideways(parts) { nav.popBackStack() } else LiveUpright(parts) { nav.popBackStack() }
    if (ending) {
        val counted = elapsedMs(t, services.now())
        DoneSheet(l, liveMinutes(counted), stringResource(R.string.live_end), {
            ending = false
        }, stringResource(R.string.live_continue)) { minutes, bonus ->
            ending = false
            scope.launch {
                services.live.end(id, minutes)
                bonus?.let { services.payments.useCredit(it.payer, listOf(id), it.minutes, it.amount) }
            }
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

/** The pieces of the live class, laid out by [LiveUpright] or [LiveSideways]. */
private class LiveParts(
    val status: @Composable () -> Unit,
    val who: @Composable () -> Unit,
    /** The ring; `fill` lets it grow as tall as its place allows (sideways). */
    val ring: @Composable (Modifier, Boolean) -> Unit,
    /** What was earned so far, on the main color or on the page. */
    val value: @Composable (Boolean) -> Unit,
    val note: @Composable () -> Unit,
    /** Descartar: a ghost button, or a quiet icon on the main color (`compact`). */
    val discard: @Composable (Boolean) -> Unit,
    val controls: @Composable () -> Unit,
)

/** Upright: status, who, the clock card with the value, the note; Pausar and Encerrar fixed below. */
@Composable
private fun LiveUpright(
    parts: LiveParts,
    onBack: () -> Unit,
) {
    ScreenScaffold(
        stringResource(R.string.live_title),
        "live-screen",
        variant = HeaderVariant.PUSH,
        onBack = onBack,
        bottomBar = parts.controls,
    ) {
        parts.status()
        parts.who()
        // The ring is drawn for the header ground (its guideline): white digits and an accent ring
        // on the main color, as in the Live mockup, so the clock keeps its contrast in every theme.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.xl))
                .background(TutoraTheme.colors.header)
                .padding(vertical = Spacing.s5, horizontal = Spacing.s4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            parts.ring(Modifier, false)
            parts.value(true)
        }
        parts.note()
        parts.discard(false)
    }
}

/**
 * Sideways (AC-017-12): the whole left side on the main color holds the ring as tall as the phone
 * allows, with the way back and the status above it; the right side holds who, the value, the
 * note and Descartar, with Pausar and Encerrar at the bottom, all in view.
 */
@Composable
private fun LiveSideways(
    parts: LiveParts,
    onBack: () -> Unit,
) {
    val c = TutoraTheme.colors
    Row(Modifier.fillMaxSize().background(c.background).testTag("live-screen")) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("live-clock-side")
                .background(c.header)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
                .padding(Spacing.s3),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    IconName.CHEVRON_LEFT,
                    stringResource(app.tutora.designsystem.R.string.ds_back),
                    onBack,
                    variant = IconButtonVariant.ON_PRIMARY,
                    size = ControlSize.SM,
                )
                parts.status()
                Box(Modifier.weight(1f))
                // Descartar as a quiet icon up here, away from Encerrar.
                parts.discard(true)
            }
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(top = Spacing.s2),
                contentAlignment = Alignment.Center,
            ) { parts.ring(Modifier, true) }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical))
                .imePadding()
                .padding(horizontal = Spacing.s5, vertical = Spacing.s3)
                .testTag("live-info-side"),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            // Taller text scrolls here; Pausar and Encerrar stay at the bottom.
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                parts.who()
                parts.value(false)
                parts.note()
            }
            parts.controls()
        }
    }
}

/** The ticking ring: only this and [LiveValue] recompose every second. */
@Composable
private fun LiveRing(
    l: Lesson,
    t: LessonTimer,
    services: AppServices,
    modifier: Modifier,
    fill: Boolean,
) {
    val now by rememberNow(services)
    val ms = elapsedMs(t, now)
    TimerRing(
        clockText(ms),
        ms / (l.plannedMin * MINUTE_MS).toFloat(),
        modifier,
        caption = stringResource(R.string.live_planned, minutesText(l.plannedMin)),
        label = "${stringResource(R.string.live_time)} ${clockText(ms)}",
        fill = fill,
    )
}

/** What the class has earned so far and its rate, or its fixed value (AC-007-5). */
@Composable
private fun LiveValue(
    l: Lesson,
    t: LessonTimer,
    services: AppServices,
    onHeader: Boolean,
) {
    val language = currentLanguage()
    val now by rememberNow(services)
    val ms = elapsedMs(t, now)
    val c = TutoraTheme.colors
    val muted = if (onHeader) c.onHeaderMuted else c.inkMuted
    val tone = if (onHeader) MoneyTone.ON_HEADER else MoneyTone.EARNINGS
    Column(
        horizontalAlignment = if (onHeader) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
    ) {
        val price = currentPrice(l.place, l.who)
        if (price != null && price.rateType == RateType.HOUR) {
            Text(stringResource(R.string.live_earned), style = TutoraTheme.type.caption, color = muted)
            Money(earnedSoFar(price, ms) ?: 0, size = MoneySize.XL, tone = tone)
            Text(
                stringResource(
                    R.string.live_rateLine,
                    formatMoney(price.cents, language),
                    formatMoney(multiplyRate(price.cents, 1, 60), language),
                ),
                style = TutoraTheme.type.meta,
                color = muted,
            )
        } else {
            // A fixed price, a salary or no price: the value doesn't tick (AC-007-5).
            Text(stringResource(R.string.lesson_value), style = TutoraTheme.type.caption, color = muted)
            val worth = l.worth()
            if (worth is Worth.Set) {
                Money(worth.cents, size = MoneySize.XL, tone = tone)
            } else {
                Text(worthText(worth), style = TutoraTheme.type.titleLg, color = if (onHeader) c.onHeader else c.ink)
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
