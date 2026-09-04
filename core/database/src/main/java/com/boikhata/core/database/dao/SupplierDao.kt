package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boikhata.core.database.entity.SupplierEntity
import com.boikhata.core.database.entity.SupplierEntryEntity

/**
 * D51: DAO for suppliers + supplier_entries (denā/publisher payable ledger).
 * supplier_entries is 🔒 append-only — INSERT only, no UPDATE/DELETE.
 */
@Dao
interface SupplierDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity)

    @Query("SELECT * FROM suppliers WHERE tenantId = :tenantId ORDER BY nameBn")
    suspend fun getSuppliers(tenantId: String): List<SupplierEntity>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: String): SupplierEntity?

    @Query("SELECT * FROM supplier_entries WHERE tenantId = :tenantId AND supplierId = :supplierId ORDER BY date ASC")
    suspend fun getEntries(tenantId: String, supplierId: String): List<SupplierEntryEntity>

    @Query("SELECT * FROM supplier_entries WHERE tenantId = :tenantId AND supplierId = :supplierId AND date >= :start AND date < :end ORDER BY date ASC")
    suspend fun getEntriesByDateRange(tenantId: String, supplierId: String, start: Long, end: Long): List<SupplierEntryEntity>

    @Query("SELECT * FROM supplier_entries WHERE tenantId = :tenantId ORDER BY date ASC")
    suspend fun getAllEntriesByTenant(tenantId: String): List<SupplierEntryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: SupplierEntryEntity)
}
