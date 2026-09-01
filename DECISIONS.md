# DECISIONS.md — বই খাতা Decision Log

**This file is append-only.** Never edit or delete a past entry — if a decision changes, add a new entry that references and supersedes the old one by number. This mirrors the app's own event-sourced ledger philosophy: the history is the source of truth, not the current state alone.

**When to add an entry:** any time you make a non-trivial choice that `ARCHITECTURE.md` doesn't already specify — a library choice between two reasonable options, a naming convention, a workaround for a platform limitation, an interpretation of an ambiguous requirement. If you're about to do something `ARCHITECTURE.md` doesn't cover, write the entry *before* you write the code, not after.

**Format:**
```
## D<number> — <short title>
**Date:** <session date>
**Phase:** <phase number from PROGRESS.md>
**Context:** what problem/ambiguity prompted this decision
**Decision:** what was chosen
**Alternatives considered:** what else was on the table and why it lost
**Supersedes:** (optional) D<n> if this replaces an earlier decision
```

Never resolve a merge conflict in this file by picking one side automatically — a conflict here means two sessions ran concurrently and need manual reconciliation.

---

## D1 — Seed entry: repo established
**Date:** 2026-08-29
**Phase:** 0
**Context:** Repository initialized with the agent constitution (Blueprint v1.0, ARCHITECTURE, CONVENTIONS, BUILD, PROGRESS, Firebase-Project-Context, .gitignore, catalog). No code-level decisions made yet — this entry establishes the log format and numbering from D2.
**Decision:** Sequential `D<n>` numbering, oldest first, never renumbered even if an early decision is later superseded.
**Alternatives considered:** Date-only entries without sequence numbers — rejected because sequence numbers keep "supersedes" references unambiguous even for same-day decisions.
**Supersedes:** —

---

## D2 — Bangladesh Demographic UI/UX Optimization ("Lal Khata" theme)
**Date:** 2026-08-29
**Phase:** 0
**Context:** Need to optimize the UI/UX architecture to cater strictly to the target demographic: 45+ year-old BD shopkeepers in noisy environments using low-end devices. Prevailing Material 3 default configurations are too subtle, hard to tap, cause eye-strain under harsh lights, and rendering PNGs on 3GB RAM devices risks OutOfMemory (OOM) crashes.
**Decision:**
1. **Receipts:** Abandon PNG rendering entirely. Use Unicode text or lightweight PDF for WhatsApp sharing.
2. **Colors & Theming:** Implement "Lal Khata" Theme (`#800000` primary, `#FDFAF6` ivory background to reduce eye strain).
3. **Accessibility:** Over-scale default Typography by 20% independent of OS settings.
4. **Touch & Feel:** Enforce 56dp–64dp touch targets, skip flat ghost buttons in favor of elevated skeuomorphic buttons, and mandate haptic feedback on saves.
5. **Layout:** Ban Hamburger menus (use Bottom Navigation) and eliminate dashboard charts (use Trident numbers: Cash, Supplier Dues, Customer Dues).
6. **Support UI:** Put a professional Vendor Card in Settings with big "Call" and "WhatsApp" buttons; no logos on login/dashboard.
**Alternatives considered:** Default Material 3 styling (rejected for poor accessibility), Chart-based dashboard (rejected for resource consumption and lack of utility to users).
**Supersedes:** —

---

## D3 — Font ownership: core/designsystem
**Date:** 2026-08-29
**Phase:** 0
**Context:** Noto Sans Bengali must live in exactly one module (P0 item 4); candidates were app-res vs core/designsystem.
**Decision:** Bundle the font under `core/designsystem/src/main/res/font` — designsystem owns all shared visual resources; app stays a thin shell.
**Alternatives considered:** app-res placement (rejected: feature modules could not reference it without an app dependency, violating module boundaries).
**Supersedes:** —

---

## D4 — KSP version normalized to 2.3.11
**Date:** 2026-08-29
**Phase:** 0
**Context:** The constitution catalog carried KSP as a ⚠ VERIFY entry (`2.3.11-1.0.32`); the P0 build required a KSP release that exactly matches the catalog's Kotlin line.
**Decision:** Resolve KSP to **2.3.11** (the release line matching Kotlin 2.3.x per the KSP releases page) and update `gradle/libs.versions.toml` accordingly; remaining ⚠ VERIFY entries stay untouched until needed.
**Alternatives considered:** Guessing a `-1.0.x` suffix (forbidden by the catalog's own rule); bumping Kotlin to match a newer KSP (out of scope for P0).
**Supersedes:** —

---

## D5 — AGP 9 built-in Kotlin path; Hilt via KSP
**Date:** 2026-08-29
**Phase:** 0
**Context:** AGP 9.x ships built-in Kotlin support; applying `org.jetbrains.kotlin.android` in Android modules conflicts with it.
**Decision:** Rely on AGP 9's built-in Kotlin in Android modules; declare KSP via its own plugin; run Hilt's processor through KSP (no kapt anywhere).
**Alternatives considered:** Applying the Kotlin-Android plugin anyway (rejected: conflicts with AGP 9 built-in Kotlin); kapt for Hilt (rejected: slower, deprecated path).
**Supersedes:** —

---

## D6 — Gradle wrapper pinned to 9.3.1
**Date:** 2026-08-29
**Phase:** 0
**Context:** AGP 9.1.1 requires a newer Gradle than the default available in the build environment; the wrapper must be pinned explicitly.
**Decision:** Pin the Gradle wrapper to **9.3.1** in `gradle/wrapper/gradle-wrapper.properties` (the verified line satisfying AGP 9.1.1 in the P0 build).
**Alternatives considered:** Letting the environment pick a default (rejected: unreproducible builds); a newer unverified Gradle (rejected: never guess versions).
**Supersedes:** —

---

## D7 — Entity home: core/database owns @Entity; core/domain owns enums + services + repo interfaces
**Date:** 2026-08-30
**Phase:** 1
**Context:** ARCHITECTURE §2 assigns "Room schema, DAO, migration" to core/database and "entity-model, repository-interface, domain-service" to core/domain. Room @Entity classes ARE the entity-models, so both modules seem to own them — a conflict.
**Decision:** @Entity data classes live in `core/database` (they carry Room annotations = build-time coupling to Room). `core/domain` owns the pure enums (CONVENTIONS §2 — no Room dependency), repository interfaces, and domain services (LicensePolicy, AgingCalculator). core/database depends on core/domain for the enums; core/domain never depends on core/database. Feature modules depend on core/domain (interfaces) + get core/database (impl) via Hilt.
**Alternatives considered:** Entities in core/domain with Room annotations (rejected: forces every feature to transitively depend on Room); duplicate DTOs mapped entity↔model (rejected: pointless boilerplate for a single-tenant-offline app).
**Supersedes:** —

---

## D8 — PIN hashing: PBKDF2-HMAC-SHA256 via javax.crypto (no external crypto dependency)
**Date:** 2026-08-30
**Phase:** 1
**Context:** CONVENTIONS §3 stores `pinHash` + `salt` on the users table. The catalog consciously excludes security-crypto (CONVENTIONS note in libs.versions.toml). A hashing scheme is needed that needs zero new dependencies and runs on minSdk 26.
**Decision:** PBKDF2-HMAC-SHA256, 10,000 iterations, 256-bit key, per-user random salt (stored as hex strings in the salt column). Implemented via `javax.crypto.SecretKeyFactory` + `PBEKeySpec` — part of the Android platform since API 1, no dependency. The `pinHash` column stores the derived key as hex.
**Alternatives considered:** BCrypt (rejected: needs a new dependency — the catalog is closed without a DECISIONS entry, and PBKDF2 is sufficient for a local 4-digit PIN); Argon2 (rejected: overkill + dependency); plaintext (forbidden — security).
**Supersedes:** —

---

## D9 — Data-meter P1 foundation: local accumulator + Wi-Fi-only toggle, no Firestore bytes yet
**Date:** 2026-08-30
**Phase:** 1
**Context:** PROGRESS P1 item 5 calls for "ডেটা-মিটার (OkHttp-নয়; Firestore-বাইট-কাউন্টার + Wi-Fi-only-টগল)". Firebase is P4 — no Firestore SDK is wired this phase, so real byte-counting cannot run yet. The foundation (the toggle + the counter plumbing) can and must land now.
**Decision:** A `DataMeter` domain service in core/domain with an in-memory byte counter (incremented by a `recordBytes(n)` call the cloud layer will call in P4) and a Wi-Fi-only toggle persisted in `cloud_sync_state.wifiOnlySync` (default true, per Blueprint §4 law 7). The toggle is read/written via a repository. No Firebase dependency is added this phase; the counter is a no-op accumulator until P4 wires real Firestore calls.
**Alternatives considered:** Deferring the data-meter entirely to P4 (rejected: PROGRESS P1 item 5 explicitly calls for the foundation now); using OkHttp interceptor (rejected by the item text itself — "OkHttp-নয়").
**Supersedes:** —

---

## D10 — SessionManager auto-lock: timestamp-checked, not a background timer
**Date:** 2026-08-30
**Phase:** 1
**Context:** ARCHITECTURE §6 mandates a 2-minute auto-lock for non-OWNER roles. A background timer (CountDownTimer / Service) drains battery on low-end 3GB devices — violates the device-spec persona.
**Decision:** SessionManager records `lastInteractionAt` (epoch-millis) on each UI touch. A `isLocked()` check compares now − lastInteractionAt > 2 min. The app's foreground check (lifecycle observer) + each screen's `onResume` enforce the lock. No background timer, no battery cost. OWNER role is exempt from auto-lock (Blueprint §7.1 — OWNER is the shop owner, always present).
**Alternatives considered:** CountDownTimer per activity (rejected: battery cost + restarts on rotation); a foreground Service (rejected: overkill, battery, user-visible).
**Supersedes:** —

---

## D11 — cloud_sync_state.wifiOnlySync column (amends CONVENTIONS §3)
**Date:** 2026-08-30
**Phase:** 1
**Context:** CONVENTIONS §3 lists cloud_sync_state columns without a Wi-Fi-only-sync toggle. Blueprint §4 law 7 mandates "Wi-Fi-only সিঙ্ক-টগল ডিফল্ট ON". D9 decided to persist the toggle in cloud_sync_state, but the column does not exist in the schema — a constitution tension.
**Decision:** Add column `wifiOnlySync Boolean DEFAULT true` to cloud_sync_state via ALTER-ADD (allowed by CONVENTIONS §3 rule: "নতুন টেবিল-যোগ বা ALTER-ADD কলাম"). This amends CONVENTIONS §3 for this one column. The schema is v1 (no migration needed yet — the table is created fresh in P1). Default true per Blueprint law 7.
**Alternatives considered:** A separate `settings` table (rejected: a one-row toggle does not justify a new table; cloud_sync_state is already the single-row state table); SharedPreferences (rejected: not Room, breaks offline-first-single-source-of-truth principle).
**Supersedes:** —

---

## D12 — Gradle JVM memory raised for AGP 9 lint engine (640m/512m → 2g/1g)
**Date:** 2026-08-30
**Phase:** 1
**Context:** CI run #3 (PR #3 merge, `586b7a8`) failed in 5m 9s during `:feature:home:lintAnalyzeDebug` — "Unexpected failure during lint analysis". The `gradle.properties` had `-Xmx640m -XX:MaxMetaspaceSize=512m`, set minimally for a low-end build machine. AGP 9's lint engine analyzing a Compose module with the full dependency graph exhausts 512MB Metaspace on the CI runner (ubuntu-latest, 7GB RAM). The sandbox reproduced the Metaspace edge ("Daemon will expire after running out of JVM Metaspace") but did not hard-fail because the sandbox has more physical RAM than the JVM allocation, letting the OS absorb the overflow.
**Decision:** Raise `org.gradle.jvmargs` to `-Xmx2g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8`. 2g heap / 1g Metaspace is the standard floor for AGP 9 + Compose + lint; the CI runner's 7GB RAM handles it comfortably. Keep `workers.max=1` and `parallel=false` (single-threaded, low-end-friendly). No lint checks are disabled — this is a memory fix, not a check-weakening.
**Alternatives considered:** Disabling lint on CI (rejected — BUILD.md §6 mandates `gradlew build` which includes lint; blanket lint-disable violates the constitution); lint task isolation via `lintOptions.checkOnly` (rejected — narrows real checks); reducing the Compose dependency graph (rejected — the deps are required by P1).
**Supersedes:** —

---

---

## D13 — Bengali fuzzy search: normalized-title column + LIKE + BengaliNormalizer
**Date:** 2026-08-29
**Phase:** 2a
**Context:** PROGRESS P2 item 1 calls for "বাংলা-ফাজি-সার্চ সহজ-রূপ: LIKE+নরমালাইজড-কলাম". Bengali spelling variations (diacritics, bindu, hasanta, vowel signs) make exact LIKE matching unreliable. A shopkeeper searching "বাংলা" should find "বাংলা", "বঙ্গ", "বাংলাা" etc. Need a lightweight, offline, Room-compatible approach — no ICU/FTS4 (overkill for a single-tenant phone).
**Decision:** Add a `titleBnNormalized` TEXT column to the `books` table, populated on insert/update by a `BengaliNormalizer` domain service. The normalizer strips vowel signs (া ি ী ু ূ ে ৈ ো ৌ ৃ), chandrabindu/bindu/visarga (ঁ ং ঃ), hasanta (্), and converts Bengali digits to Latin — leaving only base consonant skeleton. Search uses `LIKE '%normalizedQuery%'` on `titleBnNormalized`. The normalizer lives in `core/domain` (pure function, no Android deps, unit-testable). The normalized column is also applied to `khata_customers.nameBn` for customer search — but since `khata_customers` already exists in v1, the column is added via the same migration (ALTER-ADD, per CONVENTIONS §3 rule).
**Alternatives considered:** Room FTS4 full-text search (rejected: adds complexity, separate FTS table, overkill for <10k books on a single phone); SQLite ICU collation (rejected: not available on all Android versions, unreliable); manual Soundex-like phonetic matching (rejected: Bengali phonetics are too complex for a simple Soundex; the normalized-skeleton approach is simpler and "good enough" per the PROGRESS "সহজ-রূপ" qualifier).
**Supersedes:** —

---

## D14 — Khata statement format: plain-text, WhatsApp-shareable, dual digits
**Date:** 2026-08-29
**Phase:** 2a
**Context:** Blueprint §7.4 mandates "খাতা-স্টেটমেন্ট (বাকি হিসাব): শেয়ারেবল স্টেটমেন্ট". D2 banned PNG/Bitmap (OOM risk on 3GB devices). The statement must be shareable via WhatsApp — plain text is the lightest, most reliable format for WhatsApp sharing. The statement is the "দোকানী-কাস্টমার মিলনের মুহূর্ত" — it needs to be clear, show all entries with a running balance, and end with the current total due.
**Decision:** Generate the khata statement as a Unicode plain-text string (Bengali default, dual digits toggle-aware via NumberFormatter). Structure: shop header (tenant name) → customer name + area → date range → entry list (date, type label in Bengali, amount, running balance) → total due line. The statement builder is a pure domain service `KhataStatementBuilder` in `core/domain` (no Android deps, unit-testable). Sharing uses Android's `Intent.ACTION_SEND` with `text/plain` — no PDF/PNG this phase (PDF is P3 accounting-pack scope).
**Alternatives considered:** Lightweight PDF (rejected for P2a — the receipt/PDF builder module is `shared/receipt` which is P2b scope; text is sufficient for বাকি হিসাব matching); HTML-formatted text (rejected: WhatsApp strips HTML in plain-text share).
**Supersedes:** —

---

## D15 — দেনা-মুন accounting treatment: ADJUSTMENT entry bringing balance to zero
**Date:** 2026-08-29
**Phase:** 2a
**Context:** Blueprint §7.4: "১-ট্যাপ দেনা মুন → bad-debt জার্নাল-এন্ট্রি (হিসাবে দেখা যায়)". The দেনা-মুন (debt forgiveness) operation must be recorded in the khata ledger, not silently delete the due. Since khata_entries is append-only (🔒, no UPDATE/DELETE per CONVENTIONS §3 and Firestore rules), the only way to zero out a balance is to insert a new ADJUSTMENT entry. The full accounting visibility (bad-debt journal entry in P&L) is P3 — this phase creates the khata-side entry only.
**Decision:** দেনা-মুন inserts a new `KhataEntryEntity` with `type = "ADJUSTMENT"`, `amount = currentDue` (positive — the AgingCalculator treats positive ADJUSTMENT as adding credit, so we need a NEGATIVE adjustment to reduce due; but per Firebase rules, `amount > 0` is enforced — so we store the magnitude as positive and the AgingCalculator must treat ADJUSTMENT with description "দেনা মুন" as a reduction). Wait — re-examining: the AgingCalculator already handles negative ADJUSTMENT (`if (e.amount >= 0) credits.add(...) else allocatePayment(...)`), but Firestore rules require `amount > 0`. The Firestore constraint doc (Firebase-Project-Context §6.4) says negatives are uploaded as magnitude + "Negative Adj: " prefix. So locally, we CAN store negative amounts (Room has no such constraint), and the cloud backup layer (P4) handles the sign-flip. **Final decision:** দেনা-মুন inserts an ADJUSTMENT entry with `amount = -currentDue` (negative, reducing the balance to zero) and `description = "দেনা মুন"`. The local AgingCalculator handles this correctly (treats it as a payment allocation). The P4 backup mapper will apply the "Negative Adj: " prefix + sign-flip when uploading. This phase is offline-only so no Firestore constraint applies yet.
**Alternatives considered:** Storing positive magnitude + special type (rejected: would require AgingCalculator changes and doesn't match the existing ADJUSTMENT semantics); deleting entries (forbidden: append-only); a separate "forgiven" flag on customer (rejected: loses the audit trail — the entry must be visible in the statement).
**Superedes:** —

---

## D16 — Room migration v1→v2: ALTER-ADD titleBnNormalized + nameBnNormalized columns
**Date:** 2026-08-29
**Phase:** 2a
**Context:** D13 adds `titleBnNormalized` to `books` and `nameBnNormalized` to `khata_customers`. Both tables exist in v1 — a proper Room Migration is required (CONVENTIONS §3: "মাইগ্রেশন = Room-Migration ক্লাস, টেব্ল-ড্রপ নিষিদ্ধ"). The database version must bump from 1 to 2.
**Decision:** Create `Migration1To2` extending `Migration(1, 2)` with two `ALTER TABLE ... ADD COLUMN` statements: `books.titleBnNormalized TEXT NOT NULL DEFAULT ''` and `khata_customers.nameBnNormalized TEXT NOT NULL DEFAULT ''`. Register it in `DatabaseModule.provideDatabase` via `.addMigrations(Migration1To2)`. Bump `@Database(version = 2)`. Existing rows get empty strings (normalized columns populate on next add/edit). The entities get the new nullable-with-default fields.
**Alternatives considered:** `fallbackToDestructiveMigration` (forbidden: drops user data — violates the "ফোন হারালেও ডেটা থাকে" promise); separate FTS table (rejected in D13).
**Supersedes:** —

---

## D17 — KhataInstallmentDao: new DAO for the existing khata_installments table
**Date:** 2026-08-29
**Phase:** 2a
**Context:** The `khata_installments` table and `KhataInstallmentEntity` were defined in v1 (CONVENTIONS §3, registered in `@Database`), but no DAO interface or `abstract fun` existed in `BoiKhataDatabase` — P1 only created the schema, not the data access. A previous agent session reportedly hit a KSP/Hilt error trying to add this DAO; root cause was never confirmed. This phase needs installment tracking (PROGRESS P2 item 4: "কিস্তি") so the DAO must be added.
**Decision:** Add `KhataInstallmentDao` interface with `@Insert`, `@Query` for get-by-customer, get-by-entry, and mark-paid (UPDATE — khata_installments is NOT 🔒 append-only per CONVENTIONS §3, so UPDATE is allowed on `isPaid` only). Add `abstract fun khataInstallmentDao(): KhataInstallmentDao` to `BoiKhataDatabase`. Provide in `DatabaseModule`. The key KSP risk avoidance: ensure the DAO interface is in the same package as other DAOs, properly imported in the database class, and the abstract function is declared before the companion object. No new table — no migration needed for this (table already exists in v1/v2).
**Alternatives considered:** Treating installments as khata_entries (rejected: installments have different columns — dueDate, isPaid — and are planned-future entries, not actual transactions); deferring installments to P5 (rejected: PROGRESS P2 item 4 explicitly lists "কিস্তি").
**Supersedes:** —

---

## D18 — P2a navigation: bottom nav (Home/Catalog/Khata) via Navigation-Compose
**Date:** 2026-08-29
**Phase:** 2a
**Context:** Blueprint §2 mandates "দৃশ্যমান Bottom Navigation Bar (সর্বোচ্চ ৪ ট্যাব)" — no hamburger menu. P1 wired MainActivity to show HomeScreen directly with no navigation. P2a adds two new feature screens (catalog, khata) that must be accessible. Navigation-Compose is already in the catalog (navigationCompose = "2.8.5") and declared as a dependency in app + feature/home.
**Decision:** Add a `BoiKhataNavHost` in the `app` module with three routes: `home`, `catalog`, `khata`. A `BoiKhataBottomBar` composable with three NavigationBarItems (Home, ক্যাটালগ, খাতা) — Bengali labels from strings.xml. The NavHost + BottomBar live in a `BoiKhataMainScreen` composable in `app`. Feature screens are called via their public composables. The tenantId (`t_1`) is passed as a nav argument. This stays within P2a scope because navigation is the minimal infrastructure needed to access the two P2a feature screens — not a new feature itself. The 4th tab (POS/Sale) is deferred to P2b.
**Alternatives considered:** A single-screen with tabs (rejected: feature screens are complex enough to warrant full-screen routes); top-level tabs (rejected: Blueprint mandates bottom nav); deferring navigation to P2b (rejected: P2a screens would be unreachable).
**Supersedes:** —

---

*(Next entry starts at D19. Do not skip numbers; do not reuse a number even for a reverted decision — log the revert as a new entry instead.)*

---

## D19 — VAT calculation: per-line, category-based (books 0% / stationery 15%)
**Date:** 2026-08-30
**Phase:** 2b
**Context:** Blueprint §7.3 and §8 mandate "ভ্যাট (বই ০% / স্টেশনারি ১৫%)". The `bills` table has `vatAmount` and `bill_lines` has `vatAmount` per line. VAT must be calculated per line based on the book's `category` enum, then summed to the bill-level `vatAmount`. A `VatCalculator` pure domain service handles this.
**Decision:** `VatCalculator` in `core/domain/sale` — a pure function `calculateLineVat(unitPrice: Double, quantity: Int, category: BookCategory): Double`. Books (TEXTBOOK, GENERAL) = 0% VAT. Stationery = 15% VAT. OTHER = 0% (conservative default — stationery is the only explicitly taxed category per Blueprint). Line VAT = unitPrice × quantity × vatRate. Bill VAT = sum of line VATs. Discount is applied AFTER VAT calculation (discount reduces the total, not the per-line VAT — this matches typical BD bookshop practice where discount is a bill-level adjustment, not per-line). The `vatAmount` on the bill is the pre-discount total VAT; the `totalAmount` = subtotal + vatAmount − discountAmount.
**Alternatives considered:** Bill-level VAT with a single blended rate (rejected: loses per-line audit trail, and mixed carts of books + stationery are common); discount applied before VAT (rejected: reduces VAT collected, which is a tax-compliance issue); OTHER = 15% (rejected: too aggressive — books that don't fit TEXTBOOK/GENERAL but aren't stationery shouldn't default to taxed).
**Supersedes:** —

---

## D20 — Bill number format: INV-YYYYMMDD-NNNN (date-prefixed, zero-padded sequence)
**Date:** 2026-08-30
**Phase:** 2b
**Context:** Blueprint §7.3 mandates "বিল-নম্বর-জেনারেটর". The `bills` table has `billNumber` as a String. Bill numbers must be human-readable, sequential, and sortable. A shopkeeper needs to reference bills by number verbally ("বিল নম্বর ১-২-৩"). The generator must be offline-safe (no server round-trip) and handle concurrent sales within the same day (though a single-device single-user shop is the primary persona).
**Decision:** `BillNumberGenerator` in `core/domain/sale` — generates `INV-YYYYMMDD-NNNN` where YYYYMMDD is the current date and NNNN is a zero-padded sequence number that resets daily. The sequence is determined by querying the max existing bill number for that date from Room (`SELECT MAX(billNumber) FROM bills WHERE tenantId = ? AND billNumber LIKE 'INV-YYYYMMDD-%'`) and incrementing. This is a pure function that takes the current max sequence + date and produces the next number. The repository calls Room to get the max, then calls the generator. Example: first bill on 2026-08-30 = `INV-20260830-0001`.
**Alternatives considered:** UUID-based (rejected: not human-readable, shopkeeper can't reference it); global auto-increment (rejected: a single int column would need a migration and loses date context); tenant-prefixed `t_1-INV-001` (rejected: single-tenant app, prefix is redundant noise for the shopkeeper).
**Supersedes:** —

---

## D21 — Receipt format: Unicode plain-text, dual digits, WhatsApp-shareable (D2-compliant)
**Date:** 2026-08-30
**Phase:** 2b
**Context:** D2 bans PNG/Bitmap (OOM risk on 3GB devices). Blueprint §7.3 says "WhatsApp-শেয়ার (টেক্সট/PNG) = প্রাথমিক" — but PNG is banned by D2, so text is the only path. The `shared/receipt` module is the designated home per ARCHITECTURE §2. The receipt must show: shop name, bill number, date, customer (if any), line items (title, qty, unit price, line total), subtotal, discount, VAT, total, paid, due, payment method — in Bengali, with dual digits per NumberFormatter.
**Decision:** `ReceiptBuilder` in `shared/receipt` — a pure function `buildReceiptText(bill, lines, shopName, formatAmount, formatDate): String`. Structure: shop header → bill number + date → customer name (if any) → separator → line items (title × qty @ unitPrice = lineTotal) → separator → subtotal → discount (if any) → VAT (if any) → total → paid → due (if any) → payment method label → footer ("ধন্যবাদ"). Dual digits handled by injected `formatAmount`/`formatDate` lambdas (the ViewModel passes NumberFormatter with the current DigitStyle). Sharing uses `Intent.ACTION_SEND` with `text/plain` — same pattern as the khata statement (D14). No PDF this phase (PDF is P3 accounting-pack scope).
**Alternatives considered:** HTML-formatted text (rejected: WhatsApp strips HTML in plain-text share); lightweight PDF (rejected: `shared/receipt` PDF builder is P3 scope per D14); Compose-rendered image (rejected: D2 bans PNG/Bitmap).
**Supersedes:** —

---

## D22 — Partial payment → auto-khata wiring: CREDIT entry linked via khataEntryId
**Date:** 2026-08-30
**Phase:** 2b
**Context:** Blueprint §7.3 mandates "আংশিক-পেমেন্ট → অটো-খাতা (এক লেনদেনে বিক্রি+বাকি)" — the one-transaction sale+দাক-রসিদ promise. When a customer pays less than the total, the `dueAmount` must automatically become a CREDIT entry on their khata, linked to the bill via `khataEntryId` on the bill and `referenceBillId` on the khata entry. The `bills` table has `customerId?`, `paymentMethod`, `paidAmount`, `dueAmount`, and `khataEntryId?` — all the columns needed. Payment method `CREDIT` means the entire bill is on khata (paidAmount=0, dueAmount=total).
**Decision:** The `SaleRepository.createBill` method handles the entire transaction in one Room `@Transaction`: (1) insert bill entity, (2) insert bill_lines, (3) insert stock_ledger entries (SALE, negative quantity) for each line, (4) if `dueAmount > 0` AND `customerId != null`, insert a `KhataEntryEntity` with `type=CREDIT`, `amount=dueAmount`, `referenceBillId=billId`, `description="বিক্রি বাকি"` and write its ID back to `bill.khataEntryId`. If `paymentMethod == CREDIT`, the entire `totalAmount` goes to khata (paidAmount=0). The customer must exist in khata_customers before the sale (the POS screen offers a customer picker; walk-in customers have `customerId=null` and must pay full amount — no khata without a customer). This is a single atomic Room transaction — either the bill + lines + stock + khata entry all succeed, or none do.
**Alternatives considered:** Two-step (create bill, then manually add khata entry) (rejected: breaks the "one transaction" promise and risks orphaned bills if step 2 fails); negative stock on bill creation (rejected: stock is decremented via stock_ledger append-only entries, not by updating books.initialStock); auto-create customer from walk-in (rejected: khata_customers creation is OWNER-only per CONVENTIONS §4 — the POS screen can't create customers, only select existing ones).
**Supersedes:** —

---

## D23 — Discount type: PERCENTAGE or FIXED (bills.discountType stores which)
**Date:** 2026-08-30
**Phase:** 2b
**Context:** The `bills` table has `discountAmount` (Double) and `discountType` (String). Blueprint §7.3 mentions "ছাড়" but doesn't specify the type. The shopkeeper needs both: percentage discount ("১০% ছাড়") for seasonal promotions, and fixed discount ("৳৫০ ছাড়") for individual negotiations. The UI must offer both modes, and the bill must store which was used for audit trail.
**Decision:** `discountType` stores either `"PERCENTAGE"` or `"FIXED"`. For PERCENTAGE, `discountAmount` stores the calculated discount value (not the percentage rate — the percentage is entered in the UI, calculated to amount, and the amount is stored for financial correctness). The POS screen has a discount input that toggles between percentage and fixed mode. For PERCENTAGE: `discountAmount = subtotal × (percentage / 100)`. For FIXED: `discountAmount = entered amount`. In both cases, discount is capped at `subtotal + vatAmount` (can't discount below zero). The stored `discountAmount` is always the final calculated amount, not the input rate — this ensures the bill's financial records are always correct even if the book prices later change.
**Alternatives considered:** Storing the percentage rate (rejected: if book prices change later, historical bills would recalculate incorrectly); only fixed discount (rejected: BD bookshops commonly use percentage discounts during seasonal promotions); only percentage (rejected: individual negotiation discounts are often fixed amounts).
**Supersedes:** —

---

*(Next entry starts at D24. Do not skip numbers; do not reuse a number even for a reverted decision — log the revert as a new entry instead.)*

---

## D24 — Purchase auto-routing: book purchase → stock_ledger (PURCHASE), non-book → expense
**Date:** 2026-08-30
**Phase:** 3a
**Context:** Blueprint §7.8 mandates "অটো-রুট: বই-ক্রয় → ইনভেন্টরি (COGS), খরচ নয়।" A catalog-item purchase (buying books for the shop) must increase inventory via a stock_ledger entry (reason=PURCHASE, positive quantity), NOT create an expense. Non-book purchases (stationery for office use, tea, etc.) are expenses. This routing rule is a core accounting-correctness guarantee — without it, P&L would show book purchases as expenses, inflating expenses and hiding COGS.
**Decision:** A `PurchaseRouter` pure domain service in `core/domain/accounting` — `fun shouldRouteToInventory(itemType: PurchaseItemType): Boolean`. `PurchaseItemType` is an enum: BOOK_PURCHASE (→ stock_ledger) vs NON_BOOK_PURCHASE (→ expense). The repository layer calls this router to decide: if BOOK_PURCHASE, insert a `StockLedgerEntity` with `reason="PURCHASE"`, `changeQuantity=+quantity`, and do NOT create an expense. If NON_BOOK_PURCHASE, insert an `ExpenseEntity` and do NOT touch stock_ledger. The UI presents this as a toggle: "বই ক্রয়" vs "অন্যান্য খরচ". Both paths also create a `CashbookEntryEntity` (EXPENSE type, matching account) for the money outflow — the cashbook always reflects the payment, regardless of routing.
**Alternatives considered:** Always create expense + stock entry (rejected: double-counts — the expense inflates P&L while the stock entry correctly handles inventory; COGS is recognized at sale time, not purchase time); let the user manually decide (rejected: the whole point is auto-routing — the shopkeeper shouldn't need to understand COGS vs expense); no cashbook entry for book purchases (rejected: the money still left the cash/bKash account — cashbook must reflect it).
**Supersedes:** —

---

## D25 — Cashbook auto-population: every money flow creates a cashbook entry
**Date:** 2026-08-30
**Phase:** 3a
**Context:** Blueprint §7.6/§7.7 mandates cashbook auto-populate from bills, expenses, and khata collections. The `cashbook_entries` table is 🔒 append-only with `account` (CASH/BKASH/BANK), `type` (INCOME/EXPENSE/TRANSFER), `amount`, `referenceId?`, `description`. Every money flow in the app must automatically create a cashbook entry — this is the single-source-of-truth for cash position. Manual entries are also allowed for adjustments the shopkeeper makes outside the app.
**Decision:** Auto-population rules (implemented in the repository layer, not in a separate service — the routing is tightly coupled to each transaction): (1) Bill payment: if `paymentMethod=CASH` → CASH/INCOME; if `BKASH` → BKASH/INCOME; amount = `paidAmount` (not total — only what was actually paid). (2) Expense entry: `account` chosen by user (CASH or BKASH), `type=EXPENSE`, amount = expense amount. (3) Khata collection (PAYMENT entry): `account` chosen by user, `type=INCOME`, amount = payment amount. (4) Owner drawing: `account=CASH` (drawings are typically cash), `type=EXPENSE`, amount = drawing amount. (5) Book purchase: `account` chosen by user, `type=EXPENSE` (money outflow regardless of inventory routing per D24). The `referenceId` on each auto-entry links back to the source entity (bill ID, expense ID, khata entry ID, drawing ID) for audit trail. Auto-population happens in the same Room `@Transaction` as the source operation — atomic.
**Alternatives considered:** A scheduled reconciliation job that scans all tables and creates cashbook entries (rejected: not real-time, risks missing entries if the job fails); a separate CashbookService that other repositories call (rejected: adds a layer of indirection — the repository that creates the bill/expense is the right place to also create the cashbook entry, in the same transaction); no auto-populate, only manual (rejected: Blueprint explicitly mandates auto-populate).
**Supersedes:** —

---

## D26 — ঘরি (staff advance) sub-ledger: expense with special category + per-user balance
**Date:** 2026-08-30
**Phase:** 3a
**Context:** Blueprint §7.8 lists "ঘরি/অ্যাডভান্স" as a BD-specific expense category. ঘরি = money advanced to staff (an asset, not a pure expense — it's expected to be recovered via salary deduction or repayment). The expenses table has `categoryId` — a ঘরি entry is an expense with a special category. But ঘরি needs a per-user sub-ledger balance (how much has each staff member taken as advance, how much has been recovered). The CONVENTIONS §3 schema has no separate ঘরি table — so the sub-ledger is derived from expenses filtered by the ঘরি category.
**Decision:** Seed an `expense_categories` row with `nameBn="ঘরি"` and `icon="advance"`. A ঘরি entry is a regular `ExpenseEntity` with this category. A `GoriBalanceCalculator` pure domain service in `core/domain/accounting` computes per-user balance: SUM of all expenses with ঘরি categoryId for a given userId (advance given) minus SUM of expenses with ঘরি categoryId and `description` containing "ঘরি ফেরত" (advance returned). The UI shows a per-staff ঘরি balance list. Recovery is recorded as a new expense entry with description "ঘরি ফেরত" (which the calculator treats as a reduction). No UPDATE/DELETE — append-only, consistent with the money-table rule. No separate table — derived from expenses, consistent with ARCHITECTURE §4 "Balances = derived."
**Alternatives considered:** A separate `ghori_entries` table (rejected: adds schema complexity for a derived balance; the expenses table already has the columns needed); a TRANSFER cashbook entry for recovery (rejected: CashbookEntryType has no ADJUSTMENT per CONVENTIONS §2, and TRANSFER is account-to-account, not advance-recovery); treating ঘরি as a pure expense with no sub-ledger (rejected: Blueprint specifically calls it a sub-ledger).
**Supersedes:** —

---

## D27 — Recurring expense template: next-due computation + manual trigger
**Date:** 2026-08-30
**Phase:** 3a
**Context:** Blueprint §7.8 mentions "Recurring-টেমপ্লেট + মাসিক বাজেট-অ্যালার্ট". The CONVENTIONS §3 schema has no `recurring_expenses` table — recurring templates must fit within the existing schema. A recurring expense is a template that generates actual expense entries on a schedule. Since there's no WorkManager-based auto-trigger this phase (that's P3b/P3c scope with the accounting engine), the template stores the recurrence pattern and the UI offers a "apply now" button to create the actual expense entry.
**Decision:** Store recurring templates as `expense_categories` rows with `icon="recurring"` — NO, this doesn't work (categories are categories, not templates). Instead: a `RecurringExpense` domain model (not a Room entity — no new table) that the UI uses to present recurring templates. The templates are stored as JSON in a SharedPreferences-like key-value store... NO — that breaks offline-first-single-source-of-truth. **Final decision:** Defer the recurring template persistence to P3b. This phase implements the `RecurringExpenseCalculator` pure domain service (next-due computation from a frequency + last-applied date) + unit tests, but does NOT persist templates yet. The UI shows a "recurring" badge on expenses that were created from a template (via `description` prefix "মাসিক:"), and the calculator is unit-tested and ready for P3b to wire. This is honest scoping: the calculator is the pure-logic piece, the persistence + auto-trigger is P3b.
**Alternatives considered:** A new `recurring_expenses` table (rejected: needs a schema migration this phase, and the auto-trigger (WorkManager) is P3b scope — building the table without the trigger is half a feature); storing templates as expense rows with a `isRecurring` flag (rejected: no such column in CONVENTIONS §3, and adding it needs a migration); JSON in SharedPreferences (rejected: breaks Room-as-truth).
**Supersedes:** —

---

## D28 — Owner drawing (মালিকের তোলা): separate table, OWNER-only, cashbook EXPENSE
**Date:** 2026-08-30
**Phase:** 3a
**Context:** Blueprint §7.8 and CONVENTIONS §3 have a separate `owner_drawings` table (id, tenantId, amount, description, drawingDate, userId, idempotencyKey). Owner drawings are NOT expenses — they're equity withdrawals, a different accounting category. Firestore rules (Firebase-Project-Context §4) confirm: `owner_drawings` is create-only (🔒 append-only), read/write is OWNER-only. The `expenses` table is for business expenses; mixing drawings into expenses would inflate expenses and understate equity.
**Decision:** `OwnerDrawingRepository` in `core/domain` with `createDrawing(tenantId, amount, description, userId)` and `getDrawings(tenantId, startDate, endDate)`. The repository impl creates: (1) `OwnerDrawingEntity` (append-only), (2) `CashbookEntryEntity` with `account=CASH`, `type=EXPENSE`, `amount=drawing amount`, `referenceId=drawing ID` — in one Room `@Transaction` (D25 auto-populate). RBAC: OWNER-only per Firestore rules — the repository's `requireRole(OWNER)` gate. The UI is a simple form: amount + description + date, with an OWNER-only access check.
**Alternatives considered:** Treating drawings as expenses with a "মালিকের তোলা" category (rejected: drawings are equity withdrawals, not expenses — mixing them inflates expenses and corrupts P&L; the separate table exists in the schema for this reason); no cashbook entry for drawings (rejected: money left the cash account — cashbook must reflect it per D25); allowing MANAGER to create drawings (rejected: Firestore rules say OWNER-only).
**Supersedes:** —

---

## D29 — COGS-split P&L: consignment-commission vs purchase-COGS (core accounting-correctness rule)
**Date:** 2026-09-01
**Phase:** 3b
**Context:** Blueprint §7.7 mandates the consignment-vs-purchase COGS split as "the core accounting-correctness rule of this product" — "কনসাইনমেন্ট বনাম ক্রয়-COGS স্প্লিট (কনসাইনমেন্ট = খরচ নয়, কমিশন; ক্রয় = বিক্রির মুহূর্তে COGS) — এই স্প্লিট ছাড়া P&L ভুল।" A bookshop acquires inventory two ways: (1) PURCHASE — books bought outright (the cost is COGS recognized at sale time, per D24 which routes book purchases to stock_ledger not expense); (2) CONSIGNMENT — books taken on consignment from a publisher/supplier, where the shopkeeper does NOT own the book and pays the publisher only after the book sells (the shopkeeper's cost is a commission/fee, not the book's full cost). Treating both as the same COGS would either understate costs (consignment books appear free) or overstate costs (consignment books show full purchase price when only a commission is owed). The P&L must split these.
**Decision:** A `PnLCalculator` pure domain service in `core/domain/accounting`. It takes a period's bills (revenue), bill_lines (to compute per-line COGS), stock_ledger PURCHASE entries (purchase-COGS), and a consignment-settlement amount (commission paid to publishers for consignment books sold in the period). COGS is split into two lines: `cogsPurchase` (sum of purchase-price × quantity-sold for books acquired by PURCHASE) and `cogsConsignment` (the commission/fee owed to publishers for consignment books sold). Gross profit = revenue − cogsPurchase − cogsConsignment. The split is surfaced as separate P&L lines so a bank/microfinance loan officer can see the shop's true margin. The consignment-settlement amount is an input (the actual commission figures come from the supplier/consignment module which is P5 scope — for P3b the calculator accepts the number as a parameter and the repository passes 0.0 until P5 lands). Purchase-COGS is computed from bill_lines joined to books: for each line, `unitPrice` is the selling price; the COGS is `book.purchasePrice × line.quantity` (the shop's acquisition cost for that book). This requires knowing each book's `purchasePrice` (already on the books table per CONVENTIONS §3). Books acquired by consignment have `purchasePrice = 0` (the shop doesn't pay upfront) — their COGS line is zero, and the commission shows on the consignment line. This keeps the split clean without a per-book "acquisition-type" column (no schema change needed this phase).
**Alternatives considered:** A single COGS line with no split (rejected: Blueprint explicitly mandates the split as the core correctness rule); a per-book `acquisitionType` column on the books table (rejected: needs a migration and the consignment module is P5 — using purchasePrice=0 as the consignment signal is sufficient for P3b and honest about scope); computing COGS at purchase time instead of sale time (rejected: D24 already routes purchases to inventory not expense — COGS is recognized at sale time, matching the Blueprint's "ক্রয় = বিক্রির মুহূর্তে COGS").
**Supersedes:** —

---

## D30 — Dual-calendar rollup: Gregorian month + Bengali fiscal year (১ এপ্রিল–৩১ মার্চ)
**Date:** 2026-09-01
**Phase:** 3b
**Context:** Blueprint §6 + §7.7 mandate "সব মাসিক/বার্ষিক রিপোর্ট দ্বৈত: গ্রেগরিয়ান + বাংলা বর্ষ (১ এপ্রিল–৩১ মার্চ)" and "বাংলা বর্ষে ফিসক্যাল-ক্লোজ।" The Bengali fiscal year runs 1 Boishakh–30 Choitro = April 1–March 31 in Gregorian terms. Monthly P&L must be presentable both by Gregorian month (Jan, Feb, …) and by Bengali fiscal-year month (Boishakh=April, Joishtho=May, …). The rollup is a date-mapping concern, not a calculation concern — the P&L numbers are the same; only the period boundaries and labels change.
**Decision:** A `BengaliFiscalCalendar` pure domain service in `core/domain/accounting`. It maps a Gregorian date (epoch-millis) to its Bengali fiscal-year and Bengali-month. The Bengali fiscal year = the Gregorian year of the April that begins it (e.g. a date in Jan 2027 belongs to FY 2026-27 which started April 1 2026; a date in May 2026 belongs to FY 2026-27 too). Bengali month index 1-12 maps to Gregorian April-March. The service provides: `toBengaliFiscalYear(date)` → the FY label (e.g. "১৩৩৩" or the Gregorian-start-year); `toBengaliMonth(date)` → month 1-12; `bengaliMonthName(month)` → Bengali month name; `gregorianMonthRange(year, month)` → start/end epoch-millis for a Gregorian month; `bengaliFiscalYearRange(fyStartYear)` → start (April 1) / end (March 31 next year) epoch-millis. The P&L report uses these to label and bound periods. The monthly P&L is computed for a Gregorian month (the natural accounting period) and the report header shows both the Gregorian month name and the corresponding Bengali month + fiscal year. The annual rollup uses the Bengali fiscal-year range. Month names: Boishakh, Joishtho, Ashar, Shrabon, Bhadro, Ashwin, Kartik, Ogrohayon, Poush, Magh, Falgun, Choitro.
**Alternatives considered:** Store a Bengali date column on every transaction (rejected: redundant — the Gregorian date is the source of truth and the mapping is deterministic); compute Bengali dates only in the PDF (rejected: the rollup is a reusable domain concern, not a presentation concern — putting it in the PDF generator couples rendering to accounting logic); use the solar Bengali calendar with its variable month lengths (rejected: the Blueprint defines the fiscal year in Gregorian terms "১ এপ্রিল–৩১ মার্চ" — a fixed April-March mapping, not the astronomical Bengali calendar; this is a fiscal calendar, not a panchang).
**Supersedes:** —

---

## D31 — Balance-sheet lite: component list per Blueprint (assets, liabilities, equity)
**Date:** 2026-09-01
**Phase:** 3b
**Context:** Blueprint §7.7 mandates "ব্যালেন্স-শিট-লাইট" as part of the হিসাব-প্যাক. This is a lite balance sheet (not full double-entry), suitable for a bank/microfinance loan file. The components are derived from the existing data: cash position (cashbook balances across CASH/BKASH/BANK), inventory value (stock on hand × purchasePrice), khata receivables (customer due balances), ঘরি advances (staff advance sub-ledger per D26), less: supplier payables (denā — P5 scope, 0.0 for P3b), owner's equity (accumulated retained earnings − owner drawings).
**Decision:** A `BalanceSheetCalculator` pure domain service in `core/domain/accounting`. It takes the period-end snapshot: cashbook balances, inventory valuation (sum of book.purchasePrice × current stock quantity, where stock quantity = sum of stock_ledger changeQuantity per book), khata receivables (sum of customer due balances via AgingCalculator), ঘরি net advances (GoriBalanceCalculator), supplier payables (0.0 — P5), owner drawings (sum of owner_drawings), and accumulated P&L (sum of all prior months' net profit). Output: `BalanceSheetLite(assets, liabilities, equity)` where Assets = cash + inventory + receivables + ghori; Liabilities = supplierPayables (0 for now); Equity = retainedEarnings − drawings. The accounting identity Assets = Liabilities + Equity is asserted in tests. This is a point-in-time snapshot, not a period — it takes a `asOfDate`.
**Alternatives considered:** Full double-entry balance sheet with chart of accounts (rejected: Blueprint says "lite" and the app is single-entry UI / double-entry internal — a full GL is out of scope); no balance sheet, only P&L (rejected: Blueprint mandates it for the loan-file-ready হিসাব-প্যাক); store balances as columns (rejected: ARCHITECTURE §4 "Balances = derived" — all components are computed from append-only ledgers).
**Supersedes:** —

---

## D32 — Period-lock: closed month immutable; owner-approved adjustment entries only; never-lock on read/export
**Date:** 2026-09-01
**Phase:** 3b
**Context:** Blueprint §7.7 mandates "পিরিয়ড-লক: বন্ধ মাস অপরিবর্তনীয় (মালিক-অনুমোদিত অ্যাডজাস্টমেন্ট ছাড়া)।" A closed month's transactions become immutable — no edits, no deletes. The only way to correct a locked period is an owner-approved adjustment entry (a new append-only entry in the next open period, never a mutation of the locked one). The never-lock rule (Three Laws §3) mandates that read/report/export stays open on locked periods — locking blocks writes, not reads. This is distinct from the license soft-lock (which gates all writes by license state); period-lock is an accounting-integrity gate that gates writes by period state.
**Decision:** A new `period_locks` table (id PK, tenantId, periodYear, periodMonth, lockedAt, lockedByUserId) added via Migration v2→v3 (ALTER-ADD new table — no drops). A `PeriodLockDao` and `PeriodLockRepository` in core/domain. The repository: `isLocked(tenantId, year, month)`, `lockPeriod(tenantId, year, month, userId)` (OWNER-only per role matrix), `getLockedPeriods(tenantId)`. A `PeriodLockGuard` (parallel to LicenseWriteGuard) is injected into write repositories; before any money-table insert (bills, expenses, khata_entries, cashbook_entries, owner_drawings, stock_ledger), it checks whether the entry's date falls in a locked period — if locked, throws `PeriodLockedException`. Read/export paths do NOT consult the guard (never-lock). Adjustment entries are written in the NEXT open period (the entry date is the current date, with a description referencing the corrected period) — the locked period is never mutated. The guard checks the entry's `date`/`expenseDate`/`billDate` field against locked periods. The UI shows a lock icon on closed months and an OWNER-only "তালা দিন" (lock) button.
**Alternatives considered:** A boolean `isLocked` column on each money table (rejected: requires migrating every money table and violates append-only — a lock flag is metadata, not a transaction); lock by preventing the UI from opening the period (rejected: role enforcement is at the data layer per ARCHITECTURE §6, not UI-only); allow edits to locked periods with an "adjusted" flag (rejected: violates immutability — the Blueprint says অপরিবর্তনীয়/immutable).
**Superedes:** —

---

## D33 — হিসাব-প্যাক PDF: monthly report set, bank/microfinance-loan-file ready
**Date:** 2026-09-01
**Phase:** 3b
**Context:** Blueprint §7.7 mandates "মাসিক P&L + ব্যালেন্স-শিট-লাইট + খাতা-aging + ভ্যাট-সামারি = মাসিক হিসাব-প্যাক PDF (A4) — ব্যাংক/মাইক্রোফাইন্যান্স লোন-ফাইল-রেডি।" This is a single PDF containing the monthly P&L (with COGS split), balance-sheet lite, khata aging summary, and VAT summary. It must be A4, Bengali-first (dual digits), and formatted for a loan officer to read. ARCHITECTURE §4 forbids PNG/Bitmap (OOM risk) — the PDF is generated from text/structured layout, not images. The app must embed the Noto Sans Bengali font (already bundled per ARCHITECTURE §1) so Bengali glyphs render in the PDF.
**Decision:** A `HisabPackGenerator` in `core/domain/accounting` that builds a structured `HisabPack` data model (P&L + balance sheet + aging summary + VAT summary sections), and a PDF renderer in `shared/receipt` (the existing PDF/text builder module) that converts the model to an A4 PDF using Android's `PdfDocument` API with the bundled Noto Sans Bengali font painted via `Canvas`/`Paint`. The generator is a pure-data builder (no Android dependency) so it is unit-testable; the PDF rendering is an Android concern (tested via Robolectric or manual). The data model carries both Bengali and Gregorian labels (dual-calendar per D30). The PDF sections: (1) header with shop name + period (Gregorian month + Bengali month/FY), (2) P&L with the COGS split lines, (3) Balance sheet lite, (4) Khata aging summary (total due + bucket breakdown), (5) VAT summary (books 0% / stationery 15%). All money figures use NumberFormatter (dual digits). The PDF is saved to app storage and shared via Intent.ACTION_SEND (WhatsApp/email) like the receipt.
**Alternatives considered:** Use a third-party PDF library (rejected: BUILD.md §7 requires a DECISIONS entry for new dependencies, and Android's built-in PdfDocument suffices); generate HTML→PDF via WebView (rejected: heavy and unreliable offline); generate the PDF entirely in the repository layer (rejected: the data model is a pure domain concern, the rendering is an Android concern — separating them keeps the logic unit-testable).
**Supersedes:** —

---

## D34 — Cashbook auto-populate from bill payments + khata collections (completes D25)
**Date:** 2026-09-01
**Phase:** 3b
**Context:** D25 defined the auto-population rules for all money flows. P3a wired auto-populate for expenses, book purchases, and owner drawings. The P3a DEFERRED note explicitly states "bill/khata auto-populate = P3b (requires SaleRepositoryImpl + KhataRepositoryImpl updates, not in P3a scope)." This session completes D25: (1) Bill payment → cashbook INCOME entry (account from payment method: CASH→CASH, BKASH→BKASH; amount = paidAmount). (2) Khata collection (PAYMENT entry) → cashbook INCOME entry (account chosen by user, amount = payment amount). Both happen in the same Room @Transaction as the source operation (atomic, per D25).
**Decision:** `SaleRepositoryImpl.createBill` inserts a `CashbookEntryEntity` (INCOME, account from paymentMethod, amount = actualPaid, referenceId = billId) inside the existing `db.withTransaction` block — after the bill/lines/stock/khata inserts, before the return. The cashbook entry is only created when `actualPaid > 0` (a pure-credit bill with paidAmount=0 creates no cashbook entry — no money moved). `KhataRepositoryImpl.addEntry` gains a `cashbookAccount` parameter (the account the customer paid into); when `type == PAYMENT` and `amount > 0`, it inserts a `CashbookEntryEntity` (INCOME, that account, amount, referenceId = khataEntryId) in the same transaction. The `KhataRepository` interface gains the `cashbookAccount` parameter on `addEntry` (for PAYMENT type; ignored for CREDIT/ADJUSTMENT/OPENING). CREDIT entries (the sale-on-credit that creates the receivable) do NOT create a cashbook entry — the cashbook entry is created at bill-payment time in SaleRepositoryImpl, and the khata PAYMENT (collection of that receivable) creates its own cashbook entry when the customer later pays. This avoids double-counting: the bill's paidAmount creates the first cashbook entry; a later khata PAYMENT for the same bill's due creates the second.
**Alternatives considered:** Create the cashbook entry in a separate service after the transaction (rejected: not atomic — a crash between the bill and the cashbook entry corrupts the cash position); create a cashbook entry for CREDIT khata entries too (rejected: a CREDIT entry is the creation of a receivable, not a cash flow — no money moved; the cashbook entry belongs to the payment, not the credit); pass the cashbook account via a thread-local (rejected: explicit parameter is clearer and testable).
**Supersedes:** D25 (completes the bill/khata auto-populate that D25 deferred to P3b).

---

## D35 — Recurring-expense persistence + due-reminder + monthly budget alert
**Date:** 2026-09-01
**Phase:** 3b
**Context:** D27 deferred recurring-expense persistence + auto-trigger to P3b ("the calculator is the pure-logic piece, the persistence + auto-trigger is P3b scope"). Blueprint §7.8 mandates "Recurring-টেমপ্লেট + মাসিক বাজেট-অ্যালার্ট।" The calculator (next-due logic) exists and is tested; persistence and triggering do not. The budget alert depends on recurring (a monthly budget is compared against actual expenses, and the alert fires when actual exceeds budget — recurring expenses are part of actuals).
**Decision:** (1) Recurring persistence: a new `recurring_expenses` table (id PK, tenantId, categoryId, amount, description, frequency, lastAppliedDate, nextDueDate, isActive, userId, createdAt) added via Migration v2→v3 (same migration as period_locks — both are new tables, no drops). A `RecurringExpenseDao` + `RecurringExpenseRepository`. The `frequency` column stores the `RecurringExpenseCalculator.Frequency` enum name. `lastAppliedDate` and `nextDueDate` are epoch-millis. The repository: `getTemplates(tenantId)`, `addTemplate(...)`, `applyTemplate(id, userId, cashbookAccount)` — which creates an actual `ExpenseEntity` via ExpenseRepository.addExpense and updates `lastAppliedDate`/`nextDueDate` on the template (atomic). (2) Due-reminder: a `RecurringExpenseReminder` pure service that takes the templates + `now` and returns the list of due templates (using `RecurringExpenseCalculator.isDue`). The UI shows a due-reminder badge; applying is manual (the shopkeeper taps "apply" — no WorkManager auto-trigger this phase, consistent with D27's scoping; auto-trigger via WorkManager is a future enhancement, not P3b). (3) Monthly budget alert: a `BudgetAlertCalculator` pure service. A `budgets` table (id PK, tenantId, categoryId, monthlyLimit, isActive) via the same migration. The calculator takes a month's actual expenses (by category) + budgets and returns alerts where `actual >= limit * threshold` (threshold default 1.0 = at-limit; a 0.8 threshold warns at 80%). The alert returns `(category, budget, actual, percentage, severity)`. The UI shows a banner per over-budget category. All three are pure-logic + persistence, unit-tested.
**Alternatives considered:** WorkManager auto-trigger for recurring expenses (rejected: D27 explicitly scoped auto-trigger as future; the Blueprint's "auto-trigger" is the reminder, not a silent write — the shopkeeper should approve each recurring expense application); a single `budgets` JSON blob (rejected: breaks Room-as-truth and queryability); compute budget alerts in the ViewModel (rejected: the threshold comparison is pure logic — belongs in a unit-tested domain service).
**Supersedes:** —

---

*(Next entry starts at D36. Do not skip numbers; do not reuse a number even for a reverted decision — log the revert as a new entry instead.)*
