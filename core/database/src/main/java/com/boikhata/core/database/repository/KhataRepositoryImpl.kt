package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.KhataCustomerDao
import com.boikhata.core.database.dao.KhataEntryDao
import com.boikhata.core.database.entity.KhataEntryEntity
import com.boikhata.core.domain.aging.KhataEntry
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.model.KhataCustomer
import com.boikhata.core.domain.repository.KhataRepository
import javax.inject.Inject

class KhataRepositoryImpl @Inject constructor(
    private val khataCustomerDao: KhataCustomerDao,
    private val khataEntryDao: KhataEntryDao,
) : KhataRepository {

    override suspend fun getCustomers(tenantId: String): List<KhataCustomer> {
        return khataCustomerDao.getActiveByTenant(tenantId).map {
            KhataCustomer(
                id = it.id,
                nameBn = it.nameBn,
                phone = it.phone,
                address = it.address,
                creditLimit = it.creditLimit,
                isActive = it.isActive,
            )
        }
    }

    override suspend fun getEntries(tenantId: String, customerId: String): List<KhataEntry> {
        return khataEntryDao.getByCustomer(tenantId, customerId).map { it.toDomain() }
    }

    private fun KhataEntryEntity.toDomain() = KhataEntry(
        id = id,
        type = KhataEntryType.valueOf(type),
        amount = amount,
        date = date,
    )
}
