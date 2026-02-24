# Language-Agnostic Developer Workflow — Extended Edition

This repository uses a Cursor-first operational setup with:

- Root rules in `.cursor/rules/`
- Root skills in `.cursor/skills/SKILL.md`
- Subagent playbooks in `.cursor/agents/`
- Reusable slash commands in `.cursor/commands/`
- Taskmaster command flow via `.vscode/tasks.json` and `.taskmaster/tasks/tasks.json`

## Primary Flow

1. Plan with `intake-to-plan`
2. Run Taskmaster loop (`list` -> `next` -> `show`)
3. Delegate to subagents for complex work
4. Implement safely and verify continuously
5. Close tasks and document outcomes

For day-to-day execution details, see:
- `docs/workflow/dev-workflow.md`
- `docs/workflow/git-github-workflow.md`
