package com.boikhata.core.cloud

import com.boikhata.core.domain.enums.Role
import com.boikhata.core.domain.model.CloudUser
import com.boikhata.core.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D40: AuthRepository implementation — wraps Firebase Phone Auth.
 * Firebase-Project-Context.md §2: Phone OTP → ID token claims {tenantId, role}.
 * Claims are set VENDOR-SIDE — the app NEVER writes claims.
 *
 * NOTE: Actual OTP sending/verification requires a real device + network.
 * The sandbox cannot verify runtime Firebase behavior — the build verifies
 * compilation + the pure ClaimsSession logic is unit-tested separately.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val claimsExtractor: ClaimsExtractor,
) : AuthRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // Stored verification ID for the OTP flow
    @Volatile
    private var storedVerificationId: String? = null

    override suspend fun startPhoneVerification(phone: String): Boolean {
        // PhoneAuthProvider.verifyPhoneNumber requires an Activity + callbacks.
        // The actual OTP sending is triggered from the ViewModel with Activity context.
        // This method is a marker — the ViewModel handles the PhoneAuthProvider flow directly.
        // Returns true to indicate the flow was initiated.
        return true
    }

    /**
     * Verify the OTP code using the stored verification ID.
     * Called after the user enters the code received via SMS.
     */
    suspend fun verifyOtpWithVerificationId(verificationId: String, code: String): Boolean {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val result = auth.signInWithCredential(credential).await()
            result.user != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun verifyOtp(code: String): Boolean {
        val vId = storedVerificationId ?: return false
        return verifyOtpWithVerificationId(vId, code)
    }

    fun setVerificationId(id: String) {
        storedVerificationId = id
    }

    override suspend fun getCurrentCloudUser(): CloudUser? {
        val firebaseUser = auth.currentUser ?: return null
        val phone = firebaseUser.phoneNumber ?: ""
        val claims = claimsExtractor.extractClaims(firebaseUser)
        return CloudUser(
            uid = firebaseUser.uid,
            phone = phone,
            tenantId = claims?.tenantId,
            role = claims?.role,
        )
    }

    override fun isSignedIn(): Boolean {
        return auth.currentUser != null
    }

    override suspend fun signOut() {
        auth.signOut()
        storedVerificationId = null
    }
}
