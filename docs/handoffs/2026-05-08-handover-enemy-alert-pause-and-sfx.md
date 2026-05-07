# Handover: Enemy Alert Pause + Alert SFX (2026-05-08)

## Scope

This update adds an RPG-style "notice/alert" pause before enemies start chasing, and plays a sound effect when that alert phase begins.

## What Changed

### 1) Enemy alert pause is now configurable per enemy object
- New Tiled property on enemy objects: `alertPauseDuration` (seconds).
- Default value if not authored: `0.8`.
- Runtime model:
  - `Enemy` now stores:
    - `alertPauseDuration` (config)
    - `alertPauseTimer` (runtime countdown)

### 2) Enemy state flow now enforces alert pause before chase
- `EnemySystem` transition logic was updated so enemy behavior is:
  1. sees/locks player -> enter `ALERTED`
  2. stop movement and face player
  3. wait for `alertPauseTimer`
  4. then enter `CHASING`
- This prevents immediate chase override and keeps the alert phase visible in gameplay.

### 3) Alert-start SFX hook added
- New audio event: `AudioEventType.ENEMY_ALERT_STARTED`.
- New sound enum entry: `SoundEffect.ENEMY_ALERT`.
- `AudioService.onEvent(...)` now routes:
  - `ENEMY_ALERT_STARTED` -> `playSound(ENEMY_ALERT)`.
- `EnemySystem` fires alert event once when entering alert phase.
- `Main.create()` registers the sound path:
  - `audio/sfx/enemy_alert.wav`

## Files Touched

- `core/src/main/java/github/dluckycompany/clawkins/component/Enemy.java`
- `core/src/main/java/github/dluckycompany/clawkins/system/EnemySystem.java`
- `core/src/main/java/github/dluckycompany/clawkins/tiled/TiledObjectConfigurator.java`
- `core/src/main/java/github/dluckycompany/clawkins/GameScreen.java`
- `core/src/main/java/github/dluckycompany/clawkins/audio/AudioEventType.java`
- `core/src/main/java/github/dluckycompany/clawkins/audio/SoundEffect.java`
- `core/src/main/java/github/dluckycompany/clawkins/audio/AudioService.java`
- `core/src/main/java/github/dluckycompany/clawkins/Main.java`

## Authoring Notes For Future AI / Devs

- Per-enemy tuning:
  - Small: `0.4` to `0.7` (snappier enemies)
  - Medium: `0.8` to `1.2` (classic RPG notice)
  - High: `1.5+` (dramatic telegraph)
- Tiled authoring rule: if an object is already class `Enemy`, write enemy fields directly on that object (`canRoam`, `canChase`, `isTerritorial`, etc.). Do **not** add a nested `properties` class field of type `Enemy`.
- If the alert sound file is missing, current audio service behavior remains safe no-op.

## Validation

- `:core:compileJava` passed after this change.
- Lint/IDE diagnostics were clean on touched Java files.

## Follow-up Convention (Boss Interactibles)

- Default sequence for future bosses: `pre-dialogue event/check -> dialogue -> post-dialogue special event`.
- Boss encounters triggered from special/trippable interactibles must ask for confirmation after dialogue:
  - Prompt text format: `Fight {enemy name}?`
  - Selection: `Yes` or `No` (RPG-style choice)
- `Yes` should continue/start the encounter.
- `No` should not start combat and instead run a short non-combat fallback movement when authored (current tutorial boss behavior: move player left for 1 second at normal speed).
- Repeat behavior: if boss completion event is still not accomplished, interacting again should show the same `Fight {enemy name}?` prompt.
- Progress data should be tracked through `PlayerProgress` so this can later be serialized in save data.
- New interaction extension point: `InteractionSystem.registerPreDialogueCheck(...)` allows per-object checks/effects before dialogue starts (e.g., temporary music shift, gating by completion flags). Returning `false` cancels that interaction.
- Boss music lifecycle hooks now exist in `GameScreen` (encounter-id keyed): pre-battle, battle-start, mid-battle HP thresholds, and post-battle (victory/defeat).
- Intro prop baseline (applies to future bosses when using props): pre-dialogue lock player, walk prop to authored target, then auto-continue interaction flow; hide prop after boss defeat.
