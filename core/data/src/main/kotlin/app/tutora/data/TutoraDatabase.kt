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

/** What the backup page needs besides the dump (spec 011). */
@Dao
interface BackupDao {
    /** When the first class was added: the 14-day warning counts from it while no backup exists (AC-011-10). */
    @Query("SELECT MIN(created_at) FROM lesson")
    suspend fun firstLessonAt(): String?

    @Query("SELECT MIN(created_at) FROM lesson")
    fun observeFirstLessonAt(): Flow<String?>

    /** A class running now: no restore while one is (open question, Victor 29 Oct 2026). */
    @Query("SELECT COUNT(*) FROM lesson_timer")
    suspend fun liveCount(): Int
}

@Database(
    entities = [
        SettingRow::class, PlaceRow::class, StudentRow::class, LinkRow::class, GroupRow::class, MemberRow::class,
        RuleRow::class, SkipRow::class, LessonRow::class, TimerRow::class, PackageRow::class, PaymentRow::class,
        PaymentLessonRow::class, CreditRow::class,
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

    abstract fun payments(): PaymentDao

    abstract fun earnings(): EarningsDao

    abstract fun backup(): BackupDao

    companion object {
        /** The schema version this build expects; Ajustes › Sobre shows it. */
        const val VERSION = 5

        /** One entry per version step, in order. Each has a test from the previous schema. */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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

/** Spec 008: payments, packages, and who owes each class. Done classes get their payer at start-up. */
val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            listOf(
                "CREATE TABLE IF NOT EXISTS `package` (`id` TEXT NOT NULL, `student_id` TEXT NOT NULL, `lessons_total` INTEGER " +
                    "NOT NULL, `price_cents` INTEGER NOT NULL, `bought_at` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` " +
                    "TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`student_id`) REFERENCES `student`(`id`) ON UPDATE NO ACTION ON " +
                    "DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_package_student_id_bought_at` ON `package` (`student_id`, `bought_at`)",
                "CREATE TABLE IF NOT EXISTS `payment` (`id` TEXT NOT NULL, `student_id` TEXT, `group_id` TEXT, `place_id` " +
                    "TEXT, `amount_cents` INTEGER NOT NULL, `method` TEXT NOT NULL, `paid_at` TEXT NOT NULL, `note` TEXT, " +
                    "`package_id` TEXT, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN " +
                    "KEY(`student_id`) REFERENCES `student`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`group_id`) " +
                    "REFERENCES `group_class`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`place_id`) REFERENCES " +
                    "`place`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`package_id`) REFERENCES `package`(`id`) ON " +
                    "UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_payment_student_id` ON `payment` (`student_id`)",
                "CREATE INDEX IF NOT EXISTS `index_payment_group_id` ON `payment` (`group_id`)",
                "CREATE INDEX IF NOT EXISTS `index_payment_place_id` ON `payment` (`place_id`)",
                "CREATE INDEX IF NOT EXISTS `index_payment_package_id` ON `payment` (`package_id`)",
                "CREATE TABLE IF NOT EXISTS `payment_lesson` (`payment_id` TEXT NOT NULL, `lesson_id` TEXT NOT NULL, PRIMARY " +
                    "KEY(`payment_id`, `lesson_id`), FOREIGN KEY(`payment_id`) REFERENCES `payment`(`id`) ON UPDATE NO ACTION ON " +
                    "DELETE CASCADE , FOREIGN KEY(`lesson_id`) REFERENCES `lesson`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_payment_lesson_lesson_id` ON `payment_lesson` (`lesson_id`)",
                "ALTER TABLE student ADD COLUMN package_lessons INTEGER",
                "ALTER TABLE student ADD COLUMN package_price_cents INTEGER",
                "ALTER TABLE lesson ADD COLUMN payer TEXT",
                "ALTER TABLE lesson ADD COLUMN due_cents INTEGER",
                "ALTER TABLE lesson ADD COLUMN package_id TEXT REFERENCES package(id) ON UPDATE NO ACTION ON DELETE SET NULL",
                "CREATE INDEX IF NOT EXISTS `index_lesson_payer` ON `lesson` (`payer`)",
                "CREATE INDEX IF NOT EXISTS `index_lesson_package_id` ON `lesson` (`package_id`)",
            ).forEach { connection.execSQL(it) }
        }
    }

/** Spec 008, Créditos e bônus: bonuses and credit put on classes. */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            listOf(
                "CREATE TABLE IF NOT EXISTS `credit` (`id` TEXT NOT NULL, `student_id` TEXT, `group_id` TEXT, `place_id` TEXT, " +
                    "`kind` TEXT NOT NULL, `cents` INTEGER NOT NULL, `minutes` INTEGER NOT NULL, `lesson_id` TEXT, `note` TEXT, " +
                    "`created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`student_id`) " +
                    "REFERENCES `student`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`group_id`) REFERENCES " +
                    "`group_class`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`place_id`) REFERENCES `place`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`lesson_id`) REFERENCES `lesson`(`id`) ON UPDATE NO " +
                    "ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_credit_student_id` ON `credit` (`student_id`)",
                "CREATE INDEX IF NOT EXISTS `index_credit_group_id` ON `credit` (`group_id`)",
                "CREATE INDEX IF NOT EXISTS `index_credit_place_id` ON `credit` (`place_id`)",
                "CREATE INDEX IF NOT EXISTS `index_credit_lesson_id` ON `credit` (`lesson_id`)",
            ).forEach { connection.execSQL(it) }
        }
    }
