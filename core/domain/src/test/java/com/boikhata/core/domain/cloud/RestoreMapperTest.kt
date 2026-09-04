package com.boikhata.core.domain.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D47: RestoreMapper unit tests — Firestore-map→entity conversion + Negative-Adj sign flip + round-trip.
 * Firebase-Project-Context.md §6 constraints #2, #4.
 */
class RestoreMapperTest {

    // ── Negative-Adj reversal (constraint #4) ──────────────────────────────────

    @Test
    fun `reverseNegativeAdj should flip sign and strip prefix when prefix present`() {
        val (amount, desc) = RestoreMapper.reverseNegativeAdj(75.0, "Negative Adj: ভুল এন্ট্রি")
        assertThat(amount).isEqualTo(-75.0)
        assertThat(desc).isEqualTo("ভুল এন্ট্রি")
    }

    @Test
    fun `reverseNegativeAdj should keep amount and description unchanged when no prefix`() {
        val (amount, desc) = RestoreMapper.reverseNegativeAdj(100.0, "বিক্রি")
        assertThat(amount).isEqualTo(100.0)
        assertThat(desc).isEqualTo("বিক্রি")
    }

    @Test
    fun `reverseNegativeAdj should handle empty description with prefix`() {
        val (amount, desc) = RestoreMapper.reverseNegativeAdj(50.0, "Negative Adj: ")
        assertThat(amount).isEqualTo(-50.0)
        assertThat(desc).isEmpty()
    }

    // ── Field extraction helpers ───────────────────────────────────────────────

    @Test
    fun `getLong should handle Long Int and null`() {
        assertThat(RestoreMapper.getLong(mapOf("x" to 1000L), "x")).isEqualTo(1000L)
        assertThat(RestoreMapper.getLong(mapOf("x" to 2000), "x")).isEqualTo(2000L)
        assertThat(RestoreMapper.getLong(mapOf("x" to null), "x")).isNull()
        assertThat(RestoreMapper.getLong(mapOf<String, Any?>(), "x")).isNull()
    }

    @Test
    fun `getDouble should handle Double Long Int and null`() {
        assertThat(RestoreMapper.getDouble(mapOf("x" to 10.5), "x")).isEqualTo(10.5)
        assertThat(RestoreMapper.getDouble(mapOf("x" to 100L), "x")).isEqualTo(100.0)
        assertThat(RestoreMapper.getDouble(mapOf("x" to 42), "x")).isEqualTo(42.0)
        assertThat(RestoreMapper.getDouble(mapOf("x" to null), "x")).isNull()
    }

    @Test
    fun `getInt should handle Int Long and null`() {
        assertThat(RestoreMapper.getInt(mapOf("x" to 42), "x")).isEqualTo(42)
        assertThat(RestoreMapper.getInt(mapOf("x" to 100L), "x")).isEqualTo(100)
        assertThat(RestoreMapper.getInt(mapOf("x" to null), "x")).isNull()
    }

    @Test
    fun `getString should return String or null`() {
        assertThat(RestoreMapper.getString(mapOf("x" to "hello"), "x")).isEqualTo("hello")
        assertThat(RestoreMapper.getString(mapOf("x" to null), "x")).isNull()
        assertThat(RestoreMapper.getString(mapOf<String, Any?>(), "x")).isNull()
    }

    @Test
    fun `getBoolean should return Boolean or false`() {
        assertThat(RestoreMapper.getBoolean(mapOf("x" to true), "x")).isTrue()
        assertThat(RestoreMapper.getBoolean(mapOf("x" to false), "x")).isFalse()
        assertThat(RestoreMapper.getBoolean(mapOf("x" to null), "x")).isFalse()
    }

    // ── Firestore Map → entity fields ──────────────────────────────────────────

    @Test
    fun `bookFromMap should extract all fields correctly`() {
        val map = mapOf(
            "id" to "b1", "tenantId" to "t1", "isbn" to "978123",
            "titleBn" to "পদার্থ", "titleEn" to "Physics", "author" to "লেখক",
            "publisher" to "প্রকাশক", "classLevel" to "৯", "subject" to "পদার্থ",
            "editionYear" to 2024, "category" to "TEXTBOOK", "condition" to "NEW",
            "purchasePrice" to 100.0, "sellingPrice" to 120.0, "initialStock" to 10,
            "lowStockThreshold" to 2, "isActive" to true, "createdAt" to 1000L,
            "updatedAt" to 2000L,
        )
        val book = RestoreMapper.bookFromMap(map)
        assertThat(book.id).isEqualTo("b1")
        assertThat(book.tenantId).isEqualTo("t1")
        assertThat(book.titleBn).isEqualTo("পদার্থ")
        assertThat(book.sellingPrice).isEqualTo(120.0)
        assertThat(book.editionYear).isEqualTo(2024)
        assertThat(book.isActive).isTrue()
    }

    @Test
    fun `khataEntryFromMap should reverse Negative-Adj when prefix present`() {
        val map = mapOf(
            "id" to "e1", "tenantId" to "t1", "customerId" to "c1",
            "amount" to 75.0, "type" to "ADJUSTMENT",
            "description" to "Negative Adj: ভুল কেটে দেওয়া",
            "referenceBillId" to null, "collectedByUserId" to "u1",
            "date" to 1000L, "idempotencyKey" to "key1",
        )
        val entry = RestoreMapper.khataEntryFromMap(map)
        assertThat(entry.amount).isEqualTo(-75.0)
        assertThat(entry.description).isEqualTo("ভুল কেটে দেওয়া")
        assertThat(entry.type).isEqualTo("ADJUSTMENT")
    }

    @Test
    fun `khataEntryFromMap should keep positive amount unchanged`() {
        val map = mapOf(
            "id" to "e2", "tenantId" to "t1", "customerId" to "c1",
            "amount" to 500.0, "type" to "CREDIT", "description" to "বাকি",
            "referenceBillId" to "bill1", "collectedByUserId" to "u1",
            "date" to 1000L, "idempotencyKey" to "key2",
        )
        val entry = RestoreMapper.khataEntryFromMap(map)
        assertThat(entry.amount).isEqualTo(500.0)
        assertThat(entry.description).isEqualTo("বাকি")
    }

    @Test
    fun `cashbookEntryFromMap should reverse Negative-Adj when prefix present`() {
        val map = mapOf(
            "id" to "ce1", "tenantId" to "t1", "account" to "CASH",
            "type" to "EXPENSE", "amount" to 30.0,
            "description" to "Negative Adj: ভুল খরচ",
            "referenceId" to null, "date" to 1000L, "userId" to "u1",
            "idempotencyKey" to "key3",
        )
        val entry = RestoreMapper.cashbookEntryFromMap(map)
        assertThat(entry.amount).isEqualTo(-30.0)
        assertThat(entry.description).isEqualTo("ভুল খরচ")
    }

    // ── Round-trip: entity → BackupMapper → RestoreMapper → entity ─────────────

    @Test
    fun `round-trip khata_entry with negative amount should preserve original values`() {
        val originalAmount = -75.0
        val originalDesc = "ভুল কেটে দেওয়া"
        val tenantId = "t_claims"

        // Entity → BackupMapper (upload)
        val uploadMap = BackupMapper.khataEntryToMap(
            id = "e1", tenantId = tenantId, customerId = "c1",
            amount = originalAmount, type = "ADJUSTMENT", description = originalDesc,
            referenceBillId = null, collectedByUserId = "u1",
            date = 1000L, idempotencyKey = "key1",
        )

        // BackupMapper → RestoreMapper (download)
        val restored = RestoreMapper.khataEntryFromMap(uploadMap)

        // Verify round-trip identity
        assertThat(restored.amount).isEqualTo(originalAmount)
        assertThat(restored.description).isEqualTo(originalDesc)
        assertThat(restored.type).isEqualTo("ADJUSTMENT")
        assertThat(restored.tenantId).isEqualTo(tenantId)
    }

    @Test
    fun `round-trip khata_entry with positive amount should preserve original values`() {
        val originalAmount = 500.0
        val originalDesc = "বাকি বিক্রি"
        val tenantId = "t_claims"

        val uploadMap = BackupMapper.khataEntryToMap(
            id = "e2", tenantId = tenantId, customerId = "c1",
            amount = originalAmount, type = "CREDIT", description = originalDesc,
            referenceBillId = "bill1", collectedByUserId = "u1",
            date = 1000L, idempotencyKey = "key2",
        )
        val restored = RestoreMapper.khataEntryFromMap(uploadMap)

        assertThat(restored.amount).isEqualTo(originalAmount)
        assertThat(restored.description).isEqualTo(originalDesc)
    }

    @Test
    fun `round-trip cashbook_entry with negative amount should preserve original values`() {
        val originalAmount = -30.0
        val originalDesc = "ভুল খরচ"

        val uploadMap = BackupMapper.cashbookEntryToMap(
            id = "ce1", tenantId = "t1", account = "CASH",
            type = "EXPENSE", amount = originalAmount, description = originalDesc,
            referenceId = null, date = 1000L, userId = "u1",
            idempotencyKey = "key3",
        )
        val restored = RestoreMapper.cashbookEntryFromMap(uploadMap)

        assertThat(restored.amount).isEqualTo(originalAmount)
        assertThat(restored.description).isEqualTo(originalDesc)
        assertThat(restored.account).isEqualTo("CASH")
    }

    @Test
    fun `round-trip book should preserve all fields`() {
        val uploadMap = BackupMapper.bookToMap(
            id = "b1", tenantId = "t1", isbn = "978123",
            titleBn = "পদার্থ", titleEn = "Physics", author = "লেখক",
            publisher = "প্রকাশক", classLevel = "৯", subject = "পদার্থ",
            editionYear = 2024, category = "TEXTBOOK", condition = "NEW",
            purchasePrice = 100.0, sellingPrice = 120.0, initialStock = 10,
            lowStockThreshold = 2, isActive = true, createdAt = 1000L, updatedAt = 2000L,
        )
        val restored = RestoreMapper.bookFromMap(uploadMap)

        assertThat(restored.id).isEqualTo("b1")
        assertThat(restored.tenantId).isEqualTo("t1")
        assertThat(restored.isbn).isEqualTo("978123")
        assertThat(restored.titleBn).isEqualTo("পদার্থ")
        assertThat(restored.sellingPrice).isEqualTo(120.0)
        assertThat(restored.editionYear).isEqualTo(2024)
    }

    @Test
    fun `round-trip bill should preserve all fields`() {
        val uploadMap = BackupMapper.billToMap(
            id = "bill1", tenantId = "t1", billNumber = "INV-20260903-0001",
            customerId = "c1", customerNameBn = "রহিম", customerPhone = "01711",
            userId = "u1", subtotal = 100.0, discountAmount = 10.0, discountType = "PERCENTAGE",
            vatAmount = 15.0, totalAmount = 105.0, paymentMethod = "CASH",
            paidAmount = 105.0, dueAmount = 0.0, khataEntryId = null,
            billDate = 1000L, status = "PAID", idempotencyKey = "key5",
        )
        val restored = RestoreMapper.billFromMap(uploadMap)

        assertThat(restored.id).isEqualTo("bill1")
        assertThat(restored.billNumber).isEqualTo("INV-20260903-0001")
        assertThat(restored.totalAmount).isEqualTo(105.0)
        assertThat(restored.status).isEqualTo("PAID")
    }
}
