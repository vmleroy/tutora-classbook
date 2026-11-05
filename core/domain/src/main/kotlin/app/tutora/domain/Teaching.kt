package app.tutora.domain

/** "Como você dá aula?": the kinds of teaching chosen in the setup or in Ajustes (AC-014-19). */
enum class Teaching(
    val key: String,
) {
    PRIVATE("private"),
    SCHOOL("school"),
    SALINHA("salinha"),
    ;

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}

/** The kind of teaching a place belongs to. */
fun PlaceKind.teaching(): Teaching =
    when (this) {
        PlaceKind.SCHOOL -> Teaching.SCHOOL
        PlaceKind.SALINHA, PlaceKind.COURSE -> Teaching.SALINHA
        PlaceKind.PRIVATE, PlaceKind.STUDENT_HOME, PlaceKind.ONLINE -> Teaching.PRIVATE
    }

/**
 * Whether what belongs to [kind] shows: chosen, or the teacher already has a place or class of
 * that kind, so nothing they have is ever out of sight (AC-014-19, 20).
 */
fun shown(
    kind: Teaching,
    chosen: Set<Teaching>,
    existing: Set<Teaching>,
): Boolean = kind in chosen || kind in existing

/** The place kinds offered when adding a place: Escola, Salinha and Cursinho follow the choice; Particular always shows (AC-014-19). */
fun ownKindsShown(
    chosen: Set<Teaching>,
    existing: Set<Teaching>,
): List<PlaceKind> = PlaceKind.own.filter { it == PlaceKind.PRIVATE || shown(it.teaching(), chosen, existing) }
