# Manual Test Plan - Ore Height Indicator MVP

## Preconditions

- Minecraft `1.21.1` with Fabric installed
- Mod JAR in `mods/`
- World loaded (creative world recommended for vertical movement tests)

## Functional Checks

1. Toggle HUD
   - Press `H` once: HUD disappears.
   - Press `H` again: HUD appears.
   - Pass: toggle is immediate and stable.

2. Y display correctness
   - Compare HUD `Y` value against F3 position.
   - Move vertically (fly/stairs) and confirm updates.
   - Pass: values match expected height movement.

3. Ore list updates
   - Go to around `Y=-56` and check deep ores rank near top.
   - Go to around `Y=180` and check Coal becomes dominant.
   - Pass: ordering changes sensibly with height.

4. Config persistence
   - Toggle HUD and restart game.
   - Pass: previous `hudEnabled` state is retained in `.minecraft/config/oreheightindicator.json`.

5. Mod Menu config screen
   - Open Mod Menu -> select `Ore Height Indicator` -> click `Config`.
   - Change `HUD X`, `HUD Y`, `Update Interval (ticks)`, and `Max Ore Entries`.
   - Click `Done` to save.
   - Pass: values are written to `.minecraft/config/oreheightindicator.json` and apply after returning to world (or restart for provider mode change).

## Performance Checks

1. Idle stability
   - Stand still for 30-60 seconds with HUD visible.
   - Pass: no visible stutter, no rapid line flicker/rebuild.

2. Movement stability
   - Move quickly up/down (elytra or creative flight).
   - Pass: no visible frame spikes while list updates.

3. On/off impact
   - Compare gameplay feel with HUD hidden vs visible.
   - Pass: only minimal perceived difference.

## Regression Checks

- HUD hidden path: with `hudEnabled=false`, no overlay should render.
- Max entries: lowering `maxEntries` in config reduces number of ore lines shown.
- Update interval: increasing `updateIntervalTicks` reduces refresh frequency as expected.
