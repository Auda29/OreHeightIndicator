# Developer Workflow

This document provides an operational workflow for the Cursor-first setup in this repository.

[Language-Agnostic Developer Workflow — Extended Edition](../agent-workflow-v5.1.md)

## Active Configuration Paths

- Cursor settings: `.cursor/settings.json`
- Cursor rules: `.cursor/rules/`
- Cursor skills: `.cursor/skills/SKILL.md`
- Subagent playbooks: `.cursor/agents/`
- Slash commands: `.cursor/commands/`
- Taskmaster tasks: `.taskmaster/tasks/tasks.json`
- Task commands integration: `.vscode/tasks.json`

## Daily Execution Loop

1. Start with planning:
   - `@agent /use-skill intake-to-plan`
2. Use Taskmaster flow:
   - `task-master list --with-subtasks`
   - `task-master next`
   - `task-master show <id>`
3. Delegate larger work:
   - Scout for impact map
   - Implementer for scoped edits
   - Tester for coverage and regressions
   - Doc for documentation impact
4. Implement in small patches and verify continuously.
5. Close progress:
   - `task-master update-subtask --id=<id> --prompt="<notes>"`
   - `task-master set-status --id=<id> --status=done`
