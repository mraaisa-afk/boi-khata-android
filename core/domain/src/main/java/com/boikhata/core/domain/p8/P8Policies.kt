package com.boikhata.core.domain.p8

import java.security.MessageDigest

object VersionPolicy {
    data class Release(val versionCode: Int, val versionName: String, val downloadUrl: String?)
    fun updateAvailable(currentCode: Int, latest: Release): Boolean = latest.versionCode > currentCode
}

object ReferralCodeGenerator {
    fun codeForTenant(tenantId: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(tenantId.toByteArray())
        return digest.take(6).joinToString("") { "%02X".format(it) }
    }
}

object DeviceGroupPolicy {
    const val LITE_DEVICE_LIMIT = 2
    fun canAddDevice(activeDeviceCount: Int, limit: Int = LITE_DEVICE_LIMIT): Boolean = activeDeviceCount < limit
}

object DemoResetPolicy {
    data class Decision(val requiresConfirmation: Boolean, val warning: String)
    fun request(): Decision = Decision(true, "demo_reset_confirmation_required")
}

object FoundersClubPolicy {
    const val MONTHLY_FEE_TAKA = 150
    const val MAX_TENANTS = 50
    fun isEligible(tenantNumber: Int): Boolean = tenantNumber in 1..MAX_TENANTS
}
