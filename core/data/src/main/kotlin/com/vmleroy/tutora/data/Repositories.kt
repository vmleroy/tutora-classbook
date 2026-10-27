package com.vmleroy.tutora.data

import com.vmleroy.tutora.domain.Group
import com.vmleroy.tutora.domain.Lesson
import com.vmleroy.tutora.domain.Place
import com.vmleroy.tutora.domain.PlaceLink
import com.vmleroy.tutora.domain.Repeat
import com.vmleroy.tutora.domain.Student
import com.vmleroy.tutora.domain.Who
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

data class PlaceWithCount(
    val place: Place,
    val studentCount: Int,
)

data class StudentWithLinks(
    val student: Student,
    val links: List<PlaceLink>,
)

/** Reads of places, students and groups as Flows; Room re-emits after any write. */
class CatalogRepository(
    private val db: TutoraDatabase,
) {
    fun places(archived: Boolean = false): Flow<List<PlaceWithCount>> =
        db.places().observe(archived).map { rows -> rows.map { PlaceWithCount(it.place.toPlace(), it.studentCount) } }

    fun place(id: String): Flow<Place?> = db.places().observeOne(id).map { it?.toPlace() }

    fun students(archived: Boolean = false): Flow<List<StudentWithLinks>> =
        combine(db.students().observe(archived), db.students().observeVisibleLinks()) { students, links ->
            val byStudent = links.groupBy { it.studentId }
            students.map { s -> StudentWithLinks(s.toStudent(), byStudent[s.id].orEmpty().map { it.toLink() }) }
        }

    fun student(id: String): Flow<StudentWithLinks?> =
        combine(db.students().observeOne(id), db.students().observeVisibleLinks()) { s, links ->
            s?.let { row -> StudentWithLinks(row.toStudent(), links.filter { it.studentId == id }.map { it.toLink() }) }
        }

    fun studentsAt(placeId: String): Flow<List<Student>> = db.students().observeAtPlace(placeId).map { rows -> rows.map { it.toStudent() } }

    fun groups(archived: Boolean = false): Flow<List<Group>> =
        combine(db.groups().observe(archived), db.groups().observeMembers()) { groups, members ->
            val byGroup = members.groupBy({ it.groupId }, { it.studentId })
            groups.map { it.toGroup(byGroup[it.id].orEmpty()) }
        }

    suspend fun group(id: String): Group? = db.groups().get(id)?.let { it.toGroup(db.groups().members(id)) }
}

/** Reads of classes, each with who and where, ready to price. */
class ScheduleRepository(
    private val db: TutoraDatabase,
) {
    fun between(
        from: String,
        to: String,
    ): Flow<List<Lesson>> = db.schedule().observeBetween(from, to).map { rows -> rows.mapNotNull { assemble(it) } }

    fun firstAfter(now: String): Flow<Lesson?> = db.schedule().observeFirstAfter(now).map { it?.let { r -> assemble(r) } }

    fun lesson(id: String): Flow<Lesson?> = db.schedule().observeOne(id).map { it?.let { r -> assemble(r) } }

    suspend fun lastTravelFee(
        studentId: String,
        placeId: String,
    ): Long? = db.schedule().lastTravelFee(studentId, placeId)

    private suspend fun assemble(r: LessonWithRepeat): Lesson? {
        val l = r.lesson
        val place = db.places().get(l.placeId)?.toPlace() ?: return null
        val who =
            when {
                l.studentId != null -> {
                    val s = db.students().get(l.studentId)?.toStudent() ?: return null
                    val link =
                        db
                            .students()
                            .visibleLinks(s.id)
                            .firstOrNull { it.placeId == l.placeId }
                            ?.toLink()
                    Who.OfStudent(s, link)
                }
                l.groupId != null -> {
                    val g = db.groups().get(l.groupId) ?: return null
                    Who.OfGroup(g.toGroup(db.groups().members(g.id)))
                }
                else -> return null
            }
        return Lesson(
            id = l.id,
            ruleId = l.ruleId,
            occurrenceDate = l.occurrenceDate?.let(LocalDate::parse),
            repeat = r.repeat?.let(Repeat::valueOf),
            startAt = l.startAt,
            plannedMin = l.plannedMin,
            valueCents = l.valueCents,
            travelFeeCents = l.travelFeeCents,
            isDetached = l.isDetached,
            notes = l.notes,
            who = who,
            place = place,
        )
    }
}
