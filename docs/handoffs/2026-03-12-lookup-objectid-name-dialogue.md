# Dialogue ObjectId Name Lookup

## What changed

- Implemented object-id token lookup in dialogue placeholder replacement.
- Interactibles now always get a stable fallback `ObjectId` when missing or blank.

## Behavior

- Existing placeholders still work:
  - `{this}` -> current interactible `ObjectName`
  - `{objectID}` / `{object_id}` / `{ObjectIdName}` -> current interactible `ObjectId`
  - `{player}` -> player name
- New lookup behavior:
  - Any unknown placeholder token is treated as an `ObjectId` lookup key.
  - If a matching interactible is found, token resolves to that interactible's `ObjectName`.
  - Example: `{barrel_1}` resolves to the name of the interactible with `ObjectId=barrel_1`.
  - If no matching `ObjectId` exists, the token remains unchanged (`{token}`).

## ObjectId fallback rule

- `INTERACTIBLE` objects now default to:
  - `ObjectId = <normalizedObjectName>_<x>_<y>`
- This applies when `ObjectId` is missing or blank in map properties.

## Files updated

- `core/src/main/java/github/kinuseka/testproject/system/InteractionSystem.java`
  - Added interactible `ObjectId -> ObjectName` lookup map.
  - Extended placeholder replacement to resolve object-id tokens.
- `core/src/main/java/github/kinuseka/testproject/tiled/TiledObjectConfigurator.java`
  - Updated `ObjectId` fallback logic for interactibles.
- `docs/development/readme.md`
  - Documented new placeholder lookup behavior and updated default `ObjectId` rule.
