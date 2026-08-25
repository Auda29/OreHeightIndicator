# Manual test plan

## Setup

- Minecraft `1.21.1` with Fabric Loader and Fabric API
- Minecraft `1.21.1`, `1.21.11` and `26.2` with NeoForge
- Minecraft `1.21.11` and `26.2` with Fabric Loader and Fabric API
- Mod Menu and Cloth Config for Fabric settings checks
- Cloth Config for NeoForge settings checks
- One vanilla world
- One world with a small ore datapack or modpack

## HUD checks

1. Load a world and wait for the first profile build.
2. Compare the HUD Y value with F3.
3. Confirm that each row has one icon, one name and one bar. Toggle the percentage setting and confirm that the numbers appear and disappear.
4. Press `H` twice and confirm immediate hide and show behavior.
5. Change `maxEntries`, `minimumPercent`, scale, icons, position and animation in the settings screen. Confirm that each setting still applies.
6. Disable copper in `Displayed ores` and confirm that it disappears without changing height.
7. Reopen the settings and restart the game. Confirm that copper stays disabled and can be enabled again.
8. Search for `Andesite` in `Displayed ores`, enable it and confirm that it appears in the HUD with an icon and suitability value even when the active worldgen provider does not expose an andesite profile.
9. Search for `Oak Log` and one block added by a mod. Confirm that both can be enabled without first entering a world.
10. Move above and below the selected blocks and confirm that the value follows either their worldgen profile or the nearby loaded-block sample, including values below `minimumPercent`.
11. Disable Andesite, reopen the settings and confirm that it remains disabled.
12. Repeat the core HUD, key binding and settings checks on all six Minecraft and loader combinations.

## Worldgen checks

1. In plains, confirm that emerald is absent.
2. In a mountain biome, confirm that emerald appears.
3. In dripstone caves, confirm that the active copper profile changes.
4. In badlands, confirm that the extra gold feature affects the profile.
5. In the Nether, confirm that quartz, Nether gold and ancient debris appear while Overworld ores do not.
6. In the End, confirm that the list is empty unless a datapack or mod adds an ore feature.

## Datapack and modpack checks

1. Add a configured feature using `OreFeatureConfig` and attach its placed feature to a biome.
2. Enter that biome and confirm that the new ore appears with its translated name and block icon.
3. Change its height placement, reload or reopen the world and confirm that its bar changes at the affected heights.
4. Remove the feature from the biome and confirm that the row disappears.
5. Confirm that the detected modded ore appears in `Displayed ores` and can be hidden independently.
6. Add a non-ore block target through the same feature type and confirm that it appears as an optional searchable material.
7. Select a registered block that is not part of an ore feature and confirm that it appears through the nearby loaded-block fallback.

## Logs and performance

1. Check `latest.log` for `Loaded ... worldgen profiles from active worldgen`.
2. Cross a biome boundary and confirm one new profile build, not continuous rebuilding.
3. Stand still for 60 seconds and confirm that rows do not flicker.
4. Fly vertically and confirm that only cached relevance values update.
5. Track several fallback blocks, cross a 32-block sampling boundary and confirm that the HUD remains responsive while the local profiles refresh once.
