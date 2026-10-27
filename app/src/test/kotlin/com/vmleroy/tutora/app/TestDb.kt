package com.vmleroy.tutora.app

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.vmleroy.tutora.app.usecases.Catalog
import com.vmleroy.tutora.app.usecases.Ctx
import com.vmleroy.tutora.app.usecases.Schedule
import com.vmleroy.tutora.data.openTutoraDatabase
import java.io.File

/** A migrated database in a temp folder, a frozen clock and zone, and counting ids. */
class TestDb(
    dir: File,
    var now: String = "2026-10-26T12:00:00.000Z", // Monday 26 Oct 2026, 09:00 in São Paulo
    val zone: String = "America/Sao_Paulo",
) {
    private var n = 0
    val db = openTutoraDatabase(ApplicationProvider.getApplicationContext(), File(dir, "tutora.db"), AndroidSQLiteDriver()).first
    val ctx = Ctx(db, { now }, { "id-" + (++n).toString().padStart(5, '0') }, { zone })
    val catalog = Catalog(ctx)
    val schedule = Schedule(ctx)
}
