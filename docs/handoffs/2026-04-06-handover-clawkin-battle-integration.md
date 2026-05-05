# Clawkin Battle Integration Handoff

**Date:** 2026-04-06  
**Scope:** Battle system rewritten to be clawkin-driven; `image_clawkin` property added; fallback starter party removed.  
**Files changed:**
- `assets/maps/test.tiled-project`
- `core/src/main/java/github/kinuseka/testproject/character/Clawkin.java`
- `core/src/main/java/github/kinuseka/testproject/battle/BattlePhase.java`
- `core/src/main/java/github/kinuseka/testproject/battle/BattleUnit.java`
- `core/src/main/java/github/kinuseka/testproject/battle/BattleContext.java`
- `core/src/main/java/github/kinuseka/testproject/battle/PlayerBattleState.java`
- `core/src/main/java/github/kinuseka/testproject/battle/BattleStateMachine.java`
- `core/src/main/java/github/kinuseka/testproject/battle/BattleService.java`
- `core/src/main/java/github/kinuseka/testproject/battle/BattleHud.java`
- `core/src/main/java/github/kinuseka/testproject/battle/BattleOverlay.java`

**Prerequisite reading:** `docs/handoffs/2026-04-06-handover-clawkin-independent-class.md`

---

## Summary of Changes

Three distinct changes landed in this session:

1. **`image_clawkin` property** added to the `Clawkin` Tiled class — a relative path to an asset image for that clawkin's portrait.
2. **Fallback starter party removed** — if no clawkins are authored on the Player object, the party is empty. There are no hardcoded defaults any more.
3. **Battle system is now clawkin-driven** — the player-side battle unit is built from the active clawkin's stats, the portrait shown in the HUD comes from `image_clawkin`, and if the active clawkin faints the battle automatically switches to the next alive one. If none are available the battle ends in immediate defeat.

---

## Change 1: `image_clawkin` Property

### Tiled schema (`test.tiled-project`)

A new member was added to the `Clawkin` class (id `10`):

| Property name   | Type   | Default | Meaning |
|-----------------|--------|---------|---------|
| `image_clawkin` | string | `""`    | Asset-relative path to the clawkin's portrait image. Example: `ui/Clawkin_01.png` resolves to `assets/ui/Clawkin_01.png`. |

Position in the member list: after `id`, before `name`.

### `Clawkin.java`

- New field: `private final String imagePath`
- Constructor signature changed from `(id, name, level, hp, attack, defense, speed)` to `(id, name, imagePath, level, hp, attack, defense, speed)` — `imagePath` is the third parameter.
- New getter: `getImagePath()` — returns the stored path (trimmed, never null, may be empty string if not set).
- `setCurrentHp(int hp)` was also added in this session (see Change 3 below).

### `TiledObjectConfigurator.java`

`buildClawkinFromSlot` now reads `image_clawkin` from the clawkin's `MapProperties` and passes it to the constructor:

```java
String imagePath = getStringFromProps(clawkinProps, "image_clawkin", "").trim();
return new Clawkin(resolvedId, resolvedName, imagePath, level, hp, attack, defense, speed);
```

---

## Change 2: Fallback Starter Party Removed

Previously, if a PLAYER object had no `clawkin1`/`clawkin2`/`clawkin3` properties set, the loader would inject a hardcoded starter party (Swee'pea, Ginger, Dart) as a safety net:

```java
// OLD — removed
private void addStarterFallbackClawkins() {
    playerBattleState.addClawkinToParty(new Clawkin("clawkin_sweepea", "Swee'pea", 1, 50, 8, 4, 6));
    playerBattleState.addClawkinToParty(new Clawkin("clawkin_ginger",  "Ginger",   1, 60, 6, 5, 5));
    playerBattleState.addClawkinToParty(new Clawkin("clawkin_dart",    "Dart",     1, 45, 7, 6, 4));
}
```

This method has been deleted. The party is now **empty by default**. If the Player object has no clawkin properties, the party is 0 members. When that player enters an encounter, the battle immediately resolves as a defeat (see Change 3).

**Impact on existing maps:** `main.tmx` already has `clawkin1`, `clawkin2`, `clawkin3` properties on the Player object (added in the previous session), so the game still has a party in the current map. Any new Player objects on new maps will have an empty party unless clawkins are explicitly authored.

---

## Change 3: Battle System is Clawkin-Driven

### Design intent

Before this change, the player-side battle unit was built from `PlayerBattleState.createBattleUnit()` — which used the player character's stats (`playerHp`, `playerAttack`, etc. from the PLAYER Tiled object). The player fought directly.

After this change, the player-side battle unit is built from the **active clawkin's stats**. The player character no longer directly enters combat. The clawkin fights on their behalf. Stats shown in the HUD (name, portrait, HP bar) all come from the clawkin.

### New concept: active clawkin index

`PlayerBattleState` now tracks which clawkin is currently fighting:

```java
private int activeClawkinIndex = -1;  // -1 = no active clawkin
```

New methods:

| Method | Description |
|--------|-------------|
| `setActiveClawkinIndex(int)` | Set the fighting clawkin slot. |
| `getActiveClawkinIndex()` | Get it. |
| `getActiveClawkin()` | Returns `getClawkin(activeClawkinIndex)`, null if index is invalid. |
| `findNextAliveClawkinIndex(int fromIndex)` | Searches forward from `fromIndex + 1`, returns the next index where `clawkin.isAlive()`, or -1 if none. |
| `hasAnyAliveClawkin()` | Returns true if at least one clawkin in the party has HP > 0. |
| `applyClawkinBattleResult(int index, BattleUnit unit)` | Writes `unit.getHp()` back into the clawkin at `index`. Used on battle close. |

`activeClawkinIndex` is reset to -1 on `closeBattleSession()`.

### `Clawkin.java` addition: `setCurrentHp(int)`

To support writing HP back after battle:

```java
public void setCurrentHp(int hp) {
    currentHp = Math.max(0, Math.min(maxHp, hp));
}
```

### `BattleUnit.java` addition: `maxHp`

`BattleUnit` now stores `maxHp` (set from the initial `hp` constructor argument). This allows the HUD to know the max HP for the progress bar without a separate external reference.

```java
private final int maxHp;
// new getter:
public int getMaxHp() { return maxHp; }
```

### `BattleContext.java` addition: `replaceFirstAlly`

Added to support swapping the active clawkin mid-battle:

```java
public void replaceFirstAlly(BattleUnit unit) {
    if (unit != null && !allies.isEmpty()) {
        allies.set(0, unit);
    }
}
```

The internal `allies` list was always mutable (`new ArrayList<>(...)`); only `getAllies()` returns an unmodifiable view.

### `BattlePhase.java` addition: `CLAWKIN_FAINTED`

New phase inserted between `ENEMY_COMMAND` and `DEFEAT`:

```java
CLAWKIN_FAINTED
```

This phase means: the active clawkin's HP reached 0 after an enemy attack, but the battle is not over yet — the game needs to check whether another clawkin can take over. It is a transient phase (resolved the same frame by `BattleService`).

`isActive()` in `BattleStateMachine` does NOT include `CLAWKIN_FAINTED` in the terminal set — the battle is still considered active during this phase.

### `BattleStateMachine.java` changes

**In `tick()` (enemy attack resolution):** When the ally's HP reaches 0 after enemy damage:

```java
// OLD
finishAsDefeat();
lastLog = "Defeat... You were knocked out.";

// NEW
phase = BattlePhase.CLAWKIN_FAINTED;
lastLog = player.getId() + " fainted!";
```

**New methods:**

```java
public void replaceAlly(BattleUnit unit) {
    if (context != null) context.replaceFirstAlly(unit);
}

public void advanceFromFainted() {
    if (phase == BattlePhase.CLAWKIN_FAINTED) {
        phase = BattlePhase.PLAYER_COMMAND;
    }
}
```

### `BattleService.java` changes

**`startBattle(EncounterEvent event)`:**
- Calls `buildActiveClawkinUnit()` instead of `playerBattleState.createBattleUnit()`.
- `buildActiveClawkinUnit()` scans the party for the first alive clawkin, sets `activeClawkinIndex`, and returns a `BattleUnit` from that clawkin's `currentHp / baseAttack / baseDefense / baseSpeed`.
- If no alive clawkin exists, returns a dummy unit with 0 HP and calls `finishAsDefeat()` immediately after `begin(context)`.

**`update(float delta)`:**

```java
if (battleStateMachine.isActive()) {
    battleStateMachine.tick(delta);
    if (battleStateMachine.getPhase() == BattlePhase.CLAWKIN_FAINTED) {
        handleClawkinFainted();
    }
}
```

**`handleClawkinFainted()`:**
1. Gets `faintedIndex = playerBattleState.getActiveClawkinIndex()`.
2. Sets the fainted clawkin's HP to 0 via `fainted.setCurrentHp(0)`.
3. Calls `playerBattleState.findNextAliveClawkinIndex(faintedIndex)`.
4. If -1 (no more alive clawkins) → `battleStateMachine.finishAsDefeat()` → returns.
5. Otherwise: sets new `activeClawkinIndex`, builds a new `BattleUnit` from the next clawkin's stats, calls `battleStateMachine.replaceAlly(...)` and `battleStateMachine.advanceFromFainted()` to resume at `PLAYER_COMMAND`.

**`closeBattleSession()`:**
- In addition to the existing `applyBattleResult(...)` (which persists player HP), now also calls `playerBattleState.applyClawkinBattleResult(activeClawkinIndex, firstAlly())` to write the final battle HP back to the active clawkin.
- Resets `activeClawkinIndex` to -1.

### `BattleHud.java` changes

**New fields:**
```java
private Texture activeClawkinTex;     // portrait texture; disposed on clawkin change
private String lastDisplayedClawkinId; // tracks which clawkin is currently shown
```

**New method: `updateActiveClawkin(Clawkin clawkin)`**
- Called every frame from `BattleOverlay.syncHudHpFromBattleState`.
- Compares `clawkin.getId()` to `lastDisplayedClawkinId`. If same, returns early (no texture reload).
- If different:
  - Disposes `activeClawkinTex` if non-null.
  - Loads a new `Texture` from `Gdx.files.internal(clawkin.getImagePath())` — only if the path is non-blank and the file exists.
  - Calls `playerImage.setDrawable(new TextureRegionDrawable(...))` to swap the portrait.
  - Updates `playerNameLabel.setText(clawkin.getName())`.
  - Updates `lastDisplayedClawkinId`.
- `hide()` now resets `lastDisplayedClawkinId = null` so the next battle always refreshes the portrait.
- `dispose()` now disposes `activeClawkinTex`.

**Note on safe path check:** Uses `Gdx.files.internal(path).exists()` before loading. If the path is blank or the file doesn't exist, the previous portrait remains (no crash).

### `BattleOverlay.java` changes

**New field:**
```java
private PlayerBattleState playerBattleState;
```

Stored from the `init(...)` call (was previously passed as a parameter but not retained).

**`syncHudHpFromBattleState(BattleStateMachine battle)` rewritten:**

```java
// Player side: use active clawkin's maxHp and call updateActiveClawkin
Clawkin activeClawkin = playerBattleState.getActiveClawkin();
battleHud.updateActiveClawkin(activeClawkin);
float maxHp = activeClawkin != null ? activeClawkin.getMaxHp() : 100f;
battleHud.setPlayerHp(ally.getHp(), maxHp);

// Boss side: use BattleUnit.getMaxHp() (the initial HP when the unit was created)
battleHud.setBossHp(enemy.getHp(), enemy.getMaxHp());
```

The hardcoded `MAX_HP = 100f` constant has been removed.

---

## Full State Flow: Clawkin Battle

```
Encounter triggered
    ↓
BattleService.startBattle()
    ├── No alive clawkins?  → begin(dummy context) → finishAsDefeat()
    └── Has alive clawkin?  → set activeClawkinIndex = first alive slot
                              build BattleUnit from clawkin stats
                              begin(context)
                              ↓
                         PLAYER_COMMAND (player picks skill)
                              ↓
                         Enemy attacks → ally HP decremented
                              ├── HP > 0 → back to PLAYER_COMMAND
                              └── HP = 0 → CLAWKIN_FAINTED
                                              ↓
                                    handleClawkinFainted()
                                        ├── Next alive clawkin exists?
                                        │   → replaceAlly(newUnit)
                                        │   → advanceFromFainted()
                                        │   → back to PLAYER_COMMAND (new clawkin)
                                        └── No more alive clawkins?
                                            → finishAsDefeat()
                                                  ↓
                                               DEFEAT
```

---

## What Is Not Changed

- **Player skills** (from `playerBattleState.createPlayerSkills()`) still drive combat. The clawkin's skill fields (`skill1Name`, `skill1Power`, etc.) are parsed and logged but not yet used in the battle formula. They are wired to phase 2.
- **Clawkin party order** is fixed (index 0, 1, 2). Switching always picks the next index forward. There is no UI to choose which clawkin to send in.
- **Enemy side** of the battle is unchanged. Enemy stats still come from the Tiled `ENEMY` object's `EncounterZone` component.
- **Team Viewer** and **Inventory party selection** are unaffected — they read from `playerBattleState.getParty()` which is the same list.

---

## Validation Checklist

1. Author a Player with `clawkin1` set (e.g. `hp=50`, `image_clawkin=ui/Clawkin_01.png`). Enter an encounter. Confirm the HUD shows the clawkin's portrait and HP bar starts at 50.
2. Take enough hits to kill the first clawkin. Confirm the HUD switches to the next alive clawkin automatically (new portrait, new HP bar).
3. Kill all clawkins. Confirm the battle ends in defeat.
4. Author a Player with **no** clawkin properties. Enter an encounter. Confirm the battle immediately resolves as defeat without crashing.
5. Escape a battle. Re-enter. Confirm the clawkin's HP is whatever it was when you escaped (persistence between battles).
6. Set `image_clawkin` to a blank string. Confirm the HUD still shows the default portrait (no crash).
