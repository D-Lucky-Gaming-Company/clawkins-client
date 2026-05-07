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
- If the alert sound file is missing, current audio service behavior remains safe no-op.

## Validation

- `:core:compileJava` passed after this change.
- Lint/IDE diagnostics were clean on touched Java files.
