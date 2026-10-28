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
        RuleRow::class, SkipRow::class, LessonRow::class, TimerRow::class,
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
        const val VERSION = 3

        /** One entry per version step, in order. Each has a test from the previous schema. */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}

/** Spec 006: a done class freezes its price; a cancelled or missed one keeps what happened, why and its charge. */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            listOf(
                "ALTER TABLE lesson ADD COLUMN rate_cents INTEGER",
                "ALTER TABLE lesson ADD COLUMN rate_type TEXT",
                "ALTER TABLE lesson ADD COLUMN fee_cents INTEGER",
                "ALTER TABLE lesson ADD COLUMN cancel_reason TEXT",
                "ALTER TABLE lesson ADD COLUMN cancel_note TEXT",
            ).forEach { connection.execSQL(it) }
        }
    }

/** Spec 007: the clock of a live class. */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `lesson_timer` (`lesson_id` TEXT NOT NULL, `started_at` TEXT NOT NULL, " +
                    "`paused_ms` INTEGER NOT NULL, `paused_at` TEXT, `note` TEXT, `notes_before` TEXT, PRIMARY KEY(`lesson_id`), " +
                    "FOREIGN KEY(`lesson_id`) REFERENCES `lesson`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
        }
    }
