package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.BillDao
import com.boikhata.core.domain.model.BillSummary
import com.boikhata.core.domain.repository.BillRepository
import javax.inject.Inject

class BillRepositoryImpl @Inject constructor(
    private val billDao: BillDao,
) : BillRepository {

    override suspend fun getBillsByDate(tenantId: String, startOfDay: Long, endOfDay: Long): List<BillSummary> {
        return billDao.getByDateRange(tenantId, startOfDay, endOfDay).map {
            BillSummary(
                id = it.id,
                billNumber = it.billNumber,
                customerNameBn = it.customerNameBn,
                totalAmount = it.totalAmount,
                paidAmount = it.paidAmount,
                dueAmount = it.dueAmount,
                billDate = it.billDate,
                status = it.status,
            )
        }
    }

    override suspend fun getTopBills(tenantId: String, limit: Int): List<BillSummary> {
        return billDao.getByTenant(tenantId).take(limit).map {
            BillSummary(
                id = it.id,
                billNumber = it.billNumber,
                customerNameBn = it.customerNameBn,
                totalAmount = it.totalAmount,
                paidAmount = it.paidAmount,
                dueAmount = it.dueAmount,
                billDate = it.billDate,
                status = it.status,
            )
        }
    }
}
