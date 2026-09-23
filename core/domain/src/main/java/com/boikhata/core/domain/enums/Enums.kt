package com.boikhata.core.domain.enums

/**
 * CONVENTIONS §2 — মান-সেট হুবহু। বাড়তি মান হ্যালুসিনেশন।
 * Do NOT add values not listed here. Names are EXACT.
 */

enum class Role { OWNER, MANAGER, SALES, ACCOUNTANT }

enum class LicenseState { FULL, PAID_UNVERIFIED, GRACE, SOFT_LOCKED, SUSPENDED }

enum class KhataEntryType { CREDIT, PAYMENT, ADJUSTMENT, OPENING }

enum class CashbookAccount { CASH, BKASH, BANK, MOBILE }

enum class CashbookEntryType { INCOME, EXPENSE, TRANSFER }

enum class PaymentMethod { CASH, BKASH, NAGAD, CREDIT, BANK, MOBILE }

/**
 * P12/D92: payment-line category for bill_payment_lines rows. A bill records
 * one row per PAID line plus one DUE row when a remainder posts to khata.
 * CONVENTIONS §2 amended by D92 (owner ruling 2026-09-24).
 */
enum class PaymentLineCategory { CASH, BANK, MOBILE, DUE }

/**
 * P12/D92: mobile-banking provider for MOBILE payment lines. Hardcoded starter
 * set per the D92 provider-mechanism decision (upgrade path to a managed list
 * stays open via a future D-entry). নগদ is stored as NAGAD; display labels
 * disambiguate it from cash («নগদ (Nagad)»).
 */
enum class MfsProvider { BKASH, NAGAD, ROCKET, UPAY, OTHER }

enum class BookCategory { TEXTBOOK, GENERAL, STATIONERY, OTHER }

enum class StockChangeReason { SALE, PURCHASE, RETURN, ADJUSTMENT, MELA_IN, MELA_OUT }

enum class BookCondition { NEW, USED, DAMAGED }

/** D51: SupplierEntryType — supplier/denā payable ledger types (CONVENTIONS §2). */
enum class SupplierEntryType { OPENING, CONSIGNMENT, PURCHASE, PAYMENT, ADJUSTMENT }
