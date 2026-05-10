# Leveling System Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        BATTLE VICTORY                            │
│                              ↓                                   │
│                    ┌─────────────────┐                          │
│                    │  BattleService  │                          │
│                    │  - Victory      │                          │
│                    │  - Award EXP    │                          │
│                    └────────┬────────┘                          │
│                             ↓                                    │
│                    ┌─────────────────┐                          │
│                    │   ExpManager    │                          │
│                    │  - Calculate    │                          │
│                    │  - Distribute   │                          │
│                    └────────┬────────┘                          │
│                             ↓                                    │
│                    ┌─────────────────┐                          │
│                    │  ClawkinData    │                          │
│                    │  - Grant EXP    │                          │
│                    │  - Process      │                          │
│                    │    Level-ups    │                          │
│                    └────────┬────────┘                          │
│                             ↓                                    │
│              ┌──────────────┴──────────────┐                   │
│              ↓                              ↓                    │
│     ┌────────────────┐            ┌────────────────┐           │
│     │  LevelSystem   │            │   StatGrowth   │           │
│     │  - EXP Curve   │            │  - Calculate   │           │
│     │  - Thresholds  │            │    Stats       │           │
│     └────────────────┘            └────────────────┘           │
│              ↓                              ↓                    │
│     ┌────────────────┐            ┌────────────────┐           │
│     │SkillUnlock     │            │ LevelUpResult  │           │
│     │  System        │            │  - Stats       │           │
│     │  - Unlock      │            │  - Skills      │           │
│     │    Skills      │            │  - Display     │           │
│     └────────────────┘            └────────┬───────┘           │
│                                             ↓                    │
│                                    ┌────────────────┐           │
│                                    │ BattleOverlay  │           │
│                                    │  - Display     │           │
│                                    │    Level-up    │           │
│                                    └────────────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow

### 1. Battle Victory
```
Player defeats enemy
    ↓
BattleStateMachine.finishAsVictory()
    ↓
BattleService.closeBattleSession()
    ↓
BattleService.awardExpForVictory()
```

### 2. EXP Award
```
BattleService.awardExpForVictory()
    ↓
Calculate EXP reward (based on enemy level)
    ↓
PlayerBattleState.awardExpToActiveClawkin(expAmount)
    ↓
ClawkinData.grantExp(expAmount)
```

### 3. Level-Up Processing
```
ClawkinData.grantExp(expAmount)
    ↓
Check if EXP >= next level threshold
    ↓
If yes: performLevelUp()
    ↓
┌─────────────────────────────────────┐
│ 1. Increment level                  │
│ 2. Calculate new stats (StatGrowth) │
│ 3. Increase current HP              │
│ 4. Unlock new skills (if any)       │
│ 5. Create LevelUpResult             │
└─────────────────────────────────────┘
    ↓
Return List<LevelUpResult>
```

### 4. UI Display
```
BattleService stores pendingLevelUpResults
    ↓
BattleOverlay.handleDialogueAdvance()
    ↓
Check for pending level-ups
    ↓
If yes: Show level-up message
    ↓
ExpManager.formatLevelUpMessage()
    ↓
Display in dialogue box
    ↓
Player presses key to dismiss
    ↓
BattleService.clearPendingLevelUpResults()
    ↓
Close battle session
```

## Class Relationships

```
┌──────────────────────────────────────────────────────────────┐
│                      Core Systems                             │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  LevelSystem (static)                                        │
│  ├─ getExpRequiredForLevel(level)                           │
│  ├─ getExpForNextLevel(currentLevel)                        │
│  ├─ calculateLevelFromExp(totalExp)                         │
│  └─ calculateExpReward(enemyLevel, isWild)                  │
│                                                               │
│  StatGrowth                                                   │
│  ├─ GrowthRate enum                                          │
│  ├─ calculateHpAtLevel(level)                               │
│  ├─ calculateAttackAtLevel(level)                           │
│  ├─ calculateDefenseAtLevel(level)                          │
│  ├─ calculateSpeedAtLevel(level)                            │
│  └─ getGrowthForClawkin(clawkinId)                          │
│                                                               │
│  SkillUnlockSystem (static)                                  │
│  ├─ getSkillsUnlockedAtLevel(id, level)                    │
│  ├─ getAllSkillsUpToLevel(id, level)                       │
│  └─ hasSkillUnlockAtLevel(id, level)                       │
│                                                               │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                      Data Classes                             │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ClawkinData                                                  │
│  ├─ Fields: id, name, level, currentExp                     │
│  ├─ Fields: maxHp, currentHp, attack, defense, speed        │
│  ├─ Fields: unlockedSkills, statGrowth                      │
│  ├─ grantExp(expAmount) → List<LevelUpResult>              │
│  ├─ getExpProgressToNextLevel() → float                     │
│  ├─ toClawkin() → Clawkin                                   │
│  └─ syncFromClawkin(clawkin)                                │
│                                                               │
│  LevelUpResult                                                │
│  ├─ Fields: oldLevel, newLevel                              │
│  ├─ Fields: old/new stats for HP, ATK, DEF, SPEED          │
│  ├─ Fields: newlyUnlockedSkills                             │
│  ├─ getLevelsGained()                                        │
│  ├─ getHpGain(), getAttackGain(), etc.                     │
│  └─ formatLevelUpMessage()                                   │
│                                                               │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                    Manager Classes                            │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ExpManager (static)                                          │
│  ├─ awardExp(clawkinData, expAmount)                        │
│  ├─ calculateExpReward(level, hp, isWild)                   │
│  └─ formatLevelUpMessage(name, results)                     │
│                                                               │
│  ClawkinFactory (static)                                      │
│  ├─ createSweepeaLevel5()                                    │
│  ├─ createGingerLevel5()                                     │
│  └─ createDartLevel5()                                       │
│                                                               │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                  Integration Classes                          │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  PlayerBattleState                                            │
│  ├─ party: List<Clawkin>                                    │
│  ├─ partyData: List<ClawkinData>                            │
│  ├─ addClawkinToParty(clawkin)                              │
│  ├─ awardExpToActiveClawkin(exp)                            │
│  ├─ getClawkinData(index)                                    │
│  └─ getActiveClawkinData()                                   │
│                                                               │
│  BattleService                                                │
│  ├─ lastEnemyMaxHp, lastBattleWasWild                       │
│  ├─ pendingLevelUpResults                                    │
│  ├─ awardExpForVictory()                                     │
│  ├─ getPendingLevelUpResults()                               │
│  └─ clearPendingLevelUpResults()                             │
│                                                               │
│  BattleOverlay                                                │
│  ├─ DialogueFlowPhase.LEVEL_UP_DISPLAY                      │
│  └─ handleDialogueAdvance() - level-up display              │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

## State Machine

```
┌─────────────────────────────────────────────────────────────┐
│                    Battle State Flow                         │
└─────────────────────────────────────────────────────────────┘

PLAYER_COMMAND
    ↓ (player attacks)
PLAYER_RESULT (dialogue)
    ↓ (player presses key)
ENEMY_COMMAND
    ↓ (enemy attacks)
ENEMY_RESULT (dialogue)
    ↓ (player presses key)
PLAYER_COMMAND
    ↓ (repeat until...)
VICTORY
    ↓ (player presses key)
LEVEL_UP_DISPLAY (if leveled up)
    ↓ (player presses key)
BATTLE_ENDED
    ↓
Return to overworld
```

## EXP Calculation Flow

```
┌─────────────────────────────────────────────────────────────┐
│                  EXP Reward Calculation                      │
└─────────────────────────────────────────────────────────────┘

Enemy Defeated
    ↓
Estimate Enemy Level
    ├─ From enemy max HP
    └─ Formula: (HP - 40) / 8
    ↓
Calculate Base EXP
    └─ Formula: 30 + (level * 10)
    ↓
Apply Battle Type Multiplier
    ├─ Wild Battle: 1.0x
    └─ Trainer Battle: 1.5x
    ↓
Final EXP Reward
    └─ Minimum: 10 EXP
```

## Stat Calculation Flow

```
┌─────────────────────────────────────────────────────────────┐
│                   Stat Growth Calculation                    │
└─────────────────────────────────────────────────────────────┘

Level Up Triggered
    ↓
Get StatGrowth for Clawkin
    ↓
For each stat (HP, ATK, DEF, SPEED):
    ├─ Get base stat at starting level (5)
    ├─ Get growth rate (VERY_SLOW to EXTREME)
    ├─ Calculate level difference
    ├─ Multiply: growth rate * level difference
    └─ Add to base stat
    ↓
New Stats Calculated
    ↓
Apply to ClawkinData
```

## Skill Unlock Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Skill Unlock Process                      │
└─────────────────────────────────────────────────────────────┘

Level Up Triggered
    ↓
Check SkillUnlockSystem
    ↓
Get skills for Clawkin ID at new level
    ↓
If skills found:
    ├─ Add to unlockedSkills list
    ├─ Add to LevelUpResult
    └─ Display in level-up message
    ↓
Skills Available in Next Battle
```

## Data Persistence

```
┌─────────────────────────────────────────────────────────────┐
│                  Save/Load Structure                         │
└─────────────────────────────────────────────────────────────┘

ClawkinData (Serializable)
    ├─ id: String
    ├─ name: String
    ├─ level: int
    ├─ currentExp: int
    ├─ maxHp: int
    ├─ currentHp: int
    ├─ attack: int
    ├─ defense: int
    ├─ speed: int
    └─ unlockedSkills: List<String>

PlayerBattleState
    └─ partyData: List<ClawkinData>
        ↓
    Serialize to JSON
        ↓
    Save to file
        ↓
    Load from file
        ↓
    Deserialize from JSON
        ↓
    Restore ClawkinData
        ↓
    Convert to Clawkin instances
```

## Performance Considerations

### Efficient Operations
- ✅ Stat calculations: O(1) - simple arithmetic
- ✅ Level lookup: O(1) - direct calculation
- ✅ Skill unlock check: O(n) where n = number of skills (small)
- ✅ EXP award: O(1) - single operation

### Memory Usage
- ClawkinData: ~200 bytes per instance
- LevelUpResult: ~100 bytes per instance
- Total for 3-Clawkin party: ~600 bytes
- Negligible impact on performance

### Optimization Notes
- No database queries required
- All calculations are deterministic
- No random number generation
- No network calls
- Suitable for real-time gameplay

## Error Handling

```
┌─────────────────────────────────────────────────────────────┐
│                    Error Prevention                          │
└─────────────────────────────────────────────────────────────┘

EXP Award
    ├─ Null check: ClawkinData exists
    ├─ Range check: expAmount > 0
    └─ Max level check: level < 30

Level Up
    ├─ Overflow check: EXP doesn't exceed max
    ├─ Multiple level-ups: Loop until insufficient EXP
    └─ HP increase: Current HP increases with max HP

Stat Calculation
    ├─ Minimum values: All stats >= 1
    ├─ Level bounds: 1 <= level <= 30
    └─ Growth rate validation: Enum ensures valid rates

Skill Unlock
    ├─ Duplicate prevention: Skills only added once
    ├─ Level validation: Only unlock at correct level
    └─ Clawkin ID validation: Fallback to default
```

## Summary

The leveling system is designed with:
- **Modularity**: Each component is independent
- **Extensibility**: Easy to add new Clawkins and skills
- **Performance**: O(1) operations for critical paths
- **Reliability**: Comprehensive error handling
- **Maintainability**: Clear separation of concerns
- **Testability**: Pure functions and deterministic behavior
