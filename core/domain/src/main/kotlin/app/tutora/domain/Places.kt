package app.tutora.domain

enum class PlaceKind(
    val key: String,
) {
    SCHOOL("school"),
    SALINHA("salinha"),
    COURSE("course"),
    PRIVATE("private"),
    STUDENT_HOME("student_home"),
    ONLINE("online"),
    ;

    companion object {
        fun fromKey(key: String) = entries.first { it.key == key }

        /** The types a teacher can give their own places. */
        val own = listOf(SCHOOL, SALINHA, COURSE, PRIVATE)
    }
}

enum class Payer { PLACE, STUDENT }

enum class PayCycle { LESSON, WEEKLY, MONTHLY }

enum class TravelMode { VARIES, FIXED, NONE }

/** The two places every teacher has; their names come from the app language. */
enum class BuiltinPlace(
    val id: String,
    val kind: PlaceKind,
) {
    STUDENT_HOME("00000000-0000-7000-8000-000000000001", PlaceKind.STUDENT_HOME),
    ONLINE("00000000-0000-7000-8000-000000000002", PlaceKind.ONLINE),
}

/** The design system's color tags, stored by name. */
enum class TagColor { AMBER, GREEN, BLUE, VIOLET, ORANGE, RED, TEAL }

data class Place(
    val id: String,
    val builtin: BuiltinPlace?,
    /** null for built-ins: their name comes from the app language. */
    val name: String?,
    val address: String?,
    val kind: PlaceKind,
    val payer: Payer,
    val rateType: RateType,
    /** null = no price set yet. */
    val rateCents: Cents?,
    /** What the place keeps when the student pays, in basis points (30% = 3000). */
    val commissionBp: Int,
    val payCycle: PayCycle,
    val monthlySalaryCents: Cents?,
    val defaultDurationMin: Int,
    val travelMode: TravelMode,
    val travelFeeCents: Cents,
    val archivedAt: String?,
)

data class Student(
    val id: String,
    val name: String,
    val subject: String?,
    val phone: String?,
    val notes: String?,
    val color: TagColor,
    val rateType: RateType,
    val archivedAt: String?,
    /** The package terms when [rateType] is PACKAGE: how many classes, for what price (AC-008-19). */
    val packageLessons: Int? = null,
    val packagePriceCents: Cents? = null,
)

/** A student's link to a place; `rateCents` null = the place's price ("Do local"). */
data class PlaceLink(
    val placeId: String,
    val isDefault: Boolean,
    val rateCents: Cents?,
)

data class Group(
    val id: String,
    val placeId: String,
    val name: String,
    val subject: String?,
    val rateCents: Cents?,
    val memberIds: List<String>,
    val archivedAt: String?,
)

const val DEFAULT_DURATION_MIN = 60
const val MAX_COMMISSION_BP = 9000

/** Escola and Cursinho count as school and formal job in Ganhos; the rest as private lessons. */
enum class EarningsType { JOB, PRIVATE }

fun earningsType(kind: PlaceKind): EarningsType =
    if (kind == PlaceKind.SCHOOL ||
        kind == PlaceKind.COURSE
    ) {
        EarningsType.JOB
    } else {
        EarningsType.PRIVATE
    }

/** How a place pays: a monthly salary, a choice of who pays, or always the student. */
enum class PayMode { SALARY, CHOOSE, STUDENT }

fun payMode(kind: PlaceKind): PayMode =
    when (kind) {
        PlaceKind.SCHOOL -> PayMode.SALARY
        PlaceKind.SALINHA, PlaceKind.COURSE, PlaceKind.PRIVATE -> PayMode.CHOOSE
        else -> PayMode.STUDENT
    }

/** A built-in place as first created: no price until the teacher sets one (AC-004-2). */
fun builtinPlace(builtin: BuiltinPlace): Place =
    Place(
        id = builtin.id,
        builtin = builtin,
        name = null,
        address = null,
        kind = builtin.kind,
        payer = Payer.STUDENT,
        rateType = RateType.HOUR,
        rateCents = null,
        commissionBp = 0,
        payCycle = PayCycle.LESSON,
        monthlySalaryCents = null,
        defaultDurationMin = DEFAULT_DURATION_MIN,
        travelMode = if (builtin == BuiltinPlace.STUDENT_HOME) TravelMode.VARIES else TravelMode.NONE,
        travelFeeCents = 0,
        archivedAt = null,
    )

enum class CatalogError { NAME_REQUIRED, BUILTIN_FIXED, COMMISSION_RANGE, PLACE_REQUIRED, NOT_ARCHIVED, NOT_FOUND }

fun checkPlace(
    place: Place,
    before: Place?,
): CatalogError? {
    if (before?.builtin != null && (place.name != null || place.kind != before.kind)) return CatalogError.BUILTIN_FIXED
    if (place.builtin == null && place.name.isNullOrBlank()) return CatalogError.NAME_REQUIRED
    if (place.commissionBp !in 0..MAX_COMMISSION_BP) return CatalogError.COMMISSION_RANGE
    return null
}

fun checkStudent(
    student: Student,
    links: List<PlaceLink>,
): CatalogError? {
    if (student.name.isBlank()) return CatalogError.NAME_REQUIRED
    if (links.isEmpty()) return CatalogError.PLACE_REQUIRED
    return null
}

/** Exactly one starred place: the starred one if any, else the first. */
fun withDefault(links: List<PlaceLink>): List<PlaceLink> {
    val star = links.firstOrNull { it.isDefault }?.placeId ?: links.firstOrNull()?.placeId
    return links.map { it.copy(isDefault = it.placeId == star) }
}

/** Unlinks a place; if it was the default, the star moves to the next one (AC-004-11). */
fun unlink(
    links: List<PlaceLink>,
    placeId: String,
): List<PlaceLink> = withDefault(links.filter { it.placeId != placeId })

/** A tag picked from [random] in [0, 1), so new students don't all look alike (AC-004-28). */
fun randomTag(random: Double): TagColor = TagColor.entries[(random * TagColor.entries.size).toInt().coerceIn(0, TagColor.entries.lastIndex)]

/** Initials for an avatar: first and last word, or the first two letters of one word. */
fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val first = words.firstOrNull()?.firstOrNull()?.toString() ?: ""
    val last = if (words.size > 1) words.last().first().toString() else words.firstOrNull()?.getOrNull(1)?.toString() ?: ""
    return (first + last).uppercase()
}
