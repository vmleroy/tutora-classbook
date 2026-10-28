package app.tutora.domain

// Who owes what, and which classes each payment pays (spec 008).

enum class PayerKind(
    val key: String,
) {
    STUDENT("student"),
    GROUP("group"),
    PLACE("place"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key }
    }
}

/** Whoever a class's money is owed by: a student, a group as one, or a place that pays the teacher. */
data class PayerRef(
    val kind: PayerKind,
    val id: String,
)

/** How a payment came in (AC-008-5). */
enum class PayMethod(
    val key: String,
) {
    PIX("pix"),
    CASH("cash"),
    TRANSFER("transfer"),
    CARD("card"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key }
    }
}

/** A place that pays the teacher owes its classes; otherwise the group, otherwise the student (AC-008-1). */
fun payerOf(
    place: Place,
    who: Who,
): PayerRef {
    if (place.payer == Payer.PLACE && place.kind != PlaceKind.SCHOOL) return PayerRef(PayerKind.PLACE, place.id)
    return when (who) {
        is Who.OfGroup -> PayerRef(PayerKind.GROUP, who.group.id)
        is Who.OfStudent -> PayerRef(PayerKind.STUDENT, who.student.id)
    }
}

/** A student's package: how many classes it holds and how many classes have used it. */
data class PackageState(
    val id: String,
    val total: Int,
    val used: Int,
) {
    val left: Int get() = (total - used).coerceAtLeast(0)

    /** "Resta 1 aula no pacote" (AC-008-21). */
    val lastOne: Boolean get() = left == 1
}

/** The price of one class of a package: the price ÷ the classes, rounded once (AC-008-23). */
fun packageUnit(
    priceCents: Cents,
    lessons: Int,
): Cents = divideRounded(priceCents, lessons.toLong())

/** What a class owes, to whom, and the package it used, if any. */
data class Due(
    val payer: PayerRef,
    val cents: Cents,
    val packageId: String? = null,
)

/**
 * What a class owes once it is done or charged; null when it owes nothing (AC-008-1 to 3). A done
 * package class takes a class of [pkg] while one is left and then owes only its travel
 * (AC-008-20); one already on a package keeps it. A charge never uses a package (AC-008-23).
 */
fun dueOf(
    lesson: Lesson,
    pkg: PackageState?,
    packageId: String? = null,
): Due? {
    val payer = payerOf(lesson.place, lesson.who)
    return when (lesson.status) {
        LessonStatus.SCHEDULED, LessonStatus.LIVE -> null
        LessonStatus.CANCELLED, LessonStatus.MISSED -> lesson.feeCents?.takeIf { it > 0 }?.let { Due(payer, it) }
        LessonStatus.DONE -> {
            val worth = lesson.worth() as? Worth.Set ?: return null
            val onPackage = lesson.valueCents == null && lesson.frozenRate?.rateType == RateType.PACKAGE
            val usePackage = packageId ?: pkg?.takeIf { onPackage && it.left > 0 }?.id
            if (onPackage && usePackage != null) {
                Due(payer, lesson.travelFeeCents ?: 0, usePackage)
            } else {
                worth.cents.takeIf { it > 0 }?.let { Due(payer, it) }
            }
        }
    }
}

/** A class with money owed on it, for [allocate]. */
data class Owed(
    val lessonId: String,
    val startAt: String,
    val cents: Cents,
)

/** A payment, with the classes ticked for it in "Escolher aulas". */
data class Paid(
    val id: String,
    val paidAt: String,
    val cents: Cents,
    val chosen: List<String> = emptyList(),
)

data class Allocation(
    /** What is still owed on each class; 0 when it is paid in full. */
    val left: Map<String, Cents>,
    /** The classes each payment paid, by payment id, oldest first. */
    val paidBy: Map<String, List<String>>,
    /** Money from payments left after the classes it could pay. */
    val credit: Cents,
    /** What credit uses took off classes still owed: money and minutes (AC-008-30). */
    val usedCents: Cents = 0,
    val usedMinutes: Int = 0,
) {
    fun isPaid(lessonId: String): Boolean = (left[lessonId] ?: 0) == 0L
}

/** Credit or bonus minutes the teacher put on a class (AC-008-30). [cents] is what it takes off the class. */
data class CreditUse(
    val lessonId: String,
    val cents: Cents,
    val minutes: Int = 0,
)

/**
 * Which classes the payments pay (AC-008-6, 8). Credit the teacher put on a class comes off it
 * first. Payments go in date order; each first pays the classes chosen for it, then what is left
 * of each, in the same order, pays the oldest owed classes. With [holdCredit] (Créditos e bônus
 * on), that leftover only pays classes given by the payment's date, so credit waits for the
 * teacher to say where it goes (AC-008-29). Whatever remains is credit.
 */
fun allocate(
    owed: List<Owed>,
    payments: List<Paid>,
    uses: List<CreditUse> = emptyList(),
    holdCredit: Boolean = false,
): Allocation {
    val classes = owed.sortedWith(compareBy({ it.startAt }, { it.lessonId }))
    val left = classes.associate { it.lessonId to it.cents }.toMutableMap()
    var usedCents = 0L
    var usedMinutes = 0
    for (u in uses) {
        val owes = left[u.lessonId] ?: continue
        val part = minOf(owes, u.cents)
        left[u.lessonId] = owes - part
        if (u.minutes > 0) usedMinutes += u.minutes else usedCents += part
    }
    val order = payments.sortedWith(compareBy({ it.paidAt }, { it.id }))
    val remaining = order.associate { it.id to it.cents }.toMutableMap()
    val paidBy = order.associate { it.id to mutableListOf<String>() }

    fun pay(
        p: Paid,
        lessonId: String,
    ) {
        val owes = left[lessonId] ?: return
        val money = remaining.getValue(p.id)
        if (owes == 0L || money == 0L) return
        val part = minOf(owes, money)
        left[lessonId] = owes - part
        remaining[p.id] = money - part
        paidBy.getValue(p.id) += lessonId
    }
    for (p in order) {
        classes.filter { it.lessonId in p.chosen }.forEach { pay(p, it.lessonId) }
    }
    for (p in order) {
        classes.filter { !holdCredit || it.startAt <= p.paidAt }.forEach { pay(p, it.lessonId) }
    }
    return Allocation(left, paidBy.mapValues { (_, ids) -> ids.distinct() }, sum(remaining.values), usedCents, usedMinutes)
}

/** A bonus the teacher gave: free money, free minutes, or both (AC-008-31). */
data class Bonus(
    val id: String,
    val cents: Cents,
    val minutes: Int,
    val at: String,
)

/** Deve, Em dia or Crédito (AC-008-4). */
sealed interface Balance {
    data class Owes(
        val cents: Cents,
    ) : Balance

    data object Settled : Balance

    data class Credit(
        val cents: Cents,
    ) : Balance
}

fun balanceOf(
    dueCents: Cents,
    paidCents: Cents,
): Balance {
    val diff = dueCents - paidCents
    return when {
        diff > 0 -> Balance.Owes(diff)
        diff < 0 -> Balance.Credit(-diff)
        else -> Balance.Settled
    }
}

/** A class with money on it, as a payer's detail lists it. */
data class OwedClass(
    val lessonId: String,
    val startAt: String,
    val minutes: Int,
    val dueCents: Cents,
    /** The student's or group's name, so a place's list says whose class it is. */
    val who: String = "",
    /** Where it was, so a student's or group's list says so. */
    val placeId: String = "",
)

/** A payment as the screens show it, with the classes chosen for it. */
data class PaymentRecord(
    val id: String,
    val payer: PayerRef,
    val cents: Cents,
    val method: PayMethod,
    val paidAt: String,
    val note: String?,
    val packageId: String?,
    val chosen: List<String>,
)

/** A payer's owed classes, payments and which classes those payments paid (AC-008-6 to 9, 11). */
data class Ledger(
    val owed: List<OwedClass>,
    val payments: List<PaymentRecord>,
    val allocation: Allocation,
    val bonuses: List<Bonus> = emptyList(),
    val uses: List<CreditUse> = emptyList(),
    val holdCredit: Boolean = false,
) {
    val dueCents: Cents get() = sum(owed.map { it.dueCents })

    /** A package's purchase pays no class, so it is left out. */
    val paidCents: Cents get() = sum(payments.filter { it.packageId == null }.map { it.cents })

    /** What is still owed on the classes. */
    val owedCents: Cents get() = sum(allocation.left.values)

    /** Money credit: what payments didn't spend plus money bonuses, minus credit put on classes (AC-008-28, 30). */
    val creditCents: Cents get() = allocation.credit + sum(bonuses.map { it.cents }) - allocation.usedCents

    /** Bonus minutes not used yet (AC-008-31). */
    val creditMinutes: Int get() = bonuses.sumOf { it.minutes } - allocation.usedMinutes

    /** Deve while anything is owed; otherwise Crédito or Em dia. */
    val balance: Balance
        get() =
            when {
                owedCents > 0 -> Balance.Owes(owedCents)
                creditCents > 0 -> Balance.Credit(creditCents)
                else -> Balance.Settled
            }

    /** Allocation as it would be with [draft] added and [without] left out, for the payment sheet. */
    fun with(
        draft: Paid?,
        without: String? = null,
    ): Allocation =
        allocate(
            owed.map { Owed(it.lessonId, it.startAt, it.dueCents) },
            payments.filter { it.packageId == null && it.id != without }.map { Paid(it.id, it.paidAt, it.cents, it.chosen) } +
                listOfNotNull(draft),
            uses,
            holdCredit,
        )
}

/** The classes the payments pay, worked out from scratch on every read. */
fun ledgerOf(
    owed: List<OwedClass>,
    payments: List<PaymentRecord>,
    bonuses: List<Bonus> = emptyList(),
    uses: List<CreditUse> = emptyList(),
    holdCredit: Boolean = false,
): Ledger {
    val ledger = Ledger(owed, payments.sortedByDescending { it.paidAt }, Allocation(emptyMap(), emptyMap(), 0), bonuses, uses, holdCredit)
    return ledger.copy(allocation = ledger.with(null))
}

/** What a bonus of [minutes] takes off a class worth [due] for [classMinutes] at [price]: by the hour, or the whole class only when it covers it (AC-008-31). */
fun minutesCover(
    price: Price.Set?,
    classMinutes: Int,
    minutes: Int,
    due: Cents,
): Cents {
    val used = minOf(minutes, classMinutes)
    if (used <= 0 || price == null) return 0
    return if (price.rateType ==
        RateType.HOUR
    ) {
        minOf(due, lessonValue(price.cents, RateType.HOUR, used))
    } else if (used >= classMinutes) {
        due
    } else {
        0
    }
}

/** A student's class that a place owes instead of the student, with what is still owed on it (AC-008-26). */
data class OwedElsewhere(
    val owed: OwedClass,
    val left: Cents,
    val payer: PayerRef,
)
