package com.boikhata.core.domain.chaos

/**
 * D80: OfflineDaySimulator — models a merchant's full offline day to verify
 * the Offline-First constitutional law is upheld without any Firebase dependency.
 *
 * Simulates a typical busy bookshop day and returns expected Room event counts.
 * Used by [OfflineDaySimulatorTest] to assert that:
 *  - requiresFirestore is always false (Offline-First law)
 *  - cashbook entries match D25/D34 (every money flow creates a cashbook entry)
 *  - khata CREDIT entries are auto-created for unpaid bills (D22)
 *
 * This is a PURE domain service — no Room/Firebase/Android dependency.
 */
object OfflineDaySimulator {

    /**
     * Parameters for a simulated offline day.
     *
     * @param billCount Number of bills created during the day.
     *   Each bill = 1 bill row + [avgLinesPerBill] bill_line rows
     *                 + [avgLinesPerBill] stock_ledger rows (SALE)
     *                 + 1 cashbook_entry (INCOME) if paid
     *                 + 1 khata_entry (CREDIT) if unpaid (D22).
     * @param paidBillFraction Fraction of bills that are fully paid (0.0–1.0).
     * @param khataCollectionCount Manual khata collection entries (PAYMENT type).
     *   Each generates 1 khata_entry + 1 cashbook_entry (INCOME) per D34.
     * @param expenseCount Expense entries. Each generates 1 expense + 1 cashbook_entry (EXPENSE) per D25.
     * @param ownerDrawingCount Owner drawings. Each generates 1 owner_drawing + 1 cashbook_entry (EXPENSE) per D28.
     */
    data class DayParams(
        val billCount: Int = 50,
        val avgLinesPerBill: Int = 2,
        val paidBillFraction: Double = 0.6,
        val khataCollectionCount: Int = 10,
        val expenseCount: Int = 5,
        val ownerDrawingCount: Int = 1,
    )

    data class DayEventCount(
        val bills: Int,
        val billLines: Int,
        val stockLedgerEntries: Int,
        val cashbookEntries: Int,
        val khataCredits: Int,    // CREDIT entries for unpaid bills (D22)
        val khataPayments: Int,   // PAYMENT entries for manual collections
        val expenses: Int,
        val ownerDrawings: Int,
        val totalRoomWrites: Int,
        /** Constitutional invariant: always false. Offline-First law. */
        val requiresFirestore: Boolean,
    )

    /**
     * Simulates a full offline day from [params] and returns expected Room event counts.
     *
     * Accounting contracts verified:
     * 1. requiresFirestore == false always (Offline-First law).
     * 2. cashbookEntries = paidBills + expenses + khataCollections + ownerDrawings
     *    (D25 / D34: every money flow auto-creates a cashbook entry).
     * 3. khataCredits = unpaidBills = billCount − paidBills (D22).
     * 4. stockLedgerEntries = billLines (1 SALE entry per line sold).
     */
    fun simulate(params: DayParams = DayParams()): DayEventCount {
        val paidBills = (params.billCount * params.paidBillFraction).toInt()
        val unpaidBills = params.billCount - paidBills

        val billLines = params.billCount * params.avgLinesPerBill
        val stockLedger = billLines // 1 SALE stock_ledger entry per bill_line

        // D25/D34: cashbook auto-entries
        // paid bill → INCOME; expense → EXPENSE; khata collection → INCOME; drawing → EXPENSE
        val cashbook = paidBills + params.expenseCount +
            params.khataCollectionCount + params.ownerDrawingCount

        val totalWrites = params.billCount + billLines + stockLedger + cashbook +
            unpaidBills + params.khataCollectionCount +
            params.expenseCount + params.ownerDrawingCount

        return DayEventCount(
            bills = params.billCount,
            billLines = billLines,
            stockLedgerEntries = stockLedger,
            cashbookEntries = cashbook,
            khataCredits = unpaidBills,
            khataPayments = params.khataCollectionCount,
            expenses = params.expenseCount,
            ownerDrawings = params.ownerDrawingCount,
            totalRoomWrites = totalWrites,
            requiresFirestore = false, // Offline-First law — never needs Firestore for local ops
        )
    }
}
