# Handover: Battle HUD Turn/HP/SFX Stabilization (2026-05-08)

## Scope

This update stabilizes battle-cycle behavior in three focused areas:
- switching clawkins now consumes the player's turn,
- player HP bar updates from live battle damage immediately,
- battle actions now route through a dedicated per-action SFX handler.

## What Changed

### 1) Clawkin switch now consumes a turn
- Added `BattleStateMachine.consumeTurnAfterSwitch(String allyDisplayName)`.
- Added `BattleService.switchActiveClawkin(int newIndex)` to centralize switch flow:
  - validates target index and alive state,
  - syncs outgoing active clawkin HP from current `BattleUnit`,
  - swaps battle ally unit + active skills,
  - transitions phase to `ENEMY_COMMAND`.
- `BattleOverlay.performClawkinSwitch(...)` now calls `battleService.switchActiveClawkin(...)` instead of directly mutating only UI state.

Result: switching during `PLAYER_COMMAND` properly spends the player's action for that cycle.

### 2) Battle HUD HP now reflects real-time battle damage
- `BattleOverlay.syncHudHpFromBattleState(...)` now sets player HP from the live ally `BattleUnit`:
  - `battleHud.setPlayerHp(ally.getHp(), ally.getMaxHp())`
- Active `Clawkin.currentHp` is immediately synchronized from ally unit HP in that same method.

Result: when the active clawkin takes damage, the UI HP bar updates in the same battle cycle and no longer shows stale pre-hit values.

### 3) Per-action battle SFX handler prepared and wired
- New class: `BattleActionSfxHandler`.
- Current mapping uses existing registered sounds:
  - damage span -> `SoundEffect.HIT`
  - heal span -> `SoundEffect.CONFIRM`
  - defense-up span -> `SoundEffect.UI_HOVER`
  - fallback action -> `SoundEffect.UI_SELECT`
  - switch action -> `SoundEffect.UI_SELECT`
  - escape action -> `SoundEffect.UI_BACK`
- `BattleOverlay` now uses this handler when:
  - player action result opens,
  - enemy action result opens,
  - clawkin switch is confirmed,
  - escape action is submitted.

Result: each action in the battle cycle has an SFX hook path in one place for future tuning.

## Files Touched

- `core/src/main/java/github/dluckycompany/clawkins/battle/BattleStateMachine.java`
- `core/src/main/java/github/dluckycompany/clawkins/battle/BattleService.java`
- `core/src/main/java/github/dluckycompany/clawkins/battle/BattleOverlay.java`
- `core/src/main/java/github/dluckycompany/clawkins/battle/BattleActionSfxHandler.java`

## Validation

- `:core:compileJava` passed after this update.
- No linter diagnostics on edited battle files.

## Future AI Notes

- Keep switch flow in `BattleService.switchActiveClawkin(...)` as source of truth. Avoid splitting switch rules between HUD and state machine.
- For HP display, prefer `BattleStateMachine.firstAlly()` battle unit values over cached party data.
- If adding richer battle SFX later (buff/debuff/switch/faint unique clips), extend `BattleActionSfxHandler` first rather than scattering `audioService.playSound(...)` calls across overlay/state classes.
