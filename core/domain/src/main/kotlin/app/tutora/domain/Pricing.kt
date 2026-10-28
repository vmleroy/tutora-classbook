package app.tutora.domain

enum class PriceSource { PLACE, STUDENT, GROUP, HAND, PACKAGE }

sealed interface Price {
    data class Set(
        val cents: Cents,
        val rateType: RateType,
        val source: PriceSource,
    ) : Price

    data object Unset : Price

    data object Salary : Price
}

/**
 * What a lesson charges, and where the price comes from. A place that pays sets its own
 * rate; otherwise the student's own price there wins over the place's. A group has its own
 * price, and at a school the salary covers it. A missing price is Unset, never zero.
 */
fun resolvePrice(
    place: Place,
    student: Student? = null,
    link: PlaceLink? = null,
    group: Group? = null,
): Price {
    if (place.kind == PlaceKind.SCHOOL) return Price.Salary
    if (group != null) return group.rateCents?.let { Price.Set(it, RateType.LESSON, PriceSource.GROUP) } ?: Price.Unset
    if (place.payer == Payer.STUDENT && student != null) packagePrice(student)?.let { return it }
    val own = if (place.payer == Payer.STUDENT) link?.rateCents else null
    if (own != null && student != null) return Price.Set(own, student.rateType, PriceSource.STUDENT)
    return place.rateCents?.let { Price.Set(it, place.rateType, PriceSource.PLACE) } ?: Price.Unset
}

/** A package student pays its price per class wherever the student pays (AC-008-19, 23). */
fun packagePrice(student: Student): Price.Set? {
    if (student.rateType != RateType.PACKAGE) return null
    val lessons = student.packageLessons?.takeIf { it > 0 } ?: return null
    val price = student.packagePriceCents ?: return null
    return Price.Set(packageUnit(price, lessons), RateType.PACKAGE, PriceSource.PACKAGE)
}

sealed interface Preview {
    data class PlacePays(
        val earned: Cents,
    ) : Preview

    data class StudentPays(
        val paid: Cents,
        val kept: Cents,
    ) : Preview

    data object Salary : Preview

    data object Unset : Preview
}

/** What a lesson of [minutes] at this place earns, for the edit form's preview (AC-004-7). */
fun previewLesson(
    place: Place,
    minutes: Int,
): Preview {
    if (place.kind == PlaceKind.SCHOOL) return Preview.Salary
    val rate = place.rateCents ?: return Preview.Unset
    val value = lessonValue(rate, place.rateType, minutes)
    return if (place.payer ==
        Payer.PLACE
    ) {
        Preview.PlacePays(value)
    } else {
        Preview.StudentPays(value, afterCommission(value, place.commissionBp))
    }
}
