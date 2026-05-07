# TestGame - Project Handoff

## Purpose
This document is a technical handoff for the current state of the game so future work can build on a shared understanding of how it runs today.

## Current Scope
- Platform: Desktop (`lwjgl3`) via libGDX.
- Architecture: Ashley ECS in `core`.
- Map/content source: Tiled `.tmx` map with tile object spawning.
- Gameplay implemented: one controllable player entity on one map.

## Project Layout
- `core`: game logic (screens, ECS systems, components, map/entity setup).
- `lwjgl3`: desktop launcher and packaging/runtime config.
- `assets`: runtime game assets and Tiled project/maps/tilesets.
- `docs/handoffs`: documentation handoff folder (this file).

## Runtime Boot Flow
1. `lwjgl3` starts `Lwjgl3Launcher`.
2. Launcher creates `Main` (`Game` implementation).
3. `Main.create()` initializes:
   - `SpriteBatch`
   - `OrthographicCamera`
   - `FitViewport` (`WORLD_WIDTH=16`, `WORLD_HEIGHT=9`)
   - `AssetService`
   - `GLProfiler` and `FPSLogger`
4. `Main` registers `GameScreen` and sets it active.
5. `GameScreen.show()` loads map `MapAsset.MAIN` and sets it into `TiledService`.

## ECS Systems and Update Order
`GameScreen` creates one Ashley `Engine` and registers systems in this order:

1. `PlayerInputSystem` (priority 0)  
   Reads WASD/arrow input, writes movement direction, updates animation state, clamps position to world bounds.
2. `AnimationSystem` (priority 1)  
   Advances `PlayerAnimation`, writes resulting frame to `Graphic`.
3. `MoveSystem`  
   Applies `direction * speed * delta` to `Transform.position`.
4. `CameraSystem`  
   Smoothly follows entity with `CameraFollow`, clamps to map dimensions.
5. `RenderSystem`  
   Draws tiled layers + entities sorted by transform.

Per-frame, `GameScreen.render(delta)` caps `delta` to `1/30f` and calls `engine.update(delta)`.

## Entity and Component Model
Core components:
- `Transform`: position/size/scaling/rotation/z; sortable by `z -> y -> x`.
- `Graphic`: texture region and tint.
- `Move`: speed and normalized direction.
- `Player`: tag component.
- `CameraFollow`: tag component.
- `PlayerAnimation`: direction, moving flag, state time, directional animations.
- `Tiled`: source map object reference and object id.

Current object-to-entity mapping:
- Tiled object named `"Player"` receives:
  - `Player`
  - `CameraFollow`
  - `Move(3f)`
  - `PlayerAnimation` from `PlayerAnimationFactory`
  - plus base `Transform`, `Graphic`, `Tiled`.

## Tiled Map Integration
### Current Authoring Convention (Important)
- Transition zones are authored as rectangle objects in Tiled, not object templates.
- Rectangle size is intentionally dynamic per use-case (doors, room bounds, edge strips).
- Future transition runtime work should read rectangle objects and their custom properties from the `objects` layer.

### Services
- `TiledService`
  - Loads map via `AssetService`.
  - On `setMap(...)`, parses map layers.
  - Dispatches objects from `"objects"` layer to `loadObjectConsumer`.
  - Notifies `mapChangeConsumer` for systems needing current map.
- `TiledObjectConfigurator`
  - Creates entity per `TiledMapTileMapObject`.
  - Converts tile object pixel coordinates to world units using `Main.UNIT_SCALE`.
  - Adds base and object-specific components.

### Map/Layers
`assets/maps/main.tmx` contains:
- Tile layers: `ground`, `background`, `foreground`.
- Object layer: `objects`.
- A single object currently: `name="Player"` (spawn source).

### Render Layer Behavior
`RenderSystem.setMap(...)` splits map layers:
- Layers before `"objects"` -> background list.
- Layers after `"objects"` -> foreground list.
- Layer named `"objects"` itself is not rendered.

## Asset Pipeline
- `AssetService` wraps `AssetManager`.
- Custom map loader configured for `TiledMap` via `TmxMapLoader`.
- Enumerated assets:
  - `MapAsset.MAIN` -> `maps/main.tmx`
  - `TextureAsset.PLAYER` -> `maps/characters/player.png`
- `build.gradle` includes `generateAssetList` task, generating `assets/assets.txt`.

## Controls
- Movement input:
  - Up: `W` or Up Arrow
  - Down: `S` or Down Arrow
  - Left: `A` or Left Arrow
  - Right: `D` or Right Arrow
- Diagonal movement is normalized.

## Camera and World Bounds
- World constants:
  - `WORLD_WIDTH = 16f`
  - `WORLD_HEIGHT = 9f`
  - `UNIT_SCALE = 1f / 16f`
- Camera follows `CameraFollow` entity with lerp smoothing factor `4f`.
- Camera clamps to map extents computed from map width/height and tile size.

## Build and Run
- Java toolchain target: 21.
- Gradle wrapper: 9.4.0.
- Run game:
  - `./gradlew lwjgl3:run` (or `gradlew.bat lwjgl3:run` on Windows)
- Build runnable jar:
  - `./gradlew lwjgl3:jar`

## Known Observations / Risks
1. `PlayerAnimationFactory` maps both west and east to row `1`:
   - `ROW_WEST = 1`
   - `ROW_EAST = 1`
   This means east/west currently share the same row.
2. Some comments are stale relative to implementation:
   - `TextureAsset` comment describes 16x16 frame assumptions, but `SpriteSheet` uses 48x48 and 6x10 layout.
   - A factory comment references 16x16/18x30 style slicing while actual code uses `SpriteSheet` constants.
3. Position clamping occurs before movement each tick (`PlayerInputSystem`), not after movement, so a fast move step can briefly place position past bounds before next frame clamp.
4. `TiledService` currently throws for non-`TiledMapTileMapObject` objects in `"objects"` layer.
  This now conflicts with the new transition authoring convention (rectangle transition zones), so runtime parsing must be expanded before transition zones can be consumed.

## Extension Guide (How to Add Features Safely)
### Clawkin System (2026-04-06)
- `Clawkin` is a **standalone Tiled class** (id `10`) in `test.tiled-project`. Completely independent of `Player` — the Player class has no clawkin members.
- Clawkin fields: `id`, `name`, `image_clawkin` (asset-relative portrait path, e.g. `ui/Clawkin_01.png`), `level`, `hp`, `attack`, `defense`, `speed`, and three skill slots (name/power/effectType/effectStat/effectAmount/effectDurationTurns).
- To assign clawkins to a Player object in Tiled: add `clawkin1`, `clawkin2`, `clawkin3` as custom properties of type `Clawkin` on the object instance. Slots are optional — absent slots are silently skipped.
- **No fallback party.** If no clawkin properties are authored on the Player object, the party is empty. An empty party entering an encounter immediately triggers defeat.
- TMX storage: each `clawkin{n}` is a nested `<property type="class" propertytype="Clawkin">` inside the Player's `properties` block. libGDX reads it as `MapProperties` under key `"clawkin{n}"`.
- Runtime loader: `TiledObjectConfigurator.buildClawkinFromSlot(...)` reads from the nested `MapProperties` via `getNestedClawkinProperties(...)`.
- Party cap: hard cap of 3 in `PlayerBattleState.addClawkinToParty(...)`.

### Clawkin-Driven Battle System (2026-04-06)
- The player-side battle unit is built from the **active clawkin's stats** (`currentHp`, `baseAttack`, `baseDefense`, `baseSpeed`), not from the player character stats.
- `PlayerBattleState` tracks `activeClawkinIndex` (the slot currently fighting). Set at battle start to the first alive clawkin; reset to -1 on session close.
- **No alive clawkins at battle start** → immediate `DEFEAT` without entering a real battle loop.
- **Active clawkin faints mid-battle** (HP → 0 from enemy attack):
  - Phase transitions to `CLAWKIN_FAINTED` (new `BattlePhase` value).
  - `BattleService.handleClawkinFainted()` runs: writes 0 HP to the fainted clawkin, finds `findNextAliveClawkinIndex(faintedIndex)`.
  - If next alive clawkin found → `replaceAlly(newUnit)` + `advanceFromFainted()` → battle resumes at `PLAYER_COMMAND` with the new clawkin.
  - If none remain → `finishAsDefeat()`.
- **HP persistence**: on `closeBattleSession()`, the active clawkin's `currentHp` is updated from the final `BattleUnit.hp` via `applyClawkinBattleResult(...)`. HP persists between battles until healed.
- **HUD portrait**: `BattleHud.updateActiveClawkin(Clawkin)` loads `image_clawkin` as a texture and swaps the player-side portrait. Texture is only reloaded when the active clawkin ID changes. `BattleUnit` now stores `maxHp` (set at construction) so HP bars use correct max values.
- Player skills still come from `playerBattleState.createPlayerSkills()` (from Tiled PLAYER object). Clawkin skill fields are parsed/logged but not yet used in combat.
- Full technical details: see `docs/handoffs/2026-04-06-handover-clawkin-independent-class.md` and `docs/handoffs/2026-04-06-handover-clawkin-battle-integration.md`.

### Enemy Alert Pause + Alert SFX (2026-05-08)
- Enemy objects now support `alertPauseDuration` (float, seconds) to delay chase after player detection.
- Runtime state flow is explicitly `ALERTED` -> pause -> `CHASING`.
- Alert start now triggers `AudioEventType.ENEMY_ALERT_STARTED`, routed to `SoundEffect.ENEMY_ALERT`.
- Tiled rule for enemy data: when object class is `Enemy`, keep enemy fields at top-level object properties and do not wrap them in nested `properties` class field.
- Full technical details: see `docs/handoffs/2026-05-08-handover-enemy-alert-pause-and-sfx.md`.

### Boss Interaction Confirmation Rule (2026-05-08)
- **Default rule for all future bosses:** follow this sequence unless explicitly overridden:
  1. pre-dialogue event/checks
  2. dialogue
  3. post-dialogue special event
- Special-interaction bosses must prompt for confirmation after dialogue before starting combat.
- Prompt format: `Fight {enemy name}?` with `Yes/No` selection.
- `Yes` starts encounter flow.
- `No` cancels encounter start and should run authored fallback movement/behavior (tutorial baseline: move player left for ~1s at base speed).
- Prompt loop behavior: if boss event is not yet accomplished, prompt appears again on later interactions (Bert Jr. uses this now).
- `InteractionSystem` now supports per-object pre-dialogue checks:
  - `registerPreDialogueCheck(objectId, Predicate<SpecialInteractionContext>)`
  - Runs before interaction count increment, dialogue flow resolution, and post-dialogue special handler
  - Return `false` to block the interaction entirely
- Boss music hooks are now supported in `GameScreen` on a per-encounter basis:
  - pre-battle hook (before encounter publish, e.g., tension cue)
  - battle-start hook
  - mid-battle HP threshold hooks (e.g., <= 50%)
  - post-battle hook (victory/defeat)
- Boss intro prop convention (Bert Jr. baseline, now expected for future bosses with intro props):
  - keep boss prop off-screen before first trigger
  - on first trigger: lock player + run prop walk-in during pre-dialogue
  - when prop reaches target: auto-continue interaction flow (no extra manual retrigger)
  - after boss defeat: hide/remove boss prop
- Current tutorial setup uses placeholder boss tracks for Bert Jr. and demonstrates a 50% HP mid-battle music shift.
- Runtime event tracking uses `PlayerProgress` (`core/.../progress/PlayerProgress.java`) for:
  - accomplished event flags
  - per-event stats (`attempt`, `accepted`, `declined`, `win`, `loss`, `completed`)
- Current integration: Bert Jr. is considered accomplished only after battle victory.

### Add a new spawnable object from Tiled
1. Place tile object in `objects` layer in Tiled.
2. Set a unique object `name`.
3. Add a `case` in `TiledObjectConfigurator.configureByName(...)`.
4. Attach required components and systems.

### Add a new map
1. Add `.tmx` and required `.tsx`/images in `assets/maps`.
2. Add enum entry in `MapAsset`.
3. Load/set map through `TiledService`.
4. Ensure objects layer names and expected object names match configurator cases.

### Add a new animated actor
1. Add texture/atlas asset enum entry.
2. Create animation component and/or factory similar to `PlayerAnimation`.
3. Add/extend animation system to update `Graphic.region`.
4. Spawn entity through Tiled object config or runtime factory.

## Practical Next Tasks (Suggested)
- Correct east/west row mapping in `PlayerAnimationFactory`.
- Align code comments with current sprite sheet dimensions and row usage.
- Move world-bound clamp into movement stage (post-position update) if strict boundary guarantees are needed.
- Add collision/physics layer handling (currently movement is free, no collision checks).

## Key Files Reference
- Entry + app lifecycle:
  - `core/src/main/java/github/kinuseka/testproject/Main.java`
  - `core/src/main/java/github/kinuseka/testproject/GameScreen.java`
  - `lwjgl3/src/main/java/github/kinuseka/testproject/lwjgl3/Lwjgl3Launcher.java`
- Systems:
  - `core/src/main/java/github/kinuseka/testproject/system/PlayerInputSystem.java`
  - `core/src/main/java/github/kinuseka/testproject/system/AnimationSystem.java`
  - `core/src/main/java/github/kinuseka/testproject/system/MoveSystem.java`
  - `core/src/main/java/github/kinuseka/testproject/system/CameraSystem.java`
  - `core/src/main/java/github/kinuseka/testproject/system/RenderSystem.java`
- Tiled integration:
  - `core/src/main/java/github/kinuseka/testproject/tiled/TiledService.java`
  - `core/src/main/java/github/kinuseka/testproject/tiled/TiledObjectConfigurator.java`
- Components and animation:
  - `core/src/main/java/github/kinuseka/testproject/component/*`
  - `core/src/main/java/github/kinuseka/testproject/player/*`
- Assets/map:
  - `assets/maps/main.tmx`
  - `assets/maps/tilesetPlayerObject.tsx`
