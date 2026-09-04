package com.boikhata.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** D64: additive trial-redemption storage; existing business tables are untouched. */
val Migration4To5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trial_redemptions (
                id TEXT NOT NULL PRIMARY KEY,
                tenantId TEXT NOT NULL,
                deviceFingerprint TEXT NOT NULL,
                phoneHash TEXT NOT NULL,
                redeemedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}
