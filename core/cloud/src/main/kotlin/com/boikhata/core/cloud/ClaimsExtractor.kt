package com.boikhata.core.cloud

import com.boikhata.core.domain.enums.Role
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D40: ClaimsExtractor — reads custom claims {tenantId, role} from a FirebaseUser's ID token.
 * Firebase-Project-Context.md §2: claims are set VENDOR-SIDE via Admin SDK.
 * The app NEVER writes claims — it only reads them.
 */
@Singleton
class ClaimsExtractor @Inject constructor() {

    data class Claims(
        val tenantId: String,
        val role: Role,
    )

    /**
     * Extract claims from a FirebaseUser's ID token.
     * Returns null if claims are not present (pending activation).
     */
    suspend fun extractClaims(user: FirebaseUser): Claims? {
        return try {
            val idToken = user.getIdToken(false).await()
            val claims = idToken?.claims ?: return null
            extractFromMap(claims)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Pure extraction from a claims map — for unit testing without Firebase.
     */
    fun extractFromMap(claims: Map<String, Any?>): Claims? {
        val tenantId = claims["tenantId"] as? String ?: return null
        val roleStr = claims["role"] as? String ?: return null
        val role = try {
            Role.valueOf(roleStr)
        } catch (e: IllegalArgumentException) {
            null
        } ?: return null
        return Claims(tenantId, role)
    }
}
