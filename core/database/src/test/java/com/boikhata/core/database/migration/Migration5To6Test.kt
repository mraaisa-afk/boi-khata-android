package com.boikhata.core.database.migration

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.SupportSQLiteOpenHelperFactory
import com.boikhata.core.database.BoiKhataDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * D70: Tests for Migration5To6 — unique index on supplier_entries.idempotencyKey.
 *
 * Test framework: JUnit 4.13.2 (D69).
 * Naming: <ClassUnderTest>Test; methods should <expected> when <condition>.
 */
class Migration5To6Test {

    private val dbName = "migration_test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        assets = androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .assets,
        specs = listOf(BoiKhataDatabase::class.java),
        factory = SupportSQLiteOpenHelperFactory(),
    )

    @Test
    fun `should create unique index on idempotencyKey when migrating v5 to v6`() {
        // Start at v5
        val dbV5 = helper.createDatabase(dbName, 5)
        dbV5.close()

        // Run migration 5→6
        val dbV6 = helper.runMigrationsAndValidate(
            dbName,
            6,
            true,
            Migration5To6,
        )

        // Verify the index exists and is unique
        val cursor = dbV6.query(
            "SELECT name, uniqueness FROM sqlite_master " +
                "WHERE type='index' AND tbl_name='supplier_entries' " +
                "AND name='index_supplier_entries_idempotencyKey'"
        )
        cursor.use {
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.getString(it.getColumnIndexOrThrow("name")))
                .isEqualTo("index_supplier_entries_idempotencyKey")
            assertThat(it.getInt(it.getColumnIndexOrThrow("uniqueness")))
                .isEqualTo(1) // 1 = unique
        }
        dbV6.close()
    }

    @Test
    fun `should enforce uniqueness — inserting duplicate idempotencyKey throws`() {
        val dbV5 = helper.createDatabase(dbName, 5)
        dbV5.close()

        val dbV6 = helper.runMigrationsAndValidate(
            dbName,
            6,
            true,
            Migration5To6,
        )

        // Insert first row
        dbV6.execSQL(
            "INSERT INTO supplier_entries " +
                "(id, tenantId, supplierId, amount, type, description, referenceId, date, idempotencyKey) " +
                "VALUES ('row-1', 't_1', 'sup-1', 100.0, 'PURCHASE', 'test', NULL, 1000, 'sup-1_bill-1_PURCHASE')"
        )

        // Insert second row with SAME idempotencyKey — should fail
        var threw = false
        try {
            dbV6.execSQL(
                "INSERT INTO supplier_entries " +
                    "(id, tenantId, supplierId, amount, type, description, referenceId, date, idempotencyKey) " +
                    "VALUES ('row-2', 't_1', 'sup-1', 100.0, 'PURCHASE', 'duplicate', NULL, 2000, 'sup-1_bill-1_PURCHASE')"
            )
        } catch (e: Exception) {
            threw = true
        }
        assertThat(threw).isTrue()
        dbV6.close()
    }

    @Test
    fun `should not lose existing supplier_entries data during migration`() {
        val dbV5 = helper.createDatabase(dbName, 5)

        // Insert a row at v5 (before the index exists)
        dbV5.execSQL(
            "INSERT INTO supplier_entries " +
                "(id, tenantId, supplierId, amount, type, description, referenceId, date, idempotencyKey) " +
                "VALUES ('pre-migration-1', 't_1', 'sup-1', 500.0, 'OPENING', 'opening balance', NULL, 500, 'pre-key-1')"
        )
        dbV5.close()

        // Run migration
        val dbV6 = helper.runMigrationsAndValidate(
            dbName,
            6,
            true,
            Migration5To6,
        )

        // Verify the row survived
        val cursor = dbV6.query(
            "SELECT id, amount, type, idempotencyKey FROM supplier_entries WHERE id = 'pre-migration-1'"
        )
        cursor.use {
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.getString(it.getColumnIndexOrThrow("id"))).isEqualTo("pre-migration-1")
            assertThat(it.getDouble(it.getColumnIndexOrThrow("amount"))).isEqualTo(500.0)
            assertThat(it.getString(it.getColumnIndexOrThrow("type"))).isEqualTo("OPENING")
            assertThat(it.getString(it.getColumnIndexOrThrow("idempotencyKey"))).isEqualTo("pre-key-1")
        }
        dbV6.close()
    }
}
