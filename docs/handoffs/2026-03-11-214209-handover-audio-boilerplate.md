# Audio Boilerplate Handover

## Title

Audio manager scaffold with map/battle event triggers.

## Reference choice

Used the manager/event style from `GdxGame` as the primary reference because:

- it has clear centralized control over tracks/sfx,
- it fits this project's current architecture (single `Main` + shared services),
- it maps cleanly to event-driven battle/map transitions already present in `GameScreen`.

Also adopted map-driven track selection behavior inspired by `mystictutorial` (`setMap(...)` audio handling).

## What was implemented

### New audio package

- `core/src/main/java/github/kinuseka/testproject/audio/MusicTrack.java`
- `core/src/main/java/github/kinuseka/testproject/audio/SoundEffect.java`
- `core/src/main/java/github/kinuseka/testproject/audio/AudioEventType.java`
- `core/src/main/java/github/kinuseka/testproject/audio/AudioService.java`

### Lifecycle integration

- `Main`:
  - creates one `AudioService`,
  - registers placeholder music/sfx paths,
  - disposes audio service on shutdown,
  - exposes service with `getAudioService()`.

- `GameScreen`:
  - on map load/change:
    - `audioService.setMap(map)`
    - `audioService.onEvent(MAP_CHANGED)`
  - on battle session enter:
    - `audioService.onEvent(ENCOUNTER_STARTED)`
    - `audioService.onEvent(BATTLE_STARTED)`
  - on battle session end:
    - `audioService.onEvent(BATTLE_ENDED)`

## Map-triggered music behavior

- `AudioService` reads map property `musicTrack` from `TiledMap` root properties.
- Values should match `MusicTrack` enum names:
  - `EXPLORATION`
  - `BATTLE`
  - `MENU`
- Missing/invalid property falls back to `EXPLORATION`.

## Current placeholder paths

Registered in `Main.create()`:

- Music:
  - `audio/music/exploration.ogg`
  - `audio/music/battle.ogg`
  - `audio/music/menu.ogg`
- SFX:
  - `audio/sfx/confirm.wav`
  - `audio/sfx/cancel.wav`
  - `audio/sfx/hit.wav`
  - `audio/sfx/encounter.wav`

No assets are required yet; playback safely no-ops if files are missing.

## Safety/no-asset behavior

- `AudioService` checks if registered files exist before loading.
- Missing files do not crash runtime.
- This keeps feature integration ready while waiting for asset delivery.

## Developer documentation updated

- Updated `docs/development/readme.md`
- Added section: **Audio System Boilerplate**
  - class overview,
  - map property usage (`musicTrack`),
  - how to register files,
  - how to trigger from events/maps/tiles.

## Validation

- `core:compileJava` succeeds.
- Lint checks on modified files show no new issues.

## Next recommended steps

1. Add real audio files under `assets/audio/music` and `assets/audio/sfx`.
2. Set `musicTrack` per map in `.tmx`.
3. Wire `UI_CONFIRM` and other SFX events in interaction/battle actions.
4. Add user-facing volume settings and persist them.
