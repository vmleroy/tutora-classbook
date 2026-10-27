package com.vmleroy.tutora.data

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

/** A class. While scheduled it stores no price: its value follows the current price. */
@Entity(
    tableName = "lesson",
    foreignKeys = [
        ForeignKey(RuleRow::class, ["id"], ["rule_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(StudentRow::class, ["id"], ["student_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(GroupRow::class, ["id"], ["group_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PlaceRow::class, ["id"], ["place_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        // Filling a series is idempotent: one class per series date (AC-005-8).
        Index(value = ["rule_id", "occurrence_date"], unique = true),
        Index("start_at"),
        Index(value = ["student_id", "start_at"]),
        Index(value = ["place_id", "start_at"]),
        Index("group_id"),
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
)
