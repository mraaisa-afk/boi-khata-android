package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.BillDao
import com.boikhata.core.database.dao.BookDao
import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.ExpenseCategoryDao
import com.boikhata.core.database.dao.ExpenseDao
import com.boikhata.core.database.dao.KhataCustomerDao
import com.boikhata.core.database.dao.KhataEntryDao
import com.boikhata.core.database.dao.OwnerDrawingDao
import com.boikhata.core.database.dao.PeriodLockDao
import com.boikhata.core.database.dao.StockLedgerDao
import com.boikhata.core.database.entity.PeriodLockEntity
import com.boikhata.core.domain.accounting.BalanceSheetCalculator
import com.boikhata.core.domain.accounting.BengaliFiscalCalendar
import com.boikhata.core.domain.accounting.PnLCalculator
import com.boikhata.core.domain.aging.AgingCalculator
import com.boikhata.core.domain.aging.KhataEntry
import com.boikhata.core.domain.enums.BookCategory
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.model.BalanceSheetLite
import com.boikhata.core.domain.model.HisabPack
import com.boikhata.core.domain.model.KhataAgingSummary
import com.boikhata.core.domain.model.PeriodLock
import com.boikhata.core.domain.model.PnLReport
import com.boikhata.core.domain.model.VatSummary
import com.boikhata.core.domain.repository.AccountingRepository
import com.boikhata.core.domain.license.LicenseWriteGuard
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P3b: AccountingRepository implementation.
 * D29: P&L with COGS split. D30: dual-calendar. D31: balance-sheet lite.
 * D32: period-lock. D33: হিসাব-প্যাক.
 */
@Singleton
class AccountingRepositoryImpl @Inject constructor(
    private val billDao: BillDao,
    private val bookDao: BookDao,
    private val stockLedgerDao: StockLedgerDao,
    private val expenseDao: ExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val cashbookDao: CashbookDao,
    private val khataCustomerDao: KhataCustomerDao,
    private val khataEntryDao: KhataEntryDao,
    private val ownerDrawingDao: OwnerDrawingDao,
    private val periodLockDao: PeriodLockDao,
    private val writeGuard: LicenseWriteGuard,
) : AccountingRepository {

    override suspend fun getMonthlyPnL(tenantId: String, year: Int, month: Int): PnLReport {
        val start = BengaliFiscalCalendar.gregorianMonthStart(year, month)
        val end = BengaliFiscalCalendar.gregorianMonthEndExclusive(year, month)

        val bills = billDao.getByDateRange(tenantId, start, end)
        val expenses = expenseDao.getByDateRange(tenantId, start, end)
        val drawings = ownerDrawingDao.getByDateRange(tenantId, start, end)

        // Build PnLCalculator bill inputs with book purchase prices for COGS
        val bookCache = mutableMapOf<String, Double>()
        val pnlBills = bills.map { bill ->
            val lines = billDao.getLinesByBill(bill.id)
            val pnlLines = lines.map { line ->
                val purchasePrice = bookCache.getOrPut(line.bookId) {
                    bookDao.getById(line.bookId)?.purchasePrice ?: 0.0
                }
                PnLCalculator.BillLineForPnL(
                    bookId = line.bookId,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    lineTotal = line.lineTotal,
                    vatAmount = line.vatAmount,
                    bookPurchasePrice = purchasePrice,
                )
            }
            PnLCalculator.BillForPnL(
                id = bill.id,
                subtotal = bill.subtotal,
                discountAmount = bill.discountAmount,
                vatAmount = bill.vatAmount,
                totalAmount = bill.totalAmount,
                paidAmount = bill.paidAmount,
                billDate = bill.billDate,
                lines = pnlLines,
            )
        }

        val expensesTotal = expenses.sumOf { it.amount }
        val drawingsTotal = drawings.sumOf { it.amount }
        // Consignment commission = 0.0 until P5 (supplier/consignment module) lands.
        val consignmentCommission = 0.0

        return PnLCalculator.compute(
            bills = pnlBills,
            expensesTotal = expensesTotal,
            ownerDrawingsTotal = drawingsTotal,
            consignmentCommission = consignmentCommission,
            gregorianYear = year,
            gregorianMonth = month,
        )
    }

    override suspend fun getBalanceSheet(tenantId: String, asOfDate: Long): BalanceSheetLite {
        // Cash: derived from all cashbook entries up to asOfDate
        val cashEntries = cashbookDao.getByDateRange(tenantId, 0, asOfDate + 1)
            .map { it.toDomainCashbook() }
        val balances = com.boikhata.core.domain.accounting.CashbookBalanceCalculator
            .calculateAllBalances(cashEntries)

        // Inventory: current stock per book × purchasePrice
        val books = bookDao.getActiveByTenant(tenantId)
        val inventory = books.map { book ->
            val stockQty = stockLedgerDao.getStockQuantityForBook(book.id)
            BalanceSheetCalculator.BookInventory(
                bookId = book.id,
                purchasePrice = book.purchasePrice,
                stockQuantity = stockQty,
            )
        }

        // Receivables: sum of all khata customer due balances
        val customers = khataCustomerDao.getActiveByTenant(tenantId)
        var receivables = 0.0
        for (customer in customers) {
            val entries = khataEntryDao.getByCustomer(tenantId, customer.id).map { it.toDomainKhataEntry() }
            val aging = AgingCalculator.calculate(entries, asOfDate)
            receivables += aging.totalDue
        }

        // ঘরি advances: derived from ঘরি category expenses (D26)
        val ghoriCategory = expenseCategoryDao.getActiveByTenant(tenantId)
            .firstOrNull { it.nameBn == "ঘরি" }
        val ghoriAdvances = if (ghoriCategory != null) {
            val ghoriExpenses = expenseDao.getByCategory(tenantId, ghoriCategory.id)
            com.boikhata.core.domain.accounting.GoriBalanceCalculator
                .calculateBalance(ghoriExpenses.map { it.toDomainExpense() })
        } else 0.0

        // Retained earnings: sum of all prior months' net profit (approximation —
        // for P3b we compute it as total revenue − total COGS − total expenses all time)
        val allBills = billDao.getByTenant(tenantId)
        val allExpenses = expenseDao.getByTenant(tenantId)
        val allDrawings = ownerDrawingDao.getByTenant(tenantId)
        val totalRevenue = allBills.sumOf { it.subtotal - it.discountAmount }
        val totalCogs = allBills.flatMap { billDao.getLinesByBill(it.id) }.sumOf { line ->
            val pp = books.firstOrNull { it.id == line.bookId }?.purchasePrice ?: 0.0
            pp * line.quantity
        }
        val totalExpenses = allExpenses.sumOf { it.amount }
        val retainedEarnings = totalRevenue - totalCogs - totalExpenses
        val totalDrawings = allDrawings.sumOf { it.amount }

        return BalanceSheetCalculator.compute(
            cashbookBalances = balances,
            inventory = inventory,
            receivables = receivables,
            ghoriAdvances = ghoriAdvances,
            supplierPayables = 0.0, // P5 scope
            retainedEarnings = retainedEarnings,
            totalDrawings = totalDrawings,
            asOfDate = asOfDate,
        )
    }

    override suspend fun isPeriodLocked(tenantId: String, year: Int, month: Int): Boolean {
        return periodLockDao.findLock(tenantId, year, month) != null
    }

    override suspend fun lockPeriod(tenantId: String, year: Int, month: Int, userId: String): String {
        writeGuard.assertWriteAllowed()
        // Only lock if not already locked (idempotency)
        val existing = periodLockDao.findLock(tenantId, year, month)
        if (existing != null) return existing.id
        val id = UUID.randomUUID().toString()
        periodLockDao.insert(
            PeriodLockEntity(
                id = id,
                tenantId = tenantId,
                periodYear = year,
                periodMonth = month,
                lockedAt = System.currentTimeMillis(),
                lockedByUserId = userId,
            )
        )
        return id
    }

    override suspend fun getLockedPeriods(tenantId: String): List<PeriodLock> {
        return periodLockDao.getByTenant(tenantId).map {
            PeriodLock(
                id = it.id,
                tenantId = it.tenantId,
                periodYear = it.periodYear,
                periodMonth = it.periodMonth,
                lockedAt = it.lockedAt,
                lockedByUserId = it.lockedByUserId,
            )
        }
    }

    override suspend fun getKhataAgingSummary(tenantId: String, asOfDate: Long): KhataAgingSummary {
        val customers = khataCustomerDao.getActiveByTenant(tenantId)
        var totalDue = 0.0
        var green = 0.0
        var yellow = 0.0
        var red = 0.0
        for (customer in customers) {
            val entries = khataEntryDao.getByCustomer(tenantId, customer.id).map { it.toDomainKhataEntry() }
            val aging = AgingCalculator.calculate(entries, asOfDate)
            totalDue += aging.totalDue
            when (aging.bucket) {
                com.boikhata.core.domain.aging.AgingBucket.GREEN -> green += aging.totalDue
                com.boikhata.core.domain.aging.AgingBucket.YELLOW -> yellow += aging.totalDue
                com.boikhata.core.domain.aging.AgingBucket.RED -> red += aging.totalDue
                com.boikhata.core.domain.aging.AgingBucket.NONE -> { /* no due */ }
            }
        }
        return KhataAgingSummary(
            totalDue = totalDue,
            greenBucket = green,
            yellowBucket = yellow,
            redBucket = red,
            customerCount = customers.size,
        )
    }

    override suspend fun getVatSummary(tenantId: String, start: Long, end: Long): VatSummary {
        val bills = billDao.getByDateRange(tenantId, start, end)
        var booksVat = 0.0
        var stationeryVat = 0.0
        for (bill in bills) {
            val lines = billDao.getLinesByBill(bill.id)
            for (line in lines) {
                val book = bookDao.getById(line.bookId)
                val category = book?.category?.let { runCatching { BookCategory.valueOf(it) }.getOrNull() }
                if (category == BookCategory.STATIONERY) {
                    stationeryVat += line.vatAmount
                } else {
                    booksVat += line.vatAmount // books 0% — always 0 but tracked for completeness
                }
            }
        }
        return VatSummary(
            booksVat = booksVat,
            stationeryVat = stationeryVat,
            totalVat = booksVat + stationeryVat,
        )
    }

    override suspend fun getHisabPack(
        tenantId: String,
        year: Int,
        month: Int,
        shopName: String,
    ): HisabPack {
        val pnl = getMonthlyPnL(tenantId, year, month)
        val end = BengaliFiscalCalendar.gregorianMonthEndExclusive(year, month)
        val balanceSheet = getBalanceSheet(tenantId, end)
        val agingSummary = getKhataAgingSummary(tenantId, end)
        val start = BengaliFiscalCalendar.gregorianMonthStart(year, month)
        val vatSummary = getVatSummary(tenantId, start, end)
        return HisabPack(
            shopName = shopName,
            pnl = pnl,
            balanceSheet = balanceSheet,
            agingSummary = agingSummary,
            vatSummary = vatSummary,
        )
    }

    // ── mappers ──────────────────────────────────────────────────────────────
    private fun com.boikhata.core.database.entity.CashbookEntryEntity.toDomainCashbook() =
        com.boikhata.core.domain.model.CashbookEntry(
            id = id,
            account = com.boikhata.core.domain.enums.CashbookAccount.valueOf(account),
            type = com.boikhata.core.domain.enums.CashbookEntryType.valueOf(type),
            amount = amount,
            description = description,
            referenceId = referenceId,
            date = date,
            userId = userId,
        )

    private fun com.boikhata.core.database.entity.KhataEntryEntity.toDomainKhataEntry() =
        KhataEntry(
            id = id,
            type = KhataEntryType.valueOf(type),
            amount = amount,
            date = date,
            description = description,
            referenceBillId = referenceBillId,
        )

    private fun com.boikhata.core.database.entity.ExpenseEntity.toDomainExpense() =
        com.boikhata.core.domain.model.Expense(
            id = id,
            categoryId = categoryId,
            categoryNameBn = "",
            amount = amount,
            description = description,
            expenseDate = expenseDate,
            receiptPhotoPath = receiptPhotoPath,
            userId = userId,
        )
}
