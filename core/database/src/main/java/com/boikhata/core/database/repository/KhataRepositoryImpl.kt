package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.KhataCustomerDao
import com.boikhata.core.database.dao.KhataEntryDao
import com.boikhata.core.database.dao.KhataInstallmentDao
import com.boikhata.core.database.entity.KhataCustomerEntity
import com.boikhata.core.database.entity.KhataEntryEntity
import com.boikhata.core.database.entity.KhataInstallmentEntity
import com.boikhata.core.domain.aging.AgingCalculator
import com.boikhata.core.domain.aging.KhataEntry
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.KhataCustomer
import com.boikhata.core.domain.model.KhataInstallment
import com.boikhata.core.domain.repository.KhataRepository
import com.boikhata.core.domain.text.BengaliNormalizer
import java.util.UUID
import javax.inject.Inject

/**
 * P2a: KhataRepository implementation — extended with customer CRUD,
 * installment tracking, দেনা-মুন, and search (all offline Room-only).
 */
class KhataRepositoryImpl @Inject constructor(
    private val khataCustomerDao: KhataCustomerDao,
    private val khataEntryDao: KhataEntryDao,
    private val khataInstallmentDao: KhataInstallmentDao,
    private val writeGuard: LicenseWriteGuard,
) : KhataRepository {

    override suspend fun getCustomers(tenantId: String): List<KhataCustomer> {
        return khataCustomerDao.getActiveByTenant(tenantId).map { it.toDomain() }
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
    ): String {
        writeGuard.assertWriteAllowed()
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
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
            description = "দেনা মুন",
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
