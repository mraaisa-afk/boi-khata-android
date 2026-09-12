package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.boikhata.core.database.entity.KhataCustomerEntity
import com.boikhata.core.database.entity.KhataEntryEntity

@Dao
interface KhataCustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: KhataCustomerEntity)

    @Update
    suspend fun update(customer: KhataCustomerEntity)

    @Query("SELECT * FROM khata_customers WHERE tenantId = :tenantId AND isActive = 1 ORDER BY nameBn")
    suspend fun getActiveByTenant(tenantId: String): List<KhataCustomerEntity>

    @Query("SELECT * FROM khata_customers WHERE id = :id")
    suspend fun getById(id: String): KhataCustomerEntity?

    @Query("""
        SELECT * FROM khata_customers
        WHERE tenantId = :tenantId AND isActive = 1
        AND (nameBnNormalized LIKE '%' || :normalizedQuery || '%'
             OR phone LIKE '%' || :normalizedQuery || '%'
             OR address LIKE '%' || :normalizedQuery || '%')
        ORDER BY nameBn
    """)
    suspend fun search(tenantId: String, normalizedQuery: String): List<KhataCustomerEntity>
}

@Dao
interface KhataEntryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: KhataEntryEntity)

    @Query("SELECT * FROM khata_entries WHERE tenantId = :tenantId AND customerId = :customerId ORDER BY date ASC")
    suspend fun getByCustomer(tenantId: String, customerId: String): List<KhataEntryEntity>

    @Query("SELECT * FROM khata_entries WHERE tenantId = :tenantId ORDER BY date ASC")
    suspend fun getByTenant(tenantId: String): List<KhataEntryEntity>

    /**
     * D81: Aggregate sum of PAYMENT-type entries within a date window.
     * Used by HomeViewModel to compute the «খাতা আদায়» component of the D79
     * net-profit formula:  নিট লাভ = (নগদ বিক্রি + খাতা আদায়) − নগদ ব্যয়.
     * Only positive amounts are included; negative ADJUSTMENTs (দেনা মুন) are excluded.
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM khata_entries
        WHERE tenantId  = :tenantId
          AND type      = 'PAYMENT'
          AND amount    > 0
          AND date      >= :start
          AND date      <= :end
    """)
    suspend fun getPaymentSumByDateRange(tenantId: String, start: Long, end: Long): Double
}
