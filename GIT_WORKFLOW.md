# GIT_WORKFLOW.md — Boi-Khata Git Rules

> All contributors, human and agent, follow these rules without exception.
> Main merge authority stays owner-only; agents never merge their own PRs.

---

## Exact Stacked-PR Workflow (Owner Rule)

This is the required workflow for implementation work:

1. **One active workstream = one branch = one PR.**
2. A workstream may contain multiple batch commits/pushes on the same branch/PR.
3. Every push to that same branch automatically reruns CI for that PR.
4. If CI fails, fix it with another commit pushed to the same branch/PR.
5. Merge happens only after all batches for that workstream are complete and CI is green.
6. Do **not** create a new branch/PR for a follow-up fix, CI fix, small refinement, or next batch inside the same workstream.
7. Create a separate branch/PR only when the work is truly an independent workstream, a different phase gate, or the owner explicitly approves a split.

**Reason:** this avoids repeated merge cycles. A single PR can safely stack several batches because PR CI reruns on every push.

---

## Protected Branches

| Branch | Who can push | Who can merge |
| --- | --- | --- |
| `main` | Nobody directly | Owner only, via PR |
| `agent/*` | Agent, via tool | Owner only, via PR |
| `fix/*` | Owner only | Owner only |

**Rule:** the agent NEVER pushes to `main` and NEVER merges its own PR.

---

## Branch Naming

```text
agent/<workstream-slug>
```

Preferred examples:

- `agent/p10-design-rebuild`
- `agent/p10-home-formula-fixes`
- `agent/p5-exit-gate`

Legacy `agent/phase-<N>-<slug>` names remain acceptable, but the branch must represent the active workstream, not every tiny batch.

---

## Commit Message Format

```text
<type>(phase<N>): <description> [D-X, D-Y]
```

Rules:

- Use `feat`, `fix`, `test`, `refactor`, `docs`, or `chore`.
- Include every D-decision actually applied.
- Never include a D-decision number not actually applied.
- If no D-decision applies, omit the bracket. Do not write `[none]`.

Examples:

```text
feat(phase10): add HomeScreen analytics sheet [D79]
fix(phase10): align net-profit formula with paidAmount [D81]
test(phase7): add offline chaos size gate [D80]
```

---

## PR Rules

### Before opening or updating a PR

- [ ] `DEFINITION_OF_DONE.md` quick self-check passed where applicable
- [ ] Local build/test run, or clear note that CI is the verification path
- [ ] Branch is up to date with `main` or owner has accepted the current base
- [ ] No secrets, API keys, or `google-services.json` accidentally introduced

### PR title format

```text
[Phase N] <workstream description> [D-X]
```

### PR description

Use `.github/pull_request_template.md` where applicable. Fill every section.

### PR size and batching

- Keep each batch focused.
- Prefer <=~500 changed lines per batch where practical.
- A PR may exceed one batch when the owner-approved workstream needs it.
- If scope becomes unrelated, ask owner before splitting.

---

## Merge Rules

- Owner merges only after all batches are complete and CI is green.
- Agent never clicks merge.
- Squash merge is preferred for agent PRs unless owner chooses otherwise.
- After merge, delete the feature branch.
- Never force-merge over failing CI.

---

## Conflict Resolution

1. Agent reports: `Conflict in <branch> on files: [list]. Need owner decision.`
2. Owner decides whether to rebase, merge base, or resolve manually.
3. Agent rebases only after owner confirms.
4. After rebase/fix, push to the same branch/PR and let CI rerun.
5. Never force-push without explicit owner approval.

---

## What to Do if CI Fails

1. Read the full CI log, not just the summary.
2. Identify the root cause.
3. Push the fix commit to the same branch/PR.
4. CI reruns automatically on the new push.
5. Report: root cause, fix, files changed, new commit hash.
6. Append to `ERROR_LOG.md` if the mistake is non-trivial.

---

## What the Agent CANNOT Do

- Push to `main`
- Merge a PR
- Force-push without explicit owner approval
- Create extra branches for same-workstream batches
- Close/split PRs without owner instruction

---

*Updated: 2026-09-12 · Exact stacked-PR workflow enforced.*
