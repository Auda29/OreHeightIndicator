# Ore Height Indicator

Client-side Fabric mod for Minecraft `1.21.1` that displays the current height (Y) and a sorted list of ore probabilities on the HUD.

## Features (Current State)

- HUD with toggle keybind (`H`)
- Mod Menu + Cloth Config settings screen
- Display of the current player height (`Y`)
- Sorted ore list with percentage values (relative score per height)
- Ore item icons next to each HUD row (toggleable)
- Smooth animated row reordering when rankings change
- Configurable UI scale for the entire HUD
- Configurable minimum percent threshold to hide low-probability ores
- Performance-conscious update behavior:
  - No computation per frame
  - Tick interval configurable (`updateIntervalTicks`)
  - Recompute only when height changes
- Data source architecture prepared:
  - `StaticVanilla1211Provider` active (wiki-based globally-normalized data)
  - `DynamicWorldgenProviderStub` available (disabled by default)

## Compatibility / Stack

- Minecraft: `1.21.1`
- Mod Loader: Fabric Loader (`0.18.4` in `gradle.properties`)
- Fabric API: `0.102.0+1.21.1`
- Java: `21`
- Build: Gradle + Fabric Loom

## Project Structure (Key Files)

- Mod entry point: `src/main/java/dev/wecke/oreheightindicator/OreHeightIndicatorClient.java`
- Config: `src/main/java/dev/wecke/oreheightindicator/config/ModConfig.java`
- Probability logic: `src/main/java/dev/wecke/oreheightindicator/data/OreProbabilityService.java`
- HUD rendering: `src/main/java/dev/wecke/oreheightindicator/hud/OreHudRenderer.java`
- Mod metadata: `src/main/resources/fabric.mod.json`

## Getting Started

### Local Build

```bash
./gradlew build
```

The build artifact will be located in `build/libs/`.

### Using in Minecraft

1. Install Fabric for `1.21.1`.
2. Place the built mod JAR in the `mods` folder.
3. Start the game and press `H` to toggle the HUD.

## Configuration

File: `.minecraft/config/oreheightindicator.json` (created on first launch)

Configuration is available in two ways:

- In-game via **Mod Menu** -> Ore Height Indicator -> Config
- Directly via file: `.minecraft/config/oreheightindicator.json`

### HUD Settings

- `hudEnabled`: Whether the HUD is active by default
- `hudX`, `hudY`: HUD position offset
- `showOreIcons`: Show or hide ore item icons for each row
- `animateReorder`: Smooth row movement when ore ranking changes
- `uiScale`: Scales the entire HUD size (0.5 - 3.0, default 1.0)
- `minimumPercent`: Hide ores below this percentage threshold (0.0 - 50.0, default 0.5)

### Data & Performance Settings

- `updateIntervalTicks`: Recalculation interval in ticks (performance lever)
- `maxEntries`: Maximum number of ore rows shown
- `useDynamicProvider`: Prepared for future dynamic worldgen extraction (currently stub only)

## Performance Notes

- The mod is intentionally designed for minimal impact.
- No heavy worldgen scans in the render path.
- HUD text is only rebuilt when data changes.
- If needed, the mod can be quickly throttled via:
  - Higher `updateIntervalTicks`
  - `hudEnabled: false`

## Development Workflow

Repository workflows are documented in:

- `docs/workflow/dev-workflow.md`
- `docs/workflow/git-github-workflow.md`
- `docs/workflow/manual-test-plan.md`
- `.taskmaster/docs/README.md`

Quick summary:

1. Use Taskmaster loop: `task-master list --with-subtasks` -> `task-master next` -> `task-master show <id>`
2. Implement in small steps, then update status/notes
3. Commits follow Conventional Commits, PR against `main`

## Important Notes

- `task-master parse-prd` requires configured API keys (e.g., `ANTHROPIC_API_KEY` / `PERPLEXITY_API_KEY`) in your environment.
- Dynamic worldgen extraction is planned as the next development step but is not active in the current MVP.
- Architecture decision documented in `docs/decisions/adr-0001-hybrid-provider-architecture.md`.

## Roadmap (Next Steps)

- Connect dynamic provider with real worldgen extraction
- Refine distribution models and accuracy
- Optional extended UI (e.g., range view)
