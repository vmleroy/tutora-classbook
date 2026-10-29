package app.tutora.data

import app.tutora.domain.CalendarState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/** This phone's calendar setup (spec 012), in its own settings keys, which backups leave out. */
class CalendarStore(
    private val db: TutoraDatabase,
) {
    fun observe(): Flow<CalendarState> = db.settings().observeAll().map(::toState)

    suspend fun load(): CalendarState = toState(db.settings().all())

    suspend fun save(
        s: CalendarState,
        now: String,
    ) = db.settings().upsert(rows(s, now))

    suspend fun ensureDefaults(now: String) = db.settings().insertMissing(rows(CalendarState(), now))

    private fun rows(
        s: CalendarState,
        now: String,
    ): List<SettingRow> =
        listOf(
            "calendar_on" to JsonPrimitive(s.on),
            "calendar_id" to JsonPrimitive(s.calendarId),
            "calendar_name" to JsonPrimitive(s.calendarName),
            "calendar_own" to JsonPrimitive(s.own),
            "calendar_value" to JsonPrimitive(s.includeValue),
            "calendar_busy" to JsonPrimitive(s.showBusy),
            "calendar_synced_at" to JsonPrimitive(s.syncedAt),
            "calendar_count" to JsonPrimitive(s.count),
        ).map { (k, v) -> SettingRow(k, v.toString(), now) }

    private fun toState(rows: List<SettingRow>): CalendarState {
        val v =
            rows.filter { it.key.startsWith("calendar_") }.associate {
                it.key to runCatching { Json.parseToJsonElement(it.value) as? JsonPrimitive }.getOrNull()
            }

        fun bool(k: String) = v[k]?.booleanOrNull ?: false
        return CalendarState(
            on = bool("calendar_on"),
            calendarId = v["calendar_id"]?.longOrNull ?: 0,
            calendarName = v["calendar_name"]?.contentOrNull.orEmpty(),
            own = bool("calendar_own"),
            includeValue = bool("calendar_value"),
            showBusy = bool("calendar_busy"),
            syncedAt = v["calendar_synced_at"]?.contentOrNull.orEmpty(),
            count = v["calendar_count"]?.intOrNull ?: 0,
        )
    }
}
