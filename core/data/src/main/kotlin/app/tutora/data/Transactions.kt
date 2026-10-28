package app.tutora.data

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

/** Runs [block] in one SQLite transaction (constitution rule 11); a throw rolls it back. */
suspend fun <T> TutoraDatabase.inTransaction(block: suspend () -> T): T = useWriterConnection { it.immediateTransaction { block() } }
