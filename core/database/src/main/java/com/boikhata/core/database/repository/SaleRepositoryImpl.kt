package com.boikhata.core.database.repository

import androidx.room.withTransaction
import com.boikhata.core.database.BoiKhataDatabase
import com.boikhata.core.database.dao.BillDao
import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.KhataEntryDao
import com.boikhata.core.database.dao.StockLedgerDao
import com.boikhata.core.database.entity.BillEntity
import com.boikhata.core.database.entity.BillLineEntity
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.KhataEntryEntity
import com.boikhata.core.database.entity.StockLedgerEntity
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.Bill
import com.boikhata.core.domain.model.BillLine
import com.boikhata.core.domain.model.BillSummary
import com.boikhata.core.domain.pilot.TrialPolicy
import com.boikhata.core.domain.repository.BillLineInput
import com.boikhata.core.domain.repository.BillRepository
import com.boikhata.core.domain.sale.BillNumberGenerator
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
     */
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

        // D22: Determine paid and due amounts
        val actualPaid = when (paymentMethod) {
            PaymentMethod.CREDIT -> 0.0 // entire bill on khata
            else -> paidAmount.coerceAtMost(totalAmount)
        }
        val dueAmount = (totalAmount - actualPaid).coerceAtLeast(0.0)
        val status = if (dueAmount > 0.01) "PARTIAL" else "COMPLETED"

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
            paymentMethod = paymentMethod.name,
            paidAmount = actualPaid,
            dueAmount = dueAmount,
            khataEntryId = null, // set after khata entry creation if due > 0
            billDate = now,
            status = status,
            idempotencyKey = UUID.randomUUID().toString(),
        )

        // D22: Atomic transaction — bill + lines + stock + khata
        db.withTransaction {
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

            // 5. D34: Cashbook auto-populate — bill payment creates INCOME entry
            // Account from paymentMethod: CASH→CASH, BKASH→BKASH. Amount = actualPaid.
            // Only when actualPaid > 0 (pure-credit bill with paidAmount=0 → no money moved).
            if (actualPaid > 0.01) {
                val cashbookAccount = when (paymentMethod) {
                    PaymentMethod.CASH -> "CASH"
                    PaymentMethod.BKASH -> "BKASH"
                    PaymentMethod.NAGAD -> "BKASH" // NAGAD treated as mobile-money → BKASH bucket
                    PaymentMethod.CREDIT -> null     // pure credit → no cashbook entry
                }
                if (cashbookAccount != null) {
                    cashbookDao.insert(
                        CashbookEntryEntity(
                            id = UUID.randomUUID().toString(),
                            tenantId = tenantId,
                            account = cashbookAccount,
                            type = "INCOME",
                            amount = actualPaid,
                            description = "বিক্রি ($billNumber)",
                            referenceId = billId,
                            date = now,
                            userId = userId,
                            idempotencyKey = UUID.randomUUID().toString(),
                        )
                    )
                }
            }
        }

        return billId
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
