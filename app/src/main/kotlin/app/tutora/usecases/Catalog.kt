package app.tutora.usecases

import app.tutora.data.LinkRow
import app.tutora.data.MemberRow
import app.tutora.data.TutoraDatabase
import app.tutora.data.inTransaction
import app.tutora.data.toLink
import app.tutora.data.toPlace
import app.tutora.data.toRow
import app.tutora.data.toStudent
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.CatalogError
import app.tutora.domain.Group
import app.tutora.domain.Payer
import app.tutora.domain.Place
import app.tutora.domain.PlaceKind
import app.tutora.domain.PlaceLink
import app.tutora.domain.Student
import app.tutora.domain.builtinPlace
import app.tutora.domain.checkPlace
import app.tutora.domain.checkStudent
import app.tutora.domain.withDefault

/** The clock and id source every write uses, so tests can freeze both. */
class Ctx(
    val db: TutoraDatabase,
    val now: () -> String,
    val newId: () -> String,
    val zone: () -> String = {
        java.util.TimeZone
            .getDefault()
            .id
    },
)

sealed interface Result<out T> {
    data class Ok<T>(
        val value: T,
    ) : Result<T>

    data class Err(
        val error: CatalogError,
    ) : Result<Nothing>
}

enum class Kind { PLACE, STUDENT, GROUP }

/** Places, students and groups (spec 004). Every write runs in one transaction. */
class Catalog(
    private val ctx: Ctx,
) {
    private val db get() = ctx.db

    /** Adds the built-in places that are missing; saved ones are never touched. */
    suspend fun ensureBuiltinPlaces() {
        for (b in BuiltinPlace.entries) db.places().insertIgnore(builtinPlace(b).toRow(ctx.now(), ctx.now()))
    }

    /** Creates or updates a place. A built-in keeps its name and type (AC-004-2, 3). */
    suspend fun savePlace(draft: Place): Result<Place> =
        db.inTransaction {
            val beforeRow = db.places().get(draft.id)
            val before = beforeRow?.toPlace()
            val next =
                draft.copy(
                    name = draft.name?.trim()?.ifEmpty { null },
                    id = before?.id ?: draft.id.ifEmpty { ctx.newId() },
                    builtin = before?.builtin,
                    archivedAt = before?.archivedAt,
                )
            val error = checkPlace(next, before)
            if (error != null) return@inTransaction Result.Err(error)
            db.places().upsert(next.toRow(beforeRow?.createdAt ?: ctx.now(), ctx.now()))
            Result.Ok(next)
        }

    /**
     * Creates or updates a student with their places. Exactly one place is starred, and a place
     * that pays the teacher keeps no student price (AC-004-10 to 13).
     */
    suspend fun saveStudent(
        draft: Student,
        links: List<PlaceLink>,
    ): Result<Student> =
        db.inTransaction {
            val beforeRow = db.students().get(draft.id)
            val next =
                draft.copy(
                    name = draft.name.trim(),
                    id = beforeRow?.id ?: draft.id.ifEmpty { ctx.newId() },
                    archivedAt = beforeRow?.archivedAt,
                )
            val error = checkStudent(next, links)
            if (error != null) return@inTransaction Result.Err(error)
            val priced =
                withDefault(links).map { l ->
                    val place = db.places().get(l.placeId)?.toPlace()
                    if (place?.payer == Payer.PLACE || place?.kind == PlaceKind.SCHOOL) l.copy(rateCents = null) else l
                }
            db.students().upsert(next.toRow(beforeRow?.createdAt ?: ctx.now(), ctx.now()))
            // Links to archived places stay, hidden, until the place is restored (AC-004-25).
            val hidden = db.students().hiddenLinks(next.id)
            db.students().deleteLinks(next.id)
            db.students().insertLinks(
                priced.map { LinkRow(next.id, it.placeId, it.isDefault, it.rateCents, ctx.now()) } +
                    hidden.map { it.copy(isDefault = false) },
            )
            Result.Ok(next)
        }

    /** "+ Ligar aluno" on a place: links with the place's price; a first place gets the star. */
    suspend fun linkStudentToPlace(
        studentId: String,
        placeId: String,
    ) = db.inTransaction {
        val current = db.students().visibleLinks(studentId)
        db.students().insertLinks(listOf(LinkRow(studentId, placeId, current.isEmpty(), null, ctx.now())))
    }

    /** Members must be linked to the group's place; at a school the salary covers the price (AC-004-16). */
    suspend fun saveGroup(draft: Group): Result<Group> =
        db.inTransaction {
            if (draft.name.isBlank()) return@inTransaction Result.Err(CatalogError.NAME_REQUIRED)
            val beforeRow = db.groups().get(draft.id)
            val atPlace =
                db
                    .students()
                    .atPlace(draft.placeId)
                    .map { it.id }
                    .toSet()
            val school =
                db
                    .places()
                    .get(draft.placeId)
                    ?.toPlace()
                    ?.kind == PlaceKind.SCHOOL
            val next =
                draft.copy(
                    name = draft.name.trim(),
                    id = beforeRow?.id ?: draft.id.ifEmpty { ctx.newId() },
                    rateCents = if (school) null else draft.rateCents,
                    memberIds = draft.memberIds.filter { it in atPlace },
                    archivedAt = beforeRow?.archivedAt,
                )
            db.groups().upsert(next.toRow(beforeRow?.createdAt ?: ctx.now(), ctx.now()))
            db.groups().deleteMembers(next.id)
            db.groups().insertMembers(next.memberIds.map { MemberRow(next.id, it) })
            Result.Ok(next)
        }

    /** When the item was archived; null while it is active. Call [exists] first. */
    private suspend fun archivedAt(
        kind: Kind,
        id: String,
    ): String? =
        when (kind) {
            Kind.PLACE -> db.places().get(id)?.archivedAt
            Kind.STUDENT -> db.students().get(id)?.archivedAt
            Kind.GROUP -> db.groups().get(id)?.archivedAt
        }

    private suspend fun exists(
        kind: Kind,
        id: String,
    ) = when (kind) {
        Kind.PLACE -> db.places().get(id) != null
        Kind.STUDENT -> db.students().get(id) != null
        Kind.GROUP -> db.groups().get(id) != null
    }

    private suspend fun setArchived(
        kind: Kind,
        id: String,
        at: String?,
    ) = when (kind) {
        Kind.PLACE -> db.places().setArchived(id, at, ctx.now())
        Kind.STUDENT -> db.students().setArchived(id, at, ctx.now())
        Kind.GROUP -> db.groups().setArchived(id, at, ctx.now())
    }

    /** Arquivar hides an item and keeps everything about it. Built-in places stay (AC-004-23). */
    suspend fun archive(
        kind: Kind,
        id: String,
    ): Result<Unit> =
        db.inTransaction {
            if (!exists(kind, id)) return@inTransaction Result.Err(CatalogError.NOT_FOUND)
            if (kind == Kind.PLACE && db.places().get(id)?.builtin != null) return@inTransaction Result.Err(CatalogError.BUILTIN_FIXED)
            setArchived(kind, id, ctx.now())
            Result.Ok(Unit)
        }

    suspend fun restore(
        kind: Kind,
        id: String,
    ) = db.inTransaction { setArchived(kind, id, null) }

    /**
     * Removes an archived item for good, after the screen's confirmation. A place takes its
     * links and groups with it, never its students (AC-004-24).
     */
    suspend fun removePermanently(
        kind: Kind,
        id: String,
    ): Result<Unit> =
        db.inTransaction {
            if (!exists(kind, id)) return@inTransaction Result.Err(CatalogError.NOT_FOUND)
            if (archivedAt(kind, id) == null) return@inTransaction Result.Err(CatalogError.NOT_ARCHIVED)
            when (kind) {
                Kind.PLACE -> db.places().delete(id)
                Kind.STUDENT -> db.students().delete(id)
                Kind.GROUP -> db.groups().delete(id)
            }
            Result.Ok(Unit)
        }

    /** For screens that need a student's places, archived places' links excluded. */
    suspend fun visibleLinks(studentId: String): List<PlaceLink> = db.students().visibleLinks(studentId).map { it.toLink() }

    suspend fun student(id: String): Student? = db.students().get(id)?.toStudent()
}
