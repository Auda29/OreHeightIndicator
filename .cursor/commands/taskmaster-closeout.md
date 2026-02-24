---
description: Close a Taskmaster work session by logging progress, marking completion, and identifying follow-up work.
---

Run this workflow:

1. Ask for the target task or subtask ID if missing.
2. Execute `task-master update-subtask --id=<id> --prompt="<implementation notes>"` when subtask context exists.
3. Execute `task-master set-status --id=<id> --status=done`.
4. Execute `task-master next`.

Return:
- What was completed
- Final status update confirmation
- Next recommended task
