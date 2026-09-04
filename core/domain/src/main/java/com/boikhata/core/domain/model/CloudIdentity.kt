package com.boikhata.core.domain.model

import com.boikhata.core.domain.enums.Role

/**
 * D40: Cloud identity models.
 * Firebase-Project-Context.md §2: Phone OTP → ID token carries custom claims {tenantId, role}.
 * Claims are set VENDOR-SIDE — the app NEVER writes claims.
 */

/** The Firebase-authenticated user with optional claims. */
data class CloudUser(
    val uid: String,
    val phone: String,
    val tenantId: String?, // null = not yet provisioned (pending activation)
    val role: Role?,       // null = not yet provisioned
) {
    /** True if the vendor has provisioned this user (claims present in ID token). */
    val hasClaims: Boolean get() = tenantId != null && role != null
}
