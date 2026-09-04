package com.boikhata.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * D57: mela_sessions(id PK, tenantId, nameBn, location, startDate, endDate, isActive,
 * isPaused, pauseReason?, createdAt, updatedAt) — a first-class book-fair / seasonal session.
 * A paused session blocks new MELA_IN/MELA_OUT stock moves but keeps reads/stats open.
 */
@Entity(tableName = "mela_sessions")
data class MelaSessionEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val nameBn: String,
    val location: String,
    val startDate: Long, // epoch-millis
    val endDate: Long, // epoch-millis
    val isActive: Boolean,
    val isPaused: Boolean,
    val pauseReason: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
