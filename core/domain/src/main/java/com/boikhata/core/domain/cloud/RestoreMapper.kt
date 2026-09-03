package com.boikhata.core.domain.cloud

/**
 * D47: RestoreMapper — pure Firestore-map→field conversion + Negative-Adj sign flip.
 *
 * Firebase-Project-Context.md §6 constraint #2: "ALWAYS map documents field-by-field
 * manually (never toObjects() on data classes without no-arg constructors)."
 * Constraint #4: "restore must reverse the sign + strip the prefix" for negative
 * ADJUSTMENT entries uploaded with "Negative Adj: " prefix by BackupMapper (D45).
 *
 * Pure object — no Android, no Firestore SDK. Independently unit-testable.
 * The repository layer extracts the map from DocumentSnapshot.getData() and passes it here.
 */
object RestoreMapper {

    // ── Negative-Adj reversal (constraint #4) ───────────────────────────────────

    /**
     * Reverse the Negative-Adj rule for a money-table row (khata_entries, cashbook_entries).
     * If description starts with "Negative Adj: ": flip the sign (make amount negative)
     * and strip the prefix. Otherwise unchanged.
     */
    fun reverseNegativeAdj(amount: Double, description: String): Pair<Double, String> {
        return if (BackupMapper.hasNegativeAdjPrefix(description)) {
            val stripped = description.removePrefix(BackupMapper.NEGATIVE_ADJ_PREFIX)
            Pair(-kotlin.math.abs(amount), stripped)
        } else {
            Pair(amount, description)
        }
    }

    // ── Field extraction helpers (type-safe coercion) ──────────────────────────

    /** Extract a String field, or null if missing/not a String. */
    fun getString(map: Map<String, Any?>, key: String): String? =
        map[key] as? String

    /** Extract a non-null String field, or empty string if missing. */
    fun getStringOrEmpty(map: Map<String, Any?>, key: String): String =
        (map[key] as? String) ?: ""

    /** Extract a Long field — handles Long, Int, and null. */
    fun getLong(map: Map<String, Any?>, key: String): Long? = when (val v = map[key]) {
        is Long -> v
        is Int -> v.toLong()
        is Number -> v.toLong()
        else -> null
    }

    /** Extract a Double field — handles Double, Long, Int, and null. */
    fun getDouble(map: Map<String, Any?>, key: String): Double? = when (val v = map[key]) {
        is Double -> v
        is Long -> v.toDouble()
        is Int -> v.toDouble()
        is Number -> v.toDouble()
        else -> null
    }

    /** Extract an Int field — handles Int, Long, and null. */
    fun getInt(map: Map<String, Any?>, key: String): Int? = when (val v = map[key]) {
        is Int -> v
        is Long -> v.toInt()
        is Number -> v.toInt()
        else -> null
    }

    /** Extract a Boolean field — handles Boolean and null (defaults false). */
    fun getBoolean(map: Map<String, Any?>, key: String): Boolean =
        (map[key] as? Boolean) ?: false

    // ── Firestore Map → entity fields ──────────────────────────────────────────
    // Each function takes the Firestore data map and returns the field values
    // needed to construct a Room entity. The repository constructs the entity
    // from these fields (the mapper stays pure — no Room dependency).

    data class BookFields(
        val id: String, val tenantId: String, val isbn: String?, val titleBn: String,
        val titleEn: String?, val author: String, val publisher: String,
        val classLevel: String, val subject: String, val editionYear: Int,
        val category: String, val condition: String, val purchasePrice: Double,
        val sellingPrice: Double, val initialStock: Int, val lowStockThreshold: Int,
        val isActive: Boolean, val createdAt: Long, val updatedAt: Long,
    )

    fun bookFromMap(map: Map<String, Any?>): BookFields = BookFields(
        id = getStringOrEmpty(map, "id"),
        tenantId = getStringOrEmpty(map, "tenantId"),
        isbn = getString(map, "isbn"),
        titleBn = getStringOrEmpty(map, "titleBn"),
        titleEn = getString(map, "titleEn"),
        author = getStringOrEmpty(map, "author"),
        publisher = getStringOrEmpty(map, "publisher"),
        classLevel = getStringOrEmpty(map, "classLevel"),
        subject = getStringOrEmpty(map, "subject"),
        editionYear = getInt(map, "editionYear") ?: 0,
        category = getStringOrEmpty(map, "category"),
        condition = getStringOrEmpty(map, "condition"),
        purchasePrice = getDouble(map, "purchasePrice") ?: 0.0,
        sellingPrice = getDouble(map, "sellingPrice") ?: 0.0,
        initialStock = getInt(map, "initialStock") ?: 0,
        lowStockThreshold = getInt(map, "lowStockThreshold") ?: 0,
        isActive = getBoolean(map, "isActive"),
        createdAt = getLong(map, "createdAt") ?: 0L,
        updatedAt = getLong(map, "updatedAt") ?: 0L,
    )

    data class StockLedgerFields(
        val id: String, val tenantId: String, val bookId: String, val changeQuantity: Int,
        val reason: String, val referenceId: String?, val userId: String,
        val timestamp: Long, val idempotencyKey: String,
    )

    fun stockLedgerFromMap(map: Map<String, Any?>): StockLedgerFields = StockLedgerFields(
        id = getStringOrEmpty(map, "id"),
        tenantId = getStringOrEmpty(map, "tenantId"),
        bookId = getStringOrEmpty(map, "bookId"),
        changeQuantity = getInt(map, "changeQuantity") ?: 0,
        reason = getStringOrEmpty(map, "reason"),
        referenceId = getString(map, "referenceId"),
        userId = getStringOrEmpty(map, "userId"),
        timestamp = getLong(map, "timestamp") ?: 0L,
        idempotencyKey = getStringOrEmpty(map, "idempotencyKey"),
    )

    data class BillFields(
        val id: String, val tenantId: String, val billNumber: String, val customerId: String?,
        val customerNameBn: String, val customerPhone: String?, val userId: String,
        val subtotal: Double, val discountAmount: Double, val discountType: String,
        val vatAmount: Double, val totalAmount: Double, val paymentMethod: String,
        val paidAmount: Double, val dueAmount: Double, val khataEntryId: String?,
        val billDate: Long, val status: String, val idempotencyKey: String,
    )

    fun billFromMap(map: Map<String, Any?>): BillFields = BillFields(
        id = getStringOrEmpty(map, "id"),
        tenantId = getStringOrEmpty(map, "tenantId"),
        billNumber = getStringOrEmpty(map, "billNumber"),
        customerId = getString(map, "customerId"),
        customerNameBn = getStringOrEmpty(map, "customerNameBn"),
        customerPhone = getString(map, "customerPhone"),
        userId = getStringOrEmpty(map, "userId"),
        subtotal = getDouble(map, "subtotal") ?: 0.0,
        discountAmount = getDouble(map, "discountAmount") ?: 0.0,
        discountType = getStringOrEmpty(map, "discountType"),
        vatAmount = getDouble(map, "vatAmount") ?: 0.0,
        totalAmount = getDouble(map, "totalAmount") ?: 0.0,
        paymentMethod = getStringOrEmpty(map, "paymentMethod"),
        paidAmount = getDouble(map, "paidAmount") ?: 0.0,
        dueAmount = getDouble(map, "dueAmount") ?: 0.0,
        khataEntryId = getString(map, "khataEntryId"),
        billDate = getLong(map, "billDate") ?: 0L,
        status = getStringOrEmpty(map, "status"),
        idempotencyKey = getStringOrEmpty(map, "idempotencyKey"),
    )

    data class BillLineFields(
        val id: String, val tenantId: String, val billId: String, val bookId: String,
        val bookTitleBn: String, val quantity: Int, val unitPrice: Double,
        val lineTotal: Double, val vatAmount: Double,
    )

    fun billLineFromMap(map: Map<String, Any?>): BillLineFields = BillLineFields(
        id = getStringOrEmpty(map, "id"),
        tenantId = getStringOrEmpty(map, "tenantId"),
        billId = getStringOrEmpty(map, "billId"),
        bookId = getStringOrEmpty(map, "bookId"),
        bookTitleBn = getStringOrEmpty(map, "bookTitleBn"),
        quantity = getInt(map, "quantity") ?: 0,
        unitPrice = getDouble(map, "unitPrice") ?: 0.0,
        lineTotal = getDouble(map, "lineTotal") ?: 0.0,
        vatAmount = getDouble(map, "vatAmount") ?: 0.0,
    )

    data class KhataCustomerFields(
        val id: String, val tenantId: String, val nameBn: String, val phone: String?,
        val address: String?, val creditLimit: Double, val isActive: Boolean,
        val createdAt: Long, val updatedAt: Long,
    )

    fun khataCustomerFromMap(map: Map<String, Any?>): KhataCustomerFields = KhataCustomerFields(
        id = getStringOrEmpty(map, "id"),
        tenantId = getStringOrEmpty(map, "tenantId"),
        nameBn = getStringOrEmpty(map, "nameBn"),
        phone = getString(map, "phone"),
        address = getString(map, "address"),
        creditLimit = getDouble(map, "creditLimit") ?: 0.0,
        isActive = getBoolean(map, "isActive"),
        createdAt = getLong(map, "createdAt") ?: 0L,
        updatedAt = getLong(map, "updatedAt") ?: 0L,
    )

    /**
     * khata_entries — money table with Negative-Adj reversal (constraint #4).
     */
    data class KhataEntryFields(
        val id: String, val tenantId: String, val customerId: String, val amount: Double,
        val type: String, val description: String, val referenceBillId: String?,
        val collectedByUserId: String, val date: Long, val idempotencyKey: String,
    )

    fun khataEntryFromMap(map: Map<String, Any?>): KhataEntryFields {
        val rawAmount = getDouble(map, "amount") ?: 0.0
        val rawDesc = getStringOrEmpty(map, "description")
        val (amount, description) = reverseNegativeAdj(rawAmount, rawDesc)
        return KhataEntryFields(
            id = getStringOrEmpty(map, "id"),
            tenantId = getStringOrEmpty(map, "tenantId"),
            customerId = getStringOrEmpty(map, "customerId"),
            amount = amount,
            type = getStringOrEmpty(map, "type"),
            description = description,
            referenceBillId = getString(map, "referenceBillId"),
            collectedByUserId = getStringOrEmpty(map, "collectedByUserId"),
            date = getLong(map, "date") ?: 0L,
            idempotencyKey = getStringOrEmpty(map, "idempotencyKey"),
        )
    }

    data class ExpenseFields(
        val id: String, val tenantId: String, val categoryId: String, val amount: Double,
        val description: String, val expenseDate: Long, val receiptPhotoPath: String?,
        val userId: String, val idempotencyKey: String,
    )

    fun expenseFromMap(map: Map<String, Any?>): ExpenseFields = ExpenseFields(
        id = getStringOrEmpty(map, "id"),
        tenantId = getStringOrEmpty(map, "tenantId"),
        categoryId = getStringOrEmpty(map, "categoryId"),
        amount = getDouble(map, "amount") ?: 0.0,
        description = getStringOrEmpty(map, "description"),
        expenseDate = getLong(map, "expenseDate") ?: 0L,
        receiptPhotoPath = getString(map, "receiptPhotoPath"),
        userId = getStringOrEmpty(map, "userId"),
        idempotencyKey = getStringOrEmpty(map, "idempotencyKey"),
    )

    /**
     * cashbook_entries — money table with Negative-Adj reversal (constraint #4).
     */
    data class CashbookEntryFields(
        val id: String, val tenantId: String, val account: String, val type: String,
        val amount: Double, val description: String, val referenceId: String?,
        val date: Long, val userId: String, val idempotencyKey: String,
    )

    fun cashbookEntryFromMap(map: Map<String, Any?>): CashbookEntryFields {
        val rawAmount = getDouble(map, "amount") ?: 0.0
        val rawDesc = getStringOrEmpty(map, "description")
        val (amount, description) = reverseNegativeAdj(rawAmount, rawDesc)
        return CashbookEntryFields(
            id = getStringOrEmpty(map, "id"),
            tenantId = getStringOrEmpty(map, "tenantId"),
            account = getStringOrEmpty(map, "account"),
            type = getStringOrEmpty(map, "type"),
            amount = amount,
            description = description,
            referenceId = getString(map, "referenceId"),
            date = getLong(map, "date") ?: 0L,
            userId = getStringOrEmpty(map, "userId"),
            idempotencyKey = getStringOrEmpty(map, "idempotencyKey"),
        )
    }

    data class ExpenseCategoryFields(
        val id: String, val tenantId: String, val nameBn: String, val icon: String,
        val isActive: Boolean,
    )

    fun expenseCategoryFromMap(map: Map<String, Any?>): ExpenseCategoryFields = ExpenseCategoryFields(
        id = getStringOrEmpty(map, "id"),
        tenantId = getStringOrEmpty(map, "tenantId"),
        nameBn = getStringOrEmpty(map, "nameBn"),
        icon = getStringOrEmpty(map, "icon"),
        isActive = getBoolean(map, "isActive"),
    )

    data class OwnerDrawingFields(
        val id: String, val tenantId: String, val amount: Double, val description: String,
        val drawingDate: Long, val userId: String, val idempotencyKey: String,
    )

    fun ownerDrawingFromMap(map: Map<String, Any?>): OwnerDrawingFields = OwnerDrawingFields(
        id = getStringOrEmpty(map, "id"),
        tenantId = getStringOrEmpty(map, "tenantId"),
        amount = getDouble(map, "amount") ?: 0.0,
        description = getStringOrEmpty(map, "description"),
        drawingDate = getLong(map, "drawingDate") ?: 0L,
        userId = getStringOrEmpty(map, "userId"),
        idempotencyKey = getStringOrEmpty(map, "idempotencyKey"),
    )
}
