package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.SupplierDao
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.SupplierEntity
import com.boikhata.core.database.entity.SupplierEntryEntity
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.accounting.SupplierAgingCalculator
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.SupplierEntryType
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.Supplier
import com.boikhata.core.domain.model.SupplierAgingSummary
import com.boikhata.core.domain.model.SupplierBalance
import com.boikhata.core.domain.model.SupplierEntry
import com.boikhata.core.domain.model.SupplierStatement
import com.boikhata.core.domain.repository.SupplierRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D51-D54: Supplier/publisher payable ledger (দেনা-খাতা) repository.
 * supplier_entries is append-only (🔒) — INSERT only, no UPDATE/DELETE.
 * D52: balance + aging via SupplierAgingCalculator (FIFO).
 * D53: PAYMENT creates a cashbook EXPENSE entry in the chosen account (cash outflow,
 * does NOT touch the P&L which reads the expenses table, not the cashbook).
 */
@Singleton
class SupplierRepositoryImpl @Inject constructor(
    private val supplierDao: SupplierDao,
    private val cashbookDao: CashbookDao,
    private val writeGuard: LicenseWriteGuard,
    private val periodLockChecker: PeriodLockChecker,
) : SupplierRepository {

    override suspend fun getSuppliers(tenantId: String): List<Supplier> {
        return supplierDao.getSuppliers(tenantId).map { it.toDomain() }
    }

    override suspend fun getSupplier(tenantId: String, supplierId: String): Supplier? {
        return supplierDao.getSupplierById(supplierId)?.toDomain()
    }

    override suspend fun addSupplier(
        tenantId: String,
        nameBn: String,
        phone: String?,
        settlementCycle: String,
        notes: String?,
    ): String {
        writeGuard.assertWriteAllowed()
        val id = UUID.randomUUID().toString()
        supplierDao.insertSupplier(
            SupplierEntity(
                id = id,
                tenantId = tenantId,
                nameBn = nameBn,
                phone = phone,
                settlementCycle = settlementCycle,
                notes = notes,
            )
        )
        return id
    }

    override suspend fun getEntries(tenantId: String, supplierId: String): List<SupplierEntry> {
        return supplierDao.getEntries(tenantId, supplierId).map { it.toDomain() }
    }

    override suspend fun getEntriesByDateRange(tenantId: String, supplierId: String, start: Long, end: Long): List<SupplierEntry> {
        return supplierDao.getEntriesByDateRange(tenantId, supplierId, start, end).map { it.toDomain() }
    }

    override suspend fun addEntry(
        tenantId: String,
        supplierId: String,
        amount: Double,
        type: SupplierEntryType,
        description: String,
        referenceId: String?,
        date: Long,
        userId: String,
        cashbookAccount: CashbookAccount?,
    ): String {
        writeGuard.assertWriteAllowed()
        periodLockChecker.assertNotLocked(tenantId, date)

        val id = UUID.randomUUID().toString()
        supplierDao.insertEntry(
            SupplierEntryEntity(
                id = id,
                tenantId = tenantId,
                supplierId = supplierId,
                amount = amount,
                type = type.name,
                description = description,
                referenceId = referenceId,
                date = date,
                idempotencyKey = UUID.randomUUID().toString(),
            )
        )
        return id
    }

    /** D53: PAYMENT → supplier entry (reduces payable) + cashbook EXPENSE (cash outflow). */
    override suspend fun addPayment(
        tenantId: String,
        supplierId: String,
        amount: Double,
        description: String,
        trxId: String?,
        userId: String,
        cashbookAccount: CashbookAccount,
    ): String {
        writeGuard.assertWriteAllowed()
        val now = System.currentTimeMillis()
        periodLockChecker.assertNotLocked(tenantId, now)

        val supplierName = supplierDao.getSupplierById(supplierId)?.nameBn ?: ""
        val desc = buildString {
            append(description.ifBlank { "সাপ্লায়ার পেমেন্ট" })
            if (!trxId.isNullOrBlank()) append(" (trxID: $trxId)")
        }

        val entryId = UUID.randomUUID().toString()
        supplierDao.insertEntry(
            SupplierEntryEntity(
                id = entryId,
                tenantId = tenantId,
                supplierId = supplierId,
                amount = amount,
                type = SupplierEntryType.PAYMENT.name,
                description = desc,
                referenceId = trxId?.takeIf { it.isNotBlank() },
                date = now,
                idempotencyKey = UUID.randomUUID().toString(),
            )
        )

        if (amount > 0.01) {
            cashbookDao.insert(
                CashbookEntryEntity(
                    id = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    account = cashbookAccount.name,
                    type = "EXPENSE",
                    amount = amount,
                    description = "সাপ্লায়ার পেমেন্ট ($supplierName)",
                    referenceId = entryId,
                    date = now,
                    userId = userId,
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            )
        }
        return entryId
    }

    override suspend fun addOpeningBalance(tenantId: String, supplierId: String, amount: Double, userId: String): String {
        return addEntry(
            tenantId = tenantId,
            supplierId = supplierId,
            amount = amount,
            type = SupplierEntryType.OPENING,
            description = "উদ্বোধনী দেনা",
            referenceId = null,
            date = System.currentTimeMillis(),
            userId = userId,
        )
    }

    override suspend fun addConsignment(tenantId: String, supplierId: String, amount: Double, description: String, userId: String): String {
        return addEntry(
            tenantId = tenantId,
            supplierId = supplierId,
            amount = amount,
            type = SupplierEntryType.CONSIGNMENT,
            description = description.ifBlank { "কনসাইনমেন্ট গ্রহণ" },
            referenceId = null,
            date = System.currentTimeMillis(),
            userId = userId,
        )
    }

    override suspend fun addPurchase(tenantId: String, supplierId: String, amount: Double, description: String, userId: String): String {
        return addEntry(
            tenantId = tenantId,
            supplierId = supplierId,
            amount = amount,
            type = SupplierEntryType.PURCHASE,
            description = description.ifBlank { "ক্রয় (বাকি)" },
            referenceId = null,
            date = System.currentTimeMillis(),
            userId = userId,
        )
    }

    override suspend fun getSupplierBalance(tenantId: String, supplierId: String, now: Long): SupplierBalance {
        val supplier = supplierDao.getSupplierById(supplierId)?.toDomain() ?: error("সাপ্লায়ার পাওয়া যায়নি")
        val entries = supplierDao.getEntries(tenantId, supplierId).map { it.toDomain() }
        val aging = SupplierAgingCalculator.calculate(entries, now)
        val cycleDays = SupplierAgingCalculator.settlementCycleDays(supplier)
        return SupplierBalance(
            supplier = supplier,
            balance = aging.totalPayable,
            ageDays = aging.ageDays,
            bucket = aging.bucket,
            overdueForDays = (aging.ageDays - cycleDays).coerceAtLeast(0),
            reminderDue = aging.totalPayable > 0.001 && (aging.ageDays - cycleDays) > 0,
        )
    }

    override suspend fun getSupplierAgingSummary(tenantId: String, now: Long): SupplierAgingSummary {
        val balances = supplierDao.getSuppliers(tenantId).map { supplier ->
            val entries = supplierDao.getEntries(tenantId, supplier.id).map { it.toDomain() }
            val aging = SupplierAgingCalculator.calculate(entries, now)
            val cycleDays = SupplierAgingCalculator.settlementCycleDays(supplier.toDomain())
            SupplierBalance(
                supplier = supplier.toDomain(),
                balance = aging.totalPayable,
                ageDays = aging.ageDays,
                bucket = aging.bucket,
                overdueForDays = (aging.ageDays - cycleDays).coerceAtLeast(0),
                reminderDue = aging.totalPayable > 0.001 && (aging.ageDays - cycleDays) > 0,
            )
        }
        return SupplierAgingCalculator.summarize(balances)
    }

    override suspend fun getSettlementReminders(tenantId: String, now: Long): List<SupplierBalance> {
        return supplierDao.getSuppliers(tenantId).mapNotNull { supplier ->
            val entries = supplierDao.getEntries(tenantId, supplier.id).map { it.toDomain() }
            val aging = SupplierAgingCalculator.calculate(entries, now)
            val cycleDays = SupplierAgingCalculator.settlementCycleDays(supplier.toDomain())
            val overdue = aging.ageDays - cycleDays
            if (aging.totalPayable > 0.001 && overdue > 0) {
                SupplierBalance(
                    supplier = supplier.toDomain(),
                    balance = aging.totalPayable,
                    ageDays = aging.ageDays,
                    bucket = aging.bucket,
                    overdueForDays = overdue.coerceAtLeast(0),
                    reminderDue = true,
                )
            } else {
                null
            }
        }
    }

    override suspend fun getSettlementStatement(
        tenantId: String,
        supplierId: String,
        shopName: String,
        startDate: Long?,
        endDate: Long,
    ): SupplierStatement {
        val supplier = supplierDao.getSupplierById(supplierId)?.toDomain() ?: error("সাপ্লায়ার পাওয়া যায়নি")
        val entries = supplierDao.getEntries(tenantId, supplierId).map { it.toDomain() }
        val filtered = entries.filter { entry ->
            (startDate == null || entry.date >= startDate) && entry.date < endDate
        }
        return SupplierAgingCalculator.buildStatement(
            shopName = shopName,
            supplier = supplier,
            entries = filtered,
            startDate = startDate,
            endDate = endDate,
        )
    }

    // ── mappers ────────────────────────────────────────────────────────────
    private fun SupplierEntity.toDomain() = Supplier(
        id = id,
        tenantId = tenantId,
        nameBn = nameBn,
        phone = phone,
        settlementCycle = settlementCycle,
        notes = notes,
    )

    private fun SupplierEntryEntity.toDomain() = SupplierEntry(
        id = id,
        tenantId = tenantId,
        supplierId = supplierId,
        amount = amount,
        type = SupplierEntryType.valueOf(type),
        description = description,
        referenceId = referenceId,
        date = date,
    )
}
