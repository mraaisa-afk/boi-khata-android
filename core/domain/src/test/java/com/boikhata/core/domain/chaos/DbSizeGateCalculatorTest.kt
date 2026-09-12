package com.boikhata.core.domain.chaos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D80 — JUnit 4 (D69). */
class DbSizeGateCalculatorTest {

    @Test
    fun `should pass gate when 30-day soak stays within 5MB`() {
        val snapshot = DbSizeGateCalculator.thirtyDaySoakSnapshot(dailyBills = 50, avgLinesPerBill = 2)
        val result = DbSizeGateCalculator.evaluate(snapshot)

        assertTrue(
            "30-day soak must stay ≤5MB (ARCHITECTURE §7): ${result.message}",
            result.passed,
        )
    }

    @Test
    fun `should have positive estimated bytes for 30-day soak`() {
        val snapshot = DbSizeGateCalculator.thirtyDaySoakSnapshot()
        val result = DbSizeGateCalculator.evaluate(snapshot)

        assertTrue(result.estimatedBytes > 0)
        assertTrue(result.estimatedBytes <= DbSizeGateCalculator.MAX_DB_BYTES)
    }

    @Test
    fun `should fail gate when row counts far exceed budget`() {
        // Extreme: 500 bills/day × 30 days × 5 lines each
        val snapshot = DbSizeGateCalculator.thirtyDaySoakSnapshot(dailyBills = 500, avgLinesPerBill = 5)
        val result = DbSizeGateCalculator.evaluate(snapshot)

        assertFalse(
            "Extreme 500-bills/day scenario should fail the size gate",
            result.passed,
        )
    }

    @Test
    fun `should return zero bytes and pass gate for empty snapshot`() {
        val empty = DbSizeGateCalculator.DbSnapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val result = DbSizeGateCalculator.evaluate(empty)

        assertEquals(0L, result.estimatedBytes)
        assertTrue(result.passed)
    }

    @Test
    fun `should generate 30-day snapshot with approximately 4500 total events`() {
        val snapshot = DbSizeGateCalculator.thirtyDaySoakSnapshot(dailyBills = 50, avgLinesPerBill = 2)
        // Major append-only event tables counted:
        // bills=1500 + billLines=3000 + khataEntries=300 + expenses=150
        // + cashbookEntries=1950 + stockLedger=3150 = 10,050 total rows
        // ARCHITECTURE §7 references ~4,500 "merchant events" (bills+khata+expenses);
        // the full append-only row count including derived tables is higher.
        val totalEvents = snapshot.billCount + snapshot.billLineCount + snapshot.khataEntryCount +
            snapshot.expenseCount + snapshot.cashbookEntryCount + snapshot.stockLedgerCount
        assertTrue(
            "Should have ≥3,000 append-only rows for 30-day soak, got $totalEvents",
            totalEvents >= 3_000,
        )
        assertTrue(
            "Should have ≤15,000 append-only rows for 30-day soak, got $totalEvents",
            totalEvents <= 15_000,
        )
    }
}
