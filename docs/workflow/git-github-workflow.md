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
3. Run clean local builds for every supported target with Java 21 and Java 25 respectively:

   ```powershell
   .\gradlew.bat clean build
   .\gradlew.bat -p versions\1.21.1-fabric clean build
   .\gradlew.bat -p versions\1.21.1-neoforge clean build
   .\gradlew.bat -p versions\26.2 clean build
   ```

4. Commit and push the version change to `main`.

The workflow validates the version and runs the Minecraft 1.21.1 Fabric, 1.21.1 NeoForge and 1.21.11 Fabric test suites with Java 21, followed by the 26.2 Fabric suite with Java 25. If the matching tag does not exist, it creates an annotated tag on the `main` commit. It then creates the GitHub release and attaches all four production JARs and their SHA-256 checksums. Versions containing a suffix such as `0.2.0-beta.1` are published as prereleases.

Manually creating and pushing the matching annotated tag remains supported when a release needs to start from an explicit tag operation.

Never reuse or force-move a published release tag. Create a new version instead.

## CurseForge release flow

CurseForge releases are published by `.github/workflows/curseforge-release.yml`. The workflow starts after a successful `Release` workflow run and only continues when the release tag points to that workflow's commit. Regular pushes that keep an existing `mod_version` do not upload a duplicate file.

The first CurseForge file for version `0.1.0` was submitted manually for project moderation. Do not dispatch the CurseForge workflow for `v0.1.0` again.

### One-time setup

Run the setup wizard from the repository root in Git Bash, WSL, or another Bash shell:

```bash
bash scripts/setup-curseforge.sh
```

The wizard stores these values in the GitHub repository:

- Actions variable `CURSEFORGE_PROJECT_ID`: the numeric CurseForge project ID.
- Actions secret `CURSEFORGE_API_TOKEN`: an author upload token from the CurseForge API Tokens page.

The token is entered with hidden input and is sent directly to GitHub. The wizard does not write it to `.env` or another local file.

### Automatic publishing

For every new version, the GitHub release workflow builds and tests the mod first. After that workflow succeeds, the CurseForge workflow:

1. Confirms that the version tag points to the released commit.
2. Downloads all four production JARs and SHA-256 files from the GitHub release.
3. Verifies all four JAR checksums.
4. Reuses the GitHub release notes as the CurseForge changelog.
5. Reads the Minecraft versions from the Gradle projects.
6. Uploads correctly classified Fabric files for Minecraft 1.21.1, 1.21.11 and 26.2, plus a NeoForge file for Minecraft 1.21.1, through the official CurseForge Upload API.

Stable versions use the CurseForge `release` type. Versions containing `alpha` use `alpha`; other suffixed versions use `beta`. Fabric files mark Fabric API as required and Mod Menu and Cloth Config as optional. The NeoForge file marks Cloth Config as optional and does not claim Fabric-only dependencies.

The manual `workflow_dispatch` trigger can publish an existing GitHub release that is missing from CurseForge. Never run it for a tag that already has a CurseForge file.

## Modrinth release flow

Modrinth releases are published by `.github/workflows/modrinth-release.yml`. The workflow follows the same successful `Release` workflow trigger and tag-to-commit check as CurseForge. It does not rebuild the mod: it downloads the four production JARs and their SHA-256 files from the corresponding GitHub release, verifies every checksum, and uploads those immutable assets through the official Minotaur Gradle plugin.

### One-time setup

Create and have the Ore Height Indicator Modrinth project approved before the first automatic upload. Then configure these repository settings:

- Actions variable `MODRINTH_PROJECT_ID`: the public Modrinth project ID or project slug.
- Actions secret `MODRINTH_TOKEN`: a Modrinth personal access token with the `CREATE_VERSION` scope.

For example, with GitHub CLI from the repository root:

```bash
gh variable set MODRINTH_PROJECT_ID --body "your-project-id-or-slug"
gh secret set MODRINTH_TOKEN
```

The token is supplied only to Minotaur at workflow runtime. Do not add it to `gradle.properties`, `.env`, the repository, or release notes.

### Automatic publishing

For every new GitHub release, after the GitHub release workflow succeeds, the Modrinth workflow:

1. Confirms that the release tag points to the commit just released.
2. Downloads all four JARs plus their SHA-256 files from GitHub Releases.
3. Verifies every checksum before any upload.
4. Reuses GitHub release notes as the Modrinth changelog.
5. Publishes one Modrinth version per Minecraft/loader combination, so clients never receive a JAR for the wrong game version.
6. Marks Fabric API as required and Mod Menu and Cloth Config as optional on Fabric versions. The NeoForge version only marks Cloth Config as optional.

Stable versions publish as `release`; versions containing `alpha` publish as `alpha`; other suffixed versions publish as `beta`.

The manual `workflow_dispatch` trigger can publish an existing GitHub release that is missing from Modrinth. Never run it for a tag that already has a Modrinth version.
