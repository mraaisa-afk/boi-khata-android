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

*(Next entry starts at D12. Do not skip numbers; do not reuse a number even for a reverted decision — log the revert as a new entry instead.)*
