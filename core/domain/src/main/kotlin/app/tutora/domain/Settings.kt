package app.tutora.domain

import app.tutora.domain.theme.CustomPart

enum class ThemeMode(
    val key: String,
) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
}

/** The teacher's settings. Language comes from the phone on a first run; the rest is fixed. */
data class Settings(
    val language: Language,
    val themeId: String = "quadro",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val setupDone: Boolean = false,
    val textScale: String = "normal",
    val showDefaultPlaces: Boolean = true,
    val customColors: Map<CustomPart, String> = emptyMap(),
    /** Ajustes › Aulas › Aula ao vivo; off hides Iniciar aula (AC-007-3, 17). */
    val liveTimer: Boolean = true,
    /** Ajustes › Início automático, off by default (AC-007-19). */
    val autoStart: AutoStart = AutoStart.OFF,
    /** Classes discarded after starting by themselves, so they don't start again (AC-007-20). */
    val autoStartSkipped: Set<String> = emptySet(),
    /** Ajustes › Créditos e bônus, off by default; off hides every credit (AC-008-28). */
    val credits: Boolean = false,
)
