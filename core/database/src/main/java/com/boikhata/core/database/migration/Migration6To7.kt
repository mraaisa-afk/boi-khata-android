package com.boikhata.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * P12/D92: Migration v6→v7 — create bill_payment_lines (ADDITIVE-ONLY).
 *
 * Precedent: v4 (mela_sessions) and v5 (trial_redemptions) additive tables.
 * No column changes, no drops, no data transformation (CONVENTIONS §3 no-drop
 * rule). Legacy bills keep writing/reading the denormalized bills.paymentMethod
 * /paidAmount/dueAmount columns; the new table is the authoritative per-line
 * record for new checkouts and restore of new-format backups.
 */
val Migration6To7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `bill_payment_lines` (" +
                "`id` TEXT NOT NULL, " +
                "`tenantId` TEXT NOT NULL, " +
                "`billId` TEXT NOT NULL, " +
                "`method` TEXT NOT NULL, " +
                "`provider` TEXT, " +
                "`amount` REAL NOT NULL, " +
                "`cashbookEntryId` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_bill_payment_lines_billId` ON bill_payment_lines(billId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_bill_payment_lines_tenantId` ON bill_payment_lines(tenantId)"
        )
    }
}
