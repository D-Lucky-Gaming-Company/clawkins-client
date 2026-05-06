# Save/Load State Debugging - Enhanced Logging

## Problem

When loading a save state from a map other than the cottage, the map renders but no characters (player or NPCs) appear. The side menu still works, indicating the game is running but entities aren't spawning.

## Debugging Approach

Added comprehensive logging to trace the save/load process and identify where entity spawning fails.

## Changes Made

### 1. Enhanced `applySaveState()` Logging

**File:** `core/src/main/java/github/dluckycompany/clawkins/GameScreen.java`

Added detailed logging to track:

- Save state details (map, position, party size)
- Map asset resolution
- Entity count before/after removal
- Entity count after map loading
- Player entity detection
- Camera centering

**Key Log Messages:**

```
=== APPLYING SAVE STATE ===
Map: FIELD
Position: (x, y)
Party size: N
Resolved map asset: FIELD
Removed N entities before loading new map
Setting map (this will spawn entities)
After setMap, entity count: N
✓ Player entity found, applying saved position
✓ Camera centered on player
=== SAVE STATE APPLIED ===
```

Or if player not found:

```
✗ CRITICAL: Player entity not found after loading map FIELD
✗ Total entities in engine: N
  Entity 0: EntityClassName
  Entity 1: EntityClassName
  ...
```

### 2. Enhanced `TiledObjectConfigurator` Logging

**File:** `core/src/main/java/github/dluckycompany/clawkins/tiled/TiledObjectConfigurator.java`

Added logging to track:

- Each object being processed
- Existing player count check
- Whether player entity is created or skipped
- All entity spawn/skip decisions

**Key Log Messages:**

```
onLoadObject called for: player (type: player)
Configuring PLAYER entity. Existing player count: 0
Creating new PLAYER entity
✓ Spawned entity: name=player pos=(x, y)
```

Or if skipped:

```
onLoadObject called for: player (type: player)
Configuring PLAYER entity. Existing player count: 1
Skipping duplicate PLAYER entity (map transition)
✗ Skipped entity: name=player
```

## How to Use the Logs

1. **Run the game** with console output visible
2. **Load a save state** from a non-cottage map
3. **Check the console** for the log messages above
4. **Identify the issue:**
   - If `onLoadObject` is never called → TiledService isn't parsing the map
   - If `Existing player count: 1` → Entity removal didn't work
   - If `After setMap, entity count: 0` → No entities are spawning
   - If player entity found but not visible → Rendering issue

## Expected Flow

```
User clicks "Load" on save state
  ↓
GameScreen.applySaveState() called
  ↓
=== APPLYING SAVE STATE ===
Map: FIELD
  ↓
applySaveStateToPlayer() - loads party/inventory
  ↓
Resolved map asset: FIELD
  ↓
engine.removeAllEntities()
Removed N entities before loading new map
  ↓
tiledService.loadMap(FIELD)
tiledService.setMap(loadedMap)
Setting map (this will spawn entities)
  ↓
TiledService.loadMapObjects() called
  ↓
For each object in "objects" layer:
  onLoadObject called for: player (type: player)
  Configuring PLAYER entity. Existing player count: 0
  Creating new PLAYER entity
  ✓ Spawned entity: name=player pos=(x, y)

  onLoadObject called for: npc1 (type: npc)
  ✓ Spawned entity: name=npc1 pos=(x, y)

  ... (more entities)
  ↓
After setMap, entity count: N
  ↓
✓ Player entity found, applying saved position
✓ Camera centered on player
  ↓
=== SAVE STATE APPLIED ===
```

## Potential Issues and Solutions

### Issue 1: `onLoadObject` never called

**Cause:** TiledService isn't finding the "objects" layer
**Solution:** Check map file has an "objects" layer with entities

### Issue 2: `Existing player count: 1`

**Cause:** `engine.removeAllEntities()` didn't remove the player
**Solution:** Ensure removal happens before `setMap()`

### Issue 3: `After setMap, entity count: 0`

**Cause:** No objects in the map's "objects" layer
**Solution:** Check map file has objects defined

### Issue 4: Player found but not visible

**Cause:** Rendering or camera issue
**Solution:** Check camera position and RenderSystem

## Next Steps

After identifying the issue from logs:

1. Fix the root cause
2. Remove debug logging (or reduce to DEBUG level)
3. Test with multiple maps
4. Verify save/load works from both main menu and in-game

## Files Modified

1. **`core/src/main/java/github/dluckycompany/clawkins/GameScreen.java`**
   - Enhanced `applySaveState()` with detailed logging
   - Added entity count tracking
   - Added player entity debugging

2. **`core/src/main/java/github/dluckycompany/clawkins/tiled/TiledObjectConfigurator.java`**
   - Enhanced `onLoadObject()` with entry/exit logging
   - Enhanced `configureByType()` with player count logging
   - Added spawn/skip decision logging
