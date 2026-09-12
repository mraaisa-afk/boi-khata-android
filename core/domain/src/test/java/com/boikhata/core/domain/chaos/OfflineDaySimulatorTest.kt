package com.boikhata.core.domain.chaos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D80 — JUnit 4 (D69). */
class OfflineDaySimulatorTest {

    @Test
    fun `should never require Firestore for any offline day scenario`() {
        val result = OfflineDaySimulator.simulate()
        assertFalse(
            "Offline-First law: no operation on a typical merchant day should require Firestore",
            result.requiresFirestore,
        )
    }

    @Test
    fun `should produce correct bill and bill line counts`() {
        val params = OfflineDaySimulator.DayParams(billCount = 50, avgLinesPerBill = 2)
        val result = OfflineDaySimulator.simulate(params)

        assertEquals(50, result.bills)
        assertEquals(100, result.billLines)
    }

    @Test
    fun `should auto-create khata credit entries for unpaid bills (D22)`() {
        // 60% paid → 40% unpaid → 20 khata CREDIT entries
        val params = OfflineDaySimulator.DayParams(billCount = 50, paidBillFraction = 0.6)
        val result = OfflineDaySimulator.simulate(params)

        assertEquals(20, result.khataCredits)
    }

    @Test
    fun `should auto-create cashbook entries for every money flow (D25, D34)`() {
        // 30 paid bills + 5 expenses + 10 khata collections + 1 drawing = 46
        val params = OfflineDaySimulator.DayParams(
            billCount = 50,
            paidBillFraction = 0.6,
            khataCollectionCount = 10,
            expenseCount = 5,
            ownerDrawingCount = 1,
        )
        val result = OfflineDaySimulator.simulate(params)

        assertEquals(46, result.cashbookEntries)
    }

    @Test
    fun `should produce stock ledger entries equal to total bill lines (SALE)`() {
        val params = OfflineDaySimulator.DayParams(billCount = 50, avgLinesPerBill = 3)
        val result = OfflineDaySimulator.simulate(params)

        assertEquals(150, result.stockLedgerEntries)
    }

    @Test
    fun `should handle extreme airplane day with 100 bills offline`() {
        val params = OfflineDaySimulator.DayParams(
            billCount = 100,
            avgLinesPerBill = 3,
            paidBillFraction = 0.5,
            khataCollectionCount = 20,
            expenseCount = 10,
            ownerDrawingCount = 2,
        )
        val result = OfflineDaySimulator.simulate(params)

        assertFalse(result.requiresFirestore)
        assertTrue("Total writes should be positive", result.totalRoomWrites > 0)
    }

    @Test
    fun `should produce zero khata credits when all bills are fully paid`() {
        val params = OfflineDaySimulator.DayParams(billCount = 20, paidBillFraction = 1.0)
        val result = OfflineDaySimulator.simulate(params)

        assertEquals(0, result.khataCredits)
    }
}
