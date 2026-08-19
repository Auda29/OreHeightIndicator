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

## GitHub release flow

GitHub releases are created by `.github/workflows/release.yml`. A push to `main` creates a release when the current `mod_version` does not have a matching tag yet. A manually pushed version tag also starts the workflow. The tag must exactly match `mod_version` with a leading `v`.

Write commit messages, pull request titles, tag messages, and release notes in English. GitHub's generated release notes reuse this text.

1. Merge the release changes into `main`.
2. Set `mod_version` in `gradle.properties`, for example `0.1.0`.
3. Run a clean local build:

   ```powershell
   .\gradlew.bat clean build
   ```

4. Commit and push the version change to `main`.

The workflow validates the version, builds the project with Java 21, and runs the tests. If the matching tag does not exist, it creates an annotated tag on the `main` commit. It then creates the GitHub release and attaches the production JAR and a SHA-256 checksum. Versions containing a suffix such as `0.2.0-beta.1` are published as prereleases.

Manually creating and pushing the matching annotated tag remains supported when a release needs to start from an explicit tag operation.

Never reuse or force-move a published release tag. Create a new version instead.
