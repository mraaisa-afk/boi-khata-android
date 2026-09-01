package com.boikhata.core.domain.accounting

/**
 * D32: PeriodLockChecker — the injectable service that repositories consult
 * before any money-table write. It loads the locked periods and delegates to
 * the pure PeriodLockGuard. Read/export paths do NOT consult this (never-lock).
 *
 * This is the thin adapter between the pure guard logic and the DAO.
 * The implementation lives in core/database (it needs the DAO).
 */
interface PeriodLockChecker {
    /**
     * Load all locked periods for a tenant.
     */
    suspend fun getLockedPeriods(tenantId: String): Set<PeriodLockGuard.LockedPeriod>

    /**
     * Assert that a write targeting [date] is not in a locked period.
     * Throws PeriodLockedException if locked.
     */
    suspend fun assertNotLocked(tenantId: String, date: Long)
}
