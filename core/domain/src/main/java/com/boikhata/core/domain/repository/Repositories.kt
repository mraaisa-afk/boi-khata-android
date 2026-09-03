package com.boikhata.core.domain.repository

import com.boikhata.core.domain.aging.KhataEntry
import com.boikhata.core.domain.enums.BookCategory
import com.boikhata.core.domain.enums.BookCondition
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.CashbookEntryType
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.license.GraceState
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.Bill
import com.boikhata.core.domain.model.BillLine
import com.boikhata.core.domain.model.Book
import com.boikhata.core.domain.model.CashbookBalance
import com.boikhata.core.domain.model.CashbookEntry
import com.boikhata.core.domain.model.Expense
import com.boikhata.core.domain.model.ExpenseCategory
import com.boikhata.core.domain.model.KhataCustomer
import com.boikhata.core.domain.model.KhataInstallment
import com.boikhata.core.domain.model.OwnerDrawing

/**
 * Repository interfaces — core/domain owns the contracts; core/database + core/cloud
 * provide the implementations. Feature modules depend on these interfaces (CONVENTIONS §6).
 */

interface UserRepository {
    suspend fun getUsers(tenantId: String): List<com.boikhata.core.domain.model.User>
    suspend fun verifyPin(tenantId: String, pin: String): com.boikhata.core.domain.model.User?
}

// ── P2a: Catalog ──────────────────────────────────────────────────────────────

interface BookRepository {
    suspend fun getBooks(tenantId: String): List<Book>
    suspend fun searchBooks(tenantId: String, normalizedQuery: String): List<Book>
    suspend fun getBook(tenantId: String, id: String): Book?
    suspend fun addBook(
        tenantId: String,
        isbn: String?,
        titleBn: String,
        titleEn: String?,
        author: String,
        publisher: String,
        classLevel: String,
        subject: String,
        editionYear: Int,
        category: BookCategory,
        condition: BookCondition,
        purchasePrice: Double,
        sellingPrice: Double,
        initialStock: Int,
        lowStockThreshold: Int,
    ): String

    suspend fun updateBook(
        tenantId: String,
        id: String,
        isbn: String?,
        titleBn: String,
        titleEn: String?,
        author: String,
        publisher: String,
        classLevel: String,
        subject: String,
        editionYear: Int,
        category: BookCategory,
        condition: BookCondition,
        purchasePrice: Double,
        sellingPrice: Double,
        lowStockThreshold: Int,
        isActive: Boolean,
    )
}

// ── P2a: Khata depth ──────────────────────────────────────────────────────────

interface KhataRepository {
    suspend fun getCustomers(tenantId: String): List<KhataCustomer>
    suspend fun searchCustomers(tenantId: String, normalizedQuery: String): List<KhataCustomer>
    suspend fun getCustomer(tenantId: String, id: String): KhataCustomer?
    suspend fun addCustomer(
        tenantId: String,
        nameBn: String,
        phone: String?,
        address: String?,
        creditLimit: Double,
    ): String

    suspend fun getEntries(tenantId: String, customerId: String): List<KhataEntry>
    suspend fun addEntry(
        tenantId: String,
        customerId: String,
        amount: Double,
        type: com.boikhata.core.domain.enums.KhataEntryType,
        description: String,
        referenceBillId: String?,
        collectedByUserId: String,
        cashbookAccount: CashbookAccount? = null, // D34: for PAYMENT type — the account the customer paid into
    ): String

    suspend fun forgiveDebt(
        tenantId: String,
        customerId: String,
        collectedByUserId: String,
    ): String

    suspend fun getInstallments(tenantId: String, customerId: String): List<KhataInstallment>
    suspend fun addInstallment(
        tenantId: String,
        customerId: String,
        khataEntryId: String,
        dueDate: Long,
        amount: Double,
    ): String

    suspend fun markInstallmentPaid(id: String)
}

// ── P2b: POS / Billing ───────────────────────────────────────────────────────

interface BillRepository {
    suspend fun getBillsByDate(tenantId: String, startOfDay: Long, endOfDay: Long): List<com.boikhata.core.domain.model.BillSummary>
    suspend fun getTopBills(tenantId: String, limit: Int): List<com.boikhata.core.domain.model.BillSummary>
    suspend fun getAllBills(tenantId: String): List<com.boikhata.core.domain.model.BillSummary>
    suspend fun getBill(tenantId: String, billId: String): Bill?
    suspend fun getBillLines(billId: String): List<BillLine>
    suspend fun createBill(
        tenantId: String,
        customerId: String?,
        customerNameBn: String,
        customerPhone: String?,
        userId: String,
        lines: List<BillLineInput>,
        discountAmount: Double,
        discountType: String,
        paymentMethod: com.boikhata.core.domain.enums.PaymentMethod,
        paidAmount: Double,
    ): String
}

data class BillLineInput(
    val bookId: String,
    val bookTitleBn: String,
    val quantity: Int,
    val unitPrice: Double,
    val category: com.boikhata.core.domain.enums.BookCategory,
)

interface LicenseRepository {
    suspend fun getLicenseState(tenantId: String): com.boikhata.core.domain.enums.LicenseState
    suspend fun evaluateCurrentGrace(tenantId: String, now: Long): GraceState
    fun getWriteGuard(): LicenseWriteGuard
    suspend fun setWifiOnlySync(tenantId: String, enabled: Boolean)
    suspend fun isWifiOnlySync(tenantId: String): Boolean
}

// ── P3a: Expense ─────────────────────────────────────────────────────────────

interface ExpenseRepository {
    suspend fun getCategories(tenantId: String): List<ExpenseCategory>
    suspend fun getExpenses(tenantId: String): List<Expense>
    suspend fun getExpensesByDateRange(tenantId: String, start: Long, end: Long): List<Expense>
    suspend fun getExpensesByCategory(tenantId: String, categoryId: String): List<Expense>
    suspend fun getExpensesByCategoryAndUser(tenantId: String, categoryId: String, userId: String): List<Expense>
    suspend fun addExpense(
        tenantId: String,
        categoryId: String,
        amount: Double,
        description: String,
        expenseDate: Long,
        receiptPhotoPath: String?,
        userId: String,
        cashbookAccount: CashbookAccount,
    ): String
    suspend fun addBookPurchase(
        tenantId: String,
        bookId: String,
        quantity: Int,
        unitPrice: Double,
        description: String,
        userId: String,
        cashbookAccount: CashbookAccount,
    ): String
}

// ── P3a: Cashbook ────────────────────────────────────────────────────────────

interface CashbookRepository {
    suspend fun getEntries(tenantId: String): List<CashbookEntry>
    suspend fun getEntriesByDateRange(tenantId: String, start: Long, end: Long): List<CashbookEntry>
    suspend fun getBalances(tenantId: String): List<CashbookBalance>
    suspend fun addManualEntry(
        tenantId: String,
        account: CashbookAccount,
        type: CashbookEntryType,
        amount: Double,
        description: String,
        userId: String,
    ): String
}

// ── P3a: Owner Drawing ──────────────────────────────────────────────────────

interface OwnerDrawingRepository {
    suspend fun getDrawings(tenantId: String): List<OwnerDrawing>
    suspend fun getDrawingsByDateRange(tenantId: String, start: Long, end: Long): List<OwnerDrawing>
    suspend fun createDrawing(
        tenantId: String,
        amount: Double,
        description: String,
        userId: String,
    ): String
}

// ── P3b: Accounting (P&L + balance sheet + period-lock) ──────────────────────

interface AccountingRepository {
    // P&L
    suspend fun getMonthlyPnL(tenantId: String, year: Int, month: Int): com.boikhata.core.domain.model.PnLReport
    // Balance sheet lite
    suspend fun getBalanceSheet(tenantId: String, asOfDate: Long): com.boikhata.core.domain.model.BalanceSheetLite
    // Period lock
    suspend fun isPeriodLocked(tenantId: String, year: Int, month: Int): Boolean
    suspend fun lockPeriod(tenantId: String, year: Int, month: Int, userId: String): String
    suspend fun getLockedPeriods(tenantId: String): List<com.boikhata.core.domain.model.PeriodLock>
    // Khata aging summary
    suspend fun getKhataAgingSummary(tenantId: String, asOfDate: Long): com.boikhata.core.domain.model.KhataAgingSummary
    // VAT summary
    suspend fun getVatSummary(tenantId: String, start: Long, end: Long): com.boikhata.core.domain.model.VatSummary
    // Full হিসাব-প্যাক
    suspend fun getHisabPack(tenantId: String, year: Int, month: Int, shopName: String): com.boikhata.core.domain.model.HisabPack
}

// ── P3b: Recurring Expense (D35) ─────────────────────────────────────────────

interface RecurringExpenseRepository {
    suspend fun getTemplates(tenantId: String): List<com.boikhata.core.domain.model.RecurringExpenseTemplate>
    suspend fun addTemplate(
        tenantId: String,
        categoryId: String,
        amount: Double,
        description: String,
        frequency: com.boikhata.core.domain.accounting.RecurringExpenseCalculator.Frequency,
        userId: String,
    ): String
    suspend fun applyTemplate(id: String, userId: String, cashbookAccount: CashbookAccount): String
    suspend fun getDueTemplates(tenantId: String, now: Long): List<com.boikhata.core.domain.model.RecurringExpenseTemplate>
}

// ── P3b: Budget (D35) ─────────────────────────────────────────────────────────

interface BudgetRepository {
    suspend fun getBudgets(tenantId: String): List<com.boikhata.core.domain.accounting.BudgetAlertCalculator.Budget>
    suspend fun setBudget(tenantId: String, categoryId: String, monthlyLimit: Double): String
    suspend fun getMonthlyAlerts(tenantId: String, year: Int, month: Int): List<com.boikhata.core.domain.accounting.BudgetAlertCalculator.BudgetAlert>
}

// ── P3c: Cash-close (D36) ────────────────────────────────────────────────────

interface CashCloseRepository {
    suspend fun getDailyClose(
        tenantId: String,
        startOfDay: Long,
        endOfDay: Long,
        mfsFeeRate: Double,
        countedCash: Double,
    ): com.boikhata.core.domain.model.CashCloseReport
}

// ── P4a: Cloud Auth + License Sync + Tenant Rebind ──────────────────────────

interface AuthRepository {
    suspend fun startPhoneVerification(phone: String): Boolean
    suspend fun verifyOtp(code: String): Boolean
    suspend fun getCurrentCloudUser(): com.boikhata.core.domain.model.CloudUser?
    suspend fun signOut()
    fun isSignedIn(): Boolean
}

interface LicenseSyncRepository {
    suspend fun syncLicense(tenantId: String, role: com.boikhata.core.domain.enums.Role): com.boikhata.core.domain.model.LicenseSyncResult
}

interface TenantRebindRepository {
    suspend fun rebind(oldTenantId: String, newTenantId: String): Int
}

// ── P4b: Backup + Restore ────────────────────────────────────────────────────

/** D46: Result of an incremental backup. */
sealed class BackupResult {
    data class Success(val collectionsBackedUp: Int, val rowsUploaded: Int, val timestamp: Long) : BackupResult()
    data object NotOwner : BackupResult()
    data object RebindNeeded : BackupResult()
    data class Error(val message: String) : BackupResult()
    data class Partial(val collectionsBackedUp: Int, val rowsUploaded: Int, val collectionErrors: Map<String, String>) : BackupResult()
}

/** D46: Strategy for restore when both sides have data. */
enum class RestoreStrategy {
    CLOUD_OVERWRITES_LOCAL, // fresh device — wipe local + download all
    KEEP_LOCAL,             // cancel — keep local data
}

/** D46: Result of a restore. */
sealed class RestoreResult {
    data class Success(val rowsRestored: Int) : RestoreResult()
    data object NotOwner : RestoreResult()
    data object BothSidesHaveData : RestoreResult() // requires user choice — never auto-merge
    data class Error(val message: String) : RestoreResult()
}

interface BackupRepository {
    suspend fun backup(tenantId: String, role: com.boikhata.core.domain.enums.Role): BackupResult
}

interface RestoreRepository {
    suspend fun checkAndRestore(
        tenantId: String,
        role: com.boikhata.core.domain.enums.Role,
        strategy: RestoreStrategy,
    ): RestoreResult
}

// ── P4b: Subscription (manual bKash) ──────────────────────────────────────────

/** D48: Result of recording a subscription payment. */
sealed class SubscriptionResult {
    data class Success(val paymentId: String) : SubscriptionResult()
    data object NotOwner : SubscriptionResult()
    data class Error(val message: String) : SubscriptionResult()
}

interface SubscriptionRepository {
    suspend fun recordPayment(
        tenantId: String,
        role: com.boikhata.core.domain.enums.Role,
        amount: Double,
        trxId: String?,
        note: String?,
    ): SubscriptionResult
}

// ── P4b: Master Catalog Refresh (read-only) ───────────────────────────────────

/** D49: Result of a master catalog refresh. */
sealed class CatalogRefreshResult {
    data class Success(
        val newBooks: List<com.boikhata.core.domain.cloud.CatalogDeltaDetector.NewBook>,
        val priceChanges: List<com.boikhata.core.domain.cloud.CatalogDeltaDetector.PriceChange>,
        val totalInMaster: Int,
    ) : CatalogRefreshResult()
    data object Offline : CatalogRefreshResult()
    data class Error(val message: String) : CatalogRefreshResult()
}

interface MasterCatalogRepository {
    suspend fun refreshCatalog(tenantId: String): CatalogRefreshResult
    suspend fun applyPriceChange(tenantId: String, bookId: String, newPrice: Double): Boolean
}

// ── P4b: Tenant info (shop name from Firestore) ───────────────────────────────

interface TenantInfoRepository {
    suspend fun fetchShopName(tenantId: String): String?
}
