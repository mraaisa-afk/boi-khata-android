package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boikhata.core.database.entity.KhataCustomerEntity
import com.boikhata.core.database.entity.KhataEntryEntity

@Dao
interface KhataCustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: KhataCustomerEntity)

    @Query("SELECT * FROM khata_customers WHERE tenantId = :tenantId AND isActive = 1 ORDER BY nameBn")
    suspend fun getActiveByTenant(tenantId: String): List<KhataCustomerEntity>

    @Query("SELECT * FROM khata_customers WHERE id = :id")
    suspend fun getById(id: String): KhataCustomerEntity?
}

@Dao
interface KhataEntryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: KhataEntryEntity)

    @Query("SELECT * FROM khata_entries WHERE tenantId = :tenantId AND customerId = :customerId ORDER BY date ASC")
    suspend fun getByCustomer(tenantId: String, customerId: String): List<KhataEntryEntity>

    @Query("SELECT * FROM khata_entries WHERE tenantId = :tenantId ORDER BY date ASC")
    suspend fun getByTenant(tenantId: String): List<KhataEntryEntity>
}
