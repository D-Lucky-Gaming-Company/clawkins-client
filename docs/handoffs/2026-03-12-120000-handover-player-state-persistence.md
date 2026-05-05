# Player State Persistence + Enemy Data Contract Cleanup

## Summary

Implemented two related changes:

1. Removed player combat stats from enemy encounter data structures.
2. Added persistent player combat state so HP carries across battles in the same game session.

This aligns data ownership with gameplay intent:

- Enemy objects define enemy encounter data.
- Player object defines player baseline combat data.
- Battle end updates persistent player HP for future encounters.

## Why this change

The previous prototype mixed player and enemy stats in `Enemy` encounter properties. That made balancing and authoring confusing because player values were duplicated per encounter object.

The new model keeps one source of truth for player combat baseline and allows battle damage to persist naturally between fights.

## Behavior now

- On player spawn load, runtime reads player baseline combat values from `ObjectType=PLAYER` object class property `properties` (type `Player`):
  - `playerHp`
  - `playerAttack`
  - `playerDefense`
  - `playerSpeed`
- These values initialize `PlayerBattleState` once per game screen lifecycle.
- At battle start, player unit stats come from `PlayerBattleState`.
- At battle close, player HP is saved back into `PlayerBattleState`.
- Result: if player enters battle at 100 HP and exits at 50 HP, next battle starts at 50 HP.

## Scope details

### Added

- `core/src/main/java/github/kinuseka/testproject/battle/PlayerBattleState.java`

### Updated runtime wiring

- `core/src/main/java/github/kinuseka/testproject/GameScreen.java`
  - Creates one shared `PlayerBattleState`.
  - Passes it to both `BattleService` and `TiledObjectConfigurator`.

### Updated battle flow

- `core/src/main/java/github/kinuseka/testproject/battle/BattleService.java`
  - Constructor now accepts `PlayerBattleState`.
  - Uses persistent player state when creating battle context.
  - Persists player HP when closing battle session.

### Updated map object configuration

- `core/src/main/java/github/kinuseka/testproject/tiled/TiledObjectConfigurator.java`
  - Constructor now accepts `PlayerBattleState`.
  - `PLAYER` branch initializes persistent player state from player properties.
  - `ENEMY` branch no longer reads player stats.

### Updated encounter payload model

- `core/src/main/java/github/kinuseka/testproject/encounter/EncounterZone.java`
  - Removed `playerHp`, `playerAttack`, `playerDefense`, `playerSpeed`.
- `core/src/main/java/github/kinuseka/testproject/encounter/EncounterEvent.java`
  - Removed player stat payload fields.
- `core/src/main/java/github/kinuseka/testproject/encounter/EncounterDetectionSystem.java`
  - Publishes only encounter/enemy/skill data.

### Updated Tiled custom type schema

- `assets/maps/test.tiled-project`
  - `Enemy` class no longer contains player stat members.
  - `Player` class remains owner of player stat members.

## Tiled authoring contract

- `ENEMY` object + class `properties` (type `Enemy`):
  - encounter IDs, enemy stats, skill values.
- `PLAYER` object + class `properties` (type `Player`):
  - player movement speed and player combat baselines.

## Documentation updates

- Updated `docs/development/readme.md` to reflect:
  - Enemy no longer owns player stats.
  - Player stats source moved to `Player` class on `PLAYER` object.
  - Player HP persistence between battles.

## Notes / current limitation

- Persistence is currently in-memory for the game session.
- Save-file persistence across app restarts is not implemented yet.
- Recovery systems (item use / save point rest) are planned but not yet implemented.

## Validation

- No Java diagnostics reported on modified runtime files via editor error checks.
- Full Gradle compile should be run once in a clean terminal session if desired.
