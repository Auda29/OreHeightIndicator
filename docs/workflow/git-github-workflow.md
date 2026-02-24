# Git & GitHub Workflow

This document outlines the branching model, commit conventions, and pull request process.

## Branching Model

- `main`: default branch, always deployable, no direct pushes.
- `feature/<name>`: new features from `main`.
- `bugfix/<name>`: bug fixes from `main`.
- `chore/<name>`: maintenance from `main`.

## Commit Convention

Use [Conventional Commits](https://www.conventionalcommits.org/):

`<type>[optional scope]: <description>`

Common types: `feat`, `fix`, `chore`, `docs`, `style`, `refactor`, `test`.

## Pull Request Flow

1. Create branch from `main`.
2. Implement and commit changes.
3. Push branch.
4. Open PR against `main`.
5. PR title follows Conventional Commits.
6. PR body uses repository template.
7. At least one review is required.
8. All status checks must pass.
