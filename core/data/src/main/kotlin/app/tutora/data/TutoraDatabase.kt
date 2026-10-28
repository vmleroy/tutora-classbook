package app.tutora.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow

/** App settings as key → JSON value (product doc, data model). */
@Entity(tableName = "settings")
data class SettingRow(
    @PrimaryKey val key: String,
    val value: String,
    @androidx.room.ColumnInfo(name = "updated_at") val updatedAt: String,
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings")
    fun observeAll(): Flow<List<SettingRow>>

    @Query("SELECT * FROM settings")
    suspend fun all(): List<SettingRow>

    /** Inserts rows that are missing; saved values are never touched. */
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertMissing(rows: List<SettingRow>)

    @Upsert
    suspend fun upsert(rows: List<SettingRow>)
}

@Database(
    entities = [
        SettingRow::class, PlaceRow::class, StudentRow::class, LinkRow::class, GroupRow::class, MemberRow::class,
        RuleRow::class, SkipRow::class, LessonRow::class,
    ],
    version = TutoraDatabase.VERSION,
    exportSchema = true,
)
abstract class TutoraDatabase : RoomDatabase() {
    abstract fun settings(): SettingsDao

    abstract fun places(): PlaceDao

    abstract fun students(): StudentDao

    abstract fun groups(): GroupDao

    abstract fun schedule(): ScheduleDao

    companion object {
        /** The schema version this build expects; Ajustes › Sobre shows it. */
        const val VERSION = 2

        /** One entry per version step, in order. Each has a test from the previous schema. */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}

/** Spec 006: a done class freezes its price, cancellations keep a fee, and fee rules per student and place. */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            listOf(
                "ALTER TABLE lesson ADD COLUMN rate_cents INTEGER",
                "ALTER TABLE lesson ADD COLUMN rate_type TEXT",
                "ALTER TABLE lesson ADD COLUMN fee_cents INTEGER",
                "ALTER TABLE lesson ADD COLUMN fee_waived INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE lesson ADD COLUMN notice_at TEXT",
                "ALTER TABLE student ADD COLUMN fee_rule TEXT",
                "ALTER TABLE place ADD COLUMN fee_rule TEXT",
            ).forEach { connection.execSQL(it) }
        }
    }
