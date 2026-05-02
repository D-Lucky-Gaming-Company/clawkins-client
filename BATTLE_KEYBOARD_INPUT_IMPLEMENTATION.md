# Battle HUD Keyboard Input Implementation

## Overview
Updated the Battle HUD to use **keyboard-only input** with enhanced features including run confirmation and visual Clawkin selection indicators.

---

## Changes Implemented

### 1. **Keyboard-Only Input System**

#### Removed Mouse Interaction
- **BattleHud.java**: Removed `Gdx.input.setInputProcessor(stage)` from `show()` method
- All battle buttons now use `loadButtonVisualOnly()` which disables touch/click interaction
- Buttons are displayed for visual reference only (showing key bindings)

#### Key Bindings
| Key | Action |
|-----|--------|
| **1-4** | Battle actions (Attack, Defend, Special, Item) |
| **E** | Toggle Inventory (open/close) |
| **X** | Attempt to Run (shows confirmation) |
| **Z/Space/Enter** | Confirm action / Advance dialogue |
| **Escape** | Cancel run confirmation |

---

### 2. **Run Confirmation System**

#### Flow
1. Player presses **X** during battle
2. System checks if it's a wild battle (trainer battles cannot flee)
3. Displays confirmation prompt: `"Are you sure you want to run?\n[Z] Yes  [X] No"`
4. Player must confirm:
   - **Z/Space/Enter** → Confirms and attempts to run
   - **X/Escape** → Cancels and returns to battle

#### Implementation Details
- Added `DialogueFlowPhase.RUN_CONFIRMATION` state
- `showRunConfirmation()` method displays the prompt
- `handleDialogueAdvance()` processes confirmation/cancellation
- No immediate flee - always requires confirmation

---

### 3. **Inventory Toggle System**

#### Behavior
- Press **E** to open inventory from battle
- Press **E** again (or close inventory) to return to battle
- Uses `inventoryOpen` flag to track state
- Prevents duplicate UI elements

#### Implementation
```java
private void toggleInventory() {
    if (inventoryOpen) {
        // Close inventory - return to battle
        resumeFromInventory();
        inventoryOpen = false;
    } else {
        // Open inventory
        openInventoryScreen();
    }
}
```

---

### 4. **Clawkin Selection Visual Indicator**

#### Slot Assignment
- **Top slot (index 0)** → Ginger
- **Middle slot (index 1)** → Swee'pea
- **Bottom slot (index 2)** → Dart

#### Visual Highlight
The currently selected (active) Clawkin is highlighted with:
- **Brighter color tint**: `setColor(1.2f, 1.2f, 1.0f, 1.0f)` (slight yellow tint)
- Non-selected icons use normal color: `setColor(1.0f, 1.0f, 1.0f, 1.0f)`

#### Dynamic Updates
- Selection updates automatically when active Clawkin changes
- `syncHudHpFromBattleState()` calls `findClawkinIndex()` to determine which slot to highlight
- `setSelectedClawkinIndex()` triggers visual refresh

#### Implementation
```java
// In positionClawkinContainer()
for (int i = 0; i < numSlots; i++) {
    Image icon = slotIcons[i];
    boolean isSelected = (i == selectedClawkinIndex);
    
    if (icon != null) {
        if (isSelected) {
            // Brighter and slightly scaled for selection indicator
            icon.setColor(1.2f, 1.2f, 1.0f, 1.0f); // Slight yellow tint
        } else {
            // Normal appearance
            icon.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        // ... add to table
    }
}
```

---

## Files Modified

### BattleHud.java
- Added `selectedClawkinIndex` field
- Added `setSelectedClawkinIndex()` and `getSelectedClawkinIndex()` methods
- Modified `show()` to NOT set input processor
- Replaced `loadButtonWithFeedback()` with `loadButtonVisualOnly()`
- Updated `positionClawkinContainer()` to apply visual highlight to selected icon
- Removed `attemptFlee()` method (logic moved to BattleOverlay)

### BattleOverlay.java
- Added `inventoryOpen` flag
- Added `DialogueFlowPhase.RUN_CONFIRMATION` state
- Updated `update()` method with keyboard controls:
  - **E** key → `toggleInventory()`
  - **X** key → `showRunConfirmation()`
- Added `toggleInventory()` method
- Added `showRunConfirmation()` method
- Updated `handleDialogueAdvance()` to handle run confirmation
- Updated `syncHudHpFromBattleState()` to set selected Clawkin index
- Added `findClawkinIndex()` helper method

---

## Testing Checklist

### Input System
- [ ] Mouse clicks on battle buttons do nothing
- [ ] Number keys 1-4 trigger battle actions
- [ ] E key opens inventory
- [ ] E key closes inventory when open
- [ ] X key shows run confirmation (wild battles only)
- [ ] X key does nothing in trainer battles

### Run Confirmation
- [ ] Pressing X shows "Are you sure you want to run?" prompt
- [ ] Z/Space/Enter confirms and attempts to run
- [ ] X/Escape cancels and returns to battle
- [ ] Cannot trigger other actions while confirmation is showing
- [ ] Confirmation works repeatedly without issues

### Inventory Toggle
- [ ] E opens inventory from battle
- [ ] E closes inventory and returns to battle
- [ ] No duplicate UI elements created
- [ ] Battle state preserved when returning from inventory
- [ ] Can open/close inventory multiple times

### Clawkin Selection Indicator
- [ ] Active Clawkin's icon is visually highlighted (brighter)
- [ ] Only one icon is highlighted at a time
- [ ] Highlight updates when active Clawkin changes
- [ ] Highlight persists across battle turns
- [ ] Non-active icons have normal appearance

---

## Design Decisions

### Why Keyboard-Only?
- **Consistency**: Battle system is turn-based and keyboard-driven
- **Speed**: Keyboard input is faster for experienced players
- **Accessibility**: Clear key bindings reduce cognitive load
- **No Conflicts**: Eliminates mouse/touch input conflicts with other UI elements

### Why Run Confirmation?
- **Prevents Accidents**: Players won't accidentally flee from important battles
- **Clear Intent**: Explicit confirmation ensures player wants to run
- **Consistent UX**: Matches other confirmation patterns in the game

### Why Visual Selection Indicator?
- **Clarity**: Players always know which Clawkin is active
- **Feedback**: Immediate visual feedback when switching Clawkins
- **Intuitive**: Highlight pattern is familiar from other RPGs

---

## Future Enhancements

### Potential Improvements
1. **Animated Highlight**: Add pulsing or glow effect to selected icon
2. **Sound Effects**: Add audio feedback for key presses
3. **Key Rebinding**: Allow players to customize key bindings
4. **Gamepad Support**: Add controller input support
5. **Selection Border**: Add a border sprite around selected icon for stronger visual indicator

### Advanced Features
- **Quick Switch**: Allow switching active Clawkin with arrow keys
- **Skill Preview**: Show skill details when hovering over action buttons
- **Battle Log**: Display recent battle actions in a scrollable log
- **Status Effects**: Show status effect icons on Clawkin container

---

## Notes

- All mouse/touch input is disabled for battle HUD elements
- Keyboard input is processed in `BattleOverlay.update()`
- Visual indicators update automatically via `syncHudHpFromBattleState()`
- Run confirmation uses the existing dialogue system
- Inventory toggle reuses the existing inventory screen
