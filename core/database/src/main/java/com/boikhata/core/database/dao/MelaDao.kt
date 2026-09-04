package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boikhata.core.database.entity.MelaSessionEntity

/**
 * D57: DAO for mela_sessions (book-fair / seasonal session).
 */
@Dao
interface MelaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: MelaSessionEntity)

    @Query("UPDATE mela_sessions SET isActive = :isActive, isPaused = :isPaused, pauseReason = :pauseReason, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateState(id: String, isActive: Boolean, isPaused: Boolean, pauseReason: String?, updatedAt: Long)

    @Query("SELECT * FROM mela_sessions WHERE tenantId = :tenantId AND isActive = 1 ORDER BY startDate DESC LIMIT 1")
    suspend fun getActiveSession(tenantId: String): MelaSessionEntity?

    @Query("SELECT * FROM mela_sessions WHERE tenantId = :tenantId ORDER BY startDate DESC")
    suspend fun getSessions(tenantId: String): List<MelaSessionEntity>

    @Query("SELECT * FROM mela_sessions WHERE id = :id")
    suspend fun getById(id: String): MelaSessionEntity?
}
