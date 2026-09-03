package com.boikhata.core.domain.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D45: BackupMapper unit tests — entity→map conversion, Negative-Adj prefix, row filtering.
 * Firebase-Project-Context.md §6 constraints #4, #6.
 */
class BackupMapperTest {

    // ── Negative-Adj (constraint #4) ──────────────────────────────────────────

    @Test
    fun `applyNegativeAdj should keep positive amount unchanged`() {
        val (amount, desc) = BackupMapper.applyNegativeAdj(100.0, "বিক্রি প্রদান")
        assertThat(amount).isEqualTo(100.0)
        assertThat(desc).isEqualTo("বিক্রি প্রদান")
    }

    @Test
    fun `applyNegativeAdj should flip negative amount to magnitude and add prefix`() {
        val (amount, desc) = BackupMapper.applyNegativeAdj(-50.0, "ভুল এন্ট্রি")
        assertThat(amount).isEqualTo(50.0)
        assertThat(desc).isEqualTo("Negative Adj: ভুল এন্ট্রি")
    }

    @Test
    fun `applyNegativeAdj should handle zero amount unchanged`() {
        val (amount, desc) = BackupMapper.applyNegativeAdj(0.0, "টেস্ট")
        assertThat(amount).isEqualTo(0.0)
        assertThat(desc).isEqualTo("টেস্ট")
    }

    @Test
    fun `hasNegativeAdjPrefix should detect prefix`() {
        assertThat(BackupMapper.hasNegativeAdjPrefix("Negative Adj: ভুল")).isTrue()
        assertThat(BackupMapper.hasNegativeAdjPrefix("সাধারণ এন্ট্রি")).isFalse()
    }

    // ── Row filtering (incremental — constraint #6) ───────────────────────────

    @Test
    fun `filterNewRows should return all rows when lastBackupAt is null`() {
        val rows = listOf(
            mapOf("id" to "b1", "updatedAt" to 1000L),
            mapOf("id" to "b2", "updatedAt" to 2000L),
        )
        val filtered = BackupMapper.filterNewRows(BackupMapper.COL_BOOKS, rows, null)
        assertThat(filtered).hasSize(2)
    }

    @Test
    fun `filterNewRows should return only rows newer than lastBackupAt`() {
        val rows = listOf(
            mapOf("id" to "b1", "updatedAt" to 1000L),
            mapOf("id" to "b2", "updatedAt" to 2000L),
            mapOf("id" to "b3", "updatedAt" to 3000L),
        )
        val filtered = BackupMapper.filterNewRows(BackupMapper.COL_BOOKS, rows, 1500L)
        assertThat(filtered).hasSize(2)
        assertThat(filtered.map { it["id"] }).containsExactly("b2", "b3")
    }

    @Test
    fun `filterNewRows should use timestamp field for stock_ledger`() {
        val rows = listOf(
            mapOf("id" to "s1", "timestamp" to 1000L),
            mapOf("id" to "s2", "timestamp" to 3000L),
        )
        val filtered = BackupMapper.filterNewRows(BackupMapper.COL_STOCK_LEDGER, rows, 2000L)
        assertThat(filtered).hasSize(1)
        assertThat(filtered[0]["id"]).isEqualTo("s2")
    }

    @Test
    fun `filterNewRows should use billDate for bills`() {
        val rows = listOf(
            mapOf("id" to "bill1", "billDate" to 1000L),
            mapOf("id" to "bill2", "billDate" to 5000L),
        )
        val filtered = BackupMapper.filterNewRows(BackupMapper.COL_BILLS, rows, 2000L)
        assertThat(filtered).hasSize(1)
        assertThat(filtered[0]["id"]).isEqualTo("bill2")
    }

    @Test
    fun `filterNewRows should return all rows for collections without timestamp`() {
        val rows = listOf(mapOf("id" to "c1"), mapOf("id" to "c2"))
        // bill_lines has no timestamp field
        val filtered = BackupMapper.filterNewRows(BackupMapper.COL_BILL_LINES, rows, 9999L)
        assertThat(filtered).hasSize(2)
    }

    @Test
    fun `timestampFieldForCollection should return correct field per collection`() {
        assertThat(BackupMapper.timestampFieldForCollection(BackupMapper.COL_BOOKS)).isEqualTo("updatedAt")
        assertThat(BackupMapper.timestampFieldForCollection(BackupMapper.COL_STOCK_LEDGER)).isEqualTo("timestamp")
        assertThat(BackupMapper.timestampFieldForCollection(BackupMapper.COL_BILLS)).isEqualTo("billDate")
        assertThat(BackupMapper.timestampFieldForCollection(BackupMapper.COL_BILL_LINES)).isNull()
        assertThat(BackupMapper.timestampFieldForCollection(BackupMapper.COL_KHATA_ENTRIES)).isEqualTo("date")
        assertThat(BackupMapper.timestampFieldForCollection(BackupMapper.COL_CASHBOOK_ENTRIES)).isEqualTo("date")
        assertThat(BackupMapper.timestampFieldForCollection(BackupMapper.COL_EXPENSE_CATEGORIES)).isNull()
    }

    // ── Entity → Map conversion ───────────────────────────────────────────────

    @Test
    fun `bookToMap should stamp tenantId from claims not entity`() {
        val map = BackupMapper.bookToMap(
            id = "b1", tenantId = "claims_tenant", isbn = null,
            titleBn = "পদার্থবিজ্ঞান", titleEn = "Physics", author = "লেখক",
            publisher = "প্রকাশক", classLevel = "৯", subject = "পদার্থ",
            editionYear = 2024, category = "TEXTBOOK", condition = "NEW",
            purchasePrice = 100.0, sellingPrice = 120.0, initialStock = 10,
            lowStockThreshold = 2, isActive = true, createdAt = 1000L, updatedAt = 2000L,
        )
        assertThat(map["tenantId"]).isEqualTo("claims_tenant")
        assertThat(map["id"]).isEqualTo("b1")
        assertThat(map["titleBn"]).isEqualTo("পদার্থবিজ্ঞান")
        assertThat(map["sellingPrice"]).isEqualTo(120.0)
    }

    @Test
    fun `khataEntryToMap should apply Negative-Adj for negative amount`() {
        val map = BackupMapper.khataEntryToMap(
            id = "e1", tenantId = "t1", customerId = "c1",
            amount = -75.0, type = "ADJUSTMENT", description = "ভুল কেটে দেওয়া",
            referenceBillId = null, collectedByUserId = "u1",
            date = 1000L, idempotencyKey = "key1",
        )
        // Amount should be magnitude (75.0), description should have prefix
        assertThat(map["amount"]).isEqualTo(75.0)
        assertThat(map["description"] as String).startsWith("Negative Adj: ")
        assertThat(map["description"] as String).contains("ভুল কেটে দেওয়া")
    }

    @Test
    fun `khataEntryToMap should keep positive amount unchanged`() {
        val map = BackupMapper.khataEntryToMap(
            id = "e2", tenantId = "t1", customerId = "c1",
            amount = 500.0, type = "CREDIT", description = "বাকি",
            referenceBillId = "bill1", collectedByUserId = "u1",
            date = 1000L, idempotencyKey = "key2",
        )
        assertThat(map["amount"]).isEqualTo(500.0)
        assertThat(map["description"]).isEqualTo("বাকি")
        assertThat(map["type"]).isEqualTo("CREDIT")
    }

    @Test
    fun `cashbookEntryToMap should apply Negative-Adj for negative amount`() {
        val map = BackupMapper.cashbookEntryToMap(
            id = "ce1", tenantId = "t1", account = "CASH",
            type = "EXPENSE", amount = -30.0, description = "ভুল খরচ",
            referenceId = null, date = 1000L, userId = "u1",
            idempotencyKey = "key3",
        )
        assertThat(map["amount"]).isEqualTo(30.0)
        assertThat(map["description"] as String).startsWith("Negative Adj: ")
    }

    @Test
    fun `cashbookEntryToMap should keep positive amount unchanged`() {
        val map = BackupMapper.cashbookEntryToMap(
            id = "ce2", tenantId = "t1", account = "BKASH",
            type = "INCOME", amount = 200.0, description = "বিক্রি",
            referenceId = "bill1", date = 1000L, userId = "u1",
            idempotencyKey = "key4",
        )
        assertThat(map["amount"]).isEqualTo(200.0)
        assertThat(map["description"]).isEqualTo("বিক্রি")
    }

    @Test
    fun `billToMap should include all bill fields`() {
        val map = BackupMapper.billToMap(
            id = "bill1", tenantId = "t1", billNumber = "INV-20260903-0001",
            customerId = "c1", customerNameBn = "রহিম", customerPhone = "01711",
            userId = "u1", subtotal = 100.0, discountAmount = 10.0, discountType = "PERCENTAGE",
            vatAmount = 15.0, totalAmount = 105.0, paymentMethod = "CASH",
            paidAmount = 105.0, dueAmount = 0.0, khataEntryId = null,
            billDate = 1000L, status = "PAID", idempotencyKey = "key5",
        )
        assertThat(map["billNumber"]).isEqualTo("INV-20260903-0001")
        assertThat(map["totalAmount"]).isEqualTo(105.0)
        assertThat(map["status"]).isEqualTo("PAID")
    }

    @Test
    fun `ALL_BACKUP_COLLECTIONS should have exactly 10 collections without audit_logs`() {
        assertThat(BackupMapper.ALL_BACKUP_COLLECTIONS).hasSize(10)
        assertThat(BackupMapper.ALL_BACKUP_COLLECTIONS).doesNotContain("audit_logs")
        assertThat(BackupMapper.ALL_BACKUP_COLLECTIONS).doesNotContain("local_audit_logs")
    }
}
