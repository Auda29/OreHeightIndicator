# ADR 0001: Hybrid Ore Data Provider for Client HUD

## Status

Accepted

## Context

Ore Height Indicator needs to show useful ore probabilities in-game without causing visible FPS impact.
The mod also needs a clean way to evolve from static vanilla data to future dynamic extraction from game/worldgen data.

Key constraints:

- Client-side only feature (`Fabric 1.21.1`)
- Minimal runtime overhead in the render path
- Extensible architecture for future data provider changes

## Decision

Use a hybrid provider architecture:

1. `StaticVanilla1211Provider` is the default provider for MVP.
2. `OreDataProvider` defines the contract so provider logic is decoupled from UI.
3. `DynamicWorldgenProvider` is included as an experimental MVP that derives scores from vanilla placed-feature worldgen data and remains disabled by default via config.
4. If dynamic provider initialization fails, the client automatically falls back to `StaticVanilla1211Provider`.

Use strict performance guardrails in runtime flow:

- Recalculate data only on throttled client tick intervals (`updateIntervalTicks`).
- Recalculate only when Y value changed.
- Keep render callback lightweight (draw cached lines only, no heavy scans).
- Skip update work while HUD is hidden.

## Consequences

Positive:

- Stable MVP behavior on vanilla `1.21.1`.
- Easy provider swap later without rewriting HUD or probability service.
- Predictable performance profile.

Negative:

- Static provider requires maintenance for version changes.
- Dynamic provider currently targets vanilla placed-feature coverage and may not model every custom datapack/modded worldgen case.

## Follow-up

- Extend dynamic extraction coverage for additional worldgen cases beyond vanilla MVP.
- Add automatic fallback/warning when dynamic mode is enabled but unsupported.
- Re-validate static curves whenever target Minecraft version changes.
