# Ore Height Indicator

Client-side Fabric Mod fuer Minecraft `1.21.1`, die im HUD die aktuelle Hoehe (Y) und eine sortierte Liste mit Ore-Wahrscheinlichkeiten anzeigt.

## Features (aktueller Stand)

- HUD mit Toggle-Keybind (`H`)
- Anzeige der aktuellen Spielerhoehe (`Y`)
- Sortierte Ore-Liste mit Prozentwerten (relativer Score je Hoehe)
- Performance-schonendes Update-Verhalten:
  - keine Berechnung pro Frame
  - Tick-Intervall steuerbar (`updateIntervalTicks`)
  - Recompute nur bei geaenderter Hoehe
- Datenquellen-Architektur vorbereitet:
  - `StaticVanilla1211Provider` aktiv
  - `DynamicWorldgenProviderStub` vorhanden (standardmaessig aus)

## Kompatibilitaet / Stack

- Minecraft: `1.21.1`
- Mod Loader: Fabric Loader (`0.18.4` in `gradle.properties`)
- Fabric API: `0.102.0+1.21.1`
- Java: `21`
- Build: Gradle + Fabric Loom

## Projektstruktur (wichtig)

- Mod-Einstieg: `src/main/java/dev/wecke/oreheightindicator/OreHeightIndicatorClient.java`
- Config: `src/main/java/dev/wecke/oreheightindicator/config/ModConfig.java`
- Probability-Logik: `src/main/java/dev/wecke/oreheightindicator/data/OreProbabilityService.java`
- HUD-Rendering: `src/main/java/dev/wecke/oreheightindicator/hud/OreHudRenderer.java`
- Mod-Metadaten: `src/main/resources/fabric.mod.json`

## Getting Started

### Build lokal

Hinweis: Aktuell ist noch kein Gradle Wrapper (`gradlew`) im Repo. Du brauchst daher lokal installiertes `gradle`.

```bash
gradle build
```

Das Build-Artifact liegt danach in `build/libs/`.

### In Minecraft verwenden

1. Fabric fuer `1.21.1` installieren.
2. Gebaute Mod-JAR in den `mods`-Ordner legen.
3. Spiel starten und mit `H` das HUD ein-/ausblenden.

## Konfiguration

Datei: `.minecraft/config/oreheightindicator.json` (wird beim ersten Start erzeugt)

Relevante Felder:

- `hudEnabled`: HUD standardmaessig aktiv/inaktiv
- `hudX`, `hudY`: Position des HUD
- `updateIntervalTicks`: Rechenintervall (Performance-Hebel)
- `maxEntries`: maximale Anzahl angezeigter Ores
- `useDynamicProvider`: vorbereitet, aktuell nur Stub

## Performance-Hinweise

- Die Mod ist bewusst auf minimalen Impact ausgelegt.
- Keine schweren Worldgen-Scans im Render-Pfad.
- HUD-Text wird nur bei geaenderten Daten neu aufgebaut.
- Falls noetig kann die Mod schnell entschärft werden ueber:
  - hoeheres `updateIntervalTicks`
  - `hudEnabled: false`

## Development Workflow

Die Repo-Workflows sind dokumentiert in:

- `docs/workflow/dev-workflow.md`
- `docs/workflow/git-github-workflow.md`
- `docs/workflow/manual-test-plan.md`
- `.taskmaster/docs/README.md`

Kurzfassung:

1. Taskmaster Loop nutzen: `task-master list --with-subtasks` -> `task-master next` -> `task-master show <id>`
2. In kleinen Schritten implementieren, dann Status/Notizen updaten
3. Commits nach Conventional Commits, PR gegen `main`

## Wichtige Hinweise

- `task-master parse-prd` benoetigt konfigurierte API-Keys (z. B. `ANTHROPIC_API_KEY` / `PERPLEXITY_API_KEY`) in deiner Umgebung.
- Dynamic Worldgen Extraction ist als naechster Ausbauschritt vorgesehen, aber im aktuellen MVP noch nicht aktiv.
- Architekturentscheidung dokumentiert in `docs/decisions/adr-0001-hybrid-provider-architecture.md`.

## Roadmap (naechste Schritte)

- Dynamic Provider mit echter Worldgen-Auswertung anbinden
- Verfeinerung der Verteilungsmodelle/Genauigkeit
- Optionales erweitertes UI (z. B. Range-Ansicht)
