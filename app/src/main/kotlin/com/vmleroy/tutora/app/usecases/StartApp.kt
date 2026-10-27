package com.vmleroy.tutora.app.usecases

import com.vmleroy.tutora.data.SettingsRepository
import com.vmleroy.tutora.data.TutoraDatabase
import com.vmleroy.tutora.domain.Language
import com.vmleroy.tutora.domain.Settings
import com.vmleroy.tutora.domain.firstRunLanguage
import com.vmleroy.tutora.domain.startupLanguage

sealed interface StartResult {
    data class Ready(
        val db: TutoraDatabase,
        val settings: Settings,
        val schemaVersion: Int,
    ) : StartResult

    data class Failed(
        val language: Language,
        val backupPath: String?,
        val message: String,
    ) : StartResult
}

/**
 * Opens and migrates the database, then makes sure every setting exists. A first run takes
 * the language from the phone; afterwards the saved setting rules (AC-001-5, AC-001-6).
 */
class StartApp(
    private val openDatabase: () -> Pair<TutoraDatabase, String?>,
    private val phoneTags: () -> List<String>,
    private val now: () -> String,
) {
    suspend operator fun invoke(): StartResult {
        val phone = phoneTags()
        var backup: String? = null
        return try {
            val (db, copy) = openDatabase()
            backup = copy
            // The first query runs Room's pending migrations.
            val repo = SettingsRepository(db)
            repo.ensureDefaults(firstRunLanguage(phone), now())
            StartResult.Ready(db, repo.load(), TutoraDatabase.VERSION)
        } catch (e: Exception) {
            StartResult.Failed(startupLanguage(null, phone), backup, e.message ?: e.toString())
        }
    }
}
