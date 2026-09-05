# DEPENDENCY_MANIFEST.md — Boi-Khata Approved Dependencies

> **Rule (G11 / G14):** no new dependency without a D-decision number.
> The agent must STOP and propose a D-decision before adding any library not listed here.
>
> **Source of truth:** `gradle/libs.versions.toml` holds the actual pinned versions.
> Every version in this file was read directly from that catalogue on **2026-09-05**.
> If the two ever disagree, the catalogue wins and this file must be corrected.

---

## How to Add a New Dependency

1. **STOP.** Do not add it yet.
2. Tell Sakira and Builder: "I need library X for task Y because Z."
3. Wait for Builder to assign a D-decision number.
4. Builder adds the D-entry to `DECISIONS.md`.
5. Only then: add it to `libs.versions.toml` and to this file in the same PR.
6. The commit message must include `[D-<number>]`.

---

## Toolchain

| Tool | Pinned version | Notes |
| --- | --- | --- |
| Android Gradle Plugin | `9.1.1` | AGP 9.x has built-in Kotlin support |
| Kotlin | `2.4.0` | 2.4.20 is RC and is forbidden |
| KSP | `2.3.11` | Annotation processing for Room and Hilt |
| compileSdk / targetSdk | `37` | |
| minSdk | `26` | Locked by the Blueprint device spec (Android 8) |
| versionCode / versionName | `8` / `0.8.0` | |

---

## Jetpack Compose

| Library | Version | Notes |
| --- | --- | --- |
| `androidx.compose:compose-bom` | `2026.08.00` | Maps to Compose 1.12 |
| `androidx.compose.ui:ui` | BOM-managed | |
| `androidx.compose.ui:ui-graphics` | BOM-managed | |
| `androidx.compose.ui:ui-tooling` | BOM-managed | Debug only |
| `androidx.compose.ui:ui-tooling-preview` | BOM-managed | |
| `androidx.compose.foundation:foundation` | BOM-managed | |
| `androidx.compose.material3:material3` | BOM-managed | |
| `androidx.compose.material:material-icons-extended` | BOM-managed | |
| `androidx.compose.runtime:runtime` | BOM-managed | |

Bundle `compose-core` groups ui, ui-graphics, ui-tooling-preview, foundation, material3 and runtime.

---

## AndroidX Base

| Library | Version | Notes |
| --- | --- | --- |
| `androidx.core:core-ktx` | `1.15.0` | catalogue marks this VERIFY-pending |
| `androidx.activity:activity-compose` | `1.9.3` | catalogue marks this VERIFY-pending |
| `androidx.lifecycle:lifecycle-runtime-compose` | `2.8.7` | catalogue marks this VERIFY-pending |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.8.7` | catalogue marks this VERIFY-pending |
| `androidx.navigation:navigation-compose` | `2.8.5` | catalogue marks this VERIFY-pending |
| `androidx.biometric:biometric` | `1.1.0` | |

---

## Dependency Injection (Hilt)

| Library | Version | Notes |
| --- | --- | --- |
| `com.google.dagger:hilt-android` | `2.60.1` | Drop both to 2.59.2 if the compiler artifact is missing |
| `com.google.dagger:hilt-android-compiler` | `2.60.1` | |
| `androidx.hilt:hilt-work` | `1.2.0` | VERIFY-pending |
| `androidx.hilt:hilt-compiler` | `1.2.0` | VERIFY-pending |
| `androidx.hilt:hilt-navigation-compose` | `1.2.0` | VERIFY-pending |

---

## Room (Local Database)

| Library | Version | Scope | Notes |
| --- | --- | --- | --- |
| `androidx.room:room-runtime` | `2.8.4` | `:core:database` | Room 3.x is alpha and forbidden |
| `androidx.room:room-ktx` | `2.8.4` | `:core:database` | |
| `androidx.room:room-compiler` | `2.8.4` | `:core:database` (KSP) | |
| `androidx.room:room-testing` | `2.8.4` | `androidTest` | Migration testing |

---

## WorkManager

| Library | Version | Notes |
| --- | --- | --- |
| `androidx.work:work-runtime-ktx` | `2.11.1` | DailyBackupWorker |
| `androidx.work:work-testing` | `2.11.1` | `androidTest` |

---

## Firebase (Spark stack: Auth + Firestore)

| Library | Version | Notes |
| --- | --- | --- |
| `com.google.firebase:firebase-bom` | `33.10.0` | VERIFY-pending placeholder in the catalogue |
| `com.google.firebase:firebase-auth` | BOM-managed | Phone OTP |
| `com.google.firebase:firebase-firestore` | BOM-managed | Backup, sync, license, catalog |
| `com.google.gms:google-services` (plugin) | `4.4.2` | VERIFY-pending |

> Note: `firebase-functions` is **not** in the catalogue. Any earlier reference to
> `firebase-functions-ktx` under D42 does not match the current build.

---

## Serialization & Coroutines

| Library | Version | Notes |
| --- | --- | --- |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | `1.9.0` | VERIFY-pending |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.10.2` | VERIFY-pending |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | `1.10.2` | |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | `1.10.2` | `testImplementation` |

---

## Testing

| Library | Version | Scope | Notes |
| --- | --- | --- | --- |
| `junit:junit` | `4.13.2` | `testImplementation` | JUnit **4** — locked by D68 |
| `androidx.test.ext:junit` | `1.2.1` | `androidTest` | VERIFY-pending |
| `androidx.test:runner` | `1.6.2` | `androidTest` | VERIFY-pending |
| `org.robolectric:robolectric` | `4.14.1` | `testImplementation` | VERIFY-pending |
| `io.mockk:mockk` | `1.13.13` | `testImplementation` | VERIFY-pending |
| `app.cash.turbine:turbine` | `1.2.0` | `testImplementation` | Flow testing |
| `com.google.truth:truth` | `1.4.4` | `testImplementation` | VERIFY-pending |
| `androidx.compose.ui:ui-test-junit4` | BOM-managed | `androidTest` | P6+ scope |
| `androidx.compose.ui:ui-test-manifest` | BOM-managed | `androidTest` | |

### D68 — Test framework locked to JUnit 4 (amends D7)

**Decision:** the test framework for this project is **JUnit `4.13.2`**. JUnit 5
(`junit-jupiter`) is rejected. This amends D7, which had recorded JUnit 5 but was never
implemented — `libs.versions.toml` has only ever contained JUnit 4.

**Rationale:**

1. Room's `MigrationTestHelper` is a JUnit 4 `TestRule`. There is no official JUnit 5
   equivalent, so the migration tests required by G15/G16 could not be written at all.
2. Compose UI testing ships as `androidx.compose.ui:ui-test-junit4`, and
   `createAndroidComposeRule` is a JUnit 4 rule. P6 and P7 UI tests depend on it.
3. `RobolectricTestRunner` uses `@RunWith`, and `HiltAndroidRule` uses `@Rule` — both
   are JUnit 4 constructs.
4. `AndroidJUnitRunner` is JUnit 4-based. Instrumented JUnit 5 requires an unofficial
   third-party Gradle plugin whose device-test support is still experimental.
5. 296 tests already pass on JUnit 4. Migrating delivers no user-facing value, and the
   P5 build has not yet been verified — changing frameworks now is unpriced risk.
6. What JUnit 5 offers is already covered: Truth `1.4.4` gives readable assertions,
   `assertFailsWith` covers `assertThrows`, and JUnit 4's `Parameterized` runner covers
   parameterized tests. MockK, Turbine and coroutines-test behave identically on both.
7. Mixing two frameworks produces the worst possible failure mode: a green build with
   silently skipped tests, because a `org.junit.jupiter.api.Test` annotation is simply
   ignored by the JUnit 4 runner.

**Consequence for the agent:** the only correct test import is `org.junit.Test`. Never
import anything under `org.junit.jupiter`. Reinstating JUnit 5 later requires a fresh
D-entry and an owner ruling.

---

## Typography

| Item | Notes |
| --- | --- |
| Noto Sans Bengali | Bundled font asset, not a Gradle dependency. Required for Bengali script rendering. |

---

## Libraries NOT Approved

| Library | Status | Reason |
| --- | --- | --- |
| JUnit 5 / `junit-jupiter` | Rejected | D68 — Room, Compose, Robolectric and Hilt test infrastructure is JUnit 4-only |
| OkHttp family | Rejected | No REST layer — Firestore only |
| Retrofit | Rejected | No REST layer — Firestore only |
| Ktor | Rejected | No REST layer — Firestore only |
| Moshi / Gson | Rejected | kotlinx-serialization is already in use |
| Sentry | Deferred | Crash reporting postponed |
| detekt / ktlint | Deferred | Static analysis comes after CI hardening |
| `androidx.security:security-crypto` | Deferred | Raw Keystore is used instead |
| Glide / Coil | Deferred | No image loading in scope |
| Paparazzi / Roborazzi | Deferred | Screenshot tests are P7 scope |
| Timber | Deferred | Logging not yet in scope |
| DataStore | Deferred | Room handles structured storage |

Bringing any of these back requires a `DECISIONS.md` entry.

---

## Outstanding VERIFY entries in the catalogue

`libs.versions.toml` still carries a VERIFY marker on 15 entries: `androidxHilt`,
`firebaseBom`, `googleServices`, `coreKtx`, `activityCompose`, `lifecycle`,
`navigationCompose`, `kotlinxSerialization`, `kotlinxCoroutines`, `androidxTestExt`,
`androidxTestRunner`, `robolectric`, `mockk`, `turbine`, `truth`.

The catalogue's own comment block says these were to be resolved against live sources
before Phase 0. That has not happened. **Owner action required** — the agent must not
resolve them on its own, because guessing a version is exactly what the marker forbids.

---

*Last updated: 2026-09-05 · Maintained by: Builder + Sakira Suva*
*Verified against gradle/libs.versions.toml at commit ca5fc7f*
*Referenced by: AGENT_GUARDRAILS.md G11/G13/G14, AGENT_PLAYBOOK.md Step 4*
