package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.domain.accounting.CashbookBalanceCalculator
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.CashbookEntryType
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.CashbookBalance
import com.boikhata.core.domain.model.CashbookEntry
import com.boikhata.core.domain.repository.CashbookRepository
import java.util.UUID
import javax.inject.Inject

/**
 * P3a: CashbookRepository implementation.
 * D25: Balances are derived from append-only entries.
 */
class CashbookRepositoryImpl @Inject constructor(
    private val cashbookDao: CashbookDao,
    private val writeGuard: LicenseWriteGuard,
) : CashbookRepository {

    override suspend fun getEntries(tenantId: String): List<CashbookEntry> {
        return cashbookDao.getByTenant(tenantId).map { it.toDomain() }
    }

    override suspend fun getEntriesByDateRange(tenantId: String, start: Long, end: Long): List<CashbookEntry> {
        return cashbookDao.getByDateRange(tenantId, start, end).map { it.toDomain() }
    }

    override suspend fun getBalances(tenantId: String): List<CashbookBalance> {
        val entries = cashbookDao.getByTenant(tenantId).map { it.toDomain() }
        return CashbookBalanceCalculator.calculateAllBalances(entries)
    }

    override suspend fun addManualEntry(
        tenantId: String,
        account: CashbookAccount,
        type: CashbookEntryType,
        amount: Double,
        description: String,
        userId: String,
    ): String {
        writeGuard.assertWriteAllowed()
        val id = UUID.randomUUID().toString()
        cashbookDao.insert(
            com.boikhata.core.database.entity.CashbookEntryEntity(
                id = id,
                tenantId = tenantId,
                account = account.name,
                type = type.name,
                amount = amount,
                description = description,
                referenceId = null,
                date = System.currentTimeMillis(),
                userId = userId,
                idempotencyKey = UUID.randomUUID().toString(),
            )
        )
        return id
    }

    private fun com.boikhata.core.database.entity.CashbookEntryEntity.toDomain() = CashbookEntry(
        id = id,
        account = CashbookAccount.valueOf(account),
        type = CashbookEntryType.valueOf(type),
        amount = amount,
        description = description,
        referenceId = referenceId,
        date = date,
        userId = userId,
    )
}
