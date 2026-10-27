package com.vmleroy.tutora.domain

import com.vmleroy.tutora.domain.theme.CustomPart

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
)
