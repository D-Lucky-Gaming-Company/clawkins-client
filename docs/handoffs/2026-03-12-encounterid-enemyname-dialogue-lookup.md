# Dialogue EncounterId EnemyName Lookup

## What changed

- Added enemy display-name data to encounter zones.
- Extended dialogue placeholder lookup to resolve ENEMY `encounterId` tokens.

## Behavior

- Existing interactible placeholder lookup still works:
  - `{someObjectId}` -> interactible `ObjectName` by `ObjectId`.
- New enemy lookup behavior:
  - `{someEncounterId}` -> enemy display name by ENEMY `encounterId`.
  - Example: `{enemy_01}` resolves to that enemy's `enemyName`.
  - Fallback name order when ENEMY data is incomplete:
    - `enemyName` -> `Name` -> `encounterId`.
- If no matching token is found, placeholder remains unchanged (`{token}`).

## Runtime files updated

- `core/src/main/java/github/kinuseka/testproject/encounter/EncounterZone.java`
  - Added `enemyName` field and getter.
- `core/src/main/java/github/kinuseka/testproject/tiled/TiledObjectConfigurator.java`
  - ENEMY parsing now reads `enemyName` with fallback to `Name` and `encounterId`.
  - Passes `enemyName` into `EncounterZone`.
- `core/src/main/java/github/kinuseka/testproject/system/InteractionSystem.java`
  - Placeholder lookup map now includes:
    - interactible `ObjectId -> ObjectName`
    - enemy `encounterId -> enemyName`

## Documentation updated

- `docs/development/readme.md`
  - Added ENEMY `enemyName` property in encounter setup docs.
  - Added `{someEncounterId}` placeholder documentation in dialogue section.
