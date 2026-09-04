package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boikhata.core.database.entity.AuditLogEntity

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs WHERE tenantId = :tenantId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(tenantId: String, limit: Int = 50): List<AuditLogEntity>
}
