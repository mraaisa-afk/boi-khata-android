package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boikhata.core.database.entity.BillPaymentLineEntity

/**
 * P12/D92: bill_payment_lines — the authoritative per-bill payment record.
 * Rows are written ONLY inside the D22 atomic checkout transaction (or by
 * restore), never piecemeal.
 */
@Dao
interface BillPaymentLineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lines: List<BillPaymentLineEntity>)

    @Query("SELECT * FROM bill_payment_lines WHERE billId = :billId ORDER BY rowid ASC")
    suspend fun getByBill(billId: String): List<BillPaymentLineEntity>

    @Query("SELECT COUNT(*) FROM bill_payment_lines WHERE billId = :billId")
    suspend fun countForBill(billId: String): Int

    /**
     * P12: every payment line belonging to bills in [startOfDay, endOfDay) —
     * the CashClose per-line salesByMethod aggregation source. Legacy bills
     * (created before v7) have no rows here and fall back to the bill-level
     * denormalized columns.
     */
    @Query(
        "SELECT l.* FROM bill_payment_lines l " +
            "JOIN bills b ON l.billId = b.id " +
            "WHERE l.tenantId = :tenantId AND b.billDate >= :startOfDay AND b.billDate < :endOfDay " +
            "ORDER BY l.rowid ASC"
    )
    suspend fun getByBillDateRange(tenantId: String, startOfDay: Long, endOfDay: Long): List<BillPaymentLineEntity>
}
