# Cheat Console Quick Reference

## New Cheat Commands

### Speed Toggle

```
speed
```

- First use: 2x game speed
- Second use: Normal speed
- Affects: Movement, animations, encounters
- Does NOT affect: UI, audio, camera

### Map Teleport

```
tp <map>
```

Examples:

```
tp cottage
tp field
tp mountain_3
tp backalley_secret
```

### List All Maps

```
maps
```

Shows all available maps in console log.

## Supported Map Names

### Quick Aliases

- `nursery` → Nurse Interior
- `cottage` → Cottage Sample
- `shop` → Shop Interior
- `mountain` → Mountain 1
- `cave` → Cave Entrance
- `field` → Field
- `mansion` → Mansion Maze
- `backalley` → Back Alley 1

### All Maps (35 total)

**Nursery**: nurse_interior, nurse_interior_2, nurse_interior_3, nurse_interior_4  
**Cottage**: cottage_sample  
**Shop**: shop_interior, shop_interior_2, shop_interior_3  
**Mountain**: mountain_1, mountain_2, mountain_3, mountain_4, mountain_5  
**Cave**: cave_entrance, cave_1, cave_2, cave_3  
**Field**: field, field_2, field_3, field_4, field_5, field_secret  
**Mansion**: mansion_maze, mansion_garden, mansion_exit  
**Back Alley**: backalley_1, backalley_2, backalley_3, backalley_4, backalley_exit, backalley_secret  
**Test**: test_world

## Usage Tips

### Speed Testing

```
> speed          # Enable 2x speed
> tp field       # Teleport to field
> speed          # Return to normal
```

### Map Exploration

```
> maps           # See all maps
> tp cottage     # Go to cottage
> whereami       # Check position
> tp field_3     # Go to field 3
```

### Case & Whitespace

```
TP COTTAGE       # Works (case-insensitive)
tp   field       # Works (whitespace-tolerant)
```

## Error Messages

**Invalid Map:**

```
> tp invalidmap
Unknown map: invalidmap (type 'maps' for list)
```

**Empty Command:**

```
> tp
Usage: tp <map> (e.g., tp cottage)
```

## Technical Details

### Teleport Behavior

- Removes all map entities (except player)
- Loads new TMX map
- Spawns NPCs, enemies, interactables
- Rebuilds collisions
- Updates camera bounds
- Updates audio (plays map music)
- Shows area title
- Places player at spawn zone or map center

### Speed Multiplier

- Applied to engine update delta
- Affects all ECS systems uniformly
- No permanent entity state changes
- Toggle on/off anytime

## Debug Logs

Enable console logging to see:

```
[Cheat] Teleporting to map: COTTAGE_SAMPLE
[Cheat] Processing teleport to: COTTAGE_SAMPLE
[GameScreen] Removed 15 map-scoped entities
[Cheat] Teleported to spawn zone in COTTAGE_SAMPLE
[Cheat] Teleport complete
```

```
[Cheat] 2x speed enabled
[Cheat] Speed returned to normal
```
