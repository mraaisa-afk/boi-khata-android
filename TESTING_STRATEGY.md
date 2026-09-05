# TESTING_STRATEGY.md — Boi-Khata Test Rules

> Every merge to `main` must leave the test suite green and the count non-decreasing.
> Tests are not optional. No test means the task is not done (see `DEFINITION_OF_DONE.md` Gate 2).

---

## Philosophy

- Test **behavior**, not implementation details
- Prefer **pure-function unit tests** over mocked integration tests
- Every Calculator/Builder/Parser class should be fully unit-testable with no Android framework dependency
- Repository classes are tested with in-memory fakes or an in-memory Room DB
- Do not test trivial getters and setters — test logic, edge cases, and business rules

---

## Unit Tests

### What to test

- All Calculator classes: every formula, every edge case, FIFO logic, rounding
- All Builder/Parser classes: every output shape, every error path
- All Repository classes: happy path plus at least one error path per method
- All Guard classes (`LicenseWriteGuard`, `PeriodLockGuard`): every guard condition
- State machines (`ClaimsSession`): every transition and invalid transition

### Where tests live

- Module-local JVM unit tests: `<module>/src/test/java/...`
- Room-dependent tests: `<module>/src/androidTest/java/...` (Robolectric or in-memory DB)

### Naming convention

```kotlin
fun `subject action expected result`()

// Examples:
fun `AgingCalculator returns zero when no overdue entries`()
fun `PeriodLockGuard throws when period is locked`()
fun `BengaliFiscalCalendar maps April to Baishakh`()
```

### Coverage targets

| Class type | Minimum coverage |
| --- | --- |
| Calculator / Builder / Parser | at least 85% branch coverage |
| Repository | happy path + 1 error path per method |
| Guard / Policy | all guard conditions covered |
| ViewModel | core state transitions covered |
| Worker (WorkManager) | role-gating logic covered |

### Current test counts (from PROGRESS.md)

| Phase | Tests added | Total at phase end |
| --- | --- | --- |
| P0 | baseline | 0 |
| P1 | 44 | 44 |
| P2 | 44 | 88 |
| P3 | 115 | 203 |
| P4 | 93 | 296 |
| P5 | unverified | unverified |

---

## Room / Database Tests

- Use `Room.inMemoryDatabaseBuilder()` for all DAO tests
- Every Migration class must have a migration test using `MigrationTestHelper`
- Test: schema version bump, no data loss on forward migration, correct column mapping
- Any change to `TenantRebindPlanner.ALL_TENANT_TABLES` requires updating the test that asserts the total table count
- `fallbackToDestructiveMigration()` must NEVER appear

---

## Bengali Font Scale Test (mandatory from P1 onward)

**Rule (D67):** the UI must remain legible and non-overlapping at `fontScale = 1.30`.

### How to test

- Emulator: Settings -> Accessibility -> Font size -> Largest
- Or: `adb shell settings put system font_scale 1.30`
- Check every new screen manually at 1.30 scale before opening a PR
- Failure criteria: truncated Bengali text, overlapping elements, clipped buttons

### What to check

- [ ] `TridentHeroCard` — আজকের ক্যাশ / বাজারে বাকি / মহাজনের পাওনা labels not truncated
- [ ] `PrimaryPosButton` — নতুন বিক্রি label fits at 104dp height
- [ ] Bottom nav labels — হোম / খাতা / রিপোর্ট / সেটিংস not clipped
- [ ] Khata customer row — name and area not overlapping
- [ ] POS cart item row — item name and price not overlapping

---

## Compose UI Tests (activate at P6)

> NOT currently active. Activate when P6 scope begins.

- Use `ComposeTestRule` with `ActivityScenarioRule<MainActivity>`
- Test: screen renders without crash, key elements visible, click interactions
- Priority screens: Home, Sale, Khata, Login
- Use `onNodeWithText()` with Bengali string resource references, never hardcoded text

---

## Screenshot Baseline Tests (activate at P7)

> NOT currently active. Activate when P7 pilot-hardening scope begins.

- Tool: Paparazzi or Roborazzi (requires a D-decision before adding)
- Baseline per component, per screen, per dark/light mode
- CI fails if the screenshot diff exceeds the threshold
- D67: screenshot test at fontScale 1.30 is mandatory before GA

---

## CI Rules

- Every PR runs `./gradlew build` (assemble + lint + test)
- CI must be green before merge — no exceptions
- If CI fails, the agent identifies the root cause, fixes it, and re-pushes
- Test timeout: 10 minutes max for the full suite
- Flaky tests must be fixed or removed before merge — never `@Ignore` without an explanation

---

## What NOT to Test

- Trivial getters and setters with no logic
- Data class `equals`/`hashCode`
- Android framework internals
- Firestore round-trips on a real network (use fakes)
- WorkManager scheduling behavior (use `TestListenableWorkerBuilder` for logic only)

---

*Last updated: 2026-09-05 · Maintained by: Builder + Sakira Suva*
*Referenced by: DEFINITION_OF_DONE.md Gate 2, .github/pull_request_template.md*
