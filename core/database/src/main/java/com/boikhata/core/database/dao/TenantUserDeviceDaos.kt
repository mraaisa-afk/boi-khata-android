package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boikhata.core.database.entity.TenantEntity
import com.boikhata.core.database.entity.UserEntity
import com.boikhata.core.database.entity.DeviceEntity

@Dao
interface TenantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tenant: TenantEntity)

    @Query("SELECT * FROM tenants WHERE id = :id")
    suspend fun getById(id: String): TenantEntity?
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE tenantId = :tenantId AND isActive = 1 ORDER BY name")
    suspend fun getActiveByTenant(tenantId: String): List<UserEntity>

    @Query("SELECT * FROM users WHERE tenantId = :tenantId AND role = :role AND isActive = 1")
    suspend fun getByRole(tenantId: String, role: String): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?
}

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity)

    @Query("SELECT * FROM devices WHERE tenantId = :tenantId ORDER BY boundAt DESC")
    suspend fun getByTenant(tenantId: String): List<DeviceEntity>
}
