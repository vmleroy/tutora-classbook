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
    /** The live-class notification, which use cases update after their transaction (spec 007). */
    val live: LiveNotifier = LiveNotifier.None,
    /** The alarm that starts a class with the app closed (AC-007-22). */
    val alarm: AutoStartAlarm = AutoStartAlarm.None,
    /** The folder automatic backups go to, and what wakes the app for them (spec 011). */
    val backupFolder: BackupFolder = BackupFolder.None,
    val backupScheduler: BackupScheduler = BackupScheduler.None,
    /** The app's own storage, for the copy taken before a restore (AC-011-16). */
    val privateDir: java.io.File = java.io.File(System.getProperty("java.io.tmpdir"), "tutora"),
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

    /** Links with the place's price; a student's first place gets the star. Inside a transaction. */
    private suspend fun link(
        studentId: String,
        placeId: String,
    ) {
        val current = db.students().visibleLinks(studentId)
        if (current.any { it.placeId == placeId }) return
        db.students().insertLinks(listOf(LinkRow(studentId, placeId, current.isEmpty(), null, ctx.now())))
    }

    /** "+ Ligar aluno" on a place, for one student. */
    suspend fun linkStudentToPlace(
        studentId: String,
        placeId: String,
    ) = db.inTransaction { link(studentId, placeId) }

    /** "+ Adicionar" on a place's students or a student's places, several at once (AC-004-33, 34). */
    suspend fun linkStudents(links: List<Pair<String, String>>) =
        db.inTransaction {
            for ((studentId, placeId) in links) link(studentId, placeId)
        }

    /**
     * × on a student at a place: the student also leaves that place's groups, and a starred place
     * passes its star to the next one (AC-004-36).
     */
    suspend fun unlinkStudentFromPlace(
        studentId: String,
        placeId: String,
    ) = db.inTransaction {
        val wasDefault = db.students().visibleLinks(studentId).any { it.placeId == placeId && it.isDefault }
        db.students().deleteLink(studentId, placeId)
        db.groups().leaveGroupsAt(studentId, placeId)
        val next = db.students().visibleLinks(studentId).firstOrNull()
        if (wasDefault && next != null) db.students().setDefault(studentId, next.placeId)
    }

    /** Adds members to a group; a student not at its place is linked to it (AC-004-16, 33). */
    suspend fun addMembers(
        groupId: String,
        studentIds: List<String>,
    ) = db.inTransaction {
        val group = db.groups().get(groupId) ?: return@inTransaction
        for (s in studentIds) link(s, group.placeId)
        db.groups().insertMembers(studentIds.map { MemberRow(groupId, it) })
    }

    suspend fun removeMember(
        groupId: String,
        studentId: String,
    ) = db.inTransaction { db.groups().deleteMember(groupId, studentId) }

    /**
     * Moves a group to another place: its members are linked there, and its series and classes
     * still ahead move too; classes already given keep their place (AC-004-34, 35).
     */
    suspend fun moveGroup(
        groupId: String,
        placeId: String,
    ) = db.inTransaction {
        val group = db.groups().get(groupId) ?: return@inTransaction
        if (group.placeId == placeId) return@inTransaction
        db.groups().setPlace(groupId, placeId, ctx.now())
        for (s in db.groups().members(groupId)) link(s, placeId)
        db.schedule().moveGroupRules(groupId, placeId)
        db.schedule().moveGroupLessons(groupId, placeId, ctx.now())
    }

    /**
     * Saves a group's own fields. A new group needs its place and starts with [Group.memberIds]
     * (linked to that place); an existing one keeps its place and members, which change from its
     * detail. At a school the salary covers the price (AC-004-16, 37).
     */
    suspend fun saveGroup(draft: Group): Result<Group> =
        db.inTransaction {
            if (draft.name.isBlank()) return@inTransaction Result.Err(CatalogError.NAME_REQUIRED)
            val beforeRow = db.groups().get(draft.id)
            val placeId = beforeRow?.placeId ?: draft.placeId
            val place = db.places().get(placeId)?.toPlace() ?: return@inTransaction Result.Err(CatalogError.PLACE_REQUIRED)
            val next =
                draft.copy(
                    name = draft.name.trim(),
                    id = beforeRow?.id ?: draft.id.ifEmpty { ctx.newId() },
                    placeId = placeId,
                    rateCents = if (place.kind == PlaceKind.SCHOOL) null else draft.rateCents,
                    memberIds = if (beforeRow != null) db.groups().members(beforeRow.id) else draft.memberIds.distinct(),
                    archivedAt = beforeRow?.archivedAt,
                )
            db.groups().upsert(next.toRow(beforeRow?.createdAt ?: ctx.now(), ctx.now()))
            if (beforeRow == null) {
                for (m in next.memberIds) link(m, placeId)
                db.groups().insertMembers(next.memberIds.map { MemberRow(next.id, it) })
            }
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
