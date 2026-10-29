package app.tutora

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.adapters.export.Cell
import app.tutora.adapters.export.Xlsx
import app.tutora.data.BackupStore
import app.tutora.data.CatalogRepository
import app.tutora.data.EarningsRepository
import app.tutora.data.PaymentRepository
import app.tutora.domain.BackupEvery
import app.tutora.domain.BackupRead
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.Language
import app.tutora.domain.PayMethod
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.Period
import app.tutora.domain.PeriodKind
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.SheetRange
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.TypeFilter
import app.tutora.domain.backupSummary
import app.tutora.domain.readBackup
import app.tutora.ui.backup.SheetWords
import app.tutora.ui.backup.backupSheets
import app.tutora.ui.backup.sheetFileName
import app.tutora.usecases.Backups
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.PaymentDraft
import app.tutora.usecases.Payments
import app.tutora.usecases.Result
import app.tutora.usecases.WhoId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

// Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
class BackupTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var t: TestDb
    private lateinit var backups: Backups
    private lateinit var ana: Student
    private val online = BuiltinPlace.ONLINE.id
    private val folder = "content://tree/backups"

    @Before
    fun setUp() =
        runTest {
            t = TestDb(tmp.root)
            backups = Backups(t.ctx)
            t.catalog.ensureBuiltinPlaces()
            BackupStore(t.db).ensureDefaults(t.now)
            ana = student("Ana Souza")
        }

    private suspend fun student(name: String) =
        (
            t.catalog.saveStudent(
                Student("", name, null, null, null, TagColor.AMBER, RateType.HOUR, null),
                listOf(PlaceLink(online, true, 12_000)),
            ) as Result.Ok
        ).value

    private suspend fun given(
        day: Int,
        who: Student = ana,
    ) = t.lessons.logLesson(
        LessonDraft(WhoId.OfStudent(who.id), online, LocalDate(2026, 10, day), LocalTime(8, 0), 60, null, null, null),
        null,
    )

    private suspend fun pay(cents: Long) =
        Payments(t.ctx).record(PaymentDraft(PayerRef(PayerKind.STUDENT, ana.id), cents, PayMethod.PIX, LocalDate(2026, 10, 26)))

    private suspend fun names() =
        CatalogRepository(t.db)
            .students()
            .first()
            .map { it.student.name }
            .sorted()

    private suspend fun backupFile() = (backups.inspect(backups.makeBackup().second) as BackupRead.Ok).file

    @Test
    fun `AC-011-2 a backup holds every table and is named with the day and time`() =
        runTest {
            given(22)
            pay(12_000)
            val (name, bytes) = backups.makeBackup()
            assertEquals("Tutora backup 2026-10-26 09-00.json", name)
            val file = (readBackup(bytes, 5) as BackupRead.Ok).file
            val every =
                listOf(
                    "settings",
                    "place",
                    "student",
                    "student_place",
                    "group_class",
                    "group_member",
                    "schedule_rule",
                    "rule_skip",
                    "lesson",
                    "lesson_timer",
                    "package",
                    "payment",
                    "payment_lesson",
                    "credit",
                )
            assertTrue(file.tables.keys.containsAll(every))
            assertEquals(1, file.tables.getValue("lesson").size)
            assertEquals(1, file.tables.getValue("payment").size)
        }

    @Test
    fun `AC-011-4 making a backup changes only the date of the last backup`() =
        runTest {
            given(22)
            val before = BackupStore(t.db).dump()
            backups.makeBackup()
            assertEquals(before, BackupStore(t.db).dump())
            assertEquals(t.now, backups.state().lastBackupAt)
        }

    @Test
    fun `AC-011-7 the folder keeps the newest N backups, and files Tutora didn't write stay`() =
        runTest {
            t.folder.files["minhas notas.txt"] = ByteArray(1)
            backups.setAuto(true, BackupEvery.DAY, 2, folder, "Tutora backups")
            listOf("2026-10-26T23:00:00.000Z", "2026-10-27T23:00:00.000Z", "2026-10-28T23:00:00.000Z").forEach {
                t.now = it
                backups.runAuto()
            }
            assertEquals(
                setOf("minhas notas.txt", "Tutora backup 2026-10-27 20-00.json", "Tutora backup 2026-10-28 20-00.json"),
                t.folder.files.keys,
            )
        }

    @Test
    fun `AC-011-8 a run missed while the phone was off happens on the next open, once`() =
        runTest {
            backups.setAuto(true, BackupEvery.WEEK, 5, folder, "Tutora backups")
            t.now = "2026-11-16T12:00:00.000Z" // three Sundays later
            backups.runIfDue()
            backups.runIfDue()
            assertEquals(1, t.folder.files.size)
            assertEquals("2026-11-22T23:00:00.000Z", t.scheduler.planned)
        }

    @Test
    fun `AC-011-9 a folder that can't be written fails the run without losing anything, and says so`() =
        runTest {
            given(22)
            backups.setAuto(true, BackupEvery.DAY, 5, folder, "Tutora backups")
            t.folder.failing = true
            backups.runAuto()
            val s = backups.state()
            assertTrue(s.failed)
            assertFalse(s.log.first().ok)
            assertEquals(1, BackupStore(t.db).dump().getValue("lesson").size)
        }

    @Test
    fun `AC-011-13 the confirmation says what the backup holds`() =
        runTest {
            given(22)
            pay(12_000)
            val sum = backupSummary(backupFile())
            assertEquals(listOf(1, 1, 1), listOf(sum.students, sum.lessons, sum.payments))
            assertEquals(t.now, sum.exportedAt)
        }

    @Test
    fun `AC-011-14 restoring replaces all the data at once, and a failed restore changes nothing`() =
        runTest {
            given(22)
            val file = backupFile()
            student("Bia Lima")
            backups.restore(file)
            assertEquals(listOf("Ana Souza"), names())
            // Classes whose student the file lacks: the whole restore is refused.
            val broken = file.copy(tables = file.tables + ("student" to emptyList()))
            val before = BackupStore(t.db).dump()
            assertTrue(runCatching { backups.restore(broken) }.isFailure)
            assertEquals(before, BackupStore(t.db).dump())
        }

    @Test
    fun `AC-011-15 every database version has its backup step, so any older backup reaches this one`() {
        val oldest = app.tutora.domain.BackupFile("tutora", 1, "", emptyMap())
        assertEquals(
            app.tutora.data.TutoraDatabase.VERSION,
            app.tutora.domain
                .upgradeBackup(oldest, app.tutora.data.TutoraDatabase.VERSION)
                .schemaVersion,
        )
    }

    @Test
    fun `AC-011-16 Desfazer restauracao brings back what was there before`() =
        runTest {
            val file = backupFile()
            student("Bia Lima")
            backups.restore(file)
            assertTrue(backups.state().restoreUndo)
            backups.undoRestore()
            assertEquals(listOf("Ana Souza", "Bia Lima"), names())
            assertFalse(backups.state().restoreUndo)
        }

    @Test
    fun `AC-011-17 a restore keeps this phone's own backup setup`() =
        runTest {
            backups.setAuto(true, BackupEvery.DAY, 3, "content://tree/other-phone", "Outro celular")
            val file = backupFile()
            backups.setAuto(true, BackupEvery.MONTH, 4, folder, "Tutora backups")
            backups.restore(file)
            val s = backups.state()
            assertEquals(listOf(folder, "Tutora backups"), listOf(s.folderUri, s.folderName))
            assertEquals(BackupEvery.MONTH to 4, s.every to s.keep)
        }

    @Test
    fun `AC-011-18 after a restore every balance and earning is as it was`() =
        runTest {
            given(19)
            given(22)
            pay(5_000)
            val ref = PayerRef(PayerKind.STUDENT, ana.id)
            val october = Period(PeriodKind.MONTH, LocalDate(2026, 10, 1))
            val today = LocalDate(2026, 10, 26)

            suspend fun numbers() =
                PaymentRepository(t.db).balances().first()[ref] to
                    EarningsRepository(t.db).view(october, TypeFilter.ALL, today, t.zone).first().summary
            val before = numbers()
            val file = backupFile()
            pay(19_000)
            backups.restore(file)
            assertEquals(before, numbers())
        }

    private val words =
        SheetWords(
            "Classes",
            "Payments",
            "Students",
            "Places",
            "Summary",
            "Date",
            "Duration",
            "Who",
            "Place",
            "Status",
            "Value",
            "Method",
            "Amount",
            "Name",
            "Subject",
            "Phone",
            "Archived",
            "Yes",
            "Type",
            "Month",
            "Earned",
            "Received",
            "To receive",
            status = { it },
            methodName = { it.name },
            kindName = { it.name },
            builtinName = { it.name },
            monthName = { it.toString() },
        )

    @Test
    fun `AC-011-19 the spreadsheet has Aulas, Pagamentos, Alunos, Locais and Resumo, with amounts as numbers`() =
        runTest {
            given(22)
            pay(12_000)
            val data = backups.sheetData(SheetRange.ThisMonth)
            val sheets = backupSheets(data, words)
            assertEquals(listOf("Classes", "Payments", "Students", "Places", "Summary"), sheets.map { it.name })
            assertEquals(Cell.Money(12_000), sheets[1].rows.single().last())
            assertEquals(listOf(Cell.Text("2026-10-01"), Cell.Money(12_000), Cell.Money(12_000), Cell.Money(0)), sheets[4].rows.single())
            assertEquals("Tutora planilha outubro 2026.xlsx", sheetFileName("Tutora planilha", data.from, data.to, Language.PT_BR))
            val all = backups.sheetData(SheetRange.Custom(LocalDate(2026, 9, 1), LocalDate(2026, 10, 26)))
            assertEquals(2, all.months.size)
        }

    @Test
    fun `AC-011-20 a spreadsheet picked in Restaurar backup is refused`() =
        runTest {
            given(22)
            val xlsx = Xlsx.write(backupSheets(backups.sheetData(SheetRange.All), words))
            assertEquals(BackupRead.NotTutora, backups.inspect(xlsx))
        }
}
