# SKILL.md — Cursor Agent Skills Library (Root)

This file contains the active skill definitions for this repository. Agents should prefer canonical names.

## Skill Name Registry

| Canonical Name | Aliases | Description |
| --- | --- | --- |
| `intake-to-plan` | `plan`, `create-plan` | Decompose a request into an executable plan. |
| `review-pr` | `review`, `pr-review` | Review changes for bugs, risk, and quality. |
| `impact-scan` | `scan-impact`, `impact-map` | Analyze change impact before coding. |
| `implement-safely` | `safe-implement`, `small-patch` | Execute smallest safe patch with fast verification loops. |
| `taskmaster-session-start` | `tm-start`, `task-start` | Start a work session with Taskmaster command flow. |
| `taskmaster-closeout` | `tm-close`, `task-closeout` | Close a work session with status and notes updates. |

---

## Intake & Planning Skills

### skill: intake-to-plan

**Goal:** Decompose a feature or bug request into an actionable implementation plan.

**When to Use:** At the beginning of new tasks.

**Steps:**

1. Clarify ambiguous requirements.
2. Identify constraints and boundaries.
3. Break down work into verifiable units.
4. Define test strategy and rollback path.
5. Output a concise markdown plan.

**Outputs:** Structured markdown plan with tasks, risks, and verification.

### skill: review-pr

**Goal:** Review proposed changes with a bug-risk-first mindset.

**When to Use:** During PR review and pre-merge checks.

**Steps:**

1. Prioritize correctness, regression risk, and security.
2. Check test coverage for changed behavior.
3. Verify docs and rollback clarity where needed.
4. Return findings ordered by severity.

**Outputs:** Findings list, open questions, and quick quality summary.

---

## Discovery & Implementation Skills

### skill: impact-scan

**Goal:** Build an impact map before implementation starts.

**When to Use:** For multi-file work, refactors, or uncertain scope.

**Inputs:**

- Task description
- Suspected entry points
- Constraints (do-not-touch zones, deadlines, compatibility)

**Steps:**

1. Identify likely touched files and upstream/downstream dependencies.
2. Flag architecture boundaries and protected areas.
3. Recommend a smallest-safe starting patch.
4. Highlight top regression risks.

**Outputs:**

- Impact map (paths + rationale)
- Risk list
- Recommended first patch

### skill: implement-safely

**Goal:** Ship the smallest viable change with fast feedback loops.

**When to Use:** During active implementation.

**Rules:**

- One focused patch at a time.
- Run checks immediately after meaningful edits.
- Fix failures before proceeding.

**Steps:**

1. Implement smallest feasible change.
2. Run lint/test/typecheck commands available in project.
3. Iterate until green.
4. Update docs if behavior changed.

**Outputs:** Verified patch summary and remaining risks.

---

## Taskmaster Operation Skills

### skill: taskmaster-session-start

**Goal:** Start a coding session with clear task context.

**When to Use:** Before selecting work.

**Command Flow:**

1. `task-master list --with-subtasks`
2. `task-master next`
3. `task-master show <id>`
4. Optional: `task-master expand --id=<id> --research`
5. `task-master set-status --id=<id> --status=in-progress`

**Outputs:** Selected task ID, acceptance criteria, and first implementation step.

### skill: taskmaster-closeout

**Goal:** Close work with consistent status and progress logging.

**When to Use:** After implementing and verifying a task/subtask.

**Command Flow:**

1. `task-master update-subtask --id=<id> --prompt="<progress notes>"`
2. `task-master set-status --id=<id> --status=done`
3. Optional drift handling: `task-master update --from=<id> --prompt="<change context>"`

**Outputs:** Updated status, decision log, and next recommended task.
