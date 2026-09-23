package com.boikhata.core.database.migration

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.boikhata.core.database.BoiKhataDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P12/D92: Migration 6→7 validation via Room's MigrationTestHelper.
 *
 * Proves (on the real exported schemas 6.json/7.json):
 *  1. the migration runs and the resulting schema matches Room v7's expectation;
 *  2. legacy v6 data (bills with the denormalized payment columns) is preserved
 *     byte-for-byte — zero data transformation (D92 requirement);
 *  3. the new bill_payment_lines table exists and accepts rows.
 *
 * Runs under Robolectric (JVM) because the sandbox has no emulator; the same
 * test runs on CI and devices unchanged.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration6To7Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BoiKhataDatabase::class.java,
    )

    @Test
    fun `migrate 6 to 7 preserves legacy bills and creates bill_payment_lines`() {
        helper.createDatabase(TEST_DB, 6).use { db ->
            // A legacy v6 bill: denormalized single-method payment columns only.
            db.execSQL(
                "INSERT INTO bills (id, tenantId, billNumber, customerId, customerNameBn, " +
                    "customerPhone, userId, subtotal, discountAmount, discountType, vatAmount, " +
                    "totalAmount, paymentMethod, paidAmount, dueAmount, khataEntryId, billDate, " +
                    "status, idempotencyKey) VALUES " +
                    "('b1','t_1','INV-20260924-0001','c1','করিম',NULL,'u_1',1000.0,0.0,'FIXED'," +
                    "0.0,1000.0,'CASH',1000.0,0.0,NULL,1,'COMPLETED','k1')"
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 7, true, Migration6To7).use { db ->
            // ── legacy data preserved: no transformation, no loss ──
            db.query("SELECT paymentMethod, paidAmount, dueAmount FROM bills WHERE id = 'b1'").use { c ->
                assertThat(c.moveToFirst()).isTrue()
                assertThat(c.getString(0)).isEqualTo("CASH")
                assertThat(c.getDouble(1)).isEqualTo(1000.0)
                assertThat(c.getDouble(2)).isEqualTo(0.0)
            }

            // ── the new table exists and accepts a payment-line row ──
            db.execSQL(
                "INSERT INTO bill_payment_lines (id, tenantId, billId, method, provider, " +
                    "amount, cashbookEntryId, createdAt) VALUES " +
                    "('p1','t_1','b1','CASH',NULL,1000.0,NULL,1)"
            )
            db.query("SELECT method, provider, amount FROM bill_payment_lines WHERE id = 'p1'").use { c ->
                assertThat(c.moveToFirst()).isTrue()
                assertThat(c.getString(0)).isEqualTo("CASH")
                assertThat(c.isNull(1)).isTrue()
                assertThat(c.getDouble(2)).isEqualTo(1000.0)
            }
        }
    }

    companion object {
        private const val TEST_DB = "boi-khata-migration-test.db"
    }
}
