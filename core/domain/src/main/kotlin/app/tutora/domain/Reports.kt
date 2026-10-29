package app.tutora.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

// Reports for one student, group or place, and for Ganhos (spec 010).

/** Aluno, Turma or Local (AC-010-2). */
enum class SubjectKind {
    STUDENT,
    GROUP,
    PLACE,
}

/** Este mês, Mês passado, 3 meses or Outro… (AC-010-3). */
sealed interface ReportRange {
    data object ThisMonth : ReportRange

    data object LastMonth : ReportRange

    data object ThreeMonths : ReportRange

    data class Custom(
        val from: LocalDate,
        val to: LocalDate,
    ) : ReportRange

    /** De and Até: Até is never before De, and never after today. */
    fun dates(today: LocalDate): Pair<LocalDate, LocalDate> =
        when (this) {
            ThisMonth -> monthOf(today) to today
            LastMonth -> monthOf(today).minus(DatePeriod(months = 1)) to monthOf(today).minus(DatePeriod(days = 1))
            ThreeMonths -> monthOf(today).minus(DatePeriod(months = 2)) to today
            is Custom -> {
                val until = minOf(to, today)
                minOf(from, until) to until
            }
        }
}

/** The first day of each month from [from] to [to], up to [today]'s month. */
fun monthsBetween(
    from: LocalDate,
    to: LocalDate,
    today: LocalDate,
): List<LocalDate> {
    val out = mutableListOf<LocalDate>()
    var m = monthOf(from)
    while (m <= monthOf(to) && m <= monthOf(today)) {
        out += m
        m = m.plus(DatePeriod(months = 1))
    }
    return out
}

/** A class on a report: what it cost (before any commission), and what is left to pay on it. */
data class ReportClass(
    val lessonId: String,
    val day: LocalDate,
    val minutes: Int,
    val value: Cents,
    val who: String,
    val charged: Boolean,
    val bonus: Cents,
    val left: Cents,
) {
    val paid: Boolean get() = left == 0L
}

/** A payment on a report, with the days of the classes it paid. */
data class ReportPayment(
    val day: LocalDate,
    val method: PayMethod,
    val cents: Cents,
    val classDays: List<LocalDate>,
)

/** A day a school's classes were given, for a salary report (AC-010-6). */
data class TaughtDay(
    val day: LocalDate,
    val classes: Int,
)

/** One student's, group's or place's report for a period (AC-010-4 to 7). */
data class SubjectReport(
    val from: LocalDate,
    val to: LocalDate,
    val classes: List<ReportClass>,
    val payments: List<ReportPayment>,
    /** For a school paid by salary: the days taught; null otherwise. */
    val days: List<TaughtDay>?,
    val total: Cents,
    val paid: Cents,
    val left: Cents,
) {
    val count: Int get() = days?.sumOf { it.classes } ?: classes.size
    val minutes: Int get() = classes.sumOf { it.minutes }
}

/**
 * Builds a report from the period's classes. A class costs its value before any commission; what is
 * left on it comes from the payer's ledger. A school paid by salary lists the days taught and
 * totals the salary for each month, counted as received (AC-010-4 to 7).
 */
fun subjectReport(
    from: LocalDate,
    to: LocalDate,
    classes: List<ReportClass>,
    payments: List<ReportPayment>,
    salary: Salary? = null,
    today: LocalDate = to,
): SubjectReport {
    val sorted = classes.sortedByDescending { it.day }
    if (salary != null) {
        val total = salary.cents * salaryMonths(salary, monthsBetween(from, to, today)).size
        val days = sorted.groupBy { it.day }.map { (d, list) -> TaughtDay(d, list.size) }
        return SubjectReport(from, to, sorted, emptyList(), days, total, total, 0)
    }
    val total = sum(sorted.map { it.value })
    val left = sum(sorted.map { it.left })
    return SubjectReport(from, to, sorted, payments.sortedByDescending { it.day }, null, total, total - left, left)
}

/** Where each page of a table starts and ends: the first page holds fewer rows, under the summary (AC-010-9). */
fun paginate(
    rows: Int,
    firstPage: Int,
    otherPages: Int,
): List<IntRange> {
    if (rows <= firstPage) return listOf(0 until rows)
    val pages = mutableListOf(0 until firstPage)
    var start = firstPage
    while (start < rows) {
        pages += start until minOf(rows, start + otherPages)
        start += otherPages
    }
    return pages
}
