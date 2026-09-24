package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.boikhata.core.database.entity.KhataCustomerEntity
import com.boikhata.core.database.entity.KhataEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KhataCustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: KhataCustomerEntity)

    @Update
    suspend fun update(customer: KhataCustomerEntity)

    @Query("SELECT * FROM khata_customers WHERE tenantId = :tenantId AND isActive = 1 ORDER BY nameBn")
    suspend fun getActiveByTenant(tenantId: String): List<KhataCustomerEntity>

    /**
     * B-002: Room-reactive Flow over active customers for a tenant.
     * MUST stay non-suspend: Room drives it from its invalidation tracker and
     * re-emits on every khata_customers write — including inserts made from
     * other ViewModel instances (e.g., KhataAddCustomerScreen's own hiltViewModel()).
     * This is what makes a newly added customer appear without a manual reload.
     */
    @Query("SELECT * FROM khata_customers WHERE tenantId = :tenantId AND isActive = 1 ORDER BY nameBn")
    fun getActiveByTenantFlow(tenantId: String): Flow<List<KhataCustomerEntity>>

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
     * D92 restore-repair: count entries linked to a bill by referenceBillId —
     * used to detect restored bills whose বাকি CREDIT never made it into the
     * backup (bills ↔ khata_entries restore independently; Firestore has no
     * cross-collection atomicity). Idempotency guard for the repair pass.
     */
    @Query("SELECT COUNT(*) FROM khata_entries WHERE referenceBillId = :referenceBillId")
    suspend fun countByReferenceBillId(referenceBillId: String): Int

    /**
     * D81: Aggregate sum of PAYMENT-type entries within a date window.
     * D94 (2026-09-24): খাতা আদায় is no longer part of the home নিট লাভ
     * formula (নিট লাভ = (মোট বিক্রি − COGS) − খরচ); the aggregate is still
     * exposed for the খাতা আদায় stat. Only positive amounts are included;
     * negative ADJUSTMENTs (দেনা-পাওনা) are excluded.
     * Only positive amounts are included; negative ADJUSTMENTs (দেনা-পাওনা) are excluded.
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
