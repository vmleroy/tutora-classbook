package app.tutora.domain

import kotlinx.datetime.LocalDate

// The Fatura and the Relatório detalhado, made from a report (spec 020).

/** The classes in date and time order, oldest first (AC-020-3). */
fun chronological(classes: List<ReportClass>): List<ReportClass> = classes.sortedWith(compareBy({ it.day }, { it.start }))

/** One person's classes on a place's documents, with their subtotal (AC-020-13, 16). */
data class ClassGroup(
    val name: String,
    val classes: List<ReportClass>,
) {
    val minutes: Int get() = classes.sumOf { it.minutes }
    val total: Cents get() = sum(classes.map { it.value })
}

/** The classes of a period, grouped by whose they were, by name; oldest first in each. */
fun byPerson(classes: List<ReportClass>): List<ClassGroup> =
    chronological(classes)
        .groupBy { it.who }
        .map { (who, list) -> ClassGroup(who, list) }
        .sortedBy { it.name.lowercase() }

/** What a Fatura says (AC-020-7, 8, 12, 13). */
data class Invoice(
    val from: LocalDate,
    val to: LocalDate,
    val classes: List<ReportClass>,
    /** A place's classes by student; null for a student or a group. */
    val groups: List<ClassGroup>?,
    /** A school paid by salary: its months, instead of classes. */
    val salaryMonths: List<Pair<LocalDate, Cents>>,
    val total: Cents,
    val paid: Cents,
    val toPay: Cents,
) {
    /** Nothing left to pay for the period: "Pago, obrigado" (AC-020-12). */
    val settled: Boolean get() = toPay == 0L
}

/**
 * A Fatura from a report. What there is to pay is what is still owed for the period's classes
 * only; classes of other periods and credit don't count (AC-020-8).
 */
fun invoiceOf(
    r: SubjectReport,
    groupByPerson: Boolean,
): Invoice {
    val classes = chronological(r.classes)
    val salary = r.days != null
    return Invoice(
        from = r.from,
        to = r.to,
        classes = if (salary) emptyList() else classes,
        groups = if (groupByPerson && !salary) byPerson(classes) else null,
        salaryMonths = r.salaryMonths,
        total = r.total,
        paid = r.total - r.left,
        toPay = r.left,
    )
}

/** The totals above the detailed report's table; the screen leaves out the zeros (AC-020-16). */
data class DetailTotals(
    val count: Int,
    val minutes: Int,
    /** The classes' own value, without travel or charges. */
    val lessons: Cents,
    val travel: Cents,
    val charges: Cents,
    val total: Cents,
    val paid: Cents,
    val left: Cents,
)

fun detailTotals(r: SubjectReport): DetailTotals {
    val travel = sum(r.classes.map { it.travel })
    val charges = sum(r.classes.map { it.charge })
    val salary = r.days != null
    return DetailTotals(
        count = r.count,
        minutes = r.minutes,
        lessons = if (salary) r.total else sum(r.classes.map { it.value }) - travel - charges,
        travel = if (salary) 0 else travel,
        charges = if (salary) 0 else charges,
        total = r.total,
        paid = r.paid,
        left = r.left,
    )
}

/**
 * A group can be split per student when it has members and its students pay: not a group at a
 * school or a course, nor at a place that pays the teacher (AC-020-20, 22).
 */
fun canSplit(
    members: Int,
    placeKind: PlaceKind,
    placePayer: Payer,
): Boolean = members > 0 && placePayer == Payer.STUDENT && placeKind != PlaceKind.SCHOOL && placeKind != PlaceKind.COURSE

/**
 * One member's share of a group's report: every class's value, travel, charge and what is left on
 * it, split equally among [members] (AC-020-21). [index] is the member's place in the members
 * sorted by name, so the first ones take the leftover cents. The group's payments aren't split.
 */
fun memberReport(
    group: SubjectReport,
    members: Int,
    index: Int,
): SubjectReport {
    fun share(c: Cents) = splitShares(c, members)[index]
    val classes =
        group.classes.map { c ->
            c.copy(value = share(c.value), left = share(c.left), travel = share(c.travel), charge = share(c.charge), bonus = share(c.bonus))
        }
    val total = sum(classes.map { it.value })
    val left = sum(classes.map { it.left })
    return group.copy(classes = classes, payments = emptyList(), total = total, paid = total - left, left = left)
}

/** A member of a group, for the split and for "Para:" (AC-020-17, 21). */
data class Member(
    val id: String,
    val name: String,
    val guardian: String? = null,
)

/** Who owes in a period, and what is left on each of their period's classes (AC-020-23). */
data class DueSource(
    val kind: SubjectKind,
    val id: String,
    val name: String,
    val lefts: List<Cents>,
    /** A group's members, sorted by name, when it can be split; null otherwise. */
    val members: List<Member>? = null,
    val guardian: String? = null,
)

/** A row of Faturas do mês: a student, a group, a place, or one member's share of a group. */
data class DueRow(
    val kind: SubjectKind,
    val id: String,
    val name: String,
    val toPay: Cents,
    /** For a member's share: the member, and their place among the group's members. */
    val member: Member? = null,
    val memberIndex: Int = 0,
    val members: Int = 0,
    val guardian: String? = null,
)

/**
 * Faturas do mês: everyone with something still to pay for the period's classes, largest first.
 * With [splitGroups], a group that can be split is one row per member instead (AC-020-23, 26).
 */
fun invoicesDue(
    sources: List<DueSource>,
    splitGroups: Boolean,
): List<DueRow> =
    sources
        .flatMap { s ->
            val members = s.members
            if (splitGroups && s.kind == SubjectKind.GROUP && !members.isNullOrEmpty()) {
                members.mapIndexed { i, m ->
                    DueRow(s.kind, s.id, m.name, sum(s.lefts.map { splitShares(it, members.size)[i] }), m, i, members.size, m.guardian)
                }
            } else {
                listOf(DueRow(s.kind, s.id, s.name, sum(s.lefts), guardian = s.guardian))
            }
        }.filter { it.toPay > 0 }
        .sortedByDescending { it.toPay }
