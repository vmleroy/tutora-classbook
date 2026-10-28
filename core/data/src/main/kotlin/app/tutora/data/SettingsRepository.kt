package app.tutora.data

import app.tutora.domain.AutoStart
import app.tutora.domain.Language
import app.tutora.domain.Settings
import app.tutora.domain.ThemeMode
import app.tutora.domain.theme.CustomPart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/** Settings as stored rows: one key per field, the value as JSON text. */
class SettingsRepository(
    private val db: TutoraDatabase,
) {
    private val dao = db.settings()

    fun observe(): Flow<Settings> = dao.observeAll().map { toSettings(it) }

    suspend fun load(): Settings = toSettings(dao.all())

    suspend fun savedLanguage(): Language? = dao.all().firstOrNull { it.key == "language" }?.let { Language.fromTag(unquote(it.value)) }

    suspend fun ensureDefaults(
        phoneLanguage: Language,
        now: String,
    ) = dao.insertMissing(rows(Settings(language = phoneLanguage), now))

    suspend fun save(
        settings: Settings,
        now: String,
    ) = dao.upsert(rows(settings, now))

    companion object {
        private fun quote(s: String) = "\"$s\""

        private fun unquote(s: String) = s.trim('"')

        fun rows(
            s: Settings,
            now: String,
        ): List<SettingRow> =
            listOf(
                "language" to quote(s.language.tag),
                "theme_id" to quote(s.themeId),
                "theme_mode" to quote(s.themeMode.key),
                "setup_done" to s.setupDone.toString(),
                "custom_colors" to JSONObject(s.customColors.mapKeys { it.key.key }).toString(),
                "text_scale" to quote(s.textScale),
                "show_default_places" to s.showDefaultPlaces.toString(),
                "live_timer" to s.liveTimer.toString(),
                "auto_start" to quote(s.autoStart.key),
                "auto_start_skipped" to org.json.JSONArray(s.autoStartSkipped.toList()).toString(),
                "credits" to s.credits.toString(),
            ).map { (k, v) -> SettingRow(k, v, now) }

        private fun customColors(json: String): Map<CustomPart, String> {
            val o = JSONObject(json)
            return o
                .keys()
                .asSequence()
                .mapNotNull { k -> CustomPart.fromKey(k)?.let { it to o.getString(k) } }
                .toMap()
        }

        private fun skipped(json: String): Set<String> {
            val a = org.json.JSONArray(json)
            return (0 until a.length()).map { a.getString(it) }.toSet()
        }

        fun toSettings(rows: List<SettingRow>): Settings {
            val v = rows.associate { it.key to it.value }
            return Settings(
                language = v["language"]?.let { Language.fromTag(unquote(it)) } ?: Language.EN,
                themeId = v["theme_id"]?.let(::unquote) ?: "quadro",
                themeMode = ThemeMode.entries.firstOrNull { it.key == v["theme_mode"]?.let(::unquote) } ?: ThemeMode.SYSTEM,
                setupDone = v["setup_done"] == "true",
                textScale = v["text_scale"]?.let(::unquote) ?: "normal",
                showDefaultPlaces = v["show_default_places"] != "false",
                liveTimer = v["live_timer"] != "false",
                autoStart = AutoStart.fromKey(v["auto_start"]?.let(::unquote)),
                autoStartSkipped = v["auto_start_skipped"]?.let(::skipped) ?: emptySet(),
                credits = v["credits"] == "true",
                customColors = v["custom_colors"]?.let(::customColors) ?: emptyMap(),
            )
        }
    }
}
