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
