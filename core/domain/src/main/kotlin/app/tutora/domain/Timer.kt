package app.tutora.domain

import kotlin.time.Duration.Companion.minutes

// The live timer stores timestamps, never a running counter, so closing the app or restarting
// the phone neither loses nor adds time (architecture, "Live timer = timestamps").

/** A live class's clock. */
data class LessonTimer(
    val lessonId: String,
    val startedAt: String,
    /** Time spent in pauses that are over. */
    val pausedMs: Long,
    /** When the current pause began; null while the clock runs. */
    val pausedAt: String?,
    /** Anotação rápida, added to the lesson's notes when it ends. */
    val note: String? = null,
) {
    val running: Boolean get() = pausedAt == null
}

/** A new timer, running from [now]. */
fun startTimer(
    lessonId: String,
    now: String,
) = LessonTimer(lessonId, now, 0, null)

/** Time counted: from the start to now (or to the pause), minus the pauses, never below 0 (AC-007-6). */
fun elapsedMs(
    t: LessonTimer,
    now: String,
): Long {
    val until = instantOf(t.pausedAt ?: now)
    return ((until - instantOf(t.startedAt)).inWholeMilliseconds - t.pausedMs).coerceAtLeast(0)
}

/** Pausar: the clock stops where it is (AC-007-7). */
fun pause(
    t: LessonTimer,
    now: String,
): LessonTimer = if (t.running) t.copy(pausedAt = now) else t

/** Retomar: the pause joins the paused time and the clock goes on from what was counted. */
fun resume(
    t: LessonTimer,
    now: String,
): LessonTimer {
    val since = t.pausedAt ?: return t
    return t.copy(pausedMs = t.pausedMs + (instantOf(now) - instantOf(since)).inWholeMilliseconds, pausedAt = null)
}

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 3_600_000L

/** The real duration offered when ending: the nearest minute, at least 1 (AC-007-9). */
fun liveMinutes(ms: Long): Int = roundHalfAwayFromZero(ms.coerceAtLeast(0), MINUTE_MS).toInt().coerceAtLeast(1)

/** What a per-hour price has earned in [ms], rounded once; a per-lesson price doesn't tick (AC-007-5). */
fun earnedSoFar(
    price: Price.Set,
    ms: Long,
): Cents? = if (price.rateType == RateType.HOUR) multiplyRate(price.cents, ms.coerceAtLeast(0), HOUR_MS) else null

/** Ajustes › Início automático (AC-007-19): off, while the app is open, or also with it closed. */
enum class AutoStart(
    val key: String,
) {
    OFF("off"),
    APP_OPEN("app_open"),
    ALWAYS("always"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: OFF
    }
}

/**
 * The class that should start by itself now: scheduled, begun, its planned end not passed, and
 * not discarded after starting by itself; the earliest if several overlap (AC-007-20).
 */
fun dueToStart(
    lessons: List<Lesson>,
    now: String,
    skipped: Set<String>,
): Lesson? {
    val at = instantOf(now)
    return lessons
        .filter { it.status == LessonStatus.SCHEDULED && it.id !in skipped }
        .filter { l ->
            val start = instantOf(l.startAt)
            start <= at && at < start + l.plannedMin.minutes
        }.minByOrNull { it.startAt }
}
