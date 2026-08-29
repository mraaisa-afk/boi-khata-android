package com.boikhata.core.database.seed

import com.boikhata.core.database.dao.CloudSyncStateDao
import com.boikhata.core.database.dao.TenantDao
import com.boikhata.core.database.dao.UserDao
import com.boikhata.core.database.entity.CloudSyncStateEntity
import com.boikhata.core.database.entity.TenantEntity
import com.boikhata.core.database.entity.UserEntity
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * P1 item 2: seed (১ টেন্যান্ট t_1 + OWNER-ব্যবহারকারী + GRACE-লাইসেন্স-সিড).
 * Idempotent — safe to call on every fresh install; checks before inserting.
 * PIN default = "1234" (owner changes on first login — P2 UX, not this phase).
 * PIN hashing per D8: PBKDF2-HMAC-SHA256, 10k iterations, 256-bit key.
 */
class DatabaseSeeder(
    private val tenantDao: TenantDao,
    private val userDao: UserDao,
    private val cloudSyncStateDao: CloudSyncStateDao,
) {

    suspend fun seedIfEmpty() {
        if (tenantDao.getById(SEED_TENANT_ID) != null) return

        val now = System.currentTimeMillis()
        val (pinHash, salt) = hashPin(DEFAULT_PIN)

        tenantDao.insert(
            TenantEntity(
                id = SEED_TENANT_ID,
                name = "দোকান ১",
                phone = "",
                createdAt = now,
            )
        )
        userDao.insert(
            UserEntity(
                id = SEED_USER_ID,
                tenantId = SEED_TENANT_ID,
                name = "মালিক",
                role = "OWNER",
                pinHash = pinHash,
                salt = salt,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        )
        cloudSyncStateDao.upsert(
            CloudSyncStateEntity(
                id = "primary",
                tenantId = SEED_TENANT_ID,
                cloudPhone = null,
                cloudRole = null,
                isPendingActivation = true,
                lastBackupAt = null,
                lastRestoreAt = null,
                lastCatalogSyncAt = null,
                licenseExpiresAt = null,
                licenseState = "GRACE",
                wifiOnlySync = true,
                updatedAt = now,
            )
        )
    }

    companion object {
        const val SEED_TENANT_ID = "t_1"
        const val SEED_USER_ID = "u_1"
        const val DEFAULT_PIN = "1234"

        fun hashPin(pin: String): Pair<String, String> {
            val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
            val key = deriveKey(pin, salt)
            return key.toHex() to salt.toHex()
        }

        fun verifyPin(pin: String, pinHashHex: String, saltHex: String): Boolean {
            val salt = saltHex.fromHex()
            val derived = deriveKey(pin, salt)
            return derived.toHex() == pinHashHex
        }

        private fun deriveKey(pin: String, salt: ByteArray): ByteArray {
            val spec = PBEKeySpec(pin.toCharArray(), salt, 10_000, 256)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return factory.generateSecret(spec).encoded
        }

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
        private fun String.fromHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
