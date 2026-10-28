package app.tutora.data

import app.tutora.domain.LessonStatus
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.PlaceKind
import app.tutora.domain.ReportClass
import app.tutora.domain.ReportPayment
import app.tutora.domain.Salary
import app.tutora.domain.SubjectKind
import app.tutora.domain.SubjectReport
import app.tutora.domain.earnedOf
import app.tutora.domain.localOf
import app.tutora.domain.subjectReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** A report's subject: a student, a group or a place. */
data class Subject(
    val kind: SubjectKind,
    val id: String,
)

/** Reports for one student, group or place (spec 010), from the same rows as 008 and 009. */
class ReportsRepository(
    private val db: TutoraDatabase,
) {
    private val earnings = EarningsRepository(db)
    private val payments = PaymentRepository(db)

    /**
     * A student's and a group's report hold the classes they owe; a place's, every class given
     * there, with whose it was. A school paid by salary totals the salary (AC-010-4 to 7).
     */
    fun subject(
        s: Subject,
        from: LocalDate,
        to: LocalDate,
        today: LocalDate,
        zone: String,
    ): Flow<SubjectReport> {
        val payer =
            when (s.kind) {
                SubjectKind.STUDENT -> PayerRef(PayerKind.STUDENT, s.id)
                SubjectKind.GROUP -> PayerRef(PayerKind.GROUP, s.id)
                SubjectKind.PLACE -> PayerRef(PayerKind.PLACE, s.id)
            }
        return combine(
            db.earnings().observeClasses(
                with(earnings) { instant(from, zone) },
                with(earnings) { instant(to.plus(DatePeriod(days = 1)), zone) },
            ),
            db.payments().observeAllCredits(),
            payments.leftByLesson(),
            payments.ledger(payer),
            db.places().observeOne(if (s.kind == SubjectKind.PLACE) s.id else ""),
        ) { rows, credits, left, ledger, placeRow ->
            val cover = coverOf(credits)
            val mine =
                rows.filter { r ->
                    when (s.kind) {
                        SubjectKind.STUDENT -> r.payer == PayerKind.STUDENT.key && r.studentId == s.id
                        SubjectKind.GROUP -> r.payer == PayerKind.GROUP.key && r.groupId == s.id
                        SubjectKind.PLACE -> r.placeId == s.id
                    }
                }
            val classes =
                mine.mapNotNull { r ->
                    val record = with(earnings) { r.record(zone) }
                    val earned = earnedOf(record) ?: return@mapNotNull null
                    ReportClass(
                        lessonId = r.id,
                        day = record.day,
                        minutes = if (record.status == LessonStatus.DONE) r.plannedMin else 0,
                        value = earned.gross,
                        who = r.whoName.orEmpty(),
                        charged = record.status != LessonStatus.DONE,
                        bonus = cover[r.id] ?: 0,
                        left = left[r.id] ?: 0,
                    )
                }
            val starts = ledger.owed.associate { it.lessonId to localOf(it.startAt, zone).date }
            val paid =
                ledger.payments
                    .filter { it.packageId == null }
                    .map { p ->
                        ReportPayment(
                            localOf(p.paidAt, zone).date,
                            p.method,
                            p.cents,
                            ledger.allocation.paidBy[p.id]
                                .orEmpty()
                                .mapNotNull(starts::get),
                        )
                    }.filter { it.day in from..to }
            val place = placeRow?.toPlace()
            val salary =
                place?.takeIf { it.kind == PlaceKind.SCHOOL && (it.monthlySalaryCents ?: 0) > 0 }?.let {
                    Salary(
                        it.id,
                        it.name.orEmpty(),
                        it.monthlySalaryCents ?: 0,
                        localOf(placeRow.createdAt, zone).date,
                        it.archivedAt?.let { a ->
                            localOf(a, zone).date
                        },
                    )
                }
            subjectReport(from, to, classes, paid, salary, today)
        }
    }

    suspend fun subjectNow(
        s: Subject,
        from: LocalDate,
        to: LocalDate,
        today: LocalDate,
        zone: String,
    ): SubjectReport = subject(s, from, to, today, zone).first()
}
