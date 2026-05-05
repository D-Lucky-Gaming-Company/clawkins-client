# Dialogue File Support + Placeholder Expansion

## Summary

Implemented dialogue source flexibility for interactibles:

- `ObjectText` and `ObjectTextInteracted` now support:
  - direct string text (existing behavior)
  - JSON file path (new behavior)

Also implemented placeholder replacement in both dialogue speaker name and text.

## Supported dialogue JSON format

```json
{
  "DialogueFlow": [
    {
      "Name": "{this}",
      "Text": "Hello"
    },
    {
      "Name": "{player}",
      "Text": "Hey"
    }
  ]
}
```

- Runtime accepts `DialogueFlow` and `dialogueFlow` keys.
- Each flow item supports `Name` and `Text`.
- Lines are displayed in sequence and advance on interaction key.

## Placeholder support

Supported placeholders (case-insensitive), usable in both `Name` and `Text`:

- `{this}`: interactible/event display name (`ObjectName`)
- `{objectID}`: interactible identifier (`ObjectId`)
- `{player}`: player name (`PLAYER` object `Name`)

Alias support:

- `{ObjectIdName}` resolves to object ID.

## Interaction behavior updates

While dialogue is visible:

1. First confirm key reveals full current line (typewriter skip).
2. Next confirm key advances to next line in flow.
3. Dialogue closes only after final flow line.

## Runtime files changed

- `core/src/main/java/github/kinuseka/testproject/system/InteractionSystem.java`
  - Added source mode detection for string vs `.json` path.
  - Added JSON `DialogueFlow` parsing.
  - Added placeholder replacement logic.
  - Added multi-line flow progression state.

- `core/src/main/java/github/kinuseka/testproject/component/Interactible.java`
  - Added `objectId` field/getter for `{objectID}` placeholder.

- `core/src/main/java/github/kinuseka/testproject/component/PlayerProfile.java` (new)
  - Stores player name for dialogue placeholder resolution.

- `core/src/main/java/github/kinuseka/testproject/tiled/TiledObjectConfigurator.java`
  - `PLAYER`: reads `Name` and attaches `PlayerProfile`.
  - `INTERACTIBLE`: reads optional `ObjectId` and passes to `Interactible`.

## Asset/schema/docs updates

- `assets/maps/test.tiled-project`
  - Interactible class now includes `ObjectId` field.

- `assets/dialogue/interactible-sample.json` (new)
  - Sample dialogue flow file with placeholders.

- `docs/development/readme.md`
  - Updated Interactible section with file mode + placeholder contract.

## Notes

- If a JSON file path is invalid or malformed, runtime logs an error and falls back to direct text behavior.
- Existing direct-string `ObjectText` / `ObjectTextInteracted` maps remain compatible.
