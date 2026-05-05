# Data-Driven Clawkin Summary Handoff

Date: 2026-04-07  
Status: Active / Current  
Scope: Summary UI and metadata pipeline for player clawkins (map-driven, no per-character UI branching).

## Final Architecture

- Summary reads data from runtime objects only:
  - `Clawkin.SummaryProfile`
  - `BattleSkill` summary fields
- Runtime data source for party clawkins:
  - `assets/maps/main.tmx`
  - object `Player`
  - direct class properties `clawkin1`, `clawkin2`, `clawkin3`
- Loader mapping:
  - `core/src/main/java/github/kinuseka/testproject/tiled/TiledObjectConfigurator.java`
  - builds `Clawkin` + `Clawkin.SummaryProfile` + enriched `BattleSkill`

## Critical Schema Requirement

Tiled class schema must include summary fields or values can be dropped at runtime.

- Schema file:
  - `assets/maps/test.tiled-project`
- Class type:
  - `Clawkin`
- Required summary members now added:
  - `species`, `role`, `title`, `overview`
  - `summaryHp`, `summaryAttack`, `summaryDefense`, `summarySpeed`
  - `summaryHpNote`, `summaryAttackNote`, `summaryDefenseNote`, `summarySpeedNote`
  - `skillNDescription`, `skillNSummaryEffect`, `skillNSummaryScaling`

If these members are removed from the class definition, Summary can regress to `Unspecified`/fallback text even if the TMX visually contains values.

## Current Summary UI Behavior

File: `core/src/main/java/github/kinuseka/testproject/ui/SummaryScreen.java`

- STATS page:
  - larger bars with numeric values beside bars
  - overview panel from metadata
- SKILLS page:
  - left skill cards show `Skill Name`, `Type`, `Cooldown`
  - right detail panel uses sections: `COOLDOWN`, `TYPE`, `DESCRIPTION`, `EFFECTS`
  - description sanitization removes `Unlocked at ...` and excludes `Visual/Audio`

## Authoring Template (Per `clawkinN`)

### Core Combat

- `id`
- `name`
- `image_clawkin`
- `level`
- `hp`
- `attack`
- `defense`
- `speed`

### Profile

- `species`
- `role`
- `title`
- `overview`

### Stats Summary

- `summaryHp`
- `summaryAttack`
- `summaryDefense`
- `summarySpeed`
- `summaryHpNote`
- `summaryAttackNote`
- `summaryDefenseNote`
- `summarySpeedNote`

### Skills (`skill1..skill3`)

- Core:
  - `skillNName`
  - `skillNEffectType`
  - `skillNEffectBaseStat`
  - `skillNEffectStatScale`
  - `skillNEffectDurationTurns`
  - `skillNTurnCooldown`
- Display:
  - `skillNDescription`
  - `skillNSummaryEffect`
  - `skillNSummaryScaling`

## Verification Checklist

1. Open TeamViewer, select clawkin, open Summary.
2. Confirm profile values show authored metadata (not `Unspecified`).
3. Confirm skills page:
   - card shows name/type/cooldown
   - details show cooldown/type/description/effects
   - no visual/audio text
4. Build check:
   - `./gradlew.bat core:compileJava`

## Troubleshooting

- Symptom: Summary shows generic/unspecified values.
  - Check `assets/maps/test.tiled-project` still contains the summary members for `Clawkin`.
  - Check `assets/maps/main.tmx` direct `Player.clawkinN` fields are populated.
  - Check loader logs from `TiledObjectConfigurator` for parsed profile values.
