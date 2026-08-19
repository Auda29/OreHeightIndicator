# Ore Height Indicator

Client-side Fabric mod for Minecraft `1.21.11`. The HUD shows the current Y level and a short, biome-aware list of ores that fit that height.

## What the HUD shows

- Current Y level
- Up to four ore rows by default
- Ore icon and localized block name
- One small suitability bar per ore

The bar rates the current height against that ore's best height in the active biome. It is not a block probability, and the rows do not add up to 100 percent.

## Automatic worldgen data

There is no provider switch and no downloaded Wiki table in the runtime path.

In singleplayer, the mod reads the effective registry of the integrated server. This includes active vanilla features, datapacks and modded features that use Minecraft's standard `OreFeatureConfig`.

If no integrated server is available, the mod reads the worldgen JSON files from the installed Minecraft and mod classpath. This keeps the fallback tied to the installed game version. A remote server can still use private datapacks that it does not send to clients, so those changes cannot be detected by a client-only installation.

## Settings

The file is `.minecraft/config/oreheightindicator.json`. Mod Menu and Cloth Config expose the same values.

- `hudEnabled`: show or hide the HUD
- `hudX`, `hudY`: offset from the top-right corner
- `showOreIcons`: show ore icons
- `animateReorder`: animate ranking changes
- `uiScale`: scale from `0.5` to `3.0`
- `minimumPercent`: minimum height relevance, despite the legacy field name
- `hiddenOres`: detected ore IDs that should not appear in the HUD
- `maxEntries`: maximum number of ore rows
- `updateIntervalTicks`: interval for height and biome checks

The `Displayed ores` category lists ores detected from the active worldgen. Each ore can be shown or hidden without changing the datapack or modpack. Hidden selections remain available after a restart.

Press `H` to toggle the HUD.

## Requirements

- Minecraft `1.21.11`
- Fabric Loader `0.16.0` or newer
- Fabric API
- Java `21`

Mod Menu and Cloth Config are optional. Install both to use the in-game settings screen. The JSON configuration file works without them.

## Build

```bash
./gradlew build
```

The JAR is written to `build/libs/`.

The automated [GitHub release](docs/workflow/git-github-workflow.md#github-release-flow) and [CurseForge release](docs/workflow/git-github-workflow.md#curseforge-release-flow) processes are documented in the workflow guide. The one-time CurseForge credential setup uses [`scripts/setup-curseforge.sh`](scripts/setup-curseforge.sh).

## Stack

- Minecraft `1.21.11`
- Fabric Loader `0.18.4`
- Fabric API `0.141.3+1.21.11`
- Java `21`
- Gradle with Fabric Loom

## Main files

- `OreHeightIndicatorClient.java`: client entry point and tick scheduling
- `AutomaticWorldgenProvider.java`: automatic source selection
- `RuntimeWorldgenProvider.java`: active integrated-server registries
- `ClasspathWorldgenProvider.java`: installed-game fallback
- `OreProbabilityService.java`: relevance calculation and sorting
- `OreHudRenderer.java`: compact HUD
