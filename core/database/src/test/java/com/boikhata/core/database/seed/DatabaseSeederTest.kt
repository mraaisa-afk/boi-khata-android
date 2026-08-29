package com.boikhata.core.database.seed

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DatabaseSeederTest {

    @Test
    fun `should hash and verify a correct PIN`() {
        val (hash, salt) = DatabaseSeeder.hashPin("1234")
        assertThat(DatabaseSeeder.verifyPin("1234", hash, salt)).isTrue()
    }

    @Test
    fun `should reject a wrong PIN`() {
        val (hash, salt) = DatabaseSeeder.hashPin("1234")
        assertThat(DatabaseSeeder.verifyPin("0000", hash, salt)).isFalse()
    }

    @Test
    fun `should produce different salts for different calls`() {
        val (_, salt1) = DatabaseSeeder.hashPin("1234")
        val (_, salt2) = DatabaseSeeder.hashPin("1234")
        assertThat(salt1).isNotEqualTo(salt2)
    }

    @Test
    fun `should produce different hashes for same PIN with different salts`() {
        val (hash1, salt1) = DatabaseSeeder.hashPin("1234")
        val (hash2, salt2) = DatabaseSeeder.hashPin("1234")
        assertThat(hash1).isNotEqualTo(hash2)
    }

    @Test
    fun `should verify PIN after round-trip`() {
        val pin = "5678"
        val (hash, salt) = DatabaseSeeder.hashPin(pin)
        // Simulate storing and retrieving from DB
        val storedHash = hash
        val storedSalt = salt
        assertThat(DatabaseSeeder.verifyPin(pin, storedHash, storedSalt)).isTrue()
    }
}
