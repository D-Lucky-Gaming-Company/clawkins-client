# TestGame Development Guide

This document explains how to use Tiled and the existing ECS/battle boilerplate to add things like hostiles, interactables, and items.

Stat-scale skill formulas are documented separately in:

- `docs/development/skill-stat-scale-guide.md`

---

## 1. Map & Layers Overview

Current assumptions:

- Main map: `assets/maps/main.tmx`
- Important layers:
  - `ground` / `background` / `foreground`: visual tiles
  - `objects`: gameplay objects (player, enemies, triggers, etc.)
- Collision:
  - Per-tile collision objects (e.g. fences) and map borders are interpreted by `MoveSystem`.

When in doubt, put gameplay-related objects (things the player collides or interacts with) into the `objects` layer.

---

## 2. Player Spawn (for reference)

The player is spawned from a tile object in the `objects` layer:

- Layer: `objects`
- Object type: **Tile Object** (inserted from a tileset)
- Custom property `ObjectType`: `PLAYER`
- Custom property class `properties` (type `Player`) is supported for player-side values (for example `moveSpeed`, `playerHp`, `playerAttack`, `playerDefense`, `playerSpeed`, player skills).

At runtime:

- `TiledService` parses the `"objects"` layer.
- `TiledObjectConfigurator.onLoadObject(...)`:
  - Creates an ECS entity.
  - Adds:
    - `Transform`
    - `Graphic`
    - `Tiled` (back-reference to Tiled object)
    - `Player`
    - `CameraFollow`
    - `Move`
    - `PlayerAnimation`

Use this as the baseline pattern for other object types.

### 2.1 Spawn alignment behavior

- Tile object spawn position now matches Tiled coordinates directly.
- Runtime no longer applies a forced global `y -= height` offset during object spawn.
- If any existing object was previously hand-adjusted to compensate for old offset behavior, reposition it in Tiled once.

---

## 3. Hostile / Encounter Objects

Hostile encounters are currently set up as **encounter triggers**. They don’t yet switch to a full battle screen, but the boilerplate is wired all the way up to an active `BattleStateMachine`.

### 3.1. Tiled setup

1. Open `assets/maps/main.tmx` in Tiled.
2. Select the `objects` layer.
3. In the Tilesets panel, pick a tile that should visually represent the enemy or encounter.
4. Use **Insert Tile** tool and click to place a tile object on the map.
5. With the new object selected, set:
   - `ObjectType` = `ENEMY`
6. Put encounter/battle values inside class property:
   - `properties` (type `Enemy`)
6. (Optional but recommended) Add custom properties:
  - `encounterId` (string)
     - Example: `slime_01`
    - Default: `"enemy"` if omitted.
  - `enemyName` (string)
    - Example: `Green Slime`
    - Used as display name for dialogue placeholder lookup by `{encounterId}` token.
    - Default fallback order: `enemyName` -> `Name` -> `encounterId`.
   - `encounterTableId` (string)
     - Example: `field_slimes`, `boss_01`
     - Default: `"default"` if omitted.
   - `oneShot` (bool)
     - Example: `true` to allow only a single trigger.
   - Enemy battle stats (int):
     - `enemyHp` (default `40`)
     - `enemyAttack` (default `8`)
     - `enemyDefense` (default `3`)
     - `enemySpeed` (default `6`)
   - Enemy skills (3 attack types):
     - `enemySkill1Name` (default `"Bite"`), `enemySkill1Power` (default `8`)
     - `enemySkill2Name` (default `"Claw Swipe"`), `enemySkill2Power` (default `10`)
     - `enemySkill3Name` (default `"Rend"`), `enemySkill3Power` (default `12`)

Player battle stats and player skills do not belong to `Enemy`. Configure them
on the `PLAYER` object using `properties` (type `Player`) instead.

Save the map.

### 3.2. Runtime mapping

Relevant classes:

- `TiledObjectConfigurator`
- `EncounterTrigger`
- `EncounterZone`
- `EncounterDetectionSystem`
- `EncounterEventBus`
- `BattleService`
- `BattleStateMachine`

Flow:

1. `TiledService` reads `"objects"` layer and calls `TiledObjectConfigurator.onLoadObject(...)`.
2. `TiledObjectConfigurator`:
   - Always adds `Transform`, `Graphic`, `Tiled`.
   - For `ObjectType == ENEMY`:
     - Reads properties (from nested class property `properties`, with direct-property fallback):
       - `encounterId` (default: `"enemy"`)
       - `enemyName` (fallback: `Name`, then `encounterId`)
       - `encounterTableId` (default: `"default"`)
       - `oneShot` (default: `false`)
     - Adds:
       - `EncounterTrigger`
       - `EncounterZone`
3. `EncounterDetectionSystem`:
   - Looks for:
     - Player entities: `[Player + Transform]`
     - Encounter entities: `[EncounterTrigger + EncounterZone + Transform]`
   - Every frame:
     - Computes a **feet probe point** at the bottom-center of the player sprite.
     - Checks if that point lies within an encounter entity’s bounds.
     - On **overlap-enter** (first time inside a given encounter):
       - Publishes an `EncounterEvent` with:
         - `type = START_ENCOUNTER`
         - `encounterId`, `encounterTableId` from `EncounterZone`.
       - If `oneShot` is true:
         - The encounter entity is removed from the engine.
4. `BattleService`:
   - Reads events from `EncounterEventBus`.
   - When it sees `START_ENCOUNTER` and no battle is active:
     - Builds enemy stats + enemy skills from the encounter object (`Enemy` class).
     - Builds player combat data from persistent player state initialized from
       the `PLAYER` object (`Player` class).
     - Calls `BattleStateMachine.begin(context)`.
   - Calls `battleStateMachine.tick(delta)` each frame.
   - On session close, player HP is written back from battle result and persists
     for subsequent battles.
5. `BattleStateMachine`:
   - During enemy turn, picks one enemy skill randomly and applies skill power
     against player defense.

At this stage:

- A lightweight battle overlay UI exists.
- Encounter triggers from the world start a battle session.
- Player can currently:
  - Press `1` / `2` / `3` to use Skill 1/2/3
  - Press `Z` / `SPACE` / `ENTER` to quickly use Skill 1
  - Press `R` to `ESCAPE`
- After victory/defeat/escape:
  - Press `Z` / `SPACE` / `ENTER` to return to exploration.

---

## 4. Interactable (Non-Hostile) Objects

Interactables are not fully implemented yet, but you can follow a similar pattern.

### 4.1. Recommended typing in Tiled

1. Create a tile object in the `objects` layer.
2. Set `ObjectType` for gameplay intent (current runtime handles `PLAYER`, `ENEMY`, `INTERACTIBLE`, `PROP`).
3. `PROP` is currently reserved/no-op by runtime and safe to keep for future use.
3. Add custom properties, for example:
   - `interactionId` (string)
     - Example: `npc_town_guard_01`, `chest_tutorial_01`
   - `scriptId` or `dialogId` (string)
     - Example: `conv_guard_intro`, `conv_chest_hint`

### 4.2. Runtime hook points

- Extend `TiledObjectConfigurator.configureByType(...)`:
  - For each new object type (`"NPC"`, `"Chest"`, etc.):
    - Add a new ECS component (e.g. `Npc`, `Chest`).
    - Store any IDs from Tiled properties on that component.
- Add a new system such as:
  - `InteractionSystem`
    - Detects when the player is within range and presses an interaction key (e.g. `E`).
    - Resolves the correct interaction based on component data (`interactionId`, `scriptId`, etc.).

This keeps Tiled as the source of truth for where interactables live and what they represent, while ECS + systems define how they behave.

---

## 5. Items and Pickups

There is no item inventory pipeline implemented yet in TestGame, but you can prepare Tiled data for it.

### 5.1. Tiled-side setup

To define item pickups:

1. Add a tile object to `objects` layer.
2. Set `ObjectType` and class properties according to your new pickup contract (example future enum value).
3. Add properties:
   - `itemId` (string)
     - Example: `potion_small`, `potion_large`, `sword_iron`
   - `quantity` (int)
     - Example: `1`, `3`, etc.
   - `oneShot` (bool)
     - Example: `true` if it disappears after being picked up.

### 5.2. Runtime hook points

Planned pattern:

- Add a new component:
  - `ItemPickup`
    - fields: `itemId`, `quantity`, `oneShot`
- Extend `TiledObjectConfigurator.configureByType(...)` with your chosen `ObjectType` handling:
  - add the ECS component (e.g. `ItemPickup`) after reading class properties.
- Add a `PickupSystem`:
  - Detects overlap between player and `[ItemPickup + Transform]` entities.
  - On overlap + interaction key:
    - Calls into an `InventoryService` to add the item.
    - Removes or hides the pickup entity if `oneShot`.

---

## 6. Collision & Movement Notes for Designers

### 6.1. Map borders

- Map bounds come directly from:
  - `width`, `height` in `.tmx` + `tilewidth`, `tileheight`.
- `MoveSystem.setMap(...)` computes world width/height and clamps movement accordingly.
- No extra border layer is required; just ensure your map size is correct.

### 6.2. Solid vs. visual-only tiles

- `MoveSystem` inspects tile collision objects to decide blocking.
- Best practice:
  - Add collision rectangles only to tiles that should block.
  - Keep pure decorations collision-free.
  - Put decorative foreground tiles in layers after `objects` (they don’t block).

If collision looks “too thick” or “too thin”, developers can adjust:

- `HITBOX_WIDTH_FACTOR`
- `HITBOX_HEIGHT_FACTOR`

in `MoveSystem` to refine where the player collides.

---

## 7. How to Extend Safely

When adding new map-driven features:

1. Decide the Tiled representation:
   - Layer (`objects` or dedicated layer).
   - Object type (tile vs. rectangle).
   - `ObjectType` enum value.
   - Custom properties (IDs, flags, etc.).
2. Extend `TiledObjectConfigurator.configureByType(...)`:
   - Add a `case` for the new `ObjectType`.
   - Attach one or more ECS components that represent the feature.
3. Add or extend systems:
   - New system for new gameplay behavior.
   - Or extend existing ones (e.g. encounter, interaction).
4. Keep logic out of Tiled:
   - Use Tiled only for configuration/data.
   - Use Java code for behavior.

This keeps designers in Tiled and developers in code, with a clean, predictable bridge between the two. 

---

## 8. Asset Modification Guide

### 8.1. Character directional visuals

- Current player sheet does not provide a dedicated left-facing row.
- Runtime behavior mirrors the current frame horizontally when moving west:
  - Implemented in `AnimationSystem`.
- If you later add true left-facing frames, remove the mirror logic and map west to its own row in `PlayerAnimationFactory`.

### 8.2. Water/fence collision authoring

- Tile collision comes from collision objects inside `.tsx` tilesets.
- Keep collision boxes grid-aligned (integers) when possible to avoid irregular edges.
- For animated tiles, ensure all visual variants in the set use consistent collision coverage.
- If collision still feels off, tune player feet hitbox in `MoveSystem`:
  - `HITBOX_WIDTH_FACTOR`
  - `HITBOX_HEIGHT_FACTOR`

### 8.3. Safe asset update checklist

1. Update `.tmx`/`.tsx` in Tiled.
2. Save and re-open map once to confirm object names/properties persisted.
3. Run game and validate:
   - movement bounds
   - collision edges
   - trigger overlap points
4. If behavior changed unexpectedly, check:
   - layer order (before/after `objects`)
   - object `ObjectType`
   - custom property spelling

---

## 9. Battle Balancing Guide (Current Prototype)

The prototype battle resolver uses a simple deterministic formula:

- Player skill damage: `max(1, skill.power - enemy.defense)`
- Enemy skill damage: `max(1, enemySkill.power - player.defense)`
  - Fallback: `max(1, enemy.attack - player.defense)` when no enemy skill is available.

### 9.1. Balance knobs

From Tiled custom properties:

- Enemy:
  - `enemyHp`, `enemyAttack`, `enemyDefense`, `enemySpeed`
- Player:
  - Read from `PLAYER` object `properties` (type `Player`)
  - `playerHp`, `playerAttack`, `playerDefense`, `playerSpeed`
- Player skills:
  - Read from `PLAYER` object `properties` (type `Player`)
  - `playerSkill1Name`/`playerSkill1Power`
  - `playerSkill2Name`/`playerSkill2Power`
  - `playerSkill3Name`/`playerSkill3Power`
- Enemy skills:
  - Read from encounter object `properties` (type `Enemy`)
  - `enemySkill1Name`/`enemySkill1Power`
  - `enemySkill2Name`/`enemySkill2Power`
  - `enemySkill3Name`/`enemySkill3Power`

Fallback defaults are applied automatically when properties are missing.
Current HP persists between battles for the current game session.

Adjust these first before changing formulas.

### 9.2. Recommended balancing approach

1. Pick a target battle length (example: 3-5 player turns for standard enemies).
2. Tune enemy HP first.
3. Tune enemy ATK for acceptable incoming pressure.
4. Keep `min damage = 1` unless you intentionally allow stalemates.
5. Add crit/status variance only after baseline pacing feels good.

---

## 10. Clawkin System

Clawkins are party members that fight alongside the player in battle. Think of them like Pokémon — the player has up to 3, each with their own stats and skills. This section explains everything about how they are defined, how they are authored in Tiled, and how the game loads them.

---

### 10.1 What is the Clawkin class?

`Clawkin` is a **Tiled class** defined in `assets/maps/test.tiled-project`. It is completely independent — it is not embedded inside the `Player` class or the `Enemy` class. It has its own set of fields (stats and skills) that you configure per-instance.

Because it is a standalone class, you can assign it as a property to **any object on the map** — currently only the `PLAYER` object uses it, but the data shape exists for any future use (enemy parties, NPC companions, etc.).

---

### 10.2 What fields does a Clawkin have?

When you add a Clawkin property to an object in Tiled, it gives you these fields:

**Identity:**
- `id` (string) — a unique machine identifier, like `clawkin_sweepea`. Used internally to tell clawkins apart.
- `name` (string) — the display name shown in the UI, like `Swee'pea`.

**Stats:**
- `level` (int) — current level. Default: `1`. Not used in combat calculations yet, reserved for growth systems.
- `hp` (int) — max HP. Default: `50`.
- `attack` (int) — base attack stat. Default: `8`.
- `defense` (int) — base defense stat. Default: `4`.
- `speed` (int) — base speed stat. Default: `6`.

**Skills (3 slots):**

Each skill slot has 6 fields. Replace `{n}` with `1`, `2`, or `3`:

| Field name                  | Type   | Description                                    |
|-----------------------------|--------|------------------------------------------------|
| `skill{n}Name`              | string | Display name of the skill. Empty = no skill.   |
| `skill{n}Power`             | int    | Base power of the skill.                       |
| `skill{n}EffectType`        | string | Status effect type (reserved, not used yet).   |
| `skill{n}EffectStat`        | string | Which stat the effect targets (reserved).      |
| `skill{n}EffectAmount`      | int    | Magnitude of the effect (reserved).            |
| `skill{n}EffectDurationTurns` | int  | How many turns the effect lasts (reserved).    |

For the current prototype, **only `skill{n}Name` and `skill{n}Power` are actively used in combat**. The `EffectType`, `EffectStat`, `EffectAmount`, and `EffectDurationTurns` fields are parsed and logged but not yet wired to combat logic.

---

### 10.3 How to add Clawkins to the Player in Tiled

This is a step-by-step walkthrough for someone completely new to this:

1. Open `assets/maps/main.tmx` (or any map with a Player object) in Tiled.
2. Make sure `test.tiled-project` is loaded as the active Tiled project — this makes the `Clawkin` class available as a type.
3. Click the `PLAYER` object on the map to select it.
4. In the Properties panel (usually bottom-left), click the **+ (Add Property)** button.
5. A dialog will appear asking for a name and type:
   - **Name:** `clawkin1`
   - **Type:** select `class`, then choose `Clawkin` from the dropdown.
   - Click OK.
6. A `clawkin1` group will appear in the properties panel. Expand it.
7. Fill in at minimum:
   - `id` — example: `clawkin_sweepea`
   - `name` — example: `Swee'pea`
   - `hp`, `attack`, `defense`, `speed` — as desired
   - `skill1Name`, `skill1Power` — example: `Leaf Cut`, `8`
8. To add more party members, repeat from step 4 using `clawkin2` and `clawkin3` as names.
9. Save the map (`Ctrl+S`).

**Important rules:**
- The property names **must** be exactly `clawkin1`, `clawkin2`, `clawkin3`. These are the exact strings the Java code searches for.
- Slots can be omitted. If you only add `clawkin1`, the party will have 1 member. The loader silently skips any slot that is absent or has a blank `name` and `id`.
- The maximum supported party size is **3**. Adding a 4th slot will not crash anything — it simply won't be loaded.
- If you add **no** clawkin properties at all, the game falls back to a hardcoded starter party (Swee'pea, Ginger, Dart). This is a safety net so the game never runs with an empty party by accident.

---

### 10.4 How the game loads Clawkins at runtime

This is what happens behind the scenes when the map loads:

1. `TiledService` reads all objects from the `objects` layer.
2. For each object, `TiledObjectConfigurator.onLoadObject(...)` is called.
3. When the object has `ObjectType = PLAYER`, the configurator runs `loadConfiguredPlayerClawkins(...)`.
4. That method loops through slots 1, 2, and 3. For each slot, it calls `buildClawkinFromSlot(...)`.
5. `buildClawkinFromSlot(...)` calls `getNestedClawkinProperties(...)`, which navigates the in-memory property tree:
   - First retrieves the `Player` class properties (stored under the key `"properties"` by libGDX's TMX loader).
   - Then retrieves the `"clawkin1"` (or `"clawkin2"`, `"clawkin3"`) property from within that, which is itself a `MapProperties` object.
6. If the `MapProperties` is null (slot absent) or both `name` and `id` are blank, the slot is skipped.
7. Otherwise, a `Clawkin` Java object is constructed with the values and added to `playerBattleState` via `addClawkinToParty(...)`.

The `Clawkin` Java class lives at:
```
core/src/main/java/github/kinuseka/testproject/character/Clawkin.java
```

It stores id, name, level, maxHp, currentHp, baseAttack, baseDefense, baseSpeed, and supports stat boosts (temporary modifiers used in battle).

---

### 10.5 Where Clawkins appear in the UI

Once loaded into `PlayerBattleState`, the clawkin party is accessible everywhere that `PlayerBattleState` is passed:

- **Team Viewer** (`P` key): Opens a full-screen viewer showing all 3 party slots. Uses `TeamViewerScreen` and `ClawkinCard`. Empty slots show as ghost cards.
- **Inventory party target dialog**: When using a consumable item on a party member, a `PartySelectionDialog` lists the current clawkins.
- **Battle** (future): Clawkin battle participation logic is not yet wired but the data is available.

---

### 10.6 Clawkin stat boosts (temporary modifiers)

`Clawkin` supports temporary stat boosts used during battle (from skill effects). These are not authored in Tiled — they are applied and removed during a battle session:

- `addStatBoost(StatType, amount, durationTurns)` — adds a temporary modifier.
- `decrementStatBoostTimers()` — should be called each turn end; removes expired boosts.
- `getEffectiveAttack()` / `getEffectiveDefense()` / `getEffectiveSpeed()` — returns base stat + active boost.

The `EffectType`, `EffectStat`, `EffectAmount`, and `EffectDurationTurns` fields on each skill in Tiled are intended to feed this system, but the wiring from skill use → stat boost application is not yet implemented.

---

### 10.7 How to extend the Clawkin system

**Add a new Clawkin field (e.g., `element` type):**
1. Add the member to the `Clawkin` class in `test.tiled-project`.
2. Add the Java field to `Clawkin.java`.
3. In `TiledObjectConfigurator.buildClawkinFromSlot(...)`, read the new field with `getStringFromProps(clawkinProps, "element", "")`.
4. Pass it into the `Clawkin` constructor (add it as a parameter).

**Support more than 3 clawkins:**
1. Increase the loop bound in `loadConfiguredPlayerClawkins` (currently `slot <= 3`).
2. Increase the cap in `PlayerBattleState.addClawkinToParty(...)` (currently `party.size() < 3`).

**Add clawkins to Enemy objects:**
1. Add equivalent loading logic in the `ENEMY` branch of `TiledObjectConfigurator.configureByType(...)`.
2. Decide where to store the loaded party — either inside the `EncounterZone` component or a new component.
3. `EncounterZone` currently holds only the trainer stats; a separate enemy party would likely need its own component.

---

## 11. Feature Implementation Notes (Encounter + Battle)

- Encounter triggers:
  - Produced by map objects with `ObjectType=ENEMY` and `EncounterTrigger` + `EncounterZone`.
- Encounter dedup:
  - `EncounterDetectionSystem` only triggers on overlap-enter, not every frame.
  - `oneShot=true` removes trigger entity after activation.
- Data source:
  - Enemy stats and enemy skills come from `ENEMY` object custom properties.
  - Player combat baselines and player skills come from the `PLAYER` object custom properties.
  - Missing values fall back to placeholder defaults.
  - Player HP persists after battle until recovery systems (items/save points) modify it.
- Exploration lock:
  - During battle session, exploration systems are paused:
    - `PlayerInputSystem`
    - `MoveSystem`
    - `EncounterDetectionSystem`

---

## 12. Audio System Boilerplate

The project now has a centralized audio service scaffold inspired by the manager-style pattern used in `GdxGame`, with map-aware behavior similar to `mystictutorial`.

### 12.1 Core classes

- `audio/AudioService`
  - Central music/sfx manager.
  - Safe when assets are missing (no crash; playback no-ops).
- `audio/MusicTrack`
  - Enum of music channels: `EXPLORATION`, `BATTLE`, `MENU`.
- `audio/SoundEffect`
  - Enum of SFX channels: `CONFIRM`, `CANCEL`, `HIT`, `ENCOUNTER`.
- `audio/AudioEventType`
  - Event triggers: `MAP_CHANGED`, `ENCOUNTER_STARTED`, `BATTLE_STARTED`, `BATTLE_ENDED`, `UI_CONFIRM`.

### 12.2 Where it is wired

- `Main`
  - Creates and owns one `AudioService`.
  - Registers placeholder paths for tracks/sounds.
  - Disposes service on app shutdown.
- `GameScreen`
  - On map load/change: calls `audioService.setMap(map)` and `MAP_CHANGED`.
  - On battle session enter:
    - triggers `ENCOUNTER_STARTED`
    - triggers `BATTLE_STARTED`
  - On battle session exit:
    - triggers `BATTLE_ENDED`.

### 12.3 How map music works

Map music is read from map custom property:

- Property key on `.tmx`: `musicTrack`
- Value: one of `MusicTrack` enum names (`EXPLORATION`, `BATTLE`, `MENU`)
- If missing/invalid: falls back to `EXPLORATION`.

Example map property:

- `musicTrack = EXPLORATION`

### 12.4 How to add music files later

1. Place files in assets, for example:
   - `assets/audio/music/exploration.ogg`
   - `assets/audio/music/battle.ogg`
2. Register paths in `Main.create()`:
   - `audioService.registerMusic(MusicTrack.EXPLORATION, "audio/music/exploration.ogg");`
   - `audioService.registerMusic(MusicTrack.BATTLE, "audio/music/battle.ogg");`
3. Set `musicTrack` in each map `.tmx` as needed.
4. Run and verify map transitions + battle transitions.

### 12.4.1 Custom track names (example: `EXPLORATION_1`, `EXPLORATION_CABIN`)

To add custom named background tracks, do all 3 steps:

1. Add new enum values in `audio/MusicTrack`, for example:
   - `EXPLORATION_1`
   - `EXPLORATION_CABIN`
2. Register each new track path in `Main.create()`:
   - `audioService.registerMusic(MusicTrack.EXPLORATION_1, "audio/music/exploration_1.mp3");`
   - `audioService.registerMusic(MusicTrack.EXPLORATION_CABIN, "audio/music/exploration_cabin.mp3");`
3. Reference the enum name in the map `.tmx` property:
   - `musicTrack = EXPLORATION_1`
   - `musicTrack = EXPLORATION_CABIN`

Notes:

- The `musicTrack` value must exactly match the enum name (case-sensitive before normalization).
- Invalid/missing values safely fall back to `EXPLORATION`.
- You can also trigger a specific custom track from code/event:
  - `audioService.playMusic(MusicTrack.EXPLORATION_CABIN, true);`

### 12.5 How to add sound effects

1. Place files in assets, for example:
   - `assets/audio/sfx/confirm.wav`
   - `assets/audio/sfx/encounter.wav`
2. Register paths in `Main.create()`:
   - `audioService.registerSound(SoundEffect.CONFIRM, "audio/sfx/confirm.wav");`
3. Trigger from code:
   - `audioService.playSound(SoundEffect.CONFIRM);`
   - or event-based: `audioService.onEvent(AudioEventType.UI_CONFIRM);`

### 12.6 Triggering audio from specific events/maps/tiles

- **Events**
  - Battle enter/exit already mapped via `AudioEventType`.
- **Maps**
  - Set `musicTrack` on map root properties.
- **Tiles / objects**
  - Recommended pattern: add custom property on object (e.g. `sfxOnInteract=CONFIRM`), then in your interaction system:
    - read property from the object's `TiledMapTileMapObject`
    - call `audioService.playSound(SoundEffect.valueOf(value))`.

### 12.7 Safety behavior while no assets exist

- If a registered file path does not exist yet, playback is skipped silently.
- This allows feature code to be completed before audio asset delivery.

---

## 13. Battle Phases (BattlePhase)

`BattlePhase` is the high-level state of a single battle session:

- `INIT`
  - Initial state before a `BattleContext` is attached.
  - No UI interaction; transitions to `PLAYER_COMMAND` on `begin(...)`.
- `PLAYER_COMMAND`
  - Waiting for player choice.
  - Input:
    - `1`/`2`/`3` select Skill 1/2/3.
    - `Z`/`SPACE`/`ENTER` quick-use Skill 1.
    - `R` attempts escape.
  - On valid action:
    - If escape → `ESCAPE`.
    - Else → `PLAYER_RESOLVE` (conceptually; implemented inline, then moves to `ENEMY_COMMAND`).
- `PLAYER_RESOLVE`
  - Conceptual “apply player action” phase.
  - In current prototype, this is executed immediately inside `submitPlayerAction(...)`:
    - damage is applied,
    - checks for enemy defeat.
  - If enemy HP reaches 0 → `VICTORY`.
  - Else → `ENEMY_COMMAND`.
- `ENEMY_COMMAND`
  - AI/opponent’s turn owner.
  - Driven from `tick(delta)`; once enemy action is resolved:
    - If player HP reaches 0 → `DEFEAT`.
    - Else → back to `PLAYER_COMMAND`.
- `ENEMY_RESOLVE`
  - Placeholder phase for more complex enemy logic; not explicitly used yet (resolution happens inside `ENEMY_COMMAND` tick).
- `TURN_END`
  - Reserved for future accounting between rounds (status decay, regen, etc.). Not used in the current prototype.
- `VICTORY`
  - Battle end state: player wins.
  - Audio:
    - `AudioEventType.BATTLE_VICTORY` triggers `MusicTrack.VICTORY`.
  - Session stays open so overlay can show final state until player presses confirm.
- `DEFEAT`
  - Battle end state: player loses.
  - Audio:
    - `AudioEventType.BATTLE_DEFEAT` triggers `MusicTrack.DEFEAT`.
  - Session stays open until player presses confirm.
- `ESCAPE`
  - Battle end state: player successfully fled.
  - No distinct audio phase yet (treated as a non-victory/non-defeat end).

Session closure:

- When the player confirms on any end state (`VICTORY` / `DEFEAT` / `ESCAPE`), `BattleService.closeBattleSession()` is called.
- `GameScreen` detects that there is no longer a battle session and fires `AudioEventType.BATTLE_ENDED`, which returns music to map `musicTrack`.

---

## 14. Interactible Objects + Dialogue

`Interactible` objects are now map-driven gameplay objects loaded from Tiled.

### 14.1 Tiled typing and required setup

- Put the object in the `objects` layer.
- Object type should be a Tile Object (same as player/enemy objects).
- Set object custom property:
  - `ObjectType = INTERACTIBLE`
- Put Interactible values in:
  - class property `properties` of type `Interactible`

### 14.2 Supported custom properties

Properties are read from each `Interactible` object:

- `ObjectName` (string)
  - Display title in dialogue box.
  - Default: `"Object"`
- `ObjectId` (string, optional)
  - Identifier used for dialogue placeholders like `{objectID}`.
  - Default: `<normalizedObjectName>_<x>_<y>` when missing/blank.
- `ObjectText` (string)
  - First-time interaction source.
  - Supports two formats:
    - Direct text string
    - Relative `.json` file path under assets (example: `dialogue/interactible-sample.json`)
  - Default: `"..."`.
- `ObjectTextInteracted` (string, optional)
  - Interaction source shown after object has been interacted with once.
  - Supports the same two formats as `ObjectText`.
  - If missing/blank, system keeps showing `ObjectText` every time.
- `hasCollision` (bool or `"True"/"False"` string)
  - Controls whether the interactible blocks movement.
  - Default: `true` when property is missing.
- `DialoguePosition` (string, optional)
  - Allowed values: `TOP`, `BOTTOM` (case-insensitive parse).
  - Default: `BOTTOM` when missing/invalid.

### 14.3 Interaction rules

- Interaction keys:
  - `Z`, `SPACE`, `ENTER`
- Player must be:
  - close enough to the object (interaction range check), and
  - facing toward it (direction dot-product check from `PlayerAnimation.Direction`).

### 14.4 Dialogue behavior

- Press interaction near a valid target:
  - dialogue opens from `ObjectText` source.
- If source is a JSON file:
  - system reads `DialogueFlow` array entries in order.
  - each entry supports fields:
    - `Name`
    - `Text`
  - pressing interaction advances line-by-line.
- If dialogue is already open:
  - first press reveals full current line (typewriter skip),
  - next press moves to the next flow entry,
  - closes only after the last entry.
- Position:
  - top or bottom depending on `DialoguePosition`, default bottom.

#### Dialogue JSON format

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
    },
    {
      "Name": "Name1",
      "Text": "I am some random person, your name is {player} right?"
    }
  ]
}
```

#### Supported placeholders (Name and Text)

- `{this}`
  - Current interactible/event name (`ObjectName`).
- `{objectID}`
  - Current interactible identifier (`ObjectId`).
- `{player}`
  - Player name from `PLAYER` object property `Name`.
- `{someObjectId}`
  - Resolves to the `ObjectName` of the interactible whose `ObjectId` matches the token.
  - Example: `{barrel_1}` resolves to the name of the interactible with `ObjectId=barrel_1`.
- `{someEncounterId}`
  - Resolves to the enemy display name for the `ENEMY` object whose `encounterId` matches the token.
  - Example: `{enemy_01}` resolves to that enemy's `enemyName` (or fallback name).

Notes:

- Placeholders are case-insensitive.
- Alias `{ObjectIdName}` is supported for object ID.
- Unknown object-id/encounter-id tokens remain unchanged in output.
- Placeholders can appear anywhere inside both `Name` and `Text` strings.

### 14.5 Collision behavior for interactibles

- `MoveSystem` now checks map collision **and** solid interactible entities.
- Any interactible with `hasCollision=true` blocks movement.
- `hasCollision=false` allows walk-through.

### 14.6 Main implementation files

- `component/Interactible.java`
- `system/InteractionSystem.java`
- `ui/DialogueOverlay.java`
- `tiled/TiledObjectConfigurator.java` (property parsing and defaults)
- `system/MoveSystem.java` (entity blocking for solid interactibles)
- `GameScreen.java` (system + overlay wiring)
