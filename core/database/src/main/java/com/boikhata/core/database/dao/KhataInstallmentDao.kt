package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boikhata.core.database.entity.KhataInstallmentEntity

/**
 * D17: DAO for khata_installments table (exists since v1 schema, DAO added in P2a).
 * Note: khata_installments is NOT 🔒 append-only per CONVENTIONS §3 —
 * isPaid can be UPDATEd (it is a planned-future flag, not a money transaction).
 */
@Dao
interface KhataInstallmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(installment: KhataInstallmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(installments: List<KhataInstallmentEntity>)

    @Query("SELECT * FROM khata_installments WHERE tenantId = :tenantId AND customerId = :customerId ORDER BY dueDate ASC")
    suspend fun getByCustomer(tenantId: String, customerId: String): List<KhataInstallmentEntity>

    @Query("SELECT * FROM khata_installments WHERE tenantId = :tenantId AND khataEntryId = :entryId ORDER BY dueDate ASC")
    suspend fun getByEntry(tenantId: String, entryId: String): List<KhataInstallmentEntity>

    @Query("UPDATE khata_installments SET isPaid = 1 WHERE id = :id")
    suspend fun markPaid(id: String)

    @Query("SELECT COUNT(*) FROM khata_installments WHERE tenantId = :tenantId AND customerId = :customerId AND isPaid = 0")
    suspend fun countUnpaidByCustomer(tenantId: String, customerId: String): Int
}
