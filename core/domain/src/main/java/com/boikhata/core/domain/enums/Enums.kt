package com.boikhata.core.domain.enums

/**
 * CONVENTIONS §2 — মান-সেট হুবহু। বাড়তি মান হ্যালুসিনেশন।
 * Do NOT add values not listed here. Names are EXACT.
 */

enum class Role { OWNER, MANAGER, SALES, ACCOUNTANT }

enum class LicenseState { FULL, PAID_UNVERIFIED, GRACE, SOFT_LOCKED, SUSPENDED }

enum class KhataEntryType { CREDIT, PAYMENT, ADJUSTMENT, OPENING }

enum class CashbookAccount { CASH, BKASH, BANK }

enum class CashbookEntryType { INCOME, EXPENSE, TRANSFER }

enum class PaymentMethod { CASH, BKASH, NAGAD, CREDIT }

enum class BookCategory { TEXTBOOK, GENERAL, STATIONERY, OTHER }

enum class StockChangeReason { SALE, PURCHASE, RETURN, ADJUSTMENT, MELA_IN, MELA_OUT }

enum class BookCondition { NEW, USED, DAMAGED }
