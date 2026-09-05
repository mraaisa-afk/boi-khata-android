# AGENT_GUARDRAILS.md — Boi-Khata Coder Hard Constraints

> **These are absolute rules. No exceptions. No "but this case is different".**
> Violating any guardrail means an immediate STOP plus an alert to Sakira and Builder.
> The same content is mirrored in the agent's Notion instructions page.

---

## Category 1 — Git & Repo Rules

**G1.** NEVER push directly to `main`. Always use a branch named `agent/phase-<N>-<slug>`, even for a one-line fix.

**G2.** NEVER merge your own PR. Sakira merges. Always.

**G3.** NEVER create a branch name outside the `agent/phase-<N>-<slug>` format.

**G4.** NEVER commit to a branch belonging to a different phase than the current task.

**G5.** NEVER force-push to any branch. If a rebase is needed, alert Sakira first.

**G6.** NEVER include secrets, API keys, `.env` content, or `google-services.json` in a commit.

---

## Category 2 — Gate File Rules

**G7.** NEVER modify `PROGRESS.md`. It is the sole gate owner (D66). Read-only for the agent.

**G8.** NEVER modify `DECISIONS.md` to add, edit, or delete a D-entry. D-entries are assigned by Builder and Sakira only.

**G9.** NEVER invent a D-decision number. Propose the decision and wait for Builder to assign a number.

**G10.** NEVER start a new phase before the previous phase exit-gate is ticked in `PROGRESS.md`.

---

## Category 3 — Dependencies & Architecture

**G11.** NEVER add a new Gradle dependency without a D-decision number. STOP, propose, wait.

**G12.** NEVER create a new Gradle module without a D-decision. The module list in `settings.gradle.kts` is frozen.

**G13.** NEVER change `gradle/libs.versions.toml` without a D-decision.

**G14.** NEVER use a library that is not in the `DEPENDENCY_MANIFEST.md` approved list, even if it is "just a utility".

---

## Category 4 — Room & Database Rules

**G15.** NEVER modify a Room schema without writing the corresponding `Migration<N>To<N+1>` class in the same commit.

**G16.** NEVER touch `TenantRebindPlanner.ALL_TENANT_TABLES` without reading `ROOM_MIGRATION_LEDGER.md` first and updating it in the same PR.

**G17.** NEVER use `fallbackToDestructiveMigration()`. Data loss is never acceptable.

**G18.** NEVER auto-merge during restore when both sides have data. Always show the choice screen to the owner.

---

## Category 5 — Bengali & UI Rules

**G19.** NEVER hardcode a Bengali or English string in Kotlin or Compose. All strings go in `strings.xml`.

**G20.** NEVER invent a Bengali UI term that is not already in `strings.xml` or `CONVENTIONS.md`. STOP, propose, wait.

**G21.** NEVER use a line chart or a pie chart. Bar charts only.

**G22.** NEVER add a fifth bottom navigation tab. The maximum is four.

**G23.** NEVER use a hamburger menu or drawer navigation.

**G24.** NEVER use `NumberFormatter` output as a stored value. It is for display only.

---

## Category 6 — Firebase & Firestore Rules

**G25.** NEVER write Firebase custom claims from the app. Claims are vendor-side only. The app reads claims from the ID token.

**G26.** NEVER fabricate a Firestore document structure that is not documented in `Firebase-Project-Context.md`.

**G27.** NEVER bypass `LicenseWriteGuard` for any write operation.

**G28.** NEVER mark a Firestore subscription payment as anything other than `PENDING`. Status updates are vendor-side only.

---

## Category 7 — Agent Behaviour Rules

**G29.** NEVER skip the pre-session read, even for a small task.

**G30.** NEVER proceed from memory if a required file is unavailable via the GitHub tool. STOP and say which file is missing.

**G31.** NEVER ignore a pending owner ruling that touches the current task. STOP, list it, wait.

**G32.** NEVER skip writing a unit test for new logic. The test belongs in the same commit.

**G33.** NEVER paraphrase an error message when reporting. Share the full stack trace.

**G34.** NEVER work on two phases simultaneously. One phase, one branch, one PR at a time.

**G35.** NEVER use the `String` type for money amounts. Use the domain money types.

---

## What You CAN Do Without Asking

- Use your own reasoning for how to implement within the given task scope
- Choose between valid implementation patterns
- Write private helper functions within a class
- Refactor within a file for readability, with no behaviour or schema change
- Use any library already in the `DEPENDENCY_MANIFEST.md` approved list

---

*Last updated: 2026-09-05 · Maintained by: Builder + Sakira Suva*
*Mirrored in: the Boi-Khata Coder Notion instructions page*
