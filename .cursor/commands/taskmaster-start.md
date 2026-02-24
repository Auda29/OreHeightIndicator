---
description: Start a Taskmaster work session by selecting the next executable task and setting it to in-progress.
---

Run this workflow:

1. Execute `task-master list --with-subtasks`.
2. Execute `task-master next`.
3. Extract the suggested task ID.
4. Execute `task-master show <id>`.
5. If scope is complex, execute `task-master expand --id=<id> --research`.
6. Execute `task-master set-status --id=<id> --status=in-progress`.

Return:
- Selected task ID
- Acceptance criteria summary
- First implementation step
