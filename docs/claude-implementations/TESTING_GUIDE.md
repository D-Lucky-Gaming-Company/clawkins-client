# Battle System Rebalance - Testing Guide

## Quick Test Checklist

### 1. EXP System Testing (Flat 50 EXP per level)

#### Test Case 1.1: Level 1 → Level 2
**Steps:**
1. Start a new game with a level 1 Clawkin
2. Win a battle (should grant ~100 EXP)
3. Verify Clawkin levels up to level 3 (100 EXP = 2 levels)

**Expected Result:**
- Level 1 → Level 3 after first victory
- EXP bar shows correct progress

#### Test Case 1.2: Consistent EXP Requirements
**Steps:**
1. Check EXP required for multiple levels:
   - Level 5 → 6 should need 50 EXP
   - Level 10 → 11 should need 50 EXP
   - Level 20 → 21 should need 50 EXP

**Expected Result:**
- Every level requires exactly 50 EXP
- No exponential growth

---

### 2. Round EXP Testing (5 EXP per round)

#### Test Case 2.1: Normal Round Completion
**Steps:**
1. Start a battle
2. Use any attack skill
3. Wait for enemy to attack
4. Check console logs for "Round X complete -> granted 5 EXP"

**Expected Result:**
- 5 EXP granted after each complete round
- Round counter increments
- EXP accumulates correctly

#### Test Case 2.2: EXP on Clawkin KO
**Steps:**
1. Start a battle
2. Let your Clawkin's HP reach 0
3. Check if round EXP was still granted

**Expected Result:**
- Round EXP granted even if Clawkin faints
- No EXP duplication

#### Test Case 2.3: EXP on Battle Loss
**Steps:**
1. Start a battle
2. Complete 3 rounds
3. Lose the battle (all Clawkin faint)
4. Check total EXP gained

**Expected Result:**
- Should have gained 15 EXP (3 rounds × 5 EXP)
- EXP persists even on loss

#### Test Case 2.4: Long Battle EXP
**Steps:**
1. Start a battle
2. Complete 10 rounds (use healing items to survive)
3. Check total EXP

**Expected Result:**
- 50 EXP from rounds = 1 full level
- Plus victory EXP = ~3 total levels

---

### 3. Item Turn Penalty Testing

#### Test Case 3.1: Item Ends Turn
**Steps:**
1. Start a battle
2. Use an item (potion, bandage, etc.)
3. Observe battle flow

**Expected Result:**
- Item usage message appears
- Turn immediately ends
- Enemy takes their turn
- Cannot attack after using item

#### Test Case 3.2: Cannot Use Multiple Items
**Steps:**
1. Start a battle
2. Use an item
3. Try to use another item immediately

**Expected Result:**
- First item ends turn
- Cannot use second item in same turn
- Must wait for next round

#### Test Case 3.3: Item vs Attack Choice
**Steps:**
1. Start a battle
2. Try to use item AND attack in same turn

**Expected Result:**
- Only one action allowed per turn
- Item usage prevents attack
- Attack prevents item usage

---

### 4. Item Message Testing

#### Test Case 4.1: Message Display
**Steps:**
1. Use various items in battle:
   - Potion
   - Fish Snack
   - Bandage

**Expected Result:**
- Messages appear: "[Clawkin] used [Item]!"
- Examples:
  - "Ginger used Potion!"
  - "Swee'pea used Fish Snack!"
  - "Dart used Bandage!"

#### Test Case 4.2: Message Formatting
**Steps:**
1. Use items with underscores in ID:
   - potion_small → "Potion Small"
   - fish_snack → "Fish Snack"

**Expected Result:**
- Underscores replaced with spaces
- Each word capitalized
- Clean, readable format

#### Test Case 4.3: Message Timing
**Steps:**
1. Use an item
2. Observe when message appears

**Expected Result:**
- Message shows BEFORE turn ends
- Visible in battle dialog area
- Smooth transition to enemy turn

---

### 5. Victory EXP Testing (2 levels per win)

#### Test Case 5.1: Wild Battle Victory
**Steps:**
1. Win a wild battle
2. Check EXP gained

**Expected Result:**
- ~100 EXP granted
- Equals approximately 2 levels
- Plus any round EXP accumulated

#### Test Case 5.2: Trainer Battle Victory
**Steps:**
1. Win a trainer battle
2. Check EXP gained

**Expected Result:**
- ~120 EXP granted
- Equals approximately 2.4 levels
- Slightly more than wild battles

---

### 6. UI Update Testing

#### Test Case 6.1: EXP Bar Updates
**Steps:**
1. Start a battle
2. Complete several rounds
3. Watch EXP bar

**Expected Result:**
- EXP bar fills after each round
- Smooth visual updates
- Accurate percentage display

#### Test Case 6.2: Level-Up Display
**Steps:**
1. Gain enough EXP to level up
2. Observe UI changes

**Expected Result:**
- Level label updates immediately
- EXP bar resets to 0%
- New EXP requirement shown

#### Test Case 6.3: Multiple Level-Ups
**Steps:**
1. Gain 100+ EXP in one battle
2. Observe multiple level-ups

**Expected Result:**
- All level-ups processed correctly
- No EXP overflow bugs
- Final level displayed accurately

---

### 7. Edge Case Testing

#### Test Case 7.1: Max Level
**Steps:**
1. Set Clawkin to level 29
2. Gain 100 EXP
3. Check if stops at level 30

**Expected Result:**
- Levels up to 30
- Stops at max level
- No overflow to level 31+

#### Test Case 7.2: Zero EXP Gain
**Steps:**
1. Complete a round with 0 EXP reward (if possible)
2. Check for errors

**Expected Result:**
- No crashes
- Graceful handling
- No negative EXP

#### Test Case 7.3: Rapid Round Completion
**Steps:**
1. Complete rounds very quickly
2. Check for EXP duplication

**Expected Result:**
- Each round grants EXP exactly once
- No duplicate EXP triggers
- Accurate round counter

---

### 8. Integration Testing

#### Test Case 8.1: Full Battle Flow
**Steps:**
1. Start battle
2. Use attack (round 1)
3. Use item (round 2)
4. Use skill (round 3)
5. Win battle

**Expected Result:**
- 3 rounds × 5 EXP = 15 EXP
- Victory EXP = 100 EXP
- Total = 115 EXP ≈ 2.3 levels
- All messages display correctly
- No bugs or crashes

#### Test Case 8.2: Multiple Battles
**Steps:**
1. Win 5 battles in a row
2. Track total EXP gained

**Expected Result:**
- Consistent EXP per battle
- No EXP loss between battles
- Progression feels smooth

#### Test Case 8.3: Battle Loss Recovery
**Steps:**
1. Lose a battle
2. Check EXP retained
3. Win next battle

**Expected Result:**
- Round EXP from lost battle retained
- Can still progress after losses
- No EXP penalty for losing

---

## Console Log Verification

### Expected Log Messages:

#### Round Completion:
```
[BattleStateMachine] Round 1 complete -> granted 5 EXP (total accumulated: 5)
[BattleStateMachine] Round 2 complete -> granted 5 EXP (total accumulated: 10)
```

#### Item Usage:
```
[BattleStateMachine] Item used -> player=Ginger, item=Potion
```

#### Round EXP Grant:
```
[BattleOverlay] Granted 5 round EXP to Ginger (Round 1)
```

#### Level-Up:
```
[ExpManager] Ginger leveled up 1 time(s)!
[ExpManager]   Level 5 → 6
[ExpManager]   HP: 100 → 110 (+10)
```

---

## Performance Testing

### Test Case P.1: No Frame Drops
**Steps:**
1. Complete 20+ rounds in one battle
2. Monitor frame rate

**Expected Result:**
- Stable 60 FPS
- No memory leaks
- Smooth animations

### Test Case P.2: Memory Usage
**Steps:**
1. Play 10 battles
2. Check memory usage

**Expected Result:**
- No memory leaks
- Stable memory footprint
- Proper cleanup after battles

---

## Bug Verification Checklist

- [ ] No EXP duplication
- [ ] No infinite level-ups
- [ ] No EXP desync
- [ ] No turn skipping
- [ ] No multiple actions per turn
- [ ] No item spam exploits
- [ ] No message timing issues
- [ ] No UI desync
- [ ] No crashes on edge cases
- [ ] No memory leaks

---

## Regression Testing

### Verify Old Features Still Work:
- [ ] Basic attacks function correctly
- [ ] Skills work as before
- [ ] HP bars update properly
- [ ] Enemy AI behaves normally
- [ ] Battle transitions smooth
- [ ] Victory/defeat screens work
- [ ] Escape from wild battles works
- [ ] Clawkin switching works

---

## Manual Testing Script

### Quick 5-Minute Test:
```
1. Start new game
2. Enter first battle
3. Use attack → Check round EXP granted
4. Use item → Verify turn ends
5. Complete battle → Check victory EXP
6. Verify level-up occurred
7. Start second battle
8. Repeat steps 3-6
9. Check console logs for errors
10. Verify EXP bar displays correctly
```

### Comprehensive 30-Minute Test:
```
1. Test all item types
2. Complete 10 battles
3. Test battle loss scenario
4. Test max level cap
5. Test rapid round completion
6. Test multiple level-ups
7. Test long battles (10+ rounds)
8. Test Clawkin switching
9. Test wild vs trainer battles
10. Review all console logs
```

---

## Known Limitations

### Current Implementation:
- Round EXP tracked in BattleStateMachine (not persisted to ClawkinData yet)
- Full ClawkinData integration pending
- Round EXP awarded at battle end along with victory EXP

### Future Integration:
- Real-time EXP bar updates during battle
- Persistent round EXP across save/load
- Animated level-up effects
- Visual "+5 EXP" notifications

---

## Reporting Issues

### If You Find a Bug:
1. Note the exact steps to reproduce
2. Check console logs for errors
3. Record current level and EXP values
4. Note which battle phase it occurred in
5. Check if it's reproducible

### Log Files to Check:
- Console output during battle
- BattleStateMachine logs
- BattleOverlay logs
- ExpManager logs

---

## Success Criteria

### The system is working correctly if:
✅ Every level requires exactly 50 EXP
✅ Victories grant ~100 EXP (2 levels)
✅ Each round grants 5 EXP
✅ Items consume player's turn
✅ Item messages display correctly
✅ EXP bar updates after rounds
✅ Level-ups process correctly
✅ No bugs or crashes occur
✅ Performance remains stable
✅ Old features still work

---

**Testing Status:** Ready for QA
**Priority:** High (Core gameplay mechanic)
**Estimated Testing Time:** 30-60 minutes for full coverage
