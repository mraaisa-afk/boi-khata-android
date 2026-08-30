package com.boikhata.core.domain.repository

import com.boikhata.core.domain.enums.BookCategory
import com.boikhata.core.domain.enums.BookCondition
import com.boikhata.core.domain.aging.KhataEntry
import com.boikhata.core.domain.license.GraceState
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.Book
import com.boikhata.core.domain.model.KhataCustomer
import com.boikhata.core.domain.model.KhataInstallment
import com.boikhata.core.domain.model.Bill
import com.boikhata.core.domain.model.BillLine

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
