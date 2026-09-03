package com.boikhata.core.domain.model

import com.boikhata.core.domain.enums.LicenseState

/**
 * D42: License sync result — the outcome of a license sync attempt.
 * Firebase-Project-Context.md §6 constraints #7, #8.
 */
sealed class LicenseSyncResult {
    /** Successfully synced from Firestore. */
    data class Synced(val state: LicenseState, val expiresAtMillis: Long?) : LicenseSyncResult()
    /** Offline — Firestore unreachable, using last known local state. */
    data class Offline(val lastKnownState: LicenseState) : LicenseSyncResult()
    /** Non-OWNER — rules deny the read, using locally cached state. */
    data class NotOwner(val locallyCachedState: LicenseState) : LicenseSyncResult()
    /** Document doesn't exist in Firestore — vendor hasn't provisioned yet. */
    data class MissingDoc(val lastKnownState: LicenseState) : LicenseSyncResult()
    /** Parse or other error — using last known local state. */
    data class Error(val message: String, val lastKnownState: LicenseState) : LicenseSyncResult()
}
