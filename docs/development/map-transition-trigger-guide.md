# Map Transition Trigger Guide (Beginner Friendly)

## What this system is
A map transition trigger is a rectangle object placed on a map that can do two jobs:
- Trigger a map change when the player enters it
- Act as a destination anchor for incoming transitions from another map

This project uses one object type for both source and destination, which keeps authoring simple.

## Very short answer first
- Yes, targetMap means the next map to load.
- targetMap should match the map key used by runtime, not the tileset filename.
- Two triggers should not share the same transitionId on the same map.
- The same transitionId on different maps can work if lookup is map-scoped, but unique naming is strongly recommended to avoid mistakes.

## Where to place it in Tiled
1. Open a map: main.tmx, nurse_interior.tmx, or cottage_sample.tmx.
2. Select the objects layer.
3. Insert a rectangle object and size it to the trigger area.
4. Set class/type to MapTransitionTrigger.
5. Fill its properties.

Rectangle size itself is the trigger area. No extra width or height trigger properties are needed.

## Property reference (every field explained)

### ObjectType
- Type: enum (ObjectType)
- Expected value: MAP_TRANSITION
- Purpose: tells runtime this object is a transition zone.

### transitionId
- Type: string
- Required: yes
- Purpose: this object's unique anchor id on its current map.
- Used for: landing destination lookup from another map.
- Example: main_door_to_nurse_outside

### targetMap
- Type: string
- Required: yes
- Purpose: destination map key.
- Meaning: the map runtime will load next.
- Important: this should match your runtime map key (for example enum key MAIN), not a tileset name.
- Current runtime note: MapAsset currently only has MAIN, so additional maps must be added to MapAsset before runtime can switch to them.

### targetTransitionId
- Type: string
- Required: yes
- Purpose: transitionId of the destination trigger object in targetMap.
- Meaning: where the player should appear after map change.
- Example: nurse_from_main_door

### requiredFacing
- Type: enum (FacingRequirement)
- Values: ANY, NORTH, SOUTH, EAST, WEST
- Required: no (default ANY)
- Purpose: optional direction gate.
- Example behavior: if set to NORTH, transition only fires while player is facing north.

### cooldownMs
- Type: int
- Recommended: 200 to 500
- Purpose: prevents immediate re-trigger loops after teleport.
- Example: set 250 for doorways.

### notes
- Type: string
- Purpose: human notes only.

## ID rules (important)

### Rule 1: transitionId must be unique inside one map
Do not reuse transitionId twice in the same map, even if targetMap differs.

Why: destination lookup by id on the destination map becomes ambiguous if duplicates exist.

### Rule 2: cross-map duplicates are technically possible but discouraged
Example:
- main.tmx has transitionId = door_01
- nurse_interior.tmx also has transitionId = door_01

This can still work if runtime always scopes lookup by targetMap first, then id.

Recommendation: use map-prefixed ids so debugging is easier.
- main_door_north
- nurse_door_from_main
- cottage_exit_south

## How to make a two-way door link
Create one trigger in each map.

### On main.tmx door rectangle
- transitionId: main_to_nurse
- targetMap: NURSE_INTERIOR
- targetTransitionId: nurse_from_main
- requiredFacing: NORTH

### On nurse_interior.tmx door rectangle
- transitionId: nurse_from_main
- targetMap: MAIN
- targetTransitionId: main_to_nurse
- requiredFacing: SOUTH

Result:
- Entering the main door sends player to nurse interior anchor
- Exiting interior sends player back to matching main anchor

## Common mistakes and fixes

### Mistake: using map filename or tileset name in targetMap
- Wrong: nurse_interior.tmx, house.tsx
- Right: runtime map key (for example NURSE_INTERIOR)

### Mistake: duplicate transitionId in same map
- Fix: rename one id and update any targetTransitionId references to match.

### Mistake: trigger never fires
Checklist:
1. Object is on objects layer
2. ObjectType is MAP_TRANSITION
3. Runtime supports rectangle objects (not only tile objects)
4. requiredFacing is not blocking you
5. Player overlap is actually entering rectangle bounds

### Mistake: teleports to wrong place
Checklist:
1. targetMap exists in runtime map registry
2. targetTransitionId exists in destination map
3. transitionId is unique on destination map

## Naming convention that scales
Use this pattern for transitionId:
map_from_to_context_direction

Examples:
- main_to_nurse_frontdoor
- nurse_to_main_frontdoor
- main_to_cottage_westpath
- cottage_to_main_westpath

## Quick FAQ

### Is targetMap the next map it is targeting?
Yes. It is the destination map to load.

### Can two triggers have the same transitionId if targetMap is different?
Not on the same map. On different maps it can work, but avoid it. Prefer unique ids for clarity and safer debugging.

### Do I still need a separate SpawnPoint object?
No. This project uses one transition object as both trigger and destination anchor.

### Do I need triggerWidth and triggerHeight properties?
No. Rectangle object size is the trigger area.
