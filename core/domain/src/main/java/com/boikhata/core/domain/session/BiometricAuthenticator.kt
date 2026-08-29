package com.boikhata.core.domain.session

/**
 * Biometric stub interface — P1 item 3 calls for a biometric-stub-interface.
 * Real biometric wiring (BiometricPrompt) is a future phase; this interface
 * lets the login UI call a no-op stub that always "fails gracefully" (returns false)
 * so the PIN path is the default. Replaced with a real impl in a later phase.
 */
interface BiometricAuthenticator {
    /** Returns true if biometric hardware is available and enrolled. */
    fun isAvailable(): Boolean = false

    /** Attempts biometric auth; returns true on success. Stub always returns false. */
    fun authenticate(): Boolean = false
}

/** P1 stub — no biometric hardware wired yet. */
object NoOpBiometricAuthenticator : BiometricAuthenticator
