package app.tutora.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import app.tutora.domain.ClassRecord
import app.tutora.domain.CreditEntry
import app.tutora.domain.EarnedClass
import app.tutora.domain.EarningsType
import app.tutora.domain.EarningsView
import app.tutora.domain.LessonStatus
import app.tutora.domain.Payer
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.Period
import app.tutora.domain.PlaceKind
import app.tutora.domain.RateType
import app.tutora.domain.Received
import app.tutora.domain.Salary
import app.tutora.domain.TypeFilter
import app.tutora.domain.bonusCover
import app.tutora.domain.earnedOf
import app.tutora.domain.earningsType
import app.tutora.domain.isoText
import app.tutora.domain.localOf
import app.tutora.domain.localToInstant
import app.tutora.domain.monthOf
import app.tutora.domain.sources
import app.tutora.domain.summarize
import app.tutora.domain.trend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** A done or charged class with its place and the name of whoever pays, in one row. */
data class EarnedRow(
    val id: String,
    @ColumnInfo(name = "start_at") val startAt: String,
    val status: String,
    @ColumnInfo(name = "planned_min") val plannedMin: Int,
    @ColumnInfo(name = "value_cents") val valueCents: Long?,
    @ColumnInfo(name = "rate_cents") val rateCents: Long?,
    @ColumnInfo(name = "rate_type") val rateType: String?,
    @ColumnInfo(name = "travel_fee_cents") val travelFeeCents: Long?,
    @ColumnInfo(name = "fee_cents") val feeCents: Long?,
    val payer: String?,
    @ColumnInfo(name = "student_id") val studentId: String?,
    @ColumnInfo(name = "group_id") val groupId: String?,
    @ColumnInfo(name = "place_id") val placeId: String,
    @ColumnInfo(name = "place_kind") val placeKind: String,
    @ColumnInfo(name = "place_payer") val placePayer: String,
    @ColumnInfo(name = "commission_bp") val commissionBp: Int,
    @ColumnInfo(name = "who_name") val whoName: String?,
    @ColumnInfo(name = "place_name") val placeName: String?,
)

/** A payment with the type of the place it comes from, if any. */
data class ReceivedRow(
    @ColumnInfo(name = "paid_at") val paidAt: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val kind: String?,
)

data class SchoolRow(
    val id: String,
    val name: String?,
    @ColumnInfo(name = "monthly_salary_cents") val salaryCents: Long,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "archived_at") val archivedAt: String?,
)

@Dao
interface EarningsDao {
    @Query(
        """SELECT l.id, l.start_at, l.status, l.planned_min, l.value_cents, l.rate_cents, l.rate_type, l.travel_fee_cents,
             l.fee_cents, l.payer, l.student_id, l.group_id, l.place_id, p.kind AS place_kind, p.payer AS place_payer,
             p.commission_bp, COALESCE(s.name, g.name) AS who_name, p.name AS place_name
           FROM lesson l JOIN place p ON p.id = l.place_id
           LEFT JOIN student s ON s.id = l.student_id LEFT JOIN group_class g ON g.id = l.group_id
           WHERE l.status IN ('done', 'cancelled', 'missed') AND l.start_at >= :from AND l.start_at < :to""",
    )
    fun observeClasses(
        from: String,
        to: String,
    ): Flow<List<EarnedRow>>

    /** A place pays as its type; a group as its place's; a student as private (null kind). */
    @Query(
        """SELECT pay.paid_at, pay.amount_cents, COALESCE(pp.kind, gp.kind) AS kind FROM payment pay
           LEFT JOIN place pp ON pp.id = pay.place_id
           LEFT JOIN group_class g ON g.id = pay.group_id LEFT JOIN place gp ON gp.id = g.place_id
           WHERE pay.paid_at >= :from AND pay.paid_at < :to""",
    )
    fun observeReceived(
        from: String,
        to: String,
    ): Flow<List<ReceivedRow>>

    @Query(
        """SELECT id, name, monthly_salary_cents, created_at, archived_at FROM place
           WHERE kind = 'school' AND monthly_salary_cents IS NOT NULL AND monthly_salary_cents > 0""",
    )
    fun observeSchools(): Flow<List<SchoolRow>>
}

/** What bonuses covered on each class, across every payer (AC-009-4). */
internal fun coverOf(credits: List<CreditRow>): Map<String, Long> =
    credits
        .groupBy { it.payerRef() }
        .values
        .flatMap { list ->
            bonusCover(list.sortedBy { it.createdAt }.map { CreditEntry(it.lessonId, it.cents, it.minutes, it.kind == "bonus") }).entries
        }.associate { it.key to it.value }

/** Ganhos and Hoje's card, read again after any write (spec 009). */
class EarningsRepository(
    private val db: TutoraDatabase,
) {
    private val dao get() = db.earnings()
    private val payments = PaymentRepository(db)

    internal fun instant(
        day: LocalDate,
        zone: String,
    ) = localToInstant(day, LocalTime(0, 0), zone).isoText()

    internal fun EarnedRow.record(zone: String) =
        ClassRecord(
            lessonId = id,
            day = localOf(startAt, zone).date,
            status = LessonStatus.fromKey(status),
            minutes = plannedMin,
            valueCents = valueCents,
            rateCents = rateCents,
            rateType = rateType?.let(RateType::valueOf),
            travelCents = travelFeeCents,
            feeCents = feeCents,
            payer =
                when (PayerKind.fromKey(payer)) {
                    PayerKind.STUDENT -> studentId?.let { PayerRef(PayerKind.STUDENT, it) }
                    PayerKind.GROUP -> groupId?.let { PayerRef(PayerKind.GROUP, it) }
                    PayerKind.PLACE -> PayerRef(PayerKind.PLACE, placeId)
                    null -> null
                },
            payerName = if (PayerKind.fromKey(payer) == PayerKind.PLACE) placeName.orEmpty() else whoName.orEmpty(),
            placeKind = PlaceKind.fromKey(placeKind),
            placePayer = Payer.valueOf(placePayer),
            commissionBp = commissionBp,
        )

    /** The classes from [first] to [last] (local days), with what bonuses covered on each. */
    private fun classes(
        first: LocalDate,
        last: LocalDate,
        zone: String,
    ): Flow<List<EarnedClass>> =
        combine(
            dao.observeClasses(instant(first, zone), instant(last.plus(DatePeriod(days = 1)), zone)),
            db.payments().observeAllCredits(),
        ) { rows, credits ->
            val cover = coverOf(credits)
            rows.mapNotNull { earnedOf(it.record(zone), cover[it.id] ?: 0) }
        }

    /** What the classes from [first] to [last] earned, without salaries: Ganho hoje and Semana (AC-009-8). */
    fun earnedBetween(
        first: LocalDate,
        last: LocalDate,
        zone: String,
    ): Flow<Long> = classes(first, last, zone).map { list -> list.sumOf { it.earned } }

    /** Ganhos for [period] and [filter]: summary, six-month trend and sources (AC-009-5 to 7, 11 to 13). */
    fun view(
        period: Period,
        filter: TypeFilter,
        today: LocalDate,
        zone: String,
    ): Flow<EarningsView> {
        val trendFirst = monthOf(period.lastMonth).minus(DatePeriod(months = 5))
        val from = minOf(period.first, trendFirst)
        return combine(
            classes(from, period.last, zone),
            dao.observeReceived(instant(from, zone), instant(period.last.plus(DatePeriod(days = 1)), zone)),
            dao.observeSchools(),
            payments.leftByLesson(),
        ) { all, paid, schools, left ->
            val received =
                paid.map { r ->
                    Received(
                        localOf(r.paidAt, zone).date,
                        r.amountCents,
                        r.kind?.let { earningsType(PlaceKind.fromKey(it)) } ?: EarningsType.PRIVATE,
                    )
                }
            val salaries =
                schools.map { s ->
                    Salary(
                        s.id,
                        s.name.orEmpty(),
                        s.salaryCents,
                        localOf(s.createdAt, zone).date,
                        s.archivedAt?.let { localOf(it, zone).date },
                    )
                }
            val inPeriod = { d: LocalDate -> d >= period.first && d <= period.last }
            val months = period.months(today)
            val classes = all.filter { inPeriod(it.day) }
            EarningsView(
                summarize(classes, received.filter { inPeriod(it.day) }, salaries, months, left, filter),
                trend(period.lastMonth, all, received, salaries, filter, today),
                sources(classes, salaries, months, filter),
                classes.filter { filter.matches(it.type) },
                received.filter { inPeriod(it.day) && filter.matches(it.type) },
            )
        }
    }
}
