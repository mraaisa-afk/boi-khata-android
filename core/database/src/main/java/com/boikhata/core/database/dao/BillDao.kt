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

    @Query("SELECT * FROM bill_lines WHERE billId = :billId")
    suspend fun getLinesByBill(billId: String): List<BillLineEntity>
}
