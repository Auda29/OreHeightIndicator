# Ore Height Indicator

Client-side Fabric and NeoForge mod. The HUD shows the current Y level and a short, biome-aware list of ores and selected blocks that fit that height.

The settings screen and key binding support English and German. Block and ore names follow Minecraft's selected language.

For installation help and configuration screenshots, see the [user guide](docs/user-guide.md).

Each Minecraft and loader combination has its own JAR. Use the filename that matches both:

- `ore-height-indicator-<version>+mc1.21.1-fabric.jar`
- `ore-height-indicator-<version>+mc1.21.1-neoforge.jar`
- `ore-height-indicator-<version>+mc1.21.11.jar`
- `ore-height-indicator-<version>+mc1.21.11-neoforge.jar`
- `ore-height-indicator-<version>+mc26.2.jar`
- `ore-height-indicator-<version>+mc26.2-neoforge.jar`

The split is intentional. Fabric and NeoForge use different loader APIs, while Minecraft 26.x uses Java 25, unobfuscated Mojang names and different HUD APIs. Declaring one binary compatible with all combinations would be unreliable.

## What the HUD shows

- Current Y level
- Up to four rows by default
- Block icon and localized name
- One small suitability bar per entry

The bar rates the current height against that ore's best detected height in the active biome. When enabled, the number on the bar shows the same relative suitability as a percentage. It is not an absolute spawn chance, percentages for different ores are not directly comparable, and the rows do not add up to 100 percent.

## Automatic worldgen data

There is no provider switch and no downloaded Wiki table in the runtime path.

In singleplayer, the mod reads the effective registry of the integrated server. This includes active vanilla features, datapacks and modded features that use Minecraft's standard ore configured-feature type.

Every registered block is searchable in `Displayed ores`, including blocks added by mods. If a selected block such as andesite has no readable ore-style worldgen profile, the mod measures its relative height distribution in a sparse sample of nearby loaded blocks. This observed fallback works in singleplayer and multiplayer, respects the blocks the client actually received and refreshes after movement or 30 seconds. It may include blocks placed or removed by players.

If no integrated server is available, the mod reads the worldgen JSON files from the installed Minecraft and mod classpath. This keeps the fallback tied to the installed game version. A remote server can still use private datapacks that it does not send to clients, so those changes cannot be detected by a client-only installation.

## Settings

The file is `.minecraft/config/oreheightindicator.json`. Mod Menu and Cloth Config expose the same values.

- `hudEnabled`: show or hide the HUD
- `hudX`, `hudY`: offset from the top-right corner
- `showOreIcons`: show block icons
- `showSuitabilityPercent`: show the relative height-suitability percentage on each bar
- `animateReorder`: animate ranking changes
- `uiScale`: scale from `0.5` to `3.0`
- `minimumPercent`: minimum height suitability, despite the legacy field name
- `hiddenOres`: detected ore IDs that should not appear in the HUD
- `trackedMaterials`: additional registered block IDs that should appear in the HUD
- `maxEntries`: maximum number of HUD rows
- `updateIntervalTicks`: interval for height and biome checks

The `Displayed ores` category lists standard ores and the extra blocks you have added. Use the `Add block` field to search the live block registry by translated name or registry ID, choose a suggestion and save. The block then appears as its own toggle when you reopen the category. This keeps the settings screen small even with large modpacks. Ores remain enabled by default, while other blocks are opt-in. Selections remain available after a restart.

Press `H` to toggle the HUD.

## Requirements

| Minecraft | Loader | Java | Required mod |
| --- | --- | --- | --- |
| `1.21.1` | Fabric `0.16.0` or newer | `21` | Fabric API |
| `1.21.1` | NeoForge `21.1.248` or newer | `21` | None |
| `1.21.11` | Fabric `0.16.0` or newer | `21` | Fabric API |
| `1.21.11` | NeoForge `21.11.45` or newer | `21` | None |
| `26.2` | Fabric `0.19.3` or newer | `25` | Fabric API |
| `26.2` | NeoForge `26.2.0.66` or newer | `25` | None |

On Fabric, Mod Menu and Cloth Config are optional; install both to use the in-game settings screen. On NeoForge, only Cloth Config is needed for that screen. The JSON configuration file works without these optional mods.

## Build

Build the Java 21 targets:

```bash
./gradlew clean build
./gradlew -p versions/1.21.1-fabric clean build
./gradlew -p versions/1.21.1-neoforge clean build
./gradlew -p versions/1.21.11-neoforge clean build
```

Build the 26.2 JAR with Java 25:

```bash
./gradlew -p versions/26.2 clean build
./gradlew -p versions/26.2-neoforge clean build
```

The JARs are written to each target's `build/libs/` directory. The release workflow builds and publishes all six loader/version combinations.

The automated [GitHub release](docs/workflow/git-github-workflow.md#github-release-flow), [CurseForge release](docs/workflow/git-github-workflow.md#curseforge-release-flow), and [Modrinth release](docs/workflow/git-github-workflow.md#modrinth-release-flow) processes are documented in the workflow guide. The one-time CurseForge credential setup uses [`scripts/setup-curseforge.sh`](scripts/setup-curseforge.sh); Modrinth requires the repository secret `MODRINTH_TOKEN` and variable `MODRINTH_PROJECT_ID`.

## License

Ore Height Indicator is distributed under the [MIT License](LICENSE).

## Stack

- Minecraft `1.21.1` Fabric: Fabric Loader `0.18.4`, Fabric API `0.116.15+1.21.1`, Java `21`
- Minecraft `1.21.1` NeoForge: NeoForge `21.1.248`, Java `21`
- Minecraft `1.21.11` Fabric: Fabric Loader `0.18.4`, Fabric API `0.141.3+1.21.11`, Java `21`
- Minecraft `1.21.11` NeoForge: NeoForge `21.11.45`, Java `21`
- Minecraft `26.2` Fabric: Fabric Loader `0.19.3`, Fabric API `0.157.0+26.2`, Java `25`
- Minecraft `26.2` NeoForge: NeoForge `26.2.0.66`, Java `25`
- Gradle `9.5.1` with Fabric Loom `1.17` and ModDevGradle `2.0.144`

## Main files

- `OreHeightIndicatorClient.java`: client entry point and tick scheduling
- `AutomaticWorldgenProvider.java`: automatic source selection
- `RuntimeWorldgenProvider.java`: active integrated-server registries
- `ClasspathWorldgenProvider.java`: installed-game fallback
- `TrackedBlockSampler.java`: nearby loaded-block fallback for selected blocks
- `OreProbabilityService.java`: relevance calculation and sorting
- `OreHudRenderer.java`: compact HUD
