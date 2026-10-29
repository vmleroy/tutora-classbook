package app.tutora.usecases

import app.tutora.data.SettingsRepository
import app.tutora.data.inTransaction
import app.tutora.domain.SavedTheme
import app.tutora.domain.Settings
import app.tutora.domain.ThemeMode
import app.tutora.domain.theme.CustomPart
import app.tutora.domain.theme.applyQuick

/**
 * Aparência and Suas cores (spec 013). Each change saves the settings in one transaction, and
 * the app repaints at once from them (AC-002-5).
 */
class Appearance(
    private val ctx: Ctx,
) {
    private val repo = SettingsRepository(ctx.db)

    private suspend fun change(block: (Settings) -> Settings): Settings =
        ctx.db.inTransaction {
            val next = block(repo.load())
            // The colors still come from a saved theme only while they match it.
            val still =
                next.savedThemes.firstOrNull { it.id == next.savedThemeId }?.takeIf {
                    it.themeId == next.themeId && it.custom == next.customColors
                }
            val fixed = next.copy(savedThemeId = still?.id)
            repo.save(fixed, ctx.now())
            fixed
        }

    /** Claro, Escuro or Sistema (AC-013-3). */
    suspend fun setMode(mode: ThemeMode) = change { it.copy(themeMode = mode) }

    /** A theme of the four: it starts with no custom colors (AC-013-4, 6). */
    suspend fun chooseTheme(id: String) = change { it.copy(themeId = id, customColors = emptyMap(), savedThemeId = null) }

    /** A theme of the teacher's own: its theme and its colors come back (AC-013-5). */
    suspend fun chooseSaved(id: String) =
        change { s ->
            val t = s.savedThemes.firstOrNull { it.id == id } ?: return@change s
            s.copy(themeId = t.themeId, customColors = t.custom, savedThemeId = t.id)
        }

    /** Salvar como tema: the current theme and colors under [name]; returns its id (AC-013-5). */
    suspend fun saveAsTheme(name: String): String {
        val id = ctx.newId()
        change { s ->
            val theme = SavedTheme(id, name.trim().ifEmpty { "Meu tema" }, s.themeId, s.customColors)
            s.copy(savedThemes = s.savedThemes + theme, savedThemeId = id)
        }
        return id
    }

    suspend fun renameSaved(
        id: String,
        name: String,
    ) = change { s ->
        s.copy(savedThemes = s.savedThemes.map { if (it.id == id && name.isNotBlank()) it.copy(name = name.trim()) else it })
    }

    suspend fun deleteSaved(id: String) = change { s -> s.copy(savedThemes = s.savedThemes.filter { it.id != id }) }

    /** Cor principal or Cor de destaque: a preset, or null for "Do tema" (AC-013-7, 8). */
    suspend fun setQuick(
        parts: List<CustomPart>,
        hex: String?,
    ) = change { it.copy(customColors = applyQuick(it.customColors, parts, hex)) }

    /** One part in Suas cores, at once (AC-013-15). */
    suspend fun setPart(
        part: CustomPart,
        hex: String,
    ) = change { it.copy(customColors = it.customColors + (part to hex.lowercase())) }

    /** Restaurar: every part back to the theme's colors (AC-013-9, 16). */
    suspend fun restore() = change { it.copy(customColors = emptyMap()) }

    /** Normal or Grande (AC-013-10). */
    suspend fun setTextSize(large: Boolean) = change { it.copy(textScale = if (large) "large" else "normal") }
}
