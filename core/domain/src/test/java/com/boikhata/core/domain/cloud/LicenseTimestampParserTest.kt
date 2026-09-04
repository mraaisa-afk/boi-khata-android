package com.boikhata.core.domain.cloud

import com.boikhata.core.domain.enums.LicenseState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D42: LicenseTimestampParser unit tests — Timestamp parsing edge cases incl. missing-doc.
 * Firebase-Project-Context.md §6 constraints #7.
 */
class LicenseTimestampParserTest {

    @Test
    fun `should return MissingDoc when exists is false`() {
        val result = LicenseTimestampParser.parse(null, exists = false)
        assertThat(result).isEqualTo(LicenseTimestampParser.ParseResult.MissingDoc)
    }

    @Test
    fun `should return MissingDoc when data is null but exists is true`() {
        // Defensive: snapshot.exists() true but data null (shouldn't happen, but handle it)
        val result = LicenseTimestampParser.parse(null, exists = true)
        assertThat(result).isEqualTo(LicenseTimestampParser.ParseResult.MissingDoc)
    }

    @Test
    fun `should parse valid license with Timestamp fields`() {
        val data = mapOf(
            "tenantId" to "tenant_abc",
            "state" to "ACTIVE",
            "expiresAt" to mapOf("seconds" to 1764576000L, "nanoseconds" to 0),
            "updatedAt" to mapOf("seconds" to 1725148800L, "nanoseconds" to 500_000_000),
        )
        val result = LicenseTimestampParser.parse(data, exists = true)
        assertThat(result).isInstanceOf(LicenseTimestampParser.ParseResult.Success::class.java)
        val license = (result as LicenseTimestampParser.ParseResult.Success).license
        assertThat(license.tenantId).isEqualTo("tenant_abc")
        assertThat(license.state).isEqualTo(LicenseState.FULL)
        assertThat(license.expiresAtMillis).isEqualTo(1764576000000L)
        // 1725148800 * 1000 + 500_000_000 / 1_000_000 = 1725148800500
        assertThat(license.updatedAtMillis).isEqualTo(1725148800500L)
    }

    @Test
    fun `should parse license with Long timestamp fallback`() {
        val data = mapOf(
            "tenantId" to "tenant_abc",
            "state" to "GRACE",
            "expiresAt" to 1764576000000L, // epoch-millis as Long
            "updatedAt" to null,
        )
        val result = LicenseTimestampParser.parse(data, exists = true)
        val license = (result as LicenseTimestampParser.ParseResult.Success).license
        assertThat(license.state).isEqualTo(LicenseState.GRACE)
        assertThat(license.expiresAtMillis).isEqualTo(1764576000000L)
        assertThat(license.updatedAtMillis).isNull()
    }

    @Test
    fun `should parse license with epoch-seconds Long and convert to millis`() {
        // A Long value < 10_000_000_000 is treated as epoch-seconds
        val data = mapOf(
            "tenantId" to "tenant_abc",
            "state" to "ACTIVE",
            "expiresAt" to 1764576000L, // epoch-seconds
        )
        val result = LicenseTimestampParser.parse(data, exists = true)
        val license = (result as LicenseTimestampParser.ParseResult.Success).license
        assertThat(license.expiresAtMillis).isEqualTo(1764576000000L)
    }

    @Test
    fun `should handle null expiresAt`() {
        val data = mapOf(
            "tenantId" to "tenant_abc",
            "state" to "GRACE",
            "expiresAt" to null,
        )
        val result = LicenseTimestampParser.parse(data, exists = true)
        val license = (result as LicenseTimestampParser.ParseResult.Success).license
        assertThat(license.expiresAtMillis).isNull()
    }

    @Test
    fun `should handle missing expiresAt field`() {
        val data = mapOf(
            "tenantId" to "tenant_abc",
            "state" to "GRACE",
        )
        val result = LicenseTimestampParser.parse(data, exists = true)
        val license = (result as LicenseTimestampParser.ParseResult.Success).license
        assertThat(license.expiresAtMillis).isNull()
    }

    @Test
    fun `should return Error when tenantId is missing`() {
        val data = mapOf("state" to "ACTIVE")
        val result = LicenseTimestampParser.parse(data, exists = true)
        assertThat(result).isInstanceOf(LicenseTimestampParser.ParseResult.Error::class.java)
    }

    @Test
    fun `should return Error when state is missing`() {
        val data = mapOf("tenantId" to "tenant_abc")
        val result = LicenseTimestampParser.parse(data, exists = true)
        assertThat(result).isInstanceOf(LicenseTimestampParser.ParseResult.Error::class.java)
    }

    @Test
    fun `should return Error when state is unknown`() {
        val data = mapOf("tenantId" to "tenant_abc", "state" to "EXPIRED")
        val result = LicenseTimestampParser.parse(data, exists = true)
        assertThat(result).isInstanceOf(LicenseTimestampParser.ParseResult.Error::class.java)
        val error = result as LicenseTimestampParser.ParseResult.Error
        assertThat(error.message).contains("EXPIRED")
    }

    @Test
    fun `should parse Timestamp with nanoseconds fractional`() {
        // 750_000_000 nanoseconds = 750ms
        val data = mapOf(
            "tenantId" to "tenant_abc",
            "state" to "ACTIVE",
            "expiresAt" to mapOf("seconds" to 1764576000L, "nanoseconds" to 750_000_000),
        )
        val result = LicenseTimestampParser.parse(data, exists = true)
        val license = (result as LicenseTimestampParser.ParseResult.Success).license
        assertThat(license.expiresAtMillis).isEqualTo(1764576000750L)
    }

    @Test
    fun `should handle Int timestamp field`() {
        val data = mapOf(
            "tenantId" to "tenant_abc",
            "state" to "ACTIVE",
            "expiresAt" to 1764576000, // Int (epoch-seconds)
        )
        val result = LicenseTimestampParser.parse(data, exists = true)
        val license = (result as LicenseTimestampParser.ParseResult.Success).license
        assertThat(license.expiresAtMillis).isEqualTo(1764576000000L)
    }

    @Test
    fun `should parse all known LicenseState values`() {
        for (state in LicenseState.entries) {
            val data = mapOf("tenantId" to "t", "state" to state.name)
            val result = LicenseTimestampParser.parse(data, exists = true)
            assertThat(result).isInstanceOf(LicenseTimestampParser.ParseResult.Success::class.java)
            val license = (result as LicenseTimestampParser.ParseResult.Success).license
            assertThat(license.state).isEqualTo(state)
        }
    }

    // ── parseTimestampField direct tests ────────────────────────────────────

    @Test
    fun `parseTimestampField should handle null`() {
        assertThat(LicenseTimestampParser.parseTimestampField(null)).isNull()
    }

    @Test
    fun `parseTimestampField should handle unknown type`() {
        assertThat(LicenseTimestampParser.parseTimestampField("not a timestamp")).isNull()
    }

    @Test
    fun `parseTimestampField should handle map with missing seconds`() {
        val result = LicenseTimestampParser.parseTimestampField(mapOf("nanoseconds" to 0))
        assertThat(result).isNull()
    }
}
