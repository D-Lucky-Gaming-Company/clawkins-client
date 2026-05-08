# Save/Load Mechanic Handoff (Stable + Centralized)

## Goal

Document the current save/load implementation so future AI can safely extend it (save points, nursery event routing, UI metadata, and persistence details).

## Current architecture

### Persistence layer

- `core/src/main/java/github/dluckycompany/clawkins/save/SaveState.java`
  - DTO for all persisted data.
  - Now includes player metadata:
    - `playerName`
    - `playerGender`
- `core/src/main/java/github/dluckycompany/clawkins/save/SaveStateManager.java`
  - Writes/reads `.txt` save files in:
    - `%USERPROFILE%/Documents/Clawkins/save_states`
  - Save file format is `key=value` with escaped values.
  - Parser now unescapes safely and supports multiline/special text content.
  - Save file naming:
    - `save_yyyy-MM-dd_HH-mm-ss.txt`

### Runtime integration

- `core/src/main/java/github/dluckycompany/clawkins/GameScreen.java`
  - `buildSaveState()` captures runtime into `SaveState`.
  - `applySaveState(SaveState)` restores world/map/session state.
  - `applySaveStateToPlayer(SaveState)` restores party/inventory/wallet/progress.
  - Player metadata handling:
    - save: stores player name/gender
    - load: restores profile and reapplies player name component to spawned player entity

### Progress protocol (custom)

- `core/src/main/java/github/dluckycompany/clawkins/progress/PlayerProgress.java`
  - Legacy flags API still supported:
    - `writeToFlags(Map<String, String>)`
    - `loadFromFlags(Map<String, String>)`
  - Added custom protocol payload API:
    - `toProtocolPayload()`
    - `loadFromProtocolPayload(String)`
  - Payload key:
    - `PlayerProgress.PROTOCOL_FLAG_KEY` = `progress.protocol.v1`

Load precedence now:

1. Use protocol payload if present.
2. Fallback to legacy flags for backward compatibility.

## Centralized save trigger flow

### Special interaction source of truth

- Save-point interactibles are registered in one place:
  - `GameScreen.registerSavePointInteractions()`
- IDs come from:
  - `SAVE_POINT_OBJECT_IDS`

### Important gameplay rule (current)

- Only bed interactibles should directly trigger save.
- Current direct save-point set includes only:
  - `bed_cottage`
- Nursery is intentionally **not** treated as a direct save point.

This preserves flexibility for nursery to host mixed actions (heal, event scripts, optional save prompt, etc.) without forcing save behavior by object id.

## Load UI metadata behavior

- `core/src/main/java/github/dluckycompany/clawkins/ui/SaveStateScreen.java`
  - In LOAD mode, each slot can show:
    - Player name
    - Player map location name (via `MapAsset` + `MapAssetName`)
    - Player level and HP (from active clawkin, fallback first party entry)
  - Conditional rendering:
    - If a field is missing, that field is omitted.
    - If all requested details are missing, only base save line is shown.
  - No placeholder like `UNKNOWN` is shown for missing location details in this metadata section.

## Save data currently covered

- Map key + player position
- Player name + gender
- Money + active clawkin index
- Party roster/stats/skills/summary metadata
- Inventory item ids + quantities
- Progress/event stats via flags + protocol payload

## Known extension points for future AI

1. **Nursery multi-event design**
   - Keep nursery out of `SAVE_POINT_OBJECT_IDS`.
   - Register nursery-specific object ids through normal special interaction callbacks.
   - Trigger heal/save/other events based on authored object id or interaction context.

2. **Profile metadata expansion**
   - If more player identity fields are added, mirror in:
     - `SaveState`
     - `SaveStateManager.serialize()/parse()`
     - `GameScreen.buildSaveState()/restorePlayerProfile()`

3. **Protocol versioning**
   - If changing `PlayerProgress` protocol schema, increment key/version and preserve fallback path.

## Files touched by this save/load pass

- `core/src/main/java/github/dluckycompany/clawkins/GameScreen.java`
- `core/src/main/java/github/dluckycompany/clawkins/progress/PlayerProgress.java`
- `core/src/main/java/github/dluckycompany/clawkins/save/SaveState.java`
- `core/src/main/java/github/dluckycompany/clawkins/save/SaveStateManager.java`
- `core/src/main/java/github/dluckycompany/clawkins/ui/SaveStateScreen.java`
