package com.boikhata.core.domain.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * P12/D92: backup/restore round-trip for payment lines — BOTH formats.
 *
 *  - OLD-FORMAT bills (pre-P12 backups): no "paymentLines" key → restore yields
 *    zero payment-line rows (the legacy bill keeps its denormalized columns,
 *    which is exactly the pre-P12 state).
 *  - NEW-FORMAT bills: billToMap embeds the nested paymentLines array →
 *    restore extracts every line (paid + DUE) field-accurately.
 */
class PaymentLineBackupRoundTripTest {

    private val legacyBillMap = mapOf(
        "id" to "b-legacy",
        "tenantId" to "t_1",
        "billNumber" to "INV-20260901-0001",
        "customerId" to null as String?,
        "customerNameBn" to "হাটি ক্রেতা",
        "userId" to "u_1",
        "subtotal" to 500.0,
        "discountAmount" to 0.0,
        "discountType" to "FIXED",
        "vatAmount" to 0.0,
        "totalAmount" to 500.0,
        "paymentMethod" to "CASH",
        "paidAmount" to 500.0,
        "dueAmount" to 0.0,
        "khataEntryId" to null as String?,
        "billDate" to 1L,
        "status" to "COMPLETED",
        "idempotencyKey" to "k-1",
        // NOTE: no "paymentLines" key — that IS the old format.
    )

    @Test
    fun `old-format bill without paymentLines restores zero lines`() {
        val restored = RestoreMapper.paymentLinesFromBillMap(legacyBillMap)
        assertThat(restored).isEmpty()
    }

    @Test
    fun `new-format bill embeds and restores all payment lines including DUE`() {
        val lines = listOf(
            mapOf(
                "id" to "p1", "tenantId" to "t_1", "billId" to "b-new",
                "method" to "CASH", "provider" to null as String?, "amount" to 600.0,
                "cashbookEntryId" to "cb1" as String?, "createdAt" to 5L,
            ),
            mapOf(
                "id" to "p2", "tenantId" to "t_1", "billId" to "b-new",
                "method" to "MOBILE", "provider" to "NAGAD" as String?, "amount" to 300.0,
                "cashbookEntryId" to "cb2" as String?, "createdAt" to 5L,
            ),
            mapOf(
                "id" to "p3", "tenantId" to "t_1", "billId" to "b-new",
                "method" to "DUE", "provider" to null as String?, "amount" to 100.0,
                "cashbookEntryId" to null as String?, "createdAt" to 5L,
            ),
        )
        // Backup side: embed via the (P12-extended) billToMap — the same call the
        // BackupRepositoryImpl makes for every bill.
        val billMap = BackupMapper.billToMap(
            id = "b-new", tenantId = "t_1", billNumber = "INV-20260924-0009",
            customerId = "c1", customerNameBn = "করিম", customerPhone = null as String?,
            userId = "u_1", subtotal = 1000.0, discountAmount = 0.0, discountType = "FIXED",
            vatAmount = 0.0, totalAmount = 1000.0, paymentMethod = "MOBILE",
            paidAmount = 900.0, dueAmount = 100.0, khataEntryId = null as String?,
            billDate = 1L, status = "PARTIAL", idempotencyKey = "k-2",
            paymentLines = lines,
        )

        // Restore side (RestoreRepositoryImpl path): field-accurate round trip.
        val restored = RestoreMapper.paymentLinesFromBillMap(billMap)
        assertThat(restored).hasSize(3)
        val cash = restored.first { it.method == "CASH" }
        assertThat(cash.provider).isNull()
        assertThat(cash.amount).isEqualTo(600.0)
        assertThat(cash.cashbookEntryId).isEqualTo("cb1")
        val mobile = restored.first { it.method == "MOBILE" }
        assertThat(mobile.provider).isEqualTo("NAGAD")
        assertThat(mobile.amount).isEqualTo(300.0)
        val due = restored.first { it.method == "DUE" }
        assertThat(due.amount).isEqualTo(100.0)
        assertThat(due.cashbookEntryId).isNull()
    }

    @Test
    fun `legacy bill mapped without lines has no paymentLines key`() {
        // billToMap's default (no lines) must NOT add the key — that keeps
        // freshly-backed-up legacy bills byte-identical to pre-P12 backups.
        val map = BackupMapper.billToMap(
            id = "b-2", tenantId = "t_1", billNumber = "INV-1", customerId = null as String?,
            customerNameBn = "x", customerPhone = null as String?, userId = "u_1",
            subtotal = 1.0, discountAmount = 0.0, discountType = "FIXED", vatAmount = 0.0,
            totalAmount = 1.0, paymentMethod = "CASH", paidAmount = 1.0, dueAmount = 0.0,
            khataEntryId = null as String?, billDate = 1L, status = "COMPLETED", idempotencyKey = "k",
        )
        assertThat(map.containsKey("paymentLines")).isFalse()
    }
}
