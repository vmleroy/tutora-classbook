package app.tutora.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class PlaceCountRow(
    @androidx.room.Embedded val place: PlaceRow,
    @androidx.room.ColumnInfo(name = "student_count") val studentCount: Int,
)

@Dao
interface PlaceDao {
    // Casa do Aluno, then Online / Remoto (as in the mockup), then the teacher's by name.
    @Query(
        """SELECT place.*, (SELECT count(*) FROM student_place sp JOIN student s ON s.id = sp.student_id
             WHERE sp.place_id = place.id AND s.archived_at IS NULL) AS student_count
           FROM place WHERE (archived_at IS NOT NULL) = :archived
           ORDER BY CASE builtin WHEN 'STUDENT_HOME' THEN 0 WHEN 'ONLINE' THEN 1 ELSE 2 END, lower(name)""",
    )
    fun observe(archived: Boolean): Flow<List<PlaceCountRow>>

    @Query("SELECT * FROM place WHERE id = :id")
    suspend fun get(id: String): PlaceRow?

    @Query("SELECT * FROM place WHERE id = :id")
    fun observeOne(id: String): Flow<PlaceRow?>

    @Query("SELECT * FROM place")
    suspend fun all(): List<PlaceRow>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(row: PlaceRow)

    @Upsert
    suspend fun upsert(row: PlaceRow)

    @Query("UPDATE place SET archived_at = :archivedAt, updated_at = :now WHERE id = :id")
    suspend fun setArchived(
        id: String,
        archivedAt: String?,
        now: String,
    )

    @Query("DELETE FROM place WHERE id = :id AND builtin IS NULL")
    suspend fun delete(id: String)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM student WHERE (archived_at IS NOT NULL) = :archived ORDER BY lower(name)")
    fun observe(archived: Boolean): Flow<List<StudentRow>>

    @Query("SELECT * FROM student WHERE id = :id")
    suspend fun get(id: String): StudentRow?

    @Query("SELECT * FROM student WHERE id = :id")
    fun observeOne(id: String): Flow<StudentRow?>

    @Upsert
    suspend fun upsert(row: StudentRow)

    @Query("UPDATE student SET archived_at = :archivedAt, updated_at = :now WHERE id = :id")
    suspend fun setArchived(
        id: String,
        archivedAt: String?,
        now: String,
    )

    @Query("DELETE FROM student WHERE id = :id")
    suspend fun delete(id: String)

    /** Links to places that are not archived; an archived place's links stay hidden (AC-004-25). */
    @Query(
        """SELECT sp.* FROM student_place sp JOIN place p ON p.id = sp.place_id
           WHERE p.archived_at IS NULL ORDER BY sp.is_default DESC, sp.created_at""",
    )
    fun observeVisibleLinks(): Flow<List<LinkRow>>

    @Query(
        """SELECT sp.* FROM student_place sp JOIN place p ON p.id = sp.place_id
           WHERE sp.student_id = :studentId AND p.archived_at IS NULL ORDER BY sp.is_default DESC, sp.created_at""",
    )
    suspend fun visibleLinks(studentId: String): List<LinkRow>

    @Query(
        """SELECT sp.* FROM student_place sp JOIN place p ON p.id = sp.place_id
           WHERE sp.student_id = :studentId AND p.archived_at IS NOT NULL""",
    )
    suspend fun hiddenLinks(studentId: String): List<LinkRow>

    @Query("DELETE FROM student_place WHERE student_id = :studentId")
    suspend fun deleteLinks(studentId: String)

    @Query("DELETE FROM student_place WHERE student_id = :studentId AND place_id = :placeId")
    suspend fun deleteLink(
        studentId: String,
        placeId: String,
    )

    /** Stars [placeId] and unstars the student's other places. */
    @Query("UPDATE student_place SET is_default = (place_id = :placeId) WHERE student_id = :studentId")
    suspend fun setDefault(
        studentId: String,
        placeId: String,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(rows: List<LinkRow>)

    @Query(
        """SELECT s.* FROM student s JOIN student_place sp ON sp.student_id = s.id
           WHERE sp.place_id = :placeId AND s.archived_at IS NULL ORDER BY lower(s.name)""",
    )
    fun observeAtPlace(placeId: String): Flow<List<StudentRow>>

    @Query(
        """SELECT s.* FROM student s JOIN student_place sp ON sp.student_id = s.id
           WHERE sp.place_id = :placeId AND s.archived_at IS NULL""",
    )
    suspend fun atPlace(placeId: String): List<StudentRow>
}

@Dao
interface GroupDao {
    @Query(
        """SELECT g.* FROM group_class g JOIN place p ON p.id = g.place_id
           WHERE (g.archived_at IS NOT NULL) = :archived AND p.archived_at IS NULL ORDER BY lower(g.name)""",
    )
    fun observe(archived: Boolean): Flow<List<GroupRow>>

    @Query("SELECT * FROM group_class WHERE id = :id")
    suspend fun get(id: String): GroupRow?

    @Query("SELECT * FROM group_class WHERE id = :id")
    fun observeOne(id: String): Flow<GroupRow?>

    @Query("SELECT * FROM group_member")
    fun observeMembers(): Flow<List<MemberRow>>

    @Query("SELECT student_id FROM group_member WHERE group_id = :groupId")
    suspend fun members(groupId: String): List<String>

    @Upsert
    suspend fun upsert(row: GroupRow)

    @Query("DELETE FROM group_member WHERE group_id = :groupId")
    suspend fun deleteMembers(groupId: String)

    @Query("DELETE FROM group_member WHERE group_id = :groupId AND student_id = :studentId")
    suspend fun deleteMember(
        groupId: String,
        studentId: String,
    )

    /** Takes a student out of every group at a place (AC-004-36). */
    @Query("DELETE FROM group_member WHERE student_id = :studentId AND group_id IN (SELECT id FROM group_class WHERE place_id = :placeId)")
    suspend fun leaveGroupsAt(
        studentId: String,
        placeId: String,
    )

    @Query("UPDATE group_class SET place_id = :placeId, updated_at = :now WHERE id = :groupId")
    suspend fun setPlace(
        groupId: String,
        placeId: String,
        now: String,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMembers(rows: List<MemberRow>)

    @Query("UPDATE group_class SET archived_at = :archivedAt, updated_at = :now WHERE id = :id")
    suspend fun setArchived(
        id: String,
        archivedAt: String?,
        now: String,
    )

    @Query("DELETE FROM group_class WHERE id = :id")
    suspend fun delete(id: String)
}

data class LessonWithRepeat(
    @androidx.room.Embedded val lesson: LessonRow,
    val repeat: String?,
)

@Dao
interface ScheduleDao {
    @Insert
    suspend fun insertRule(row: RuleRow)

    @Query("SELECT * FROM schedule_rule WHERE id = :id")
    suspend fun rule(id: String): RuleRow?

    @Query("UPDATE schedule_rule SET ends_on = :endsOn WHERE id = :id")
    suspend fun endRule(
        id: String,
        endsOn: String,
    )

    @Query("UPDATE schedule_rule SET generated_until = :until WHERE id = :id")
    suspend fun setGeneratedUntil(
        id: String,
        until: String,
    )

    @Query("SELECT * FROM schedule_rule WHERE ends_on IS NULL OR ends_on >= :today")
    suspend fun activeRules(today: String): List<RuleRow>

    /** A group that moves takes its series and its classes still ahead with it (AC-004-35). */
    @Query("UPDATE schedule_rule SET place_id = :placeId WHERE group_id = :groupId")
    suspend fun moveGroupRules(
        groupId: String,
        placeId: String,
    )

    @Query(
        "UPDATE lesson SET place_id = :placeId, updated_at = :now WHERE group_id = :groupId AND status = 'scheduled' AND start_at > :now",
    )
    suspend fun moveGroupLessons(
        groupId: String,
        placeId: String,
        now: String,
    )

    @Query("SELECT occurrence_date FROM rule_skip WHERE rule_id = :ruleId")
    suspend fun skips(ruleId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSkip(row: SkipRow)

    /** One class per series date: an existing one is left alone (AC-005-8). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLessons(rows: List<LessonRow>)

    @Query("SELECT * FROM lesson WHERE id = :id")
    suspend fun lesson(id: String): LessonRow?

    @Query("SELECT * FROM lesson WHERE rule_id = :ruleId AND occurrence_date = :date")
    suspend fun lessonAt(
        ruleId: String,
        date: String,
    ): LessonRow?

    @Upsert
    suspend fun upsertLesson(row: LessonRow)

    @Query("DELETE FROM lesson WHERE id = :id")
    suspend fun deleteLesson(id: String)

    @Query(
        """DELETE FROM lesson WHERE rule_id = :ruleId AND occurrence_date >= :from AND start_at > :now
           AND (:detachedToo OR is_detached = 0)""",
    )
    suspend fun deleteSeriesFrom(
        ruleId: String,
        from: String,
        now: String,
        detachedToo: Boolean,
    )

    @Query(
        """SELECT travel_fee_cents FROM lesson WHERE student_id = :studentId AND place_id = :placeId
           AND travel_fee_cents IS NOT NULL ORDER BY start_at DESC LIMIT 1""",
    )
    suspend fun lastTravelFee(
        studentId: String,
        placeId: String,
    ): Long?

    // The joins make Room refresh these lists when a price, a student or a place changes
    // too, not only a lesson (AC-005-16).
    @Query(
        """SELECT lesson.*, r.repeat AS repeat FROM lesson
           LEFT JOIN schedule_rule r ON r.id = lesson.rule_id
           LEFT JOIN place p ON p.id = lesson.place_id
           LEFT JOIN student s ON s.id = lesson.student_id
           LEFT JOIN student_place sp ON sp.student_id = lesson.student_id AND sp.place_id = lesson.place_id
           LEFT JOIN group_class g ON g.id = lesson.group_id
           WHERE lesson.start_at >= :from AND lesson.start_at < :to ORDER BY lesson.start_at""",
    )
    fun observeBetween(
        from: String,
        to: String,
    ): Flow<List<LessonWithRepeat>>

    @Query(
        """SELECT lesson.*, r.repeat AS repeat FROM lesson
           LEFT JOIN schedule_rule r ON r.id = lesson.rule_id
           LEFT JOIN place p ON p.id = lesson.place_id
           LEFT JOIN student s ON s.id = lesson.student_id
           LEFT JOIN student_place sp ON sp.student_id = lesson.student_id AND sp.place_id = lesson.place_id
           WHERE lesson.start_at > :now AND lesson.status = 'scheduled' ORDER BY lesson.start_at LIMIT 1""",
    )
    fun observeFirstAfter(now: String): Flow<LessonWithRepeat?>

    @Query("SELECT lesson.*, r.repeat AS repeat FROM lesson LEFT JOIN schedule_rule r ON r.id = lesson.rule_id WHERE lesson.id = :id")
    fun observeOne(id: String): Flow<LessonWithRepeat?>

    // The live class and its clock (spec 007). Only one class is live at a time.

    @Query("SELECT * FROM lesson WHERE status = 'live' LIMIT 1")
    suspend fun liveLesson(): LessonRow?

    @Query(
        """SELECT lesson.*, r.repeat AS repeat FROM lesson LEFT JOIN schedule_rule r ON r.id = lesson.rule_id
           LEFT JOIN lesson_timer t ON t.lesson_id = lesson.id WHERE lesson.status = 'live' LIMIT 1""",
    )
    fun observeLive(): Flow<LessonWithRepeat?>

    @Query("SELECT * FROM lesson_timer WHERE lesson_id = :lessonId")
    suspend fun timer(lessonId: String): TimerRow?

    @Query("SELECT * FROM lesson_timer WHERE lesson_id = :lessonId")
    fun observeTimer(lessonId: String): Flow<TimerRow?>

    @Upsert
    suspend fun upsertTimer(row: TimerRow)

    @Query("DELETE FROM lesson_timer WHERE lesson_id = :lessonId")
    suspend fun deleteTimer(lessonId: String)

    /** Clocks left from classes that ended and weren't undone. */
    @Query("DELETE FROM lesson_timer WHERE lesson_id NOT IN (SELECT id FROM lesson WHERE status = 'live')")
    suspend fun deleteStaleTimers()
}
