# Cheat Console Testing Guide

## What Was Fixed

### 1. Input Isolation ✅

- **Problem**: Typing in the console would move the character
- **Solution**:
  - Added `cheatConsoleLocked` flag to `syncSystemStates()`
  - PlayerInputSystem is now disabled when console is open
  - MoveSystem is disabled when console is open
  - Input processor is set to the console stage only

### 2. Game State Verification ✅

- **Problem**: Need to verify cheats actually modify game values
- **Solution**:
  - Added detailed logging for all cheat executions
  - Added before/after value logging
  - Money changes reflect in inventory screen
  - All cheats modify actual PlayerBattleState

### 3. Rendering Fix ✅

- **Problem**: Console wasn't rendering (game appeared frozen)
- **Solution**:
  - Created dedicated `cheatConsoleStage` for independent rendering
  - Console now renders on its own pipeline

## How to Test

### Test 1: Input Isolation

1. **Start the game** and enter gameplay
2. **Press F12** - Console should appear
3. **Type "WASD"** - Character should NOT move
4. **Type "money"** and press ENTER
5. **Press ESC** to close console
6. **Press WASD** - Character SHOULD move now

**Expected Result**: ✅ No character movement while console is open

### Test 2: Money Cheat Verification

1. **Open inventory** (press E, then INVENTORY) and note current money
2. **Close inventory** and **press F12**
3. **Type "money"** and press ENTER
4. **Check console logs** - Should show: `Money cheat: X -> Y`
5. **Open inventory** - Should show +1000 coins
6. **Type "rich"** and press ENTER
7. **Open inventory** - Should show +10000 more coins

**Expected Result**: ✅ Money increases and shows in inventory

### Test 3: Items Cheat Verification

1. **Open inventory** (press E, then select INVENTORY)
2. **Note current item counts**
3. **Close inventory** and **press F12**
4. **Type "items"** and press ENTER
5. **Check console logs** - Should show before/after item counts
6. **Open inventory again**
7. **Verify items were added**:
   - +5 Potions
   - +2 Elixirs
   - +3 Attack Boosts
   - +3 Defense Boosts

**Expected Result**: ✅ Items appear in inventory

### Test 4: Heal Cheat Verification

1. **Get into a battle** (walk around until encounter)
2. **Take damage** (let enemy hit you)
3. **Escape from battle** (if possible) or win
4. **Press F12** to open console
5. **Type "heal"** and press ENTER
6. **Check console logs** - Should show HP changes for each party member
7. **Open team viewer** (press E, then CLAWKINS)
8. **Verify all party members are at full HP**

**Expected Result**: ✅ All party members healed to max HP

### Test 5: Console Behavior

1. **Press F12** - Console opens
2. **Type invalid cheat** (e.g., "asdf") and press ENTER
3. **Should see RED error message**: "Unknown cheat: asdf"
4. **Type "help"** and press ENTER
5. **Should see GREEN success message** and check console logs for cheat list
6. **Press ESC** - Console closes
7. **Press F12** again - Console reopens (empty input field)

**Expected Result**: ✅ Proper feedback for valid/invalid cheats

## Console Logs to Watch

When testing, watch the console output for these messages:

### Initialization

```
[CheatConsoleOverlay] Initializing cheat console overlay
[CheatConsoleOverlay] Creating UI components
[CheatConsoleOverlay] Console positioned at: X, Y
[CheatConsoleOverlay] Stage viewport: 800.0x600.0
[CheatConsoleOverlay] Console table added to stage. Actors in stage: 1
[CheatConsoleOverlay] Cheat console overlay initialized successfully
```

### Money Cheat

```
[CheatCodeManager] Money cheat: 500 -> 1500
[GameScreen] HUD wallet updated after cheat: 1500
[CheatConsole] Executed cheat: money | Result: Added 1000 coins! Total: 1500
```

### Items Cheat

```
[CheatCodeManager] Items cheat:
  Potions: 3 -> 8
  Elixirs: 1 -> 3
  Attack Boosts: 1 -> 4
  Defense Boosts: 1 -> 4
[CheatConsole] Executed cheat: items | Result: Added test items...
```

### Heal Cheat

```
[CheatCodeManager] Heal cheat results:
  Clawkin1: 45 -> 100 HP
  Clawkin2: 67 -> 120 HP
  Clawkin3: Already at full HP (80)
[CheatConsole] Executed cheat: heal | Result: Healed 2 party member(s) to full HP!
```

## All Available Cheats

| Cheat      | Effect                 | Verification                    |
| ---------- | ---------------------- | ------------------------------- |
| `money`    | +1000 coins            | Check inventory screen          |
| `rich`     | +10000 coins           | Check inventory screen          |
| `poor`     | Remove all money       | Check inventory screen          |
| `heal`     | Heal all party members | Open team viewer (E → CLAWKINS) |
| `items`    | Add test items         | Open inventory (E → INVENTORY)  |
| `whereami` | Show location          | Check console logs              |
| `help`     | List all cheats        | Check console logs              |

## Troubleshooting

### Character still moves when typing

- Check console logs for "cheatConsoleLocked" in syncSystemStates
- Verify PlayerInputSystem.setProcessing(false) is being called
- Make sure you're typing in the console, not just pressing keys

### Items don't appear in inventory

- Check console logs for "Items cheat:" with before/after counts
- Verify no exceptions in the logs
- Make sure you're checking the correct inventory screen

### Console doesn't appear

- Check console logs for "CheatConsoleOverlay" initialization messages
- Verify you're in gameplay (not menu or battle)
- Try pressing F12 multiple times

## Success Criteria

All tests should pass with these results:

✅ **Input Isolation**: No character movement while console is open  
✅ **Money Cheat**: Money shows in inventory, logs show correct values  
✅ **Items Cheat**: Items appear in inventory, logs show quantities  
✅ **Heal Cheat**: Party members healed, logs show HP changes  
✅ **Console UI**: Proper feedback messages, opens/closes correctly

## Notes

- All cheats modify the **actual game state** (PlayerBattleState)
- Changes are **persistent** for the current game session
- Console logs provide **detailed verification** of all changes
- Input is **completely isolated** when console is open
