package com.boikhata.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * D70: Migration v5→v6 — add a unique index on supplier_entries.idempotencyKey.
 *
 * Idempotency key contract (deterministic):
 *   idempotencyKey = "{supplierId}_{sourceEntityId}_{entryType}"
 *
 * The key MUST NOT include timestamps or random components. Same business
 * operation retried = same key, always. The unique index turns a duplicate
 * insert into a constraint error, which the repository catches as an
 * idempotent no-op.
 *
 * No table drops, no column changes (no-drop rule, CONVENTIONS §3).
 */
val Migration5To6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "index_supplier_entries_idempotencyKey ON supplier_entries(idempotencyKey)"
        )
    }
}
