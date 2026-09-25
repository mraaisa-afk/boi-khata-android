package com.boikhata.core.database.repository

import androidx.room.withTransaction
import com.boikhata.core.database.BoiKhataDatabase
import com.boikhata.core.database.dao.BillDao
import com.boikhata.core.database.dao.BillPaymentLineDao
import com.boikhata.core.database.dao.BookDao
import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.KhataEntryDao
import com.boikhata.core.database.dao.StockLedgerDao
import com.boikhata.core.database.entity.BillEntity
import com.boikhata.core.database.entity.BillLineEntity
import com.boikhata.core.database.entity.BillPaymentLineEntity
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.KhataEntryEntity
import com.boikhata.core.database.entity.StockLedgerEntity
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.enums.MfsProvider
import com.boikhata.core.domain.enums.PaymentLineCategory
import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.Bill
import com.boikhata.core.domain.model.BillLine
import com.boikhata.core.domain.model.BillSummary
import com.boikhata.core.domain.pilot.TrialPolicy
import com.boikhata.core.domain.repository.BillLineInput
import com.boikhata.core.domain.repository.BillRepository
import com.boikhata.core.domain.repository.PaymentLine
import com.boikhata.core.domain.repository.PaymentLineSpec
import com.boikhata.core.domain.sale.BillNumberGenerator
import com.boikhata.core.domain.sale.InsufficientStockException
import com.boikhata.core.domain.sale.VatCalculator
import java.util.UUID
import javax.inject.Inject

/**
 * P2b: BillRepository implementation with POS sale flow.
 * D22: createBill is an atomic Room transaction — bill + lines + stock + khata.
 * D32: Period-lock check before write.
 * D34: Cashbook auto-populate from bill payment (INCOME, account from paymentMethod).
 */
class SaleRepositoryImpl @Inject constructor(
    private val db: BoiKhataDatabase,
    private val billDao: BillDao,
    private val billPaymentLineDao: BillPaymentLineDao,
    private val bookDao: BookDao, // P14: live-stock gate inside the D22 transaction
    private val stockLedgerDao: StockLedgerDao,
    private val khataEntryDao: KhataEntryDao,
    private val cashbookDao: CashbookDao,
    private val writeGuard: LicenseWriteGuard,
    private val periodLockChecker: PeriodLockChecker,
) : BillRepository {

    override suspend fun getBillsByDate(tenantId: String, startOfDay: Long, endOfDay: Long): List<BillSummary> {
        return billDao.getByDateRange(tenantId, startOfDay, endOfDay).map { it.toSummary() }
    }

    override suspend fun getTopBills(tenantId: String, limit: Int): List<BillSummary> {
        return billDao.getByTenant(tenantId).take(limit).map { it.toSummary() }
    }

    override suspend fun getAllBills(tenantId: String): List<BillSummary> {
        return billDao.getByTenant(tenantId).map { it.toSummary() }
    }

    /** B-006: sales history for the khata customer detail screen. */
    override suspend fun getBillsByCustomer(tenantId: String, customerId: String): List<BillSummary> {
        return billDao.getByCustomer(tenantId, customerId).map { it.toSummary() }
    }

    override suspend fun getBill(tenantId: String, billId: String): Bill? {
        val entity = billDao.getById(billId) ?: return null
        return entity.toDomain()
    }

    override suspend fun getBillLines(billId: String): List<BillLine> {
        return billDao.getLinesByBill(billId).map {
            BillLine(
                id = it.id,
                billId = it.billId,
                bookId = it.bookId,
                bookTitleBn = it.bookTitleBn,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                lineTotal = it.lineTotal,
                vatAmount = it.vatAmount,
            )
        }
    }

    /**
     * D22: Atomic bill creation — bill + lines + stock ledger + auto-khata in one transaction.
     * Either everything succeeds or nothing does.
     *
     * P12: legacy single-method entry point kept for existing callers/tests —
     * delegates to [createBillWithPaymentLines] with an equivalent line set
     * (CREDIT → pure বাকি; CASH → one cash line; BKASH/NAGAD → one MOBILE line
     * with the legacy provider; remainder → DUE).
     */
    @Deprecated("P12: use createBillWithPaymentLines")
    override suspend fun createBill(
        tenantId: String,
        customerId: String?,
        customerNameBn: String,
        customerPhone: String?,
        userId: String,
        lines: List<BillLineInput>,
        discountAmount: Double,
        discountType: String,
        paymentMethod: PaymentMethod,
        paidAmount: Double,
    ): String {
        val paidLines = when (paymentMethod) {
            PaymentMethod.CREDIT -> emptyList()
            PaymentMethod.CASH -> listOf(PaymentLineSpec(PaymentLineCategory.CASH, null, paidAmount))
            PaymentMethod.BKASH -> listOf(PaymentLineSpec(PaymentLineCategory.MOBILE, MfsProvider.BKASH, paidAmount))
            PaymentMethod.NAGAD -> listOf(PaymentLineSpec(PaymentLineCategory.MOBILE, MfsProvider.NAGAD, paidAmount))
            // P12 summary values for bills restored from newer backups:
            PaymentMethod.BANK -> listOf(PaymentLineSpec(PaymentLineCategory.BANK, null, paidAmount))
            PaymentMethod.MOBILE -> listOf(PaymentLineSpec(PaymentLineCategory.MOBILE, MfsProvider.OTHER, paidAmount))
        }.filter { it.amount > 0.0 }
        return createBillWithPaymentLines(
            tenantId = tenantId,
            customerId = customerId,
            customerNameBn = customerNameBn,
            customerPhone = customerPhone,
            userId = userId,
            lines = lines,
            discountAmount = discountAmount,
            discountType = discountType,
            paidLines = paidLines,
        )
    }

    /**
     * P12/D92: multi-line checkout — the authoritative payment record is
     * bill_payment_lines. Everything below runs in ONE D22 atomic transaction:
     * bill + item lines + stock ledger + khata CREDIT (বাকি) + one cashbook
     * INCOME mirror PER PAID LINE (CASH→CASH, BANK→BANK, MOBILE→MOBILE bucket —
     * owner ruling: mobile banking no longer reuses the BKASH bucket) + the
     * bill_payment_lines rows themselves.
     *
     * Validation is fail-fast (IllegalArgumentException) BEFORE the transaction:
     * - every paid line amount > 0
     * - MOBILE lines must carry a provider; CASH/BANK must not
     * - sum(paid) > total is allowed ONLY for a named customer — the excess
     *   posts to their khata as a জমা entry (D93); walk-ins are rejected
     * - বাকি > 0 requires a customer (posts to that customer's খাতা)
     */
    override suspend fun createBillWithPaymentLines(
        tenantId: String,
        customerId: String?,
        customerNameBn: String,
        customerPhone: String?,
        userId: String,
        lines: List<BillLineInput>,
        discountAmount: Double,
        discountType: String,
        paidLines: List<PaymentLineSpec>,
    ): String {
        writeGuard.assertWriteAllowed()
        TrialPolicy.assertCanAddBill(TrialPolicy.Usage(billDao.countForTenant(tenantId), 0))
        // D32: Period-lock check — the bill date must not fall in a locked period
        val now = System.currentTimeMillis()
        periodLockChecker.assertNotLocked(tenantId, now)

        val billId = UUID.randomUUID().toString()

        // D20: Generate bill number
        val datePattern = BillNumberGenerator.datePattern(now)
        val maxBillNumber = billDao.getMaxBillNumber(tenantId, datePattern)
        val maxSeq = maxBillNumber?.let { BillNumberGenerator.extractSequence(it) } ?: 0
        val billNumber = BillNumberGenerator.generate(now, maxSeq)

        // D19: Calculate per-line totals and VAT
        val billLines = lines.map { input ->
            val lineBase = input.unitPrice * input.quantity
            val lineVat = VatCalculator.calculateLineVat(input.unitPrice, input.quantity, input.category)
            BillLineEntity(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                billId = billId,
                bookId = input.bookId,
                bookTitleBn = input.bookTitleBn,
                quantity = input.quantity,
                unitPrice = input.unitPrice,
                lineTotal = lineBase, // pre-VAT line total
                vatAmount = lineVat,
            )
        }

        val subtotal = billLines.sumOf { it.lineTotal }
        val vatAmount = billLines.sumOf { it.vatAmount }
        val cappedDiscount = discountAmount.coerceAtMost(subtotal + vatAmount).coerceAtLeast(0.0)
        val totalAmount = subtotal + vatAmount - cappedDiscount

        // ── P12/D92 validation (fail-fast before the transaction) ──
        paidLines.forEach { line ->
            require(line.amount > 0.0) { "পেমেন্ট লাইনের পরিমাণ ০-এর বেশি হতে হবে" }
            when (line.category) {
                PaymentLineCategory.MOBILE ->
                    require(line.provider != null) { "মোবাইল ব্যাংকিং লাইনে প্রোভাইডার নির্বাচন করুন" }
                PaymentLineCategory.CASH, PaymentLineCategory.BANK ->
                    require(line.provider == null) { "এই মাধ্যমে প্রোভাইডার প্রযোজ্য নয়" }
                PaymentLineCategory.DUE ->
                    throw IllegalArgumentException("বাকি লাইন নিজে থেকে গণনা হয় — আলাদাভাবে দেওয়া যাবে না")
            }
        }
        val sumPaid = paidLines.sumOf { it.amount }
        // D93 (owner ruling 2026-09-24): overpayment is ALLOWED for a named
        // customer — the excess becomes a জমা (PAYMENT) entry on their khata
        // (e.g. ৳1500 handed over for a ৳1000 book while an old ৳1200 due stands).
        // For a walk-in (হাটি ক্রেতা) there is no khata to absorb the excess → reject.
        val overpayment = (sumPaid - totalAmount).coerceAtLeast(0.0)
        if (overpayment > 0.01) {
            require(customerId != null) { "হাটি ক্রেতার জন্য অতিরিক্ত জমা রাখা সম্ভব নয়। অতিরিক্ত ফেরত দিন।" }
        }
        val dueAmount = ((totalAmount - sumPaid).coerceAtLeast(0.0))
        if (dueAmount > 0.01) {
            require(customerId != null) { "বাকি থাকলে ক্রেতা নির্বাচন করুন" }
        }
        val status = if (dueAmount > 0.01) "PARTIAL" else "COMPLETED"

        // Display summary for the denormalized bills column (legacy format kept
        // parseable; the authoritative breakdown lives in bill_payment_lines).
        val summaryMethod = when {
            paidLines.isEmpty() -> PaymentMethod.CREDIT
            paidLines.size == 1 -> {
                val l = paidLines.first()
                when (l.category) {
                    PaymentLineCategory.CASH -> PaymentMethod.CASH
                    PaymentLineCategory.BANK -> PaymentMethod.BANK
                    PaymentLineCategory.MOBILE -> when (l.provider) {
                        MfsProvider.BKASH -> PaymentMethod.BKASH
                        MfsProvider.NAGAD -> PaymentMethod.NAGAD
                        else -> PaymentMethod.MOBILE
                    }
                    PaymentLineCategory.DUE -> PaymentMethod.CREDIT
                }
            }
            else -> when {
                paidLines.any { it.category == PaymentLineCategory.MOBILE } -> PaymentMethod.MOBILE
                paidLines.any { it.category == PaymentLineCategory.BANK } -> PaymentMethod.BANK
                else -> PaymentMethod.CASH
            }
        }

        val billEntity = BillEntity(
            id = billId,
            tenantId = tenantId,
            billNumber = billNumber,
            customerId = customerId,
            customerNameBn = customerNameBn,
            customerPhone = customerPhone,
            userId = userId,
            subtotal = subtotal,
            discountAmount = cappedDiscount,
            discountType = discountType,
            vatAmount = vatAmount,
            totalAmount = totalAmount,
            paymentMethod = summaryMethod.name,
            // "paid toward THIS bill" — capped at the total; the excess is the
            // khata জমা written in the transaction below (D93).
            paidAmount = sumPaid.coerceAtMost(totalAmount),
            dueAmount = dueAmount,
            khataEntryId = null, // set after khata entry creation if due > 0
            billDate = now,
            status = status,
            idempotencyKey = UUID.randomUUID().toString(),
        )

        // D22: Atomic transaction — bill + lines + stock + khata + payment lines
        db.withTransaction {
            // 0. P14 (owner device ruling — «মাইনাস নয়»): negative-stock gate —
            // FIRST statement in the transaction, before any write. LIVE stock =
            // books.initialStock + stock_ledger delta (B-005 derivation). A sale
            // line may never exceed it; throwing here aborts the WHOLE D22
            // transaction — no bill, no lines, no ledger rows, no khata/cashbook.
            // The device finding (stock driven to −9) is impossible after this gate.
            for (line in billLines) {
                val openingStock = bookDao.getById(line.bookId)?.initialStock ?: 0
                val ledgerDelta = stockLedgerDao.getStockQuantityForBook(line.bookId)
                val availableStock = openingStock + ledgerDelta
                if (line.quantity > availableStock) {
                    throw InsufficientStockException(
                        bookTitleBn = line.bookTitleBn,
                        availableStock = availableStock,
                        requestedQuantity = line.quantity,
                    )
                }
            }

            // 1. Insert bill
            billDao.insert(billEntity)

            // 2. Insert bill lines
            billDao.insertLines(billLines)

            // 3. Insert stock ledger entries (SALE, negative quantity)
            for (line in billLines) {
                stockLedgerDao.insert(
                    StockLedgerEntity(
                        id = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        bookId = line.bookId,
                        changeQuantity = -line.quantity, // sale = negative
                        reason = "SALE",
                        referenceId = billId,
                        userId = userId,
                        timestamp = now,
                        idempotencyKey = UUID.randomUUID().toString(),
                    )
                )
            }

            // 4. D22: Auto-khata — if due > 0 and customer exists, create CREDIT entry
            if (dueAmount > 0.01 && customerId != null) {
                val khataEntryId = UUID.randomUUID().toString()
                khataEntryDao.insert(
                    KhataEntryEntity(
                        id = khataEntryId,
                        tenantId = tenantId,
                        customerId = customerId,
                        amount = dueAmount,
                        type = KhataEntryType.CREDIT.name,
                        description = "বিক্রি বাকি ($billNumber)",
                        referenceBillId = billId,
                        collectedByUserId = userId,
                        date = now,
                        idempotencyKey = UUID.randomUUID().toString(),
                    )
                )
                // Link khata entry back to bill
                billDao.updateKhataEntryId(billId, khataEntryId)
            }

            // 4b. D93: overpayment → জমা (PAYMENT) entry on the customer's khata,
            // reducing their outstanding balance — SAME D22 transaction (owner:
            // the credit write must be inside this transaction, not a separate call).
            // NO extra cashbook row: the money was already mirrored in full via the
            // paid lines — a second INCOME would double-count the cash.
            if (overpayment > 0.01 && customerId != null) {
                khataEntryDao.insert(
                    KhataEntryEntity(
                        id = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        customerId = customerId,
                        amount = overpayment,
                        type = KhataEntryType.PAYMENT.name,
                        description = "অতিরিক্ত জমা (খাতায়) ($billNumber)",
                        referenceBillId = billId,
                        collectedByUserId = userId,
                        date = now,
                        idempotencyKey = UUID.randomUUID().toString(),
                    )
                )
            }

            // 5. D34 per-line cashbook mirror + 6. P12 payment-line rows —
            // one INCOME row per PAID line (CASH→CASH, BANK→BANK, MOBILE→MOBILE
            // bucket; owner ruling 2026-09-24: mobile banking gets its own bucket,
            // never reuses BKASH). DUE line: no cashbook row (no money moved).
            val paymentLineEntities = mutableListOf<BillPaymentLineEntity>()
            for (line in paidLines) {
                val cashbookAccount = when (line.category) {
                    PaymentLineCategory.CASH -> "CASH"
                    PaymentLineCategory.BANK -> "BANK"
                    PaymentLineCategory.MOBILE -> "MOBILE"
                    PaymentLineCategory.DUE -> null
                }
                var cashbookEntryId: String? = null
                if (cashbookAccount != null) {
                    val entryId = UUID.randomUUID().toString()
                    cashbookEntryId = entryId
                    val providerLabel = line.provider?.name // e.g. "BKASH"/"NAGAD"/"ROCKET"
                    cashbookDao.insert(
                        CashbookEntryEntity(
                            id = entryId,
                            tenantId = tenantId,
                            account = cashbookAccount,
                            type = "INCOME",
                            amount = line.amount,
                            description = if (providerLabel != null) {
                                "বিক্রি ($billNumber) — $providerLabel"
                            } else {
                                "বিক্রি ($billNumber)"
                            },
                            referenceId = billId,
                            date = now,
                            userId = userId,
                            idempotencyKey = UUID.randomUUID().toString(),
                        )
                    )
                }
                paymentLineEntities += BillPaymentLineEntity(
                    id = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    billId = billId,
                    method = line.category.name,
                    provider = line.provider?.name,
                    amount = line.amount,
                    cashbookEntryId = cashbookEntryId,
                    createdAt = now,
                )
            }
            if (dueAmount > 0.01) {
                paymentLineEntities += BillPaymentLineEntity(
                    id = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    billId = billId,
                    method = PaymentLineCategory.DUE.name,
                    provider = null,
                    amount = dueAmount,
                    cashbookEntryId = null,
                    createdAt = now,
                )
            }
            billPaymentLineDao.insertAll(paymentLineEntities)
        }

        return billId
    }

    override suspend fun getPaymentLines(billId: String): List<PaymentLine> {
        return billPaymentLineDao.getByBill(billId).map {
            PaymentLine(
                id = it.id,
                billId = it.billId,
                category = PaymentLineCategory.valueOf(it.method),
                provider = it.provider?.let { p -> MfsProvider.valueOf(p) },
                amount = it.amount,
            )
        }
    }

    /** D94: COGS over all bills in the window (credit sales included). */
    override suspend fun getCogsByDateRange(tenantId: String, start: Long, end: Long): Double {
        return billDao.getCogsByDateRange(tenantId, start, end)
    }

    private fun BillEntity.toSummary() = BillSummary(
        id = id,
        billNumber = billNumber,
        customerNameBn = customerNameBn,
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        dueAmount = dueAmount,
        billDate = billDate,
        status = status,
    )

    private fun BillEntity.toDomain() = Bill(
        id = id,
        billNumber = billNumber,
        customerId = customerId,
        customerNameBn = customerNameBn,
        customerPhone = customerPhone,
        userId = userId,
        subtotal = subtotal,
        discountAmount = discountAmount,
        discountType = discountType,
        vatAmount = vatAmount,
        totalAmount = totalAmount,
        paymentMethod = PaymentMethod.valueOf(paymentMethod),
        paidAmount = paidAmount,
        dueAmount = dueAmount,
        khataEntryId = khataEntryId,
        billDate = billDate,
        status = status,
    )
}
