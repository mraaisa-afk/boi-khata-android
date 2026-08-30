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
