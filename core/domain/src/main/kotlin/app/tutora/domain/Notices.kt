package app.tutora.domain

/** A notification Tutora sent, kept for Notificações (AC-014-39, 40). */
enum class NoticeKind(
    val key: String,
) {
    CLASS("class"),
    OWED("owed"),
    CONFIRM("confirm"),
    ;

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}

/** What it said, kept as data so it reads in the app language of the day it is shown. */
data class Notice(
    val id: String,
    val kind: NoticeKind,
    val at: String,
    val lessonId: String? = null,
    val count: Int = 0,
    val cents: Cents = 0,
    val minutes: Int = 0,
    val onlyStudents: Boolean = true,
    val seen: Boolean = false,
)

/** How long Notificações keeps what was sent (AC-014-40). */
const val NOTICE_DAYS = 30
