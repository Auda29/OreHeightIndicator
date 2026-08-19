# ADR 0001: Automatic worldgen provider

## Status

Accepted, replacing the earlier static and experimental-provider decision.

## Context

The original provider normalized every ore against its own peak and then treated those unrelated values as probabilities. It also relied on a fixed Wiki snapshot. That made cross-ore percentages wrong and ignored the active biome, datapacks and modpacks.

The HUD only needs to answer whether the current height is useful for an ore. It does not need to claim an exact block probability.

## Decision

`AutomaticWorldgenProvider` selects the best source without a user-facing switch.

In an integrated world, `RuntimeWorldgenProvider` reads the current biome's `GenerationSettings`. It finds configured features that use `OreFeatureConfig`, evaluates their actual placement modifiers on the server thread and rebuilds the snapshot when the dimension or biome changes.

Without an integrated server, `ClasspathWorldgenProvider` parses biome, placed-feature and configured-feature JSON from the installed Minecraft and mod classpath.

Each ore profile is normalized only against that ore's best height. The HUD calls this relevance and draws a bar. It never normalizes different ores to a shared 100 percent total.

## Limits

- A remote server does not send all placed and configured feature registries to a client.
- Private server datapacks therefore need a future optional server-side data channel.
- A mod with a completely custom ore feature config needs a dedicated adapter. Standard `OreFeatureConfig` features work automatically.
- The provider models height suitability, not the exact number of blocks left in already generated chunks.

## Performance

- Registry sampling runs on the integrated server thread after a biome or dimension change.
- The render callback only draws cached rows.
- The configurable client tick interval controls context checks.
- No loaded chunks are scanned for hidden ore blocks.
