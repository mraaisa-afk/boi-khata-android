package com.boikhata.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * D57: Migration v3→v4 — add the mela_sessions table for the book-fair / seasonal mode.
 * New table only (CREATE TABLE), no drops (CONVENTIONS §3 rule).
 */
val Migration3To4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS mela_sessions (
                id TEXT NOT NULL PRIMARY KEY,
                tenantId TEXT NOT NULL,
                nameBn TEXT NOT NULL,
                location TEXT NOT NULL,
                startDate INTEGER NOT NULL,
                endDate INTEGER NOT NULL,
                isActive INTEGER NOT NULL,
                isPaused INTEGER NOT NULL,
                pauseReason TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mela_sessions_tenantId ON mela_sessions(tenantId)")
    }
}
