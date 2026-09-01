package com.boikhata.core.domain.cloud

import com.boikhata.core.domain.enums.LicenseState

/**
 * D42: LicenseTimestampParser — parses Firestore Timestamp fields + handles missing-doc.
 * Firebase-Project-Context.md §6 constraint #7: "expiresAt arrives as Firestore Timestamp →
 * parse getTimestamp() with getLong() fallback; ALWAYS check snapshot.exists() (missing doc
 * throws NO exception — the catch block alone is insufficient)."
 *
 * The Firestore SDK's com.google.firebase.Timestamp is an Android concern. This pure service
 * accepts the field data as a Map<String, Any?> (the document snapshot's data) so the parsing
 * logic is unit-testable without Firestore. The repository layer extracts the map from the
 * snapshot and passes it here.
 *
 * Pure function — no Android, no Firestore. Independently unit-testable.
 */
object LicenseTimestampParser {

    /** The parsed license document. */
    data class ParsedLicense(
        val tenantId: String,
        val state: LicenseState,
        val expiresAtMillis: Long?,
        val updatedAtMillis: Long?,
    )

    /** Result of parsing: either a valid license, a missing doc, or a parse error. */
    sealed class ParseResult {
        data class Success(val license: ParsedLicense) : ParseResult()
        data object MissingDoc : ParseResult()
        data class Error(val message: String) : ParseResult()
    }

    /**
     * Parse a Firestore document's data map into a ParsedLicense.
     *
     * Firestore Timestamp fields arrive as maps with "seconds" and "nanoseconds" keys
     * when extracted via getData(). The repository may also pass a Long (if the field
     * was stored as a number). This parser handles both.
     *
     * @param data the document snapshot's data map (from snapshot.getData()), or null if !exists()
     * @param exists whether the document exists (snapshot.exists())
     */
    fun parse(data: Map<String, Any?>?, exists: Boolean): ParseResult {
        if (!exists) return ParseResult.MissingDoc
        if (data == null) return ParseResult.MissingDoc

        try {
            val tenantId = data["tenantId"] as? String
                ?: return ParseResult.Error("missing tenantId field")

            val stateStr = data["state"] as? String
                ?: return ParseResult.Error("missing state field")

            // Firebase-Project-Context §2 uses "ACTIVE" in Firestore; the local enum
            // uses FULL (same meaning — fully paid/active license). Map ACTIVE → FULL.
            val mappedStateStr = when (stateStr) {
                "ACTIVE" -> "FULL"
                else -> stateStr
            }
            val state = try {
                LicenseState.valueOf(mappedStateStr)
            } catch (e: IllegalArgumentException) {
                return ParseResult.Error("unknown state: $stateStr")
            }

            val expiresAtMillis = parseTimestampField(data["expiresAt"])
            val updatedAtMillis = parseTimestampField(data["updatedAt"])

            return ParseResult.Success(
                ParsedLicense(
                    tenantId = tenantId,
                    state = state,
                    expiresAtMillis = expiresAtMillis,
                    updatedAtMillis = updatedAtMillis,
                )
            )
        } catch (e: Exception) {
            return ParseResult.Error(e.message ?: "parse error")
        }
    }

    /**
     * Parse a Firestore Timestamp field into epoch-millis.
     * Handles three forms:
     * 1. Firestore Timestamp as a Map: {"seconds": Long, "nanoseconds": Int}
     * 2. A plain Long (epoch-millis or epoch-seconds — we detect by magnitude)
     * 3. null / missing → null
     */
    fun parseTimestampField(value: Any?): Long? {
        if (value == null) return null

        // Form 1: Firestore Timestamp as a Map {seconds, nanoseconds}
        if (value is Map<*, *>) {
            val seconds = value["seconds"] as? Long
                ?: (value["seconds"] as? Int)?.toLong()
                ?: return null
            val nanoseconds = value["nanoseconds"] as? Int
                ?: (value["nanoseconds"] as? Long)?.toInt()
                ?: 0
            return seconds * 1000L + nanoseconds / 1_000_000L
        }

        // Form 2: a plain Long
        if (value is Long) {
            // Detect epoch-seconds (small number < year 3000 in seconds) vs epoch-millis
            // 32503680000L = year 3000 in millis; 32503680000L in seconds would be year ~3000
            // We use: if value < 10_000_000_000L (year 2286 in seconds), treat as seconds
            return if (value < 10_000_000_000L) value * 1000L else value
        }

        if (value is Int) {
            return value.toLong() * 1000L
        }

        return null
    }
}
