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
    /** Tamanho da interface: everything, not only text (Victor, 4 Nov 2026; AC-013-21). */
    val uiScale: String = "normal",
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
    /** Ajustes › Aulas › Marcar como dada antes do horário, off by default (AC-006-25). */
    val earlyDone: Boolean = false,
    /** Ajustes › Geral › Durações em HH:MM, off by default: "90 min" or "01:30" everywhere. */
    val clockDurations: Boolean = false,
    /** Ajustes › Relatórios: the teacher's name and Pix key, printed on reports when set (AC-010-16). */
    val teacherName: String = "",
    val pixKey: String = "",
    /** The line at the top of the PDF reports; empty keeps "TUTORA · CONTROLE DE AULAS" (AC-019-15). */
    val reportTitle: String = "",
    /** Seus dados › Cidade, needed with the name and key for the Fatura's Pix QR code (AC-020-18). */
    val pixCity: String = "",
    /** Themes of the teacher's own: a theme with custom colors, under a name (AC-013-5). */
    val savedThemes: List<SavedTheme> = emptyList(),
    /** The saved theme the current colors came from, if they still match it. */
    val savedThemeId: String? = null,
    /** Ajustes › Aulas › Pedir confirmação (AC-014-1). */
    val confirmWhen: ConfirmWhen = ConfirmWhen.END,
    /** Ajustes › Aulas › Aulas passadas (AC-014-2). */
    val pastClasses: PastClasses = PastClasses.ASK,
    /** Aviso antes da aula, in minutes; 0 = Desligado (AC-014-12). Off until the setup or Ajustes turns it on. */
    val remindBefore: Int = 0,
    /** Quem te deve, weekly (AC-014-13): on from the start, Monday 08:00. */
    val owedWeekly: Boolean = true,
    val owedDay: kotlinx.datetime.DayOfWeek = kotlinx.datetime.DayOfWeek.MONDAY,
    val owedHour: Int = 8,
    val owedMinute: Int = 0,
    /** Aulas para confirmar, daily (AC-014-14): on, 20:00. */
    val confirmDaily: Boolean = true,
    val confirmHour: Int = 20,
    val confirmMinute: Int = 0,
    /** Como você dá aula (AC-014-19); all three for teachers from before spec 014. */
    val teaching: Set<Teaching> = Teaching.entries.toSet(),
    /** Bloquear o Tutora (AC-015-1), off at first. */
    val appLock: Boolean = false,
    /** Bloquear após (AC-015-4). */
    val lockAfter: LockAfter = LockAfter.DEFAULT,
    /** The offer made once when the Pix key is first saved (AC-015-5). */
    val lockOffered: Boolean = false,
)

data class SavedTheme(
    val id: String,
    val name: String,
    val themeId: String,
    val custom: Map<CustomPart, String>,
)

/** Tamanho do texto (AC-013-10). */
enum class TextSize(
    val key: String,
    val factor: Float,
) {
    NORMAL("normal", 1f),
    LARGE("large", 1.15f),
    XLARGE("xlarge", 1.3f),
    ;

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: NORMAL
    }
}

/** Tamanho da interface: buttons, spacing, icons and text together, like a zoom (AC-013-21). */
enum class UiSize(
    val key: String,
    val factor: Float,
) {
    COMPACT("compact", 0.9f),
    NORMAL("normal", 1f),
    LARGE("large", 1.15f),
    ;

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: NORMAL
    }
}

/** Custom colors that no saved theme holds: switching theme asks first (AC-013-6). */
fun Settings.unsavedColors(): Boolean =
    customColors.isNotEmpty() &&
        savedThemes.none { it.themeId == themeId && it.custom == customColors }
