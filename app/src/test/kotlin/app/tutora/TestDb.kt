package app.tutora

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import app.tutora.data.openTutoraDatabase
import app.tutora.usecases.AutoStartAlarm
import app.tutora.usecases.Catalog
import app.tutora.usecases.Ctx
import app.tutora.usecases.Lessons
import app.tutora.usecases.Live
import app.tutora.usecases.LiveNotifier
import app.tutora.usecases.Schedule
import java.io.File

/** A migrated database in a temp folder, a frozen clock and zone, and counting ids. */
class TestDb(
    dir: File,
    var now: String = "2026-10-26T12:00:00.000Z", // Monday 26 Oct 2026, 09:00 in São Paulo
    val zone: String = "America/Sao_Paulo",
    val notifier: LiveNotifier = LiveNotifier.None,
    val alarm: AutoStartAlarm = AutoStartAlarm.None,
) {
    private var n = 0
    val db = openTutoraDatabase(ApplicationProvider.getApplicationContext(), File(dir, "tutora.db"), AndroidSQLiteDriver()).first
    val ctx = Ctx(db, { now }, { "id-" + (++n).toString().padStart(5, '0') }, { zone }, notifier, alarm)
    val catalog = Catalog(ctx)
    val schedule = Schedule(ctx)
    val lessons = Lessons(ctx)
    val live = Live(ctx)
}
