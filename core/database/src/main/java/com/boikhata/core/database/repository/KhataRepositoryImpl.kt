package com.boikhata.core.database.repository

import androidx.room.withTransaction
import com.boikhata.core.database.BoiKhataDatabase
import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.KhataCustomerDao
import com.boikhata.core.database.dao.KhataEntryDao
import com.boikhata.core.database.dao.KhataInstallmentDao
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.KhataCustomerEntity
import com.boikhata.core.database.entity.KhataEntryEntity
import com.boikhata.core.database.entity.KhataInstallmentEntity
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.aging.AgingCalculator
import com.boikhata.core.domain.aging.KhataEntry
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.KhataCustomer
import com.boikhata.core.domain.model.KhataInstallment
import com.boikhata.core.domain.repository.KhataRepository
import com.boikhata.core.domain.text.BengaliNormalizer
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * P2a: KhataRepository implementation — extended with customer CRUD,
 * installment tracking, দেনা-মুন, and search (all offline Room-only).
 * D32: Period-lock check before write.
 * D34: Cashbook auto-populate from khata collections (PAYMENT → INCOME).
 * D81: getKhataCollectionByDateRange — delegates to KhataEntryDao aggregate query.
 */
class KhataRepositoryImpl @Inject constructor(
    private val db: BoiKhataDatabase,
    private val khataCustomerDao: KhataCustomerDao,
    private val khataEntryDao: KhataEntryDao,
    private val khataInstallmentDao: KhataInstallmentDao,
    private val cashbookDao: CashbookDao,
    private val writeGuard: LicenseWriteGuard,
    private val periodLockChecker: PeriodLockChecker,
) : KhataRepository {

    override suspend fun getCustomers(tenantId: String): List<KhataCustomer> {
        return khataCustomerDao.getActiveByTenant(tenantId).map { it.toDomain() }
    }

    /**
     * B-002: Room-driven reactive list. Room re-emits whenever khata_customers
     * changes, so the khata list screen updates instantly even when the insert
     * comes from a different ViewModel instance (KhataAddCustomerScreen).
     */
    override fun getCustomersFlow(tenantId: String): Flow<List<KhataCustomer>> {
        return khataCustomerDao.getActiveByTenantFlow(tenantId).map { customers ->
            customers.map { it.toDomain() }
        }
    }

    override suspend fun searchCustomers(tenantId: String, normalizedQuery: String): List<KhataCustomer> {
        if (normalizedQuery.isBlank()) return getCustomers(tenantId)
        return khataCustomerDao.search(tenantId, normalizedQuery).map { it.toDomain() }
    }

    override suspend fun getCustomer(tenantId: String, id: String): KhataCustomer? {
        return khataCustomerDao.getById(id)?.toDomain()
    }

    override suspend fun addCustomer(
        tenantId: String,
        nameBn: String,
        phone: String?,
        address: String?,
        creditLimit: Double,
    ): String {
        writeGuard.assertWriteAllowed()
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        khataCustomerDao.insert(
            KhataCustomerEntity(
                id = id,
                tenantId = tenantId,
                nameBn = nameBn,
                phone = phone,
                address = address,
                creditLimit = creditLimit,
                isActive = true,
                nameBnNormalized = BengaliNormalizer.normalize(nameBn),
                createdAt = now,
                updatedAt = now,
            )
        )
        return id
    }

    /**
     * U-002: customer + OPENING entry in ONE atomic transaction (D22 pattern —
     * mirrors SaleRepositoryImpl.createBill). A crash between the two inserts
     * must never leave a "customer exists but their recorded previous due is
     * silently lost" half-state. Reuses the guarded addCustomer/addEntry paths
     * (write-guard, D32 period-lock, D34 cashbook rule) — the OPENING entry
     * mirrors nothing to the cashbook because cashbookAccount stays null AND
     * type != PAYMENT (KhataRepositoryImpl.addEntry D34 branch).
     */
    override suspend fun addCustomerWithOpeningDue(
        tenantId: String,
        nameBn: String,
        phone: String?,
        address: String?,
        creditLimit: Double,
        openingDue: Double,
        collectedByUserId: String,
    ): String {
        writeGuard.assertWriteAllowed()
        return db.withTransaction {
            val customerId = addCustomer(tenantId, nameBn, phone, address, creditLimit)
            if (openingDue > 0.01) {
                addEntry(
                    tenantId = tenantId,
                    customerId = customerId,
                    amount = openingDue,
                    type = KhataEntryType.OPENING,
                    description = "পূর্বের বাকি",
                    referenceBillId = null,
                    collectedByUserId = collectedByUserId,
                    cashbookAccount = null, // D34: OPENING is a receivable, not a cash flow
                )
            }
            customerId
        }
    }

    override suspend fun getEntries(tenantId: String, customerId: String): List<KhataEntry> {
        return khataEntryDao.getByCustomer(tenantId, customerId).map { it.toDomain() }
    }

    override suspend fun addEntry(
        tenantId: String,
        customerId: String,
        amount: Double,
        type: KhataEntryType,
        description: String,
        referenceBillId: String?,
        collectedByUserId: String,
        cashbookAccount: CashbookAccount?,
    ): String {
        writeGuard.assertWriteAllowed()
        // D32: Period-lock check
        val now = System.currentTimeMillis()
        periodLockChecker.assertNotLocked(tenantId, now)

        val id = UUID.randomUUID().toString()
        khataEntryDao.insert(
            KhataEntryEntity(
                id = id,
                tenantId = tenantId,
                customerId = customerId,
                amount = amount,
                type = type.name,
                description = description,
                referenceBillId = referenceBillId,
                collectedByUserId = collectedByUserId,
                date = now,
                idempotencyKey = UUID.randomUUID().toString(),
            )
        )
        // D34: Cashbook auto-populate — khata collection (PAYMENT) creates INCOME entry.
        // Only for PAYMENT type with amount > 0 (money flowing in). CREDIT/ADJUSTMENT/OPENING
        // do not create cashbook entries (CREDIT is a receivable, not a cash flow).
        if (type == KhataEntryType.PAYMENT && amount > 0.01 && cashbookAccount != null) {
            cashbookDao.insert(
                CashbookEntryEntity(
                    id = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    account = cashbookAccount.name,
                    type = "INCOME",
                    amount = amount,
                    description = "খাতা আদায় ($description)",
                    referenceId = id,
                    date = now,
                    userId = collectedByUserId,
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            )
        }
        return id
    }

    /**
     * D15: দেনা-মুন — inserts an ADJUSTMENT entry with amount = -currentDue
     * to bring the customer's balance to zero. Append-only (no delete).
     */
    override suspend fun forgiveDebt(
        tenantId: String,
        customerId: String,
        collectedByUserId: String,
    ): String {
        writeGuard.assertWriteAllowed()
        val entries = getEntries(tenantId, customerId)
        val aging = AgingCalculator.calculate(entries, System.currentTimeMillis())
        val currentDue = aging.totalDue
        if (currentDue <= 0.01) return "" // nothing to forgive

        return addEntry(
            tenantId = tenantId,
            customerId = customerId,
            amount = -currentDue, // D15: negative ADJUSTMENT reduces balance
            type = KhataEntryType.ADJUSTMENT,
            description = "দেনা-পাওনা",
            referenceBillId = null,
            collectedByUserId = collectedByUserId,
        )
    }

    override suspend fun getInstallments(tenantId: String, customerId: String): List<KhataInstallment> {
        return khataInstallmentDao.getByCustomer(tenantId, customerId).map {
            KhataInstallment(
                id = it.id,
                customerId = it.customerId,
                khataEntryId = it.khataEntryId,
                dueDate = it.dueDate,
                amount = it.amount,
                isPaid = it.isPaid,
            )
        }
    }

    override suspend fun addInstallment(
        tenantId: String,
        customerId: String,
        khataEntryId: String,
        dueDate: Long,
        amount: Double,
    ): String {
        writeGuard.assertWriteAllowed()
        val id = UUID.randomUUID().toString()
        khataInstallmentDao.insert(
            KhataInstallmentEntity(
                id = id,
                tenantId = tenantId,
                customerId = customerId,
                khataEntryId = khataEntryId,
                dueDate = dueDate,
                amount = amount,
                isPaid = false,
            )
        )
        return id
    }

    override suspend fun markInstallmentPaid(id: String) {
        writeGuard.assertWriteAllowed()
        khataInstallmentDao.markPaid(id)
    }

    /**
     * D81: Returns the sum of PAYMENT-type khata entries within [start, end].
     * This is the «খাতা আদায়» component of the D79 net-profit formula.
     * Delegates to the DAO-level COALESCE(SUM(amount), 0.0) aggregate.
     */
    override suspend fun getKhataCollectionByDateRange(
        tenantId: String,
        start: Long,
        end: Long,
    ): Double {
        return khataEntryDao.getPaymentSumByDateRange(tenantId, start, end)
    }

    private fun KhataCustomerEntity.toDomain() = KhataCustomer(
        id = id,
        nameBn = nameBn,
        phone = phone,
        address = address,
        creditLimit = creditLimit,
        isActive = isActive,
    )

    private fun KhataEntryEntity.toDomain() = KhataEntry(
        id = id,
        type = KhataEntryType.valueOf(type),
        amount = amount,
        date = date,
        description = description,
        referenceBillId = referenceBillId,
    )
}
