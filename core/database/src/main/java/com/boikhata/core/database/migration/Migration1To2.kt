package com.boikhata.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * D16: Migration v1→v2 — ALTER-ADD normalized columns for Bengali fuzzy search (D13).
 * - books.titleBnNormalized TEXT NOT NULL DEFAULT ''
 * - khata_customers.nameBnNormalized TEXT NOT NULL DEFAULT ''
 *
 * No table drops (CONVENTIONS §3 rule). Existing rows get '' (populated on next add/edit).
 */
val Migration1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE books ADD COLUMN titleBnNormalized TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE khata_customers ADD COLUMN nameBnNormalized TEXT NOT NULL DEFAULT ''")
    }
}
