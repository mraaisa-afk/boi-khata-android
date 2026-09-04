package com.boikhata.core.domain.license

import com.boikhata.core.domain.enums.LicenseState

/**
 * ARCH §5: LicenseWriteGuard — সব রাইট-রিপোজিটরিতে ইনজেক্টেড একক-গেট.
 * SOFT_LOCKED/SUSPENDED → LicenseBlockedException.
 * Reads/exports are NEVER blocked (never-lock law).
 */
class LicenseBlockedException(message: String) : Exception(message)

class LicenseWriteGuard {
    @Volatile
    private var currentState: LicenseState = LicenseState.GRACE

    fun updateState(state: LicenseState) {
        currentState = state
    }

    fun getState(): LicenseState = currentState

    /**
     * Call before ANY write mutation. Throws if write-blocked.
     * Reads/exports do NOT call this — they are always allowed.
     */
    fun assertWriteAllowed() {
        when (currentState) {
            LicenseState.FULL, LicenseState.PAID_UNVERIFIED, LicenseState.GRACE -> { /* writes allowed */ }
            LicenseState.SOFT_LOCKED -> throw LicenseBlockedException(
                "লাইসেন্স মেয়াদোত্তীর্ণ — নতুন এন্ট্রি বন্ধ। পড়া ও রিপোর্ট খোলা আছে।"
            )
            LicenseState.SUSPENDED -> throw LicenseBlockedException(
                "সাসপেন্ডেড — নতুন এন্ট্রি বন্ধ। পড়া ও রিপোর্ট খোলা আছে।"
            )
        }
    }

    fun isWriteAllowed(): Boolean = currentState in setOf(
        LicenseState.FULL, LicenseState.PAID_UNVERIFIED, LicenseState.GRACE,
    )
}
