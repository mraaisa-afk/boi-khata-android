package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.PeriodLockDao
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.accounting.PeriodLockGuard
import com.boikhata.core.domain.accounting.PeriodLockedException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D32: PeriodLockChecker implementation — loads locked periods from Room and
 * delegates to the pure PeriodLockGuard.
 */
@Singleton
class PeriodLockCheckerImpl @Inject constructor(
    private val periodLockDao: PeriodLockDao,
) : PeriodLockChecker {

    override suspend fun getLockedPeriods(tenantId: String): Set<PeriodLockGuard.LockedPeriod> {
        return periodLockDao.getAllForTenant(tenantId).map {
            PeriodLockGuard.LockedPeriod(it.periodYear, it.periodMonth)
        }.toSet()
    }

    override suspend fun assertNotLocked(tenantId: String, date: Long) {
        val locked = getLockedPeriods(tenantId)
        if (PeriodLockGuard.isLocked(locked, date)) {
            val (year, month) = PeriodLockGuard.yearMonth(date)
            throw PeriodLockedException(year, month)
        }
    }
}
