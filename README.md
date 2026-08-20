# Ore Height Indicator

Client-side Fabric mod for Minecraft `1.21.11` and `26.2`. The HUD shows the current Y level and a short, biome-aware list of ores that fit that height.

For installation help and configuration screenshots, see the [user guide](docs/user-guide.md).

Each Minecraft generation has its own JAR. Use the filename that matches your game:

- `ore-height-indicator-<version>+mc1.21.11.jar`
- `ore-height-indicator-<version>+mc26.2.jar`

The split is intentional. Minecraft 26.x uses Java 25, unobfuscated Mojang names and different Fabric HUD APIs, so declaring one binary compatible with both versions would be unreliable.

## What the HUD shows

- Current Y level
- Up to four ore rows by default
- Ore icon and localized block name
- One small suitability bar per ore

The bar rates the current height against that ore's best detected height in the active biome. When enabled, the number on the bar shows the same relative suitability as a percentage. It is not an absolute spawn chance, percentages for different ores are not directly comparable, and the rows do not add up to 100 percent.

## Automatic worldgen data

There is no provider switch and no downloaded Wiki table in the runtime path.

In singleplayer, the mod reads the effective registry of the integrated server. This includes active vanilla features, datapacks and modded features that use Minecraft's standard ore configured-feature type.

If no integrated server is available, the mod reads the worldgen JSON files from the installed Minecraft and mod classpath. This keeps the fallback tied to the installed game version. A remote server can still use private datapacks that it does not send to clients, so those changes cannot be detected by a client-only installation.

## Settings

The file is `.minecraft/config/oreheightindicator.json`. Mod Menu and Cloth Config expose the same values.

- `hudEnabled`: show or hide the HUD
- `hudX`, `hudY`: offset from the top-right corner
- `showOreIcons`: show ore icons
- `showSuitabilityPercent`: show the relative height-suitability percentage on each bar
- `animateReorder`: animate ranking changes
- `uiScale`: scale from `0.5` to `3.0`
- `minimumPercent`: minimum height suitability, despite the legacy field name
- `hiddenOres`: detected ore IDs that should not appear in the HUD
- `maxEntries`: maximum number of ore rows
- `updateIntervalTicks`: interval for height and biome checks

The `Displayed ores` category lists ores detected from the active worldgen. Each ore can be shown or hidden without changing the datapack or modpack. Hidden selections remain available after a restart.

Press `H` to toggle the HUD.

## Requirements

| Minecraft | Java | Fabric Loader | Fabric API |
| --- | --- | --- | --- |
| `1.21.11` | `21` | `0.16.0` or newer | Required |
| `26.2` | `25` | `0.19.3` or newer | Required |

Mod Menu and Cloth Config are optional. Install both to use the in-game settings screen. The JSON configuration file works without them.

## Build

Build the 1.21.11 JAR with Java 21:

```bash
./gradlew clean build
```

Build the 26.2 JAR with Java 25:

```bash
./gradlew -p versions/26.2 clean build
```

The JARs are written to `build/libs/` and `versions/26.2/build/libs/` respectively. The release workflow builds and publishes both.

The automated [GitHub release](docs/workflow/git-github-workflow.md#github-release-flow) and [CurseForge release](docs/workflow/git-github-workflow.md#curseforge-release-flow) processes are documented in the workflow guide. The one-time CurseForge credential setup uses [`scripts/setup-curseforge.sh`](scripts/setup-curseforge.sh).

## Stack

- Minecraft `1.21.11`: Fabric Loader `0.18.4`, Fabric API `0.141.3+1.21.11`, Java `21`
- Minecraft `26.2`: Fabric Loader `0.19.3`, Fabric API `0.157.0+26.2`, Java `25`
- Gradle `9.5.1` with Fabric Loom `1.17`

## Main files

- `OreHeightIndicatorClient.java`: client entry point and tick scheduling
- `AutomaticWorldgenProvider.java`: automatic source selection
- `RuntimeWorldgenProvider.java`: active integrated-server registries
- `ClasspathWorldgenProvider.java`: installed-game fallback
- `OreProbabilityService.java`: relevance calculation and sorting
- `OreHudRenderer.java`: compact HUD
