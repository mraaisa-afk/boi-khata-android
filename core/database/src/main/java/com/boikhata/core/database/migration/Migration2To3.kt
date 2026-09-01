package com.boikhata.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * D32/D35: Migration v2→v3 — add three new tables for the accounting engine.
 * - period_locks: locked accounting periods (D32)
 * - recurring_expenses: recurring-expense templates (D35)
 * - budgets: monthly budget limits per category (D35)
 *
 * No table drops (CONVENTIONS §3 rule). All three are new tables (CREATE TABLE).
 */
val Migration2To3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // D32: period_locks
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS period_locks (
                id TEXT NOT NULL PRIMARY KEY,
                tenantId TEXT NOT NULL,
                periodYear INTEGER NOT NULL,
                periodMonth INTEGER NOT NULL,
                lockedAt INTEGER NOT NULL,
                lockedByUserId TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_period_locks_tenantId ON period_locks(tenantId)")

        // D35: recurring_expenses
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS recurring_expenses (
                id TEXT NOT NULL PRIMARY KEY,
                tenantId TEXT NOT NULL,
                categoryId TEXT NOT NULL,
                amount REAL NOT NULL,
                description TEXT NOT NULL,
                frequency TEXT NOT NULL,
                lastAppliedDate INTEGER NOT NULL,
                nextDueDate INTEGER NOT NULL,
                isActive INTEGER NOT NULL,
                userId TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_expenses_tenantId ON recurring_expenses(tenantId)")

        // D35: budgets
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS budgets (
                id TEXT NOT NULL PRIMARY KEY,
                tenantId TEXT NOT NULL,
                categoryId TEXT NOT NULL,
                monthlyLimit REAL NOT NULL,
                isActive INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_tenantId ON budgets(tenantId)")
    }
}
