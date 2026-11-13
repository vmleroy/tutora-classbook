package app.tutora.data

import app.tutora.domain.DueRow
import app.tutora.domain.DueSource
import app.tutora.domain.LessonStatus
import app.tutora.domain.Member
import app.tutora.domain.Payer
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.PlaceKind
import app.tutora.domain.ReportClass
import app.tutora.domain.ReportPayment
import app.tutora.domain.Salary
import app.tutora.domain.SubjectKind
import app.tutora.domain.SubjectReport
import app.tutora.domain.canSplit
import app.tutora.domain.earnedOf
import app.tutora.domain.invoicesDue
import app.tutora.domain.localOf
import app.tutora.domain.subjectReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import java.text.Normalizer

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
            combine(
                db.payments().observeAllCredits(),
                db.payments().observeShareRows(),
                ReceiptRepository(db).byPayment(),
            ) { c, sh, rc -> Triple(c, sh, rc) },
            payments.leftByLesson(),
            payments.ledger(payer),
            db.places().observeOne(if (s.kind == SubjectKind.PLACE) s.id else ""),
        ) { rows, (credits, shareRows, receipts), left, ledger, placeRow ->
            val cover = coverOf(credits)
            // A student's share of each split group's class (AC-020-36).
            val myShares =
                if (s.kind ==
                    SubjectKind.STUDENT
                ) {
                    shareRows.filter { it.studentId == s.id }.associate { it.lessonId to it.dueCents }
                } else {
                    emptyMap()
                }
            val mine =
                rows.filter { r ->
                    when (s.kind) {
                        SubjectKind.STUDENT -> (r.payer == PayerKind.STUDENT.key && r.studentId == s.id) || r.id in myShares
                        SubjectKind.GROUP -> r.payer == PayerKind.GROUP.key && r.groupId == s.id
                        SubjectKind.PLACE -> r.placeId == s.id
                    }
                }
            val classes =
                mine.mapNotNull { r ->
                    val record = with(earnings) { r.record(zone) }
                    val earned = earnedOf(record) ?: return@mapNotNull null
                    val share = myShares[r.id]
                    ReportClass(
                        lessonId = r.id,
                        day = record.day,
                        minutes = if (record.status == LessonStatus.DONE) r.plannedMin else 0,
                        value = share ?: earned.gross,
                        who = r.whoName.orEmpty(),
                        charged = record.status != LessonStatus.DONE,
                        bonus = cover[r.id] ?: 0,
                        left = if (share != null) ledger.allocation.left[r.id] ?: 0 else left[r.id] ?: 0,
                        start = localOf(r.startAt, zone).time.let { LocalTime(it.hour, it.minute) },
                        travel = if (record.status == LessonStatus.DONE && share == null) r.travelFeeCents ?: 0 else 0,
                        charge = if (record.status == LessonStatus.DONE) 0 else share ?: earned.gross,
                        placeId = r.placeId,
                        placeName = r.placeName,
                        notes = r.notes?.trim()?.takeIf { it.isNotEmpty() },
                        groupId = r.groupId,
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
                            // Its comprovantes, which a Fatura or Relatório detalhado can carry (AC-021-7).
                            id = p.id,
                            receipts = receipts[p.id].orEmpty(),
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

    /** Every group's members, sorted by name ignoring accents and capitals, as splits use them (AC-020-21). */
    fun members(): Flow<Map<String, List<Member>>> =
        combine(db.groups().observeMembers(), db.students().observeEvery()) { links, students ->
            val byId = students.associateBy { it.id }
            links
                .groupBy { it.groupId }
                .mapValues { (_, ls) ->
                    ls
                        .mapNotNull { l -> byId[l.studentId]?.let { Member(it.id, it.name, it.guardian) } }
                        .sortedBy { fold(it.name) }
                }
        }

    /** Each student's Responsável, for "Para:" on a Fatura (AC-020-6, 17). */
    fun guardians(): Flow<Map<String, String>> =
        db.students().observeEvery().map { rows -> rows.mapNotNull { r -> r.guardian?.let { r.id to it } }.toMap() }

    /**
     * Faturas do mês: who still owes for the period's classes, with what is left on each class,
     * from the same rows and ledgers as each report (AC-020-23, 26).
     */
    fun due(
        from: LocalDate,
        to: LocalDate,
        zone: String,
        splitGroups: Boolean,
    ): Flow<List<DueRow>> =
        combine(
            db.earnings().observeClasses(
                with(earnings) { instant(from, zone) },
                with(earnings) { instant(to.plus(DatePeriod(days = 1)), zone) },
            ),
            combine(
                payments.leftByLesson(),
                payments.leftByPayer(),
                db.payments().observeShareRows(),
            ) { l, byPayer, sh -> Triple(l, byPayer, sh) },
            members(),
            guardians(),
            db.students().observeEvery(),
        ) { rows, (left, leftByPayer, shareRows), members, guardians, students ->
            val names = students.associate { it.id to it.name }
            val sharesOf = shareRows.groupBy { it.lessonId }
            val sources =
                rows
                    .flatMap { r ->
                        val record = with(earnings) { r.record(zone) }
                        earnedOf(record) ?: return@flatMap emptyList()
                        val split = sharesOf[r.id]
                        // A split class is owed by each member: it goes to them, not to the group (AC-020-36).
                        if (split != null) {
                            split.map { sh ->
                                val p = PayerRef(PayerKind.STUDENT, sh.studentId)
                                Triple(p, r.copy(whoName = names[sh.studentId]), leftByPayer[p]?.get(r.id) ?: 0L)
                            }
                        } else {
                            val payer = record.payer ?: return@flatMap emptyList()
                            listOf(Triple(payer, r, left[r.id] ?: 0L))
                        }
                    }.groupBy { it.first }
                    .map { (payer, list) ->
                        val r = list.first().second
                        val kind =
                            when (payer.kind) {
                                PayerKind.STUDENT -> SubjectKind.STUDENT
                                PayerKind.GROUP -> SubjectKind.GROUP
                                PayerKind.PLACE -> SubjectKind.PLACE
                            }
                        val split =
                            members[payer.id]?.takeIf {
                                kind == SubjectKind.GROUP && canSplit(it.size, PlaceKind.fromKey(r.placeKind), Payer.valueOf(r.placePayer))
                            }
                        DueSource(
                            kind,
                            payer.id,
                            if (kind == SubjectKind.PLACE) r.placeName.orEmpty() else r.whoName.orEmpty(),
                            list.map { it.third },
                            split,
                            guardians[payer.id],
                        )
                    }
            invoicesDue(sources, splitGroups)
        }
}

private fun fold(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()
