# Skill Ownership Refactor + Random Enemy Skill Actions

## Summary

Implemented a data-contract and battle-flow cleanup:

1. Player skills now belong to the `Player` class data (Tiled `properties` on `PLAYER` object).
2. Enemy skills belong to the `Enemy` class data (Tiled `properties` on `ENEMY` object).
3. Enemy turn now randomly selects one of its configured skills and uses its power for damage.

This removes the earlier ambiguity where Enemy skill fields were being used by player actions.

## Behavior changes

- Player action hotkeys (`1/2/3`) now use player skills from persistent player state, initialized from the `PLAYER` object.
- Enemy action phase now randomly picks an enemy skill each turn.
- Enemy skill damage formula now uses chosen enemy skill power:
  - `max(1, enemySkill.power - player.defense)`
  - fallback to `enemy.attack` if no enemy skills are available.

## Runtime files updated

- `core/src/main/java/github/kinuseka/testproject/battle/PlayerBattleState.java`
  - Stores persistent player skills in addition to persistent stats/HP.
  - Exposes `createPlayerSkills()` for battle context creation.

- `core/src/main/java/github/kinuseka/testproject/tiled/TiledObjectConfigurator.java`
  - `PLAYER` branch now reads:
    - `playerSkill1Name/playerSkill1Power`
    - `playerSkill2Name/playerSkill2Power`
    - `playerSkill3Name/playerSkill3Power`
  - `ENEMY` branch now reads enemy skills from explicit keys:
    - `enemySkill1Name/enemySkill1Power`
    - `enemySkill2Name/enemySkill2Power`
    - `enemySkill3Name/enemySkill3Power`
  - Backward-compatible fallback retained for legacy `skill1/2/3*` keys.

- `core/src/main/java/github/kinuseka/testproject/encounter/EncounterZone.java`
  - Renamed encounter skill payload fields/getters to enemy-specific names.

- `core/src/main/java/github/kinuseka/testproject/encounter/EncounterEvent.java`
  - Renamed encounter skill payload fields/getters to enemy-specific names.

- `core/src/main/java/github/kinuseka/testproject/encounter/EncounterDetectionSystem.java`
  - Updated event publication to use renamed enemy skill payload getters.

- `core/src/main/java/github/kinuseka/testproject/battle/BattleContext.java`
  - Added separate `enemySkills` list alongside `playerSkills`.

- `core/src/main/java/github/kinuseka/testproject/battle/BattleService.java`
  - Builds player skills from `PlayerBattleState`.
  - Builds enemy skills from encounter event payload.

- `core/src/main/java/github/kinuseka/testproject/battle/BattleStateMachine.java`
  - Enemy turn now uses random enemy skill selection.

## Tiled schema updates

- `assets/maps/test.tiled-project`
  - `Enemy` class skills renamed to:
    - `enemySkill1Name/enemySkill1Power`
    - `enemySkill2Name/enemySkill2Power`
    - `enemySkill3Name/enemySkill3Power`
  - `Player` class now includes:
    - `playerSkill1Name/playerSkill1Power`
    - `playerSkill2Name/playerSkill2Power`
    - `playerSkill3Name/playerSkill3Power`

## Developer docs updated

- `docs/development/readme.md`
  - Clarified ownership:
    - Player stats + player skills from `Player` class.
    - Enemy stats + enemy skills from `Enemy` class.
  - Updated key names in setup and balancing sections.
  - Documented random enemy skill behavior.

## Notes

- Existing maps using legacy enemy keys (`skill1Name/skill1Power` etc.) continue to work due runtime fallback.
- New map authoring should use explicit `enemySkill*` and `playerSkill*` keys.
