package com.boikhata.core.cloud

import com.boikhata.core.database.dao.CloudSyncStateDao
import com.boikhata.core.domain.cloud.LicenseTimestampParser
import com.boikhata.core.domain.enums.LicenseState
import com.boikhata.core.domain.enums.Role
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.LicenseSyncResult
import com.boikhata.core.domain.repository.LicenseSyncRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D42: LicenseSyncRepository implementation.
 * Firebase-Project-Context.md §2: /license_records/{tenantId} — doc ID = tenantId.
 * §6 constraint #7: check snapshot.exists() (missing doc throws NO exception).
 * §6 constraint #8: License read is OWNER-only → gate on role == OWNER.
 * Offline fallback = last known local state (never fabricated).
 *
 * NOTE: Actual Firestore round-trip requires a real device + network.
 * The sandbox cannot verify runtime Firestore behavior.
 */
@Singleton
class LicenseSyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudSyncStateDao: CloudSyncStateDao,
    private val writeGuard: LicenseWriteGuard,
) : LicenseSyncRepository {

    override suspend fun syncLicense(
        tenantId: String,
        role: Role,
    ): LicenseSyncResult {
        // §6 constraint #8: gate on OWNER — non-owners use cached state
        val localState = getLocalState()
        if (role != Role.OWNER) {
            return LicenseSyncResult.NotOwner(localState)
        }

        return try {
            val snapshot = firestore.collection("license_records")
                .document(tenantId)
                .get()
                .await()

            // §6 constraint #7: ALWAYS check exists() — missing doc throws no exception
            val exists = snapshot.exists()
            val data = if (exists) snapshot.data else null

            val parseResult = LicenseTimestampParser.parse(data, exists)

            when (parseResult) {
                is LicenseTimestampParser.ParseResult.Success -> {
                    val parsed = parseResult.license
                    // Update local state
                    cloudSyncStateDao.updateLicenseState(parsed.state.name, System.currentTimeMillis())
                    cloudSyncStateDao.updateLicenseExpiry(parsed.expiresAtMillis, System.currentTimeMillis())
                    writeGuard.updateState(parsed.state)
                    LicenseSyncResult.Synced(parsed.state, parsed.expiresAtMillis)
                }
                is LicenseTimestampParser.ParseResult.MissingDoc -> {
                    // Vendor hasn't provisioned — use last known local state (never fabricate)
                    LicenseSyncResult.MissingDoc(localState)
                }
                is LicenseTimestampParser.ParseResult.Error -> {
                    LicenseSyncResult.Error(parseResult.message, localState)
                }
            }
        } catch (e: Exception) {
            // Network error / offline — use last known local state (never fabricate)
            LicenseSyncResult.Offline(localState)
        }
    }

    private suspend fun getLocalState(): LicenseState {
        val state = cloudSyncStateDao.get()
        val licenseState = if (state != null) {
            try { LicenseState.valueOf(state.licenseState) } catch (e: Exception) { LicenseState.GRACE }
        } else LicenseState.GRACE
        writeGuard.updateState(licenseState)
        return licenseState
    }
}
