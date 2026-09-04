package com.boikhata.core.domain.model

/**
 * D56-D57: Mela mode (বইমেলা / seasonal) domain models.
 * Pure data — no Room, no Android.
 */

/** A first-class mela/seasonal session (D57). */
data class MelaSession(
    val id: String,
    val tenantId: String,
    val nameBn: String,
    val location: String,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean,
    val isPaused: Boolean,
    val pauseReason: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

/** A low-stock soft-reservation warning (D56) — a warning, never a hard freeze. */
data class LowStockAlert(
    val bookId: String,
    val bookTitleBn: String,
    val melaStock: Int, // stock currently at the mela stall (atMela)
    val softThreshold: Int, // 3 by default (Blueprint §8 "≤৩ পরিমাণে")
    val atShop: Int, // how much of the total stock is still at the shop
)

/** An oversell / reconciliation alert (D56) — stock went negative (sold > available). */
data class OversellAlert(
    val bookId: String,
    val bookTitleBn: String,
    val currentStock: Int, // negative when oversold
    val oversoldBy: Int, // abs(currentStock)
)

/** Per-book stock breakdown for the mela report. */
data class MelaStockLine(
    val bookId: String,
    val bookTitleBn: String,
    val netStock: Int, // total inventory (shop + mela)
    val melaIn: Int, // total MELA_IN quantity
    val melaOut: Int, // total MELA_OUT quantity
    val atMela: Int, // melaIn - melaOut
)
