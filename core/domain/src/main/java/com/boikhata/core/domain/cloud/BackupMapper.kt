package com.boikhata.core.domain.cloud

/**
 * D45: BackupMapper — pure entity→Firestore-map conversion.
 *
 * Firebase-Project-Context.md §6 constraints:
 * #4: negative ADJUSTMENT entries uploaded as magnitude + "Negative Adj: " prefix.
 * #6: incremental only (lastBackupAt filter) — re-uploading = update = denied on append-only.
 * §2: every document carries tenantId from claims (claims are authoritative, not the entity).
 *
 * CONVENTIONS §5: 10 backup-scope collections (audit_logs NEVER uploaded).
 *
 * Pure object — no Android, no Firestore SDK. Independently unit-testable.
 * The repository layer calls these functions and handles Firestore I/O.
 */
object BackupMapper {

    /** Prefix for negative ADJUSTMENT entries (constraint #4). */
    const val NEGATIVE_ADJ_PREFIX = "Negative Adj: "

    // ── Collection names (CONVENTIONS §5) ──────────────────────────────────────
    const val COL_BOOKS = "books"
    const val COL_STOCK_LEDGER = "stock_ledger"
    const val COL_BILLS = "bills"
    const val COL_BILL_LINES = "bill_lines"
    const val COL_KHATA_CUSTOMERS = "khata_customers"
    const val COL_KHATA_ENTRIES = "khata_entries"
    const val COL_EXPENSES = "expenses"
    const val COL_CASHBOOK_ENTRIES = "cashbook_entries"
    const val COL_EXPENSE_CATEGORIES = "expense_categories"
    const val COL_OWNER_DRAWINGS = "owner_drawings"

    /** All 10 backup-scope collections (audit_logs excluded — LOCAL-ONLY per §3). */
    val ALL_BACKUP_COLLECTIONS = listOf(
        COL_BOOKS,
        COL_STOCK_LEDGER,
        COL_BILLS,
        COL_BILL_LINES,
        COL_KHATA_CUSTOMERS,
        COL_KHATA_ENTRIES,
        COL_EXPENSES,
        COL_CASHBOOK_ENTRIES,
        COL_EXPENSE_CATEGORIES,
        COL_OWNER_DRAWINGS,
    )

    // ── Row filtering (incremental — constraint #6) ─────────────────────────────

    /**
     * Filter rows to only those newer than lastBackupAt.
     * Each collection has a different "last-modified" field:
     * - books: updatedAt
     * - stock_ledger: timestamp
     * - bills: billDate
     * - bill_lines: (no timestamp — always include if parent bill is new)
     * - khata_customers: updatedAt
     * - khata_entries: date
     * - expenses: expenseDate
     * - cashbook_entries: date
     * - expense_categories: (no timestamp — always include)
     * - owner_drawings: drawingDate
     *
     * If lastBackupAt is null (first backup), all rows are included.
     */
    fun filterNewRows(
        collectionName: String,
        rows: List<Map<String, Any?>>,
        lastBackupAt: Long?,
    ): List<Map<String, Any?>> {
        if (lastBackupAt == null) return rows // first backup — all rows
        val timestampField = timestampFieldForCollection(collectionName) ?: return rows
        return rows.filter { row ->
            val ts = row[timestampField] as? Long
            ts != null && ts > lastBackupAt
        }
    }

    /** The timestamp field used for incremental filtering, or null if the collection has no timestamp. */
    fun timestampFieldForCollection(collectionName: String): String? = when (collectionName) {
        COL_BOOKS -> "updatedAt"
        COL_STOCK_LEDGER -> "timestamp"
        COL_BILLS -> "billDate"
        COL_BILL_LINES -> null // no timestamp — filtered with parent bill
        COL_KHATA_CUSTOMERS -> "updatedAt"
        COL_KHATA_ENTRIES -> "date"
        COL_EXPENSES -> "expenseDate"
        COL_CASHBOOK_ENTRIES -> "date"
        COL_EXPENSE_CATEGORIES -> null // no timestamp — always include
        COL_OWNER_DRAWINGS -> "drawingDate"
        else -> null
    }

    // ── Negative-Adj handling (constraint #4) ───────────────────────────────────

    /**
     * Apply the Negative-Adj rule to a money-table row (khata_entries, cashbook_entries).
     * If amount < 0: upload abs(amount) and prepend the prefix to description.
     * If amount >= 0: unchanged.
     */
    fun applyNegativeAdj(amount: Double, description: String): Pair<Double, String> {
        return if (amount < 0) {
            Pair(kotlin.math.abs(amount), "$NEGATIVE_ADJ_PREFIX$description")
        } else {
            Pair(amount, description)
        }
    }

    /**
     * Check if a description carries the Negative-Adj prefix (for restore-side detection).
     */
    fun hasNegativeAdjPrefix(description: String): Boolean {
        return description.startsWith(NEGATIVE_ADJ_PREFIX)
    }

    // ── Entity → Firestore Map conversion ──────────────────────────────────────
    // Each function takes the entity fields + the claims tenantId and returns
    // a Map<String, Any?> for Firestore. tenantId is ALWAYS from claims.

    fun bookToMap(
        id: String, tenantId: String, isbn: String?, titleBn: String, titleEn: String?,
        author: String, publisher: String, classLevel: String, subject: String,
        editionYear: Int, category: String, condition: String, purchasePrice: Double,
        sellingPrice: Double, initialStock: Int, lowStockThreshold: Int, isActive: Boolean,
        createdAt: Long, updatedAt: Long,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "tenantId" to tenantId,
        "isbn" to isbn,
        "titleBn" to titleBn,
        "titleEn" to titleEn,
        "author" to author,
        "publisher" to publisher,
        "classLevel" to classLevel,
        "subject" to subject,
        "editionYear" to editionYear,
        "category" to category,
        "condition" to condition,
        "purchasePrice" to purchasePrice,
        "sellingPrice" to sellingPrice,
        "initialStock" to initialStock,
        "lowStockThreshold" to lowStockThreshold,
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

    fun stockLedgerToMap(
        id: String, tenantId: String, bookId: String, changeQuantity: Int,
        reason: String, referenceId: String?, userId: String, timestamp: Long,
        idempotencyKey: String,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "tenantId" to tenantId,
        "bookId" to bookId,
        "changeQuantity" to changeQuantity,
        "reason" to reason,
        "referenceId" to referenceId,
        "userId" to userId,
        "timestamp" to timestamp,
        "idempotencyKey" to idempotencyKey,
    )

    fun billToMap(
        id: String, tenantId: String, billNumber: String, customerId: String?,
        customerNameBn: String, customerPhone: String?, userId: String,
        subtotal: Double, discountAmount: Double, discountType: String, vatAmount: Double,
        totalAmount: Double, paymentMethod: String, paidAmount: Double, dueAmount: Double,
        khataEntryId: String?, billDate: Long, status: String, idempotencyKey: String,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "tenantId" to tenantId,
        "billNumber" to billNumber,
        "customerId" to customerId,
        "customerNameBn" to customerNameBn,
        "customerPhone" to customerPhone,
        "userId" to userId,
        "subtotal" to subtotal,
        "discountAmount" to discountAmount,
        "discountType" to discountType,
        "vatAmount" to vatAmount,
        "totalAmount" to totalAmount,
        "paymentMethod" to paymentMethod,
        "paidAmount" to paidAmount,
        "dueAmount" to dueAmount,
        "khataEntryId" to khataEntryId,
        "billDate" to billDate,
        "status" to status,
        "idempotencyKey" to idempotencyKey,
    )

    fun billLineToMap(
        id: String, tenantId: String, billId: String, bookId: String,
        bookTitleBn: String, quantity: Int, unitPrice: Double, lineTotal: Double,
        vatAmount: Double,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "tenantId" to tenantId,
        "billId" to billId,
        "bookId" to bookId,
        "bookTitleBn" to bookTitleBn,
        "quantity" to quantity,
        "unitPrice" to unitPrice,
        "lineTotal" to lineTotal,
        "vatAmount" to vatAmount,
    )

    fun khataCustomerToMap(
        id: String, tenantId: String, nameBn: String, phone: String?,
        address: String?, creditLimit: Double, isActive: Boolean, createdAt: Long, updatedAt: Long,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "tenantId" to tenantId,
        "nameBn" to nameBn,
        "phone" to phone,
        "address" to address,
        "creditLimit" to creditLimit,
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

    /**
     * khata_entries — money table with Negative-Adj rule (constraint #4).
     * amount + description are transformed if amount < 0.
     */
    fun khataEntryToMap(
        id: String, tenantId: String, customerId: String, amount: Double,
        type: String, description: String, referenceBillId: String?,
        collectedByUserId: String, date: Long, idempotencyKey: String,
    ): Map<String, Any?> {
        val (uploadAmount, uploadDesc) = applyNegativeAdj(amount, description)
        return mapOf(
            "id" to id,
            "tenantId" to tenantId,
            "customerId" to customerId,
            "amount" to uploadAmount,
            "type" to type,
            "description" to uploadDesc,
            "referenceBillId" to referenceBillId,
            "collectedByUserId" to collectedByUserId,
            "date" to date,
            "idempotencyKey" to idempotencyKey,
        )
    }

    fun expenseToMap(
        id: String, tenantId: String, categoryId: String, amount: Double,
        description: String, expenseDate: Long, receiptPhotoPath: String?,
        userId: String, idempotencyKey: String,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "tenantId" to tenantId,
        "categoryId" to categoryId,
        "amount" to amount,
        "description" to description,
        "expenseDate" to expenseDate,
        "receiptPhotoPath" to receiptPhotoPath,
        "userId" to userId,
        "idempotencyKey" to idempotencyKey,
    )

    /**
     * cashbook_entries — money table with Negative-Adj rule (constraint #4).
     */
    fun cashbookEntryToMap(
        id: String, tenantId: String, account: String, type: String, amount: Double,
        description: String, referenceId: String?, date: Long, userId: String,
        idempotencyKey: String,
    ): Map<String, Any?> {
        val (uploadAmount, uploadDesc) = applyNegativeAdj(amount, description)
        return mapOf(
            "id" to id,
            "tenantId" to tenantId,
            "account" to account,
            "type" to type,
            "amount" to uploadAmount,
            "description" to uploadDesc,
            "referenceId" to referenceId,
            "date" to date,
            "userId" to userId,
            "idempotencyKey" to idempotencyKey,
        )
    }

    fun expenseCategoryToMap(
        id: String, tenantId: String, nameBn: String, icon: String, isActive: Boolean,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "tenantId" to tenantId,
        "nameBn" to nameBn,
        "icon" to icon,
        "isActive" to isActive,
    )

    fun ownerDrawingToMap(
        id: String, tenantId: String, amount: Double, description: String,
        drawingDate: Long, userId: String, idempotencyKey: String,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "tenantId" to tenantId,
        "amount" to amount,
        "description" to description,
        "drawingDate" to drawingDate,
        "userId" to userId,
        "idempotencyKey" to idempotencyKey,
    )
}
