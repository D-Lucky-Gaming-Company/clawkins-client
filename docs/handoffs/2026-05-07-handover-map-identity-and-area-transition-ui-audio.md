# Handover: Map Identity + Area Transition UI/Audio

## Scope of This Change
This handover covers two related updates:

1. Map identity cleanup and display-name mapping.
2. RPG-style area transition title at top-center, plus placeholder transition SFX hook.

The goal is to make map references cleaner (`MapAsset` keys), provide human-readable area names, and make transitions feel more polished.

## What Was Changed

### 1) Map identifier cleanup (`MAIN` removed)
- `MapAsset.MAIN` was removed because `main.tmx` is not present and runtime startup already uses `MapAsset.COTTAGE`.
- `MapAsset.fromKey(...)` behavior remains intact for valid keys.
- Tiled project enum/defaults were aligned away from `MAIN`:
  - `assets/maps/test.tiled-project`
  - `assets/maps/templates/trigger_transition.tx`

### 2) New display-name enum for maps
- Added `MapAssetName`:
  - `core/src/main/java/github/dluckycompany/clawkins/asset/MapAssetName.java`
- It maps each `MapAsset` to a display name string (example: `NURSE_INTERIOR -> Clinic`, `BACKALLEY -> Back Alley`).
- Helper method:
  - `MapAssetName.fromAsset(MapAsset asset)` returns display name or `null`.

### 3) Area title UI shown on map transitions
- Implemented in:
  - `core/src/main/java/github/dluckycompany/clawkins/GameScreen.java`
- Behavior:
  - On successful `performMapTransition(...)`, game now calls `showAreaTitle(targetAsset)`.
  - Title is rendered at top-center of screen using a short fade-in/fade-out timing.
- Font source:
  - `assets/font/TheWildBreathOfZelda-15Lv.ttf`

### 4) Placeholder transition sound event
- Added new enum entry:
  - `SoundEffect.AREA_TRANSITION`
- Added new audio event:
  - `AudioEventType.AREA_TRANSITION`
- Wired in audio service:
  - `AudioService.onEvent(...)` handles `AREA_TRANSITION` by playing `SoundEffect.AREA_TRANSITION`.
- Event fired during map transition:
  - `GameScreen.performMapTransition(...)` calls `audioService.onEvent(AudioEventType.AREA_TRANSITION)`.
- Placeholder registration path in `Main.create()`:
  - `audio/sfx/area_transition.wav`
- This is safe if file is missing (existing audio service is no-op when unresolved).

## Runtime Flow (Transition Path)
When player enters a transition trigger:

1. `MapTransitionSystem` raises callback.
2. `GameScreen` stores pending transition and starts fade.
3. During fade swap window, `performMapTransition(...)` loads/sets new map.
4. Same method now:
   - resolves and shows area title via `MapAssetName`
   - fires `AREA_TRANSITION` audio event
5. Player repositioning and cooldown logic continue as before.

## Tuning Knobs (Fast Adjustments)
In `GameScreen` constants:

- `AREA_TITLE_FONT_SIZE` (current `36`)
- `AREA_TITLE_Y` (current `560f`)
- `AREA_TITLE_DURATION_SECONDS` (current `2.2f`)
- `AREA_TITLE_FADE_SECONDS` (current `0.35f`)

Font path constant:
- `AREA_TITLE_FONT_PATH = "font/TheWildBreathOfZelda-15Lv.ttf"`

If the custom font is missing, code falls back to `new BitmapFont()` and logs a message.

## Files Touched

- `core/src/main/java/github/dluckycompany/clawkins/asset/MapAsset.java`
- `core/src/main/java/github/dluckycompany/clawkins/asset/MapAssetName.java`
- `core/src/main/java/github/dluckycompany/clawkins/GameScreen.java`
- `core/src/main/java/github/dluckycompany/clawkins/audio/SoundEffect.java`
- `core/src/main/java/github/dluckycompany/clawkins/audio/AudioEventType.java`
- `core/src/main/java/github/dluckycompany/clawkins/audio/AudioService.java`
- `core/src/main/java/github/dluckycompany/clawkins/Main.java`
- `assets/maps/test.tiled-project`
- `assets/maps/templates/trigger_transition.tx`

## Validation Notes
- Java lint check on touched files: clean.
- `:core:compileJava` completed successfully.

## Follow-up Suggestions for Future Agents
- If user adds real SFX, place file at `assets/audio/sfx/area_transition.wav` (or update registration path in `Main`).
- If user wants first area title on initial spawn, call `showAreaTitle(MapAsset.COTTAGE)` in `show()` after map setup.
- If area names should be localized later, replace `MapAssetName` string literals with a localization lookup map.
