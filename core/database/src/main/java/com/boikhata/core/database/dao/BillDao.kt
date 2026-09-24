package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boikhata.core.database.entity.BillEntity
import com.boikhata.core.database.entity.BillLineEntity

@Dao
interface BillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: BillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<BillLineEntity>)

    @Query("SELECT * FROM bills WHERE tenantId = :tenantId AND billDate >= :startOfDay AND billDate < :endOfDay ORDER BY billDate DESC")
    suspend fun getByDateRange(tenantId: String, startOfDay: Long, endOfDay: Long): List<BillEntity>

    @Query("SELECT * FROM bills WHERE tenantId = :tenantId ORDER BY billDate DESC")
    suspend fun getByTenant(tenantId: String): List<BillEntity>

    /** B-006: customer-linked bills — the khata detail screen's sales history source. */
    @Query("SELECT * FROM bills WHERE tenantId = :tenantId AND customerId = :customerId ORDER BY billDate DESC")
    suspend fun getByCustomer(tenantId: String, customerId: String): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :billId")
    suspend fun getById(billId: String): BillEntity?

    @Query("SELECT * FROM bill_lines WHERE billId = :billId")
    suspend fun getLinesByBill(billId: String): List<BillLineEntity>

    @Query("SELECT billNumber FROM bills WHERE tenantId = :tenantId AND billNumber LIKE :pattern ORDER BY billNumber DESC LIMIT 1")
    suspend fun getMaxBillNumber(tenantId: String, pattern: String): String?

    @Query("SELECT COUNT(*) FROM bills WHERE tenantId = :tenantId")
    suspend fun countForTenant(tenantId: String): Int

    @Query("UPDATE bills SET khataEntryId = :khataEntryId WHERE id = :billId")
    suspend fun updateKhataEntryId(billId: String, khataEntryId: String)

    /**
     * D94: COGS for bills in the window — Σ(quantity × purchasePrice) over ALL
     * bills (credit sales included). LEFT JOIN: books deleted later, or rows
     * without a price, contribute 0 — same disclosed basis as the D29 P&L
     * (AccountingRepositoryImpl bookCache ?: 0.0).
     */
    @Query(
        "SELECT COALESCE(SUM(bl.quantity * COALESCE(bk.purchasePrice, 0.0)), 0.0) " +
            "FROM bill_lines bl INNER JOIN bills bi ON bl.billId = bi.id " +
            "LEFT JOIN books bk ON bl.bookId = bk.id " +
            "WHERE bi.tenantId = :tenantId AND bi.billDate >= :startOfDay AND bi.billDate < :endOfDay"
    )
    suspend fun getCogsByDateRange(tenantId: String, startOfDay: Long, endOfDay: Long): Double
}
