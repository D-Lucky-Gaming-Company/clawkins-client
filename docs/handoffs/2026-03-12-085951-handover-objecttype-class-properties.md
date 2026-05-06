# ObjectType + Class Properties Migration

## Summary

Reworked map object classification to use Tiled custom property `ObjectType` instead of relying on object `name`, and added support for nested class property `properties` for gameplay values (enemy/interactible/player contracts).

## Why this change

- Object names are now free for designer labeling and no longer hard-coupled to runtime behavior.
- Runtime behavior is determined by a clear enum contract in Tiled:
  - `PLAYER`
  - `PROP`
  - `INTERACTIBLE`
  - `ENEMY`
- Gameplay values can be grouped under class property `properties`, which is easier to manage in Tiled.

## Runtime changes

Updated `core/src/main/java/github/kinuseka/testproject/tiled/TiledObjectConfigurator.java`:

- Added enum-based routing using `ObjectType`.
- Added parser support for nested class property key:
  - `properties` (preferred)
- Added compatibility fallbacks:
  - direct object-level properties
  - legacy class property keys (`enemyObj`, `interactibleObj`, `playerObj`)
  - legacy name-based object detection (`Player`, `Enemy`, `EncounterZone`, `Interactible`)
- Kept `PROP` recognized and intentionally no-op for now.

### Property resolution order

For each required field (example: `encounterId`, `DialogueDirectory`, `playerHp`), runtime now checks:

1. direct object property
2. nested class property `properties`
3. legacy nested class keys (`enemyObj`, `interactibleObj`, `playerObj`)
4. fallback default value in code

## Map changes

Updated `assets/maps/main.tmx` object authoring on `objects` layer:

- Player object:
  - `ObjectType=PLAYER`
  - `properties` class set to `Playable`
- Enemy object:
  - `ObjectType=ENEMY`
  - `properties` class set to `Enemy`
  - nested values include `encounterId` and `encounterTableId`
- Interactible object:
  - `ObjectType=INTERACTIBLE`
  - `properties` class set to `Interactible`

## Documentation updates

Updated `docs/development/readme.md` to reflect:

- Object classification now based on `ObjectType` (not object name).
- Hostile setup uses `ObjectType=ENEMY` and class property `properties`.
- Interactible setup uses `ObjectType=INTERACTIBLE` and class property `properties`.
- Extension guidance now references `configureByType(...)`.
- `PROP` is documented as reserved/no-op currently.

## Validation

- `./gradlew.bat core:compileJava` passes.
- Lint check for changed Java/doc files shows no new issues.
