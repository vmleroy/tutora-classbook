package app.tutora.domain

enum class Language(
    val tag: String,
) {
    PT_BR("pt-BR"),
    EN("en"),
    ;

    companion object {
        fun fromTag(tag: String): Language? = entries.firstOrNull { it.tag == tag }
    }
}

/** Portuguese from any region opens in pt-BR; every other phone language in English. */
fun firstRunLanguage(phoneTags: List<String>): Language =
    if (phoneTags.firstOrNull()?.substringBefore('-')?.lowercase() == "pt") Language.PT_BR else Language.EN

/** The saved setting rules once it exists (AC-001-6). */
fun startupLanguage(
    saved: Language?,
    phoneTags: List<String>,
): Language = saved ?: firstRunLanguage(phoneTags)
