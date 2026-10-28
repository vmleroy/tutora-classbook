package app.tutora.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Tables from specs 004 and 005 (architecture doc, data model). Ids are UUIDv7 text, money is
// integer cents, instants are UTC ISO text, local dates are YYYY-MM-DD.

@Entity(tableName = "place", indices = [Index(value = ["builtin"], unique = true)])
data class PlaceRow(
    @PrimaryKey val id: String,
    val builtin: String?,
    val name: String?,
    val address: String?,
    val kind: String,
    val payer: String,
    @ColumnInfo(name = "rate_type") val rateType: String,
    @ColumnInfo(name = "rate_cents") val rateCents: Long?,
    @ColumnInfo(name = "commission_bp") val commissionBp: Int,
    @ColumnInfo(name = "pay_cycle") val payCycle: String,
    @ColumnInfo(name = "monthly_salary_cents") val monthlySalaryCents: Long?,
    @ColumnInfo(name = "default_duration_min") val defaultDurationMin: Int,
    @ColumnInfo(name = "travel_mode") val travelMode: String,
    @ColumnInfo(name = "travel_fee_cents") val travelFeeCents: Long,
    @ColumnInfo(name = "archived_at") val archivedAt: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)

@Entity(tableName = "student")
data class StudentRow(
    @PrimaryKey val id: String,
    val name: String,
    val subject: String?,
    val phone: String?,
    val notes: String?,
    val color: String,
    @ColumnInfo(name = "rate_type") val rateType: String,
    @ColumnInfo(name = "archived_at") val archivedAt: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "package_lessons") val packageLessons: Int? = null,
    @ColumnInfo(name = "package_price_cents") val packagePriceCents: Long? = null,
)

@Entity(
    tableName = "student_place",
    primaryKeys = ["student_id", "place_id"],
    foreignKeys = [
        ForeignKey(StudentRow::class, ["id"], ["student_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PlaceRow::class, ["id"], ["place_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("place_id")],
)
data class LinkRow(
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "place_id") val placeId: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    @ColumnInfo(name = "rate_cents") val rateCents: Long?,
    @ColumnInfo(name = "created_at") val createdAt: String,
)

@Entity(
    tableName = "group_class",
    foreignKeys = [ForeignKey(PlaceRow::class, ["id"], ["place_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("place_id")],
)
data class GroupRow(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "place_id") val placeId: String,
    val name: String,
    val subject: String?,
    @ColumnInfo(name = "rate_cents") val rateCents: Long?,
    @ColumnInfo(name = "archived_at") val archivedAt: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)

@Entity(
    tableName = "group_member",
    primaryKeys = ["group_id", "student_id"],
    foreignKeys = [
        ForeignKey(GroupRow::class, ["id"], ["group_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(StudentRow::class, ["id"], ["student_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("student_id")],
)
data class MemberRow(
    @ColumnInfo(name = "group_id") val groupId: String,
    @ColumnInfo(name = "student_id") val studentId: String,
)

/** A repeating class: wall-clock time in its own zone, filled 8 weeks ahead (spec 005). */
@Entity(
    tableName = "schedule_rule",
    foreignKeys = [
        ForeignKey(StudentRow::class, ["id"], ["student_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(GroupRow::class, ["id"], ["group_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PlaceRow::class, ["id"], ["place_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("student_id"), Index("group_id"), Index("place_id")],
)
data class RuleRow(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "student_id") val studentId: String?,
    @ColumnInfo(name = "group_id") val groupId: String?,
    @ColumnInfo(name = "place_id") val placeId: String,
    @ColumnInfo(name = "start_time") val startTime: String,
    @ColumnInfo(name = "duration_min") val durationMin: Int,
    val repeat: String,
    @ColumnInfo(name = "starts_on") val startsOn: String,
    @ColumnInfo(name = "ends_on") val endsOn: String?,
    @ColumnInfo(name = "time_zone") val timeZone: String,
    @ColumnInfo(name = "value_cents") val valueCents: Long?,
    @ColumnInfo(name = "travel_fee_cents") val travelFeeCents: Long?,
    @ColumnInfo(name = "generated_until") val generatedUntil: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
)

/** A date removed from a series with "Só esta aula", so filling never brings it back. */
@Entity(
    tableName = "rule_skip",
    primaryKeys = ["rule_id", "occurrence_date"],
    foreignKeys = [ForeignKey(RuleRow::class, ["id"], ["rule_id"], onDelete = ForeignKey.CASCADE)],
)
data class SkipRow(
    @ColumnInfo(name = "rule_id") val ruleId: String,
    @ColumnInfo(name = "occurrence_date") val occurrenceDate: String,
)

/**
 * A class. While scheduled it stores no price: its value follows the current price. Marked done,
 * it freezes the price in `rate_cents` / `rate_type` and its travel; cancelled or missed, it keeps
 * what happened, why and its charge (spec 006). Once done or charged it also freezes who owes it,
 * what it owes and the package it used (spec 008).
 */
@Entity(
    tableName = "lesson",
    foreignKeys = [
        ForeignKey(RuleRow::class, ["id"], ["rule_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(StudentRow::class, ["id"], ["student_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(GroupRow::class, ["id"], ["group_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PlaceRow::class, ["id"], ["place_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PackageRow::class, ["id"], ["package_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        // Filling a series is idempotent: one class per series date (AC-005-8).
        Index(value = ["rule_id", "occurrence_date"], unique = true),
        Index("start_at"),
        Index(value = ["student_id", "start_at"]),
        Index(value = ["place_id", "start_at"]),
        Index("group_id"),
        Index("payer"),
        Index("package_id"),
    ],
)
data class LessonRow(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "rule_id") val ruleId: String?,
    @ColumnInfo(name = "occurrence_date") val occurrenceDate: String?,
    @ColumnInfo(name = "student_id") val studentId: String?,
    @ColumnInfo(name = "group_id") val groupId: String?,
    @ColumnInfo(name = "place_id") val placeId: String,
    @ColumnInfo(name = "start_at") val startAt: String,
    @ColumnInfo(name = "planned_min") val plannedMin: Int,
    val status: String = "scheduled",
    @ColumnInfo(name = "value_cents") val valueCents: Long?,
    @ColumnInfo(name = "travel_fee_cents") val travelFeeCents: Long?,
    @ColumnInfo(name = "is_detached") val isDetached: Boolean = false,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "rate_cents") val rateCents: Long? = null,
    @ColumnInfo(name = "rate_type") val rateType: String? = null,
    @ColumnInfo(name = "fee_cents") val feeCents: Long? = null,
    @ColumnInfo(name = "cancel_reason") val cancelReason: String? = null,
    @ColumnInfo(name = "cancel_note") val cancelNote: String? = null,
    /** `student`, `group` or `place`, set once the class is done, cancelled or missed. */
    val payer: String? = null,
    /** What the class owes its payer; null when it owes nothing. */
    @ColumnInfo(name = "due_cents") val dueCents: Long? = null,
    @ColumnInfo(name = "package_id") val packageId: String? = null,
)

/** The clock of a live class (spec 007): timestamps only, so nothing is lost if the app closes. */
@Entity(
    tableName = "lesson_timer",
    foreignKeys = [ForeignKey(LessonRow::class, ["id"], ["lesson_id"], onDelete = ForeignKey.CASCADE)],
)
data class TimerRow(
    @PrimaryKey @ColumnInfo(name = "lesson_id") val lessonId: String,
    @ColumnInfo(name = "started_at") val startedAt: String,
    @ColumnInfo(name = "paused_ms") val pausedMs: Long,
    @ColumnInfo(name = "paused_at") val pausedAt: String?,
    val note: String?,
    /** The lesson's notes before ending added the quick note, so Desfazer can put them back. */
    @ColumnInfo(name = "notes_before") val notesBefore: String?,
)

/** A prepaid package of classes (spec 008). The classes used are those whose `package_id` is it. */
@Entity(
    tableName = "package",
    foreignKeys = [ForeignKey(StudentRow::class, ["id"], ["student_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["student_id", "bought_at"])],
)
data class PackageRow(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "lessons_total") val lessonsTotal: Int,
    @ColumnInfo(name = "price_cents") val priceCents: Long,
    @ColumnInfo(name = "bought_at") val boughtAt: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)

/**
 * Money received from a student, a group or a place: exactly one of the three is set, which
 * the use cases check. A package's purchase has `package_id` and pays no class.
 */
@Entity(
    tableName = "payment",
    foreignKeys = [
        ForeignKey(StudentRow::class, ["id"], ["student_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(GroupRow::class, ["id"], ["group_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PlaceRow::class, ["id"], ["place_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PackageRow::class, ["id"], ["package_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("student_id"), Index("group_id"), Index("place_id"), Index("package_id")],
)
data class PaymentRow(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "student_id") val studentId: String?,
    @ColumnInfo(name = "group_id") val groupId: String?,
    @ColumnInfo(name = "place_id") val placeId: String?,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val method: String,
    @ColumnInfo(name = "paid_at") val paidAt: String,
    val note: String?,
    @ColumnInfo(name = "package_id") val packageId: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)

/** A class ticked for a payment in "Escolher aulas", which that payment pays first. */
@Entity(
    tableName = "payment_lesson",
    primaryKeys = ["payment_id", "lesson_id"],
    foreignKeys = [
        ForeignKey(PaymentRow::class, ["id"], ["payment_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(LessonRow::class, ["id"], ["lesson_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("lesson_id")],
)
data class PaymentLessonRow(
    @ColumnInfo(name = "payment_id") val paymentId: String,
    @ColumnInfo(name = "lesson_id") val lessonId: String,
)
