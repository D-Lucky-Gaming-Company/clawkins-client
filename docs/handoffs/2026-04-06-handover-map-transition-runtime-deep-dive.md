# Map Transition Runtime Deep Dive (For Future AI/Developers)

## Why this handoff exists
This file documents the exact contract and implementation expectations for rectangle-based map transitions.

Current authoring has been decided. Runtime implementation is still pending.

## Final authoring model (already decided)
- Transition zones are rectangle objects in Tiled objects layer.
- One object serves as both:
  - Trigger source
  - Destination anchor
- No separate SpawnPoint object.
- Rectangle bounds are the trigger area.

## Property contract to consume at runtime
On each MAP_TRANSITION rectangle object:
- ObjectType = MAP_TRANSITION
- transitionId (string)             # this object's anchor id
- targetMap (string)                # destination map key
- targetTransitionId (string)       # destination anchor id in target map
- requiredFacing (enum)             # ANY/NORTH/SOUTH/EAST/WEST
- cooldownMs (int)                  # anti-loop lockout window
- notes (string)                    # editor note only

## Current project constraints you must account for
1. Map registry currently has only MAIN in MapAsset.
2. TiledService currently throws for non-TiledMapTileMapObject in objects layer.
3. Transition zones are now rectangle objects, so parser must support RectangleMapObject.

## Files that matter
- core/src/main/java/github/kinuseka/testproject/asset/MapAsset.java
- core/src/main/java/github/kinuseka/testproject/tiled/TiledService.java
- core/src/main/java/github/kinuseka/testproject/tiled/TiledObjectConfigurator.java
- core/src/main/java/github/kinuseka/testproject/GameScreen.java
- core/src/main/java/github/kinuseka/testproject/system/MoveSystem.java
- core/src/main/java/github/kinuseka/testproject/system/PlayerInputSystem.java
- core/src/main/java/github/kinuseka/testproject/component/PlayerAnimation.java
- core/src/main/java/github/kinuseka/testproject/encounter/EncounterDetectionSystem.java

## Recommended runtime architecture

### 1) Add map keys first
Expand MapAsset with all destination maps used by transitions:
- MAIN("main.tmx")
- NURSE_INTERIOR("nurse_interior.tmx")
- COTTAGE_SAMPLE("cottage_sample.tmx")

targetMap values in Tiled should match these map keys exactly.

### 2) Parse rectangle transition objects from objects layer
TiledService should not throw on rectangle objects in objects layer.

Approach:
- Keep tile object handling as-is.
- Add rectangle object handling branch for RectangleMapObject.
- Dispatch rectangle transition objects to configurator.

### 3) Add a transition component
Create component example: MapTransitionZone
Fields:
- Rectangle worldBounds
- String transitionId
- String targetMap
- String targetTransitionId
- FacingRequirement requiredFacing
- int cooldownMs

### 4) Configure transition entities in TiledObjectConfigurator
When object ObjectType is MAP_TRANSITION:
- Read properties from rectangle object
- Convert rectangle pixel bounds to world units using Main.UNIT_SCALE
- Create lightweight entity with:
  - Transform optional (for debug)
  - MapTransitionZone required

### 5) Add detection system
Create MapTransitionDetectionSystem similar to EncounterDetectionSystem:
- Query player entity + transition entities
- Build player feet/body probe hitbox
- Check overlap with transition rectangle bounds
- Ensure enter-once semantics (do not fire every frame while standing)
- Validate requiredFacing against PlayerAnimation direction
- Publish transition request event

### 6) Add transition coordinator/service
In GameScreen, own a coordinator that:
- Receives transition request
- Temporarily locks exploration systems/input
- Applies cooldown lock
- Optionally runs fade out/in overlay
- Loads target map via TiledService
- Finds destination transition object by targetTransitionId in loaded map
- Repositions player to destination anchor center/bottom alignment
- Restores systems/input

### 7) Map-scoped entity cleanup is mandatory
Before spawning objects for the new map, clear map-scoped entities.
Persistent entities (player, persistent UI state, battle state holders) must survive.

Do not duplicate player entity on map change.

### 8) Destination lookup rules
Lookup key:
- map = targetMap
- id = targetTransitionId

Uniqueness policy:
- transitionId must be unique inside each map.

### 9) Safety behavior
- If target map key invalid: log error and abort transition.
- If targetTransitionId missing in destination map: log error and abort transition.
- Keep player at original location on failure.

### 10) Cooldown behavior
Use cooldownMs to block immediate retrigger after spawn.
Suggested default: 250ms.

### 11) Facing requirement behavior
requiredFacing values:
- ANY allows all
- NORTH/SOUTH/EAST/WEST only allow matching player facing

Mapping should match PlayerAnimation direction enum.

## Suggested implementation order
1. Expand MapAsset.
2. Add rectangle parsing path in TiledService.
3. Add transition component + configurator mapping.
4. Add detection system with enter-once semantics.
5. Add GameScreen coordinator for map swap + relocation.
6. Add cooldown + requiredFacing checks.
7. Add logging and error handling.

## Acceptance checklist
- Main to nurse transition works.
- Nurse back to main works.
- Main to cottage works.
- No duplicate player spawn.
- No immediate bounce back loop.
- Invalid targetMap fails safely.
- Missing targetTransitionId fails safely.
- requiredFacing gates correctly.

## Notes for future AI agents
- Do not reintroduce SpawnPoint unless explicitly requested.
- Do not reintroduce triggerWidth/triggerHeight properties.
- Rectangle bounds are authoritative trigger shape.
- Keep map key casing exact to MapAsset enum keys.
