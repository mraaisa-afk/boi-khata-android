# GIT_WORKFLOW.md — Boi-Khata Git Rules

> All contributors, human and agent, follow these rules without exception.
> Sakira Suva is the sole merge authority on `main`.

---

## Protected Branches

| Branch | Who can push | Who can merge |
| --- | --- | --- |
| `main` | Nobody directly | Sakira only, via PR |
| `agent/*` | Agent, via tool | Sakira only, via PR |
| `fix/*` | Sakira only | Sakira only |

**Rule:** the agent NEVER pushes to `main` and NEVER merges its own PR.

---

## Branch Naming

```text
agent/phase-<N>-<slug>
```

| Part | Format | Example |
| --- | --- | --- |
| `<N>` | Phase number, 0 to 8 | `1`, `5`, `8` |
| `<slug>` | kebab-case task summary, max 5 words | `license-write-guard`, `khata-installment-ui` |

**Examples:**

- `agent/phase-1-license-write-guard`
- `agent/phase-3-pnl-calculator`
- `agent/phase-5-supplier-aging`

**Rules:**

- One branch per PR. One PR per phase task.
- NEVER reuse a branch for a new task after merge. Create a fresh branch.
- NEVER commit to a branch belonging to a different phase than the current task.

---

## Commit Message Format

```text
<type>(phase<N>): <description> [D-X, D-Y]
```

| Part | Values | Example |
| --- | --- | --- |
| `<type>` | `feat`, `fix`, `test`, `refactor`, `docs`, `chore` | `feat` |
| `phase<N>` | Phase number | `phase1`, `phase5` |
| `<description>` | Imperative mood, max 72 chars | `add LicenseWriteGuard to all write repos` |
| `[D-X, D-Y]` | All D-decisions applied | `[D14, D25]` |

**Full examples:**

```text
feat(phase1): add LicenseWriteGuard to SaleRepository [D14]
feat(phase3): implement PnLCalculator with COGS split [D29]
fix(phase5): correct FIFO aging for consignment entries [D52]
test(phase2): add BillNumberGenerator edge cases [D21]
```

**Rules:**

- NEVER include a D-decision number you did not actually apply.
- NEVER omit D-decision numbers if the task implements a decision.
- If no D-decision applies, omit the bracket. Do not write `[none]`.

---

## PR Rules

### Before opening a PR

- [ ] `DEFINITION_OF_DONE.md` quick self-check passed
- [ ] `./gradlew build` green locally or in CI
- [ ] Branch is up to date with `main` (alert Sakira before rebasing)
- [ ] No secrets, API keys, or `google-services.json` in the diff

### PR title format

```text
[Phase N] <description>
```

Example: `[Phase 3] Implement PnLCalculator with Bengali fiscal year rollup`

### PR description

Use `.github/pull_request_template.md` exactly. Fill every section.

### PR size

- Prefer small, focused PRs — one logical unit per PR
- Max diff around 500 changed lines, excluding generated files
- If larger, split into multiple PRs

---

## Merge Rules

- **Only Sakira merges.** The agent never clicks merge.
- **Merge method:** squash merge preferred for agent PRs
- **After merge:** delete the feature branch
- **NEVER force-merge** over a failing CI run

---

## Conflict Resolution

1. The agent alerts Sakira: "Conflict in `agent/phase-N-slug` on files: [list]. Need rebase."
2. Sakira decides: rebase or resolve manually
3. The agent rebases ONLY after Sakira confirms
4. After rebase, re-run `./gradlew build` to confirm it is still green
5. NEVER force-push without explicit Sakira approval

---

## What to Do if CI Fails

1. Read the full CI log, not just the summary
2. Identify the root cause — the agent diagnoses, not Builder
3. Push a fix commit to the same branch: `fix(phaseN): what was wrong and what was fixed [D-X]`
4. CI re-runs automatically on the new push
5. Report to Sakira: "CI failed due to X. Fixed by Y. New commit: hash."
6. Append to `ERROR_LOG.md` if it was a non-trivial mistake

---

## What the Agent CANNOT Do

- Push to `main`
- Merge a PR
- Force-push without Sakira approval
- Create a branch outside the `agent/*` namespace
- Commit to a branch mid-review without flagging it

---

*Last updated: 2026-09-05 · Maintained by: Builder + Sakira Suva*
*Referenced by: AGENT_PLAYBOOK.md, .github/pull_request_template.md, AGENT_GUARDRAILS.md G1-G6*
