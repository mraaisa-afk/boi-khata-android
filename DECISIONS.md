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

*(Next entry starts at D7. Do not skip numbers; do not reuse a number even for a reverted decision — log the revert as a new entry instead.)*
