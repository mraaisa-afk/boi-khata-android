package com.boikhata.core.domain.datameter

import com.boikhata.core.domain.enums.LicenseState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D9: Data-meter P1 foundation — local accumulator + Wi-Fi-only toggle.
 * No Firestore this phase; the counter is a no-op accumulator until P4 wires real calls.
 * Blueprint §4 law 7: "অ্যাপে মাসিক MB-ব্যবহার দেখাও; Wi-Fi-only সিঙ্ক-টগল ডিফল্ট ON".
 */
@Singleton
class DataMeter @Inject constructor() {

    @Volatile
    private var monthlyBytes: Long = 0L

    @Volatile
    private var wifiOnlySync: Boolean = true // default ON per Blueprint law 7

    /**
     * Called by the cloud layer (P4) after each Firestore operation.
     * Accumulates bytes for the current month. P1: no real Firestore calls yet.
     */
    fun recordBytes(byteCount: Long) {
        if (byteCount > 0) {
            monthlyBytes += byteCount
        }
    }

    fun getMonthlyBytes(): Long = monthlyBytes

    fun getMonthlyMb(): Double = monthlyBytes / (1024.0 * 1024.0)

    fun resetMonthly() {
        monthlyBytes = 0L
    }

    fun setWifiOnlySync(enabled: Boolean) {
        wifiOnlySync = enabled
    }

    fun isWifiOnlySync(): Boolean = wifiOnlySync

    /**
     * Whether a cloud operation should proceed given the current toggle.
     * P1: always returns the toggle value (no network check wired yet).
     * P4 will add the actual Wi-Fi state check.
     */
    fun shouldSyncOverWifi(): Boolean = wifiOnlySync
}
