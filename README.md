# Boi Khata (বই খাতা)

**দোকানের ডিজিটাল খাতা — বইয়ের বিক্রি, ক্রেতার বাকি, সাপ্লায়ারের দেনা ও দৈনন্দিন হিসাব।**

Boi Khata is a Bengali-first, offline-first Android project for bookshops in Bangladesh. It brings sales, stock, customer credit and supplier accounts into one local-first workflow, with particular attention to bookshop and consignment needs rather than generic POS breadth.

[![Android CI — main](https://github.com/mraaisa-afk/boi-khata-android/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/mraaisa-afk/boi-khata-android/actions/workflows/ci.yml?query=branch%3Amain)

> **Under active development.** Code delivery, a green CI run and real-device acceptance are different milestones. Release gates remain open; this README does not certify production readiness. Evaluate with non-sensitive test data, not as the sole record of a live shop's accounts.

This repository contains the **Android client**. Vendor-side activation and renewal tooling is documented separately and is not included here. Cloning this repository does not provision a complete SaaS backend or grant access to the existing Firebase environment.

## Contents

- [Project status](#project-status)
- [Core workflows](#core-workflows)
- [Offline, cloud and data integrity](#offline-cloud-and-data-integrity)
- [Architecture and stack](#architecture-and-stack)
- [Getting started](#getting-started)
- [Testing and CI](#testing-and-ci)
- [Known limitations and roadmap](#known-limitations-and-roadmap)
- [Documentation map](#documentation-map)
- [Contributing and review](#contributing-and-review)
- [License, security and support](#license-security-and-support)

## Project status

Documentation snapshot: **2026-09-06**, reviewed against code baseline [`f065b1c`](https://github.com/mraaisa-afk/boi-khata-android/commit/f065b1cfb82943bdd63e28869fc369daff27b7cf).

- Declared app build metadata: `0.8.0`, version code `8`. This is not a claim that a public release is available.
- No GitHub Release was listed at this snapshot. Consult the [Releases page](https://github.com/mraaisa-afk/boi-khata-android/releases) for any subsequently published releases.
- P1, P5, P6, P7 and P8 exit-gates remain unchecked in [PROGRESS.md](PROGRESS.md). Some later-phase code has already been delivered; unchecked gates must not be confused with an absence of all implementation.
- [PROGRESS.md](PROGRESS.md) is the sole phase-gate authority. [PHASE_PLAN.md](PHASE_PLAN.md) is a navigation aid, not a second completion checklist.
- The badge above reports the **main-branch CI workflow**, not device acceptance, security certification or release approval.

No screenshot or demo recording is presented here as evidence of a verified device flow. Future screenshots should identify the actual build and use sample data; design mockups must be labeled separately.

## Core workflows

The scope below is summarized from the implementation notes in [PROGRESS.md](PROGRESS.md), not a claim that every workflow has passed end-to-end device acceptance.

| Area | Delivered scope recorded in the project | Important boundary |
| --- | --- | --- |
| Catalog and stock | Book catalog, Bengali search and stock-ledger workflows | Master-catalog refresh is network-backed |
| Sales and receipts | Cart, discounts, category-based VAT logic, partial-payment-to-khata flow and text receipts | Share-sheet behavior needs device verification; PNG receipts are not the supported path |
| Customer khata | Credit entries, collections, installments and shareable statements | Dispute-freeze and cohort features are deferred in the progress notes |
| Expenses and accounting | Expense entries, cashbook, cash close, monthly P&L and balance-sheet-lite | Report availability is not a tax-compliance or accounting-certification claim |
| Supplier accounts | Payable ledger, consignment/purchase/payment entries, aging and publisher statements | Statements are plain text; supplier PDF rendering and book-level inventory routing remain deferred |
| Mela mode | Book-fair sessions, stock movements, stock warnings and seasonal pause | Supplier/mela cloud backup is not included in the documented P5 scope |
| Identity and service access | Phone OTP, claims-based activation, license sync, backup/restore and manual payment records | Requires authorized Firebase access and vendor-side provisioning; live integration acceptance remains separate |

Domain terms: **খাতা** = customer credit; **দেনা** = supplier payables; **মেলা** = book-fair operations. Business intent and commercial policy live in the [Master Blueprint](Boi-Khata-Master-Blueprint.md).

## Offline, cloud and data integrity

### Local-first does not mean network-free onboarding

- **Room is the local source of truth.** The design keeps shop operations and derived balances on the device rather than making each transaction depend on a server.
- **Firebase Auth and Firestore** provide identity, license information, scoped backup/restore and the shared master catalog. OTP, activation, cloud refresh and backup/restore require network access.
- Vendor-side tooling supplies the `tenantId` and `role` custom claims. The Android client does not issue those claims or approve its own subscription payments.
- A phone replacement can only recover data included in a successful backup. Offline-first is not a zero-data-loss guarantee.

### Financial and access-control rules

The architecture requires append-only financial ledgers, derived balances, repository-level role checks, license write guards and period locking. The never-lock policy keeps reads, reports and exports available when new writes are restricted.

These are **design and implementation constraints**, not blanket guarantees that every edge case has been proven. Read [ARCHITECTURE.md](ARCHITECTURE.md), [CONVENTIONS.md](CONVENTIONS.md) and the current test results before changing financial behavior.

### Backup boundaries

The documented backup scope covers selected business collections, not the entire Room database. Local audit logs are excluded. Supplier and mela tables remain outside the P5 backup/restore scope (decision D58); do not assume those records survive phone loss through cloud restore.

Consult [Firebase-Project-Context.md](Firebase-Project-Context.md), [CONVENTIONS.md](CONVENTIONS.md) and [DECISIONS.md](DECISIONS.md) for the precise scope and rules.

## Architecture and stack

| Concern | Technology or ownership |
| --- | --- |
| UI | Kotlin, Jetpack Compose and Material 3 |
| Local persistence | Room |
| Dependency injection | Hilt, with KSP processing |
| Background work | WorkManager |
| Cloud | Firebase Phone Auth and Firestore |
| Build | Gradle Kotlin DSL and the checked-in Gradle wrapper |
| JVM tests | JUnit 4; other test dependencies vary by module |
| Language and presentation | Bengali-first resources, bundled Noto Sans Bengali and digit-aware formatting |

```text
app/                   Application shell, navigation and app-lock host
core/domain/           Domain logic, models, enums and repository interfaces
core/database/         Room entities, DAOs, migrations and repository implementations
core/cloud/            Firebase integration and cloud-facing implementations
core/designsystem/     Shared theme, fonts and number formatting
core/common/           Shared utilities
feature/               home, sale, catalog, khata, expense, supplier,
                       reports, subscription, melamode, support
shared/receipt/        Shared receipt and statement builders
```

There are **17 included Gradle modules** in [settings.gradle.kts](settings.gradle.kts). The module dependency rule is `feature/*` → `core/*` and `shared/*`, never direct feature-to-feature imports. A vendor application is future scope, not an included module.

Exact dependency versions belong in [gradle/libs.versions.toml](gradle/libs.versions.toml). Do not duplicate a full version catalog here or interpret its existing `VERIFY` comments as permission to upgrade dependencies.

## Getting started

### Prerequisites

- Git and **JDK 17**; configure Android Studio's Gradle JDK to match if using the IDE.
- An Android SDK installation and command-line tools. The project declares `minSdk 26`, `compileSdk 37` and `targetSdk 37` in the version catalog.
- SDK packages matching the checked-in CI workflow. Its current installation command is:

```sh
sdkmanager --install "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
```

- Network access for the initial Gradle/dependency download. The app's offline-first design does not make an uncached source build offline.
- A compatible emulator or Android device for device-only checks.

Use the repository's wrapper rather than a separately installed Gradle version.

### Local debug build

```sh
git clone https://github.com/mraaisa-afk/boi-khata-android.git
cd boi-khata-android
chmod +x gradlew
./gradlew --version
./gradlew assembleDebug
```

On Windows, omit `chmod` and use `gradlew.bat` (or `.\gradlew.bat` in PowerShell) for the same tasks.

Open the repository root in Android Studio, configure its SDK location and allow Gradle sync to complete. For command-line use, configure a valid SDK location, for example through the ignored `local.properties` file. Keep `.env.example` in place; never commit local credentials or machine-specific configuration.

Expected debug output after a successful build:

```text
app/build/outputs/apk/debug/app-debug.apk
```

These commands are the documented entry points. They are **not a claim that a fresh-clone build was executed during this README update**. Build failures must be reported with their actual logs, not treated as success.

### Firebase configuration and device access

`app/google-services.json` is already tracked, and the app applies the Google Services plugin. There is no per-session requirement to attach that file again. Decision **D68** records this correction; older wording in `BUILD.md` has not yet been reconciled.

A tracked client configuration is not an admin credential and does not grant tenant access. Do not replace the existing configuration, modify live rules or test against real shop records as part of onboarding. Arrange an authorized test tenant and vendor-issued claims with the maintainer first.

Real-device Phone Auth also depends on the signing fingerprint registered in Firebase. A locally signed debug APK can differ from the shared CI signing identity. Coordinate fingerprint registration with the Firebase owner; see [Firebase-Project-Context.md](Firebase-Project-Context.md).

### Local signing, CI signing and release signing

Without `-PdebugKeystore`, the debug variant keeps its normal Android debug-signing configuration. The repository additionally supports a supplied shared debug keystore through `debugKeystore`, `debugKeystorePassword` and `debugKeystoreAlias` Gradle properties.

The current CI workflow decodes a keystore from repository secrets. Forks do not automatically receive those secrets, so a fork's unchanged CI configuration may need maintainer-approved setup. Never copy secret values into this README, a PR or a chat.

The release variant enables R8 and can also use the supplied shared debug key. **A minified or debug-signed APK is not automatically a production distribution artifact.** Production signing and release approval remain maintainer-controlled.

## Testing and CI

Run from the repository root:

```sh
./gradlew test
./gradlew build --stacktrace
```

Tests and build checks are distinct from real-device acceptance. Use [TESTING_STRATEGY.md](TESTING_STRATEGY.md) and [DEFINITION_OF_DONE.md](DEFINITION_OF_DONE.md) to identify the checks required by a change.

The checked-in [Android CI workflow](.github/workflows/ci.yml):

- Runs on pull requests, pushes to `main` and manual dispatch.
- Sets up JDK 17, Gradle and the Android SDK.
- Uses the `DEBUG_KEYSTORE_BASE64` and `DEBUG_KEYSTORE_PASSWORD` repository secrets for shared debug signing.
- Executes `assembleDebug` and then `build --stacktrace` with the configured signing properties.
- Uploads `boi-khata-debug-apk` from the debug APK output path after successful build steps.

An Actions artifact is a development/testing artifact, not a GitHub Release. Use the run's actual conclusion and test reports; do not infer coverage percentages or total passing counts from a badge. An ignored regression test is not a passing test.

Device/Firebase checks include airplane-mode behavior, OTP and activation, backup/restore round trips, WorkManager scheduling, Bengali TTS and Android sharing. Automated build success does not prove these flows.

## Known limitations and roadmap

At the reviewed snapshot:

- P1's airplane-mode demonstration remains a device-verification gate.
- P5's exit-gate checkbox is still open. Some progress notes predate the later CI runs; use the actual run evidence alongside the gate record rather than copying old execution claims.
- Supplier PDF rendering, book-level supplier-to-inventory routing and supplier/mela cloud backup remain deferred in decisions D53, D54 and D58.
- P6/P7/P8 contain delivered code and outstanding device, pilot and activation gates. Implementation notes do not certify completion of those gates.
- Source-code license selection and a documented private security-reporting channel remain maintainer decisions.

Follow [PROGRESS.md](PROGRESS.md) for gates, [PHASE_PLAN.md](PHASE_PLAN.md) for navigation, [DECISIONS.md](DECISIONS.md) for rationale and [CHANGELOG.md](CHANGELOG.md) for recorded changes. The [Master Blueprint](Boi-Khata-Master-Blueprint.md) describes product direction, not a promise that every planned capability has shipped.

## Documentation map

| Need | Start here |
| --- | --- |
| Product intent and commercial policy | [Boi-Khata-Master-Blueprint.md](Boi-Khata-Master-Blueprint.md) |
| Architecture and module boundaries | [ARCHITECTURE.md](ARCHITECTURE.md) |
| Names, enums and data contracts | [CONVENTIONS.md](CONVENTIONS.md) |
| Build mechanics | [BUILD.md](BUILD.md) and [CI workflow](.github/workflows/ci.yml) |
| Firebase integration and vendor prerequisites | [Firebase-Project-Context.md](Firebase-Project-Context.md) |
| Phase gates and progress | [PROGRESS.md](PROGRESS.md) |
| Phase navigation | [PHASE_PLAN.md](PHASE_PLAN.md) |
| Decision history | [DECISIONS.md](DECISIONS.md) |
| Testing and completion criteria | [TESTING_STRATEGY.md](TESTING_STRATEGY.md), [DEFINITION_OF_DONE.md](DEFINITION_OF_DONE.md) |
| Schema migrations | [ROOM_MIGRATION_LEDGER.md](ROOM_MIGRATION_LEDGER.md) |
| Code conventions and approved dependencies | [CODING_STANDARDS.md](CODING_STANDARDS.md), [DEPENDENCY_MANIFEST.md](DEPENDENCY_MANIFEST.md) |
| Contribution workflow | [GIT_WORKFLOW.md](GIT_WORKFLOW.md), [PR template](.github/pull_request_template.md), [CODEOWNERS](.github/CODEOWNERS) |
| Agent workflow and constraints | [AGENT_PLAYBOOK.md](AGENT_PLAYBOOK.md), [AGENT_GUARDRAILS.md](AGENT_GUARDRAILS.md) |
| Known mistakes and blockers | [ERROR_LOG.md](ERROR_LOG.md) |
| Pilot onboarding and recorded changes | [PILOT-ONBOARDING-CHECKLIST.md](PILOT-ONBOARDING-CHECKLIST.md), [CHANGELOG.md](CHANGELOG.md) |

This README is an entry point, not a new source of architectural, commercial or gate policy. Documentation conflicts should be surfaced to the maintainer rather than silently resolved by changing code or checking a phase complete.

## Contributing and review

Read [GIT_WORKFLOW.md](GIT_WORKFLOW.md), the [agent playbook](AGENT_PLAYBOOK.md) where applicable, and the [guardrails](AGENT_GUARDRAILS.md) before making changes.

- Use a fresh `agent/phase-<N>-<slug>` branch for each scoped agent task. Do not push directly to `main` or reuse a merged task branch.
- Keep PRs focused and complete the [PR template](.github/pull_request_template.md) honestly. Unrun checks must remain unclaimed.
- Do not mix dependency, schema, license or phase-gate changes into an unrelated documentation task.
- Obtain the required owner decisions before protected-scope changes. Do not invent decision numbers or bypass financial/access-control guards.
- **Sakira Suva is the sole merge authority.** Wait for green CI and owner review; squash merge is preferred. Delete the task branch after merge.

These are the repository's contribution rules, not a claim that GitHub settings mechanically enforce every rule.

## License, security and support

### Source-code license

No repository-level `LICENSE` file is present at this documentation snapshot. Public visibility is not an open-source license declaration. This README does not select MIT, Apache or another license, or grant new reuse or redistribution rights. Ask the repository owner about intended permissions; third-party dependencies retain their own licenses.

### Security reporting

There is no repository-level `SECURITY.md` policy in this snapshot. Do not publish credentials, signing keys, service-account material, customer records or sensitive vulnerability details in public issues or PRs. Obtain a maintainer-approved private reporting channel before sharing sensitive material.

### Ownership and support

Repository owner: [@mraaisa-afk](https://github.com/mraaisa-afk). Merge authority: **Sakira Suva**.

Product and vendor-support information is documented in the [Master Blueprint](Boi-Khata-Master-Blueprint.md); backend prerequisites are in [Firebase-Project-Context.md](Firebase-Project-Context.md). A source checkout is not a self-service subscription activation or a production support agreement.
