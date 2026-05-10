# Handover: Boss Reward Framework & XP Leveling Curve

**Date:** 2026-05-11  
**Status:** Boss 0 (Bert Jr.) complete — Boss 1 (Spartacus) & Boss 2 (Cerberus) are stubs.

---

## What Was Implemented

### 1. XP Leveling Curve (`LevelSystem.java`)

The `expNeededFromLevel()` method now uses a **two-phase curve**:

| Phase | Levels | XP per level | Formula |
|-------|--------|-------------|---------|
| Flat  | 1 – 8  | 50          | constant |
| Scaling | 9 – 20 | 95 → 545  | `50 + (level - 8) × 45` |

**Player starts at Level 4** (150 cumulative XP). Total XP to reach max level is **3,920**.

Constants in `LevelSystem.java`:
```java
private static final int FLAT_XP_PER_LEVEL  = 50;
private static final int SCALING_START_LEVEL = 8;
private static final int SCALING_STEP_EXP    = 45;
```

---

### 2. Boss XP Rewards (`GameScreen.java`)

`applyVictoryExperienceReward(String encounterId)` checks a per-boss override map before falling back to `DEFAULT_BATTLE_XP_REWARD = 25`.

```java
// GameScreen.java  ~line 185
private static final java.util.Map<String, Integer> BOSS_XP_REWARDS_BY_ENCOUNTER_ID =
        java.util.Map.ofEntries(
                java.util.Map.entry(ENCOUNTER_BERT_JR_ID,  50)
                // java.util.Map.entry(ENCOUNTER_SPARTACUS_ID, ???),  // TODO boss_1
                // java.util.Map.entry(ENCOUNTER_CERBERUS_ID,  ???)   // TODO boss_2
        );
```

The method is called from `BattleOverlay.tryOpenVictoryDialogue()` with the live encounter ID and **returns** the XP actually awarded so the victory screen displays the correct value.

---

### 3. Boss Coin Rewards (`BattleOverlay.java`)

`BOSS_COIN_REWARDS_BY_ENCOUNTER_ID` works identically — if an encounter ID matches, that fixed amount overrides the level-scaled default.

```java
// BattleOverlay.java  ~line 128
private static final Map<String, Integer> BOSS_COIN_REWARDS_BY_ENCOUNTER_ID = Map.ofEntries(
        Map.entry("boss_0_encounter", 500)
        // Map.entry("boss_1_encounter", ???),  // TODO boss_1
        // Map.entry("boss_2_encounter", ???)   // TODO boss_2
);
```

---

## How to Add Rewards for Boss 1 (Spartacus) or Boss 2 (Cerberus)

> Both steps must be done together or neither will be complete.

**Step 1 — XP override** in `GameScreen.java`:
```java
java.util.Map.entry(ENCOUNTER_SPARTACUS_ID, <xpAmount>),
```
Set `<xpAmount>` to the XP needed to push the player from their expected pre-boss level to the target post-boss level. Use `LevelSystem.getExpForNextLevel(level)` to calculate.

**Step 2 — Coin override** in `BattleOverlay.java`:
```java
Map.entry("boss_1_encounter", <coinAmount>),
```
500 coins for boss 0 is the baseline; scale up for harder bosses.

---

## Encounter ID Reference

| Constant | String value | Boss |
|----------|-------------|------|
| `ENCOUNTER_BERT_JR_ID`  | `"boss_0_encounter"` | Bert Jr. (implemented) |
| `ENCOUNTER_SPARTACUS_ID`| `"boss_1_encounter"` | Spartacus (stub) |
| `ENCOUNTER_CERBERUS_ID` | `"boss_2_encounter"` | Cerberus (stub) |

All three constants are declared in `GameScreen.java` near line 177.

---

## Level Design Reference

| Milestone | Target level | Cumulative XP needed | Notes |
|-----------|-------------|---------------------|-------|
| Game start | 4 | 150 | `PlayerProgress.STARTING_PLAYER_LEVEL = 4` |
| After Boss 0 | 5 | 200 | Boss awards 50 XP (guaranteed) |
| After Boss 1 | TBD | — | Set `ENCOUNTER_SPARTACUS_ID` entry |
| After Boss 2 | TBD | — | Set `ENCOUNTER_CERBERUS_ID` entry |
| Max level | 20 | 3,920 | — |

---

## Files Changed in This Session

| File | What changed |
|------|-------------|
| `character/LevelSystem.java` | New two-phase XP curve (flat 50 × 8, then +45/level) |
| `GameScreen.java` | `applyVictoryExperienceReward(encounterId)` + `BOSS_XP_REWARDS_BY_ENCOUNTER_ID` |
| `battle/BattleOverlay.java` | Passes `encounterId` to `applyVictoryExperienceReward`; expanded coin map with stubs |
