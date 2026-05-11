# Handoff: Macaramboni Item and Level Boost System

**Date**: 2026-05-11  
**Status**: Complete  
**Related Tasks**: Cheat code system, item system, level system

## Overview

Implemented the Macaramboni item - a special consumable that boosts a Clawkin's level by 1. This includes:
- New `LevelBoostEffect` class for level manipulation
- Macaramboni item definition in `ItemFactory`
- Two new cheat codes: `maxlevel` and `macaramboni`
- Extended `PartySelectionDialog` to handle level boost items
- Added `SPECIAL` item type to support non-combat items

## Implementation Details

### 1. LevelBoostEffect Class
**File**: `core/src/main/java/github/dluckycompany/clawkins/item/LevelBoostEffect.java`

New effect class that implements `ItemEffect` interface:
- Boosts a Clawkin's level by a specified amount (default: 1)
- Uses `Clawkin.syncStatsToSharedExperienceLevel()` to properly update stats
- Validates that target is not already at max level
- Returns `false` if already at max level (prevents item consumption)

**Key Methods**:
```java
public boolean applyLevelBoost(Clawkin target)
public boolean requiresPartySelection()
```

### 2. Macaramboni Item
**File**: `core/src/main/java/github/dluckycompany/clawkins/item/ItemFactory.java`

Added new item constant:
```java
public static final Item MACARAMBONI = new Item(
    "macaramboni",
    "Macaramboni",
    "A delicious pasta dish that boosts a Clawkin's level by 1.",
    Item.ItemType.SPECIAL,
    new LevelBoostEffect(1),
    500,
    true,
    "68_macncheese_dish"
);
```

**Properties**:
- ID: `macaramboni`
- Type: `SPECIAL` (new type)
- Effect: Level boost +1
- Price: 500 coins
- Usable in battle: Yes
- Image: `68_macncheese_dish.png` (already exists in assets)

### 3. Item Type Extension
**File**: `core/src/main/java/github/dluckycompany/clawkins/item/Item.java`

Added new item type to enum:
```java
public enum ItemType {
    POTION,      // Healing item
    REVIVE,      // Revive item
    STAT_BOOSTER, // Stat boosting item
    SPECIAL      // Special items (e.g., level boost, rare items)
}
```

Added public getter for effect field:
```java
public ItemEffect getEffect()
```

### 4. Party Selection Dialog Updates
**File**: `core/src/main/java/github/dluckycompany/clawkins/ui/PartySelectionDialog.java`

Extended `canUseItemOn()` method to handle SPECIAL items:
- Checks if item is a `LevelBoostEffect`
- Validates target is not at max level
- Provides appropriate error message if already maxed

Updated transaction handler to track level changes:
- Records `previousLevel` and `newLevel`
- Provides level-specific feedback message
- Logs level boost transactions separately from HP restoration

**Validation Logic**:
```java
case SPECIAL -> {
    if (item.getEffect() instanceof LevelBoostEffect) {
        yield target.getLevel() < LevelSystem.MAX_LEVEL;
    }
    yield true;
}
```

### 5. Cheat Codes
**File**: `core/src/main/java/github/dluckycompany/clawkins/debug/CheatCodeManager.java`

#### `maxlevel` Cheat
- Instantly sets all party Clawkins to max level (20)
- Uses `syncStatsToSharedExperienceLevel()` for proper stat scaling
- Provides feedback: "All Clawkins maxed to level 20!"

#### `macaramboni` Cheat
- Adds 5 Macaramboni items to inventory
- Provides feedback with total quantity
- Useful for testing level boost functionality

Updated help cheat to include both new commands.

## Usage Flow

### Using Macaramboni Item
1. Player opens inventory (I key)
2. Selects Macaramboni from item list
3. Clicks "USE" button
4. Party selection dialog appears
5. Player selects target Clawkin
6. If not at max level:
   - Level increases by 1
   - Stats scale automatically (HP, ATK, DEF, SPD)
   - Item is consumed
   - Feedback message shows level change
7. If already at max level:
   - Error dialog appears
   - Item is NOT consumed
   - Player can select different Clawkin

### Using Cheat Codes
1. Press ` (backtick) to open cheat console
2. Type `maxlevel` to max all Clawkins
3. Type `macaramboni` to get 5 level boost items
4. Type `help` to see all available cheats

## Technical Notes

### Level System Integration
- Uses existing `LevelSystem.MAX_LEVEL` constant (20)
- Leverages `Clawkin.syncStatsToSharedExperienceLevel()` for stat growth
- Respects level caps and growth curves
- No direct EXP manipulation - only level changes

### Stat Scaling
When a Clawkin levels up via Macaramboni:
- HP increases based on growth curve
- Current HP adjusts proportionally (maintains relative health)
- ATK, DEF, SPD scale according to `StatGrowth` curves
- Skills unlock if new level threshold reached

### Party Selection Pattern
The existing `PartySelectionDialog` handles:
- Data binding (Clawkin → UI row)
- Validation (can use on target?)
- Transaction (apply effect → consume item)
- Feedback (success/error messages)
- State reconciliation (refresh inventory UI)

This pattern works seamlessly for level boost items without modification to the core dialog logic.

## Testing Checklist

- [x] Macaramboni item appears in inventory
- [x] Item image loads correctly (68_macncheese_dish.png)
- [x] Party selection dialog opens when using Macaramboni
- [x] Level boost applies correctly (level +1)
- [x] Stats scale properly after level boost
- [x] Item is consumed after successful use
- [x] Error message shows if Clawkin already at max level
- [x] Item is NOT consumed if already at max level
- [x] `maxlevel` cheat works for all party members
- [x] `macaramboni` cheat adds 5 items
- [x] Help cheat lists new commands

## Future Enhancements

### Potential Improvements
1. **Visual Effects**: Add level-up animation when Macaramboni is used
2. **Sound Effects**: Play level-up sound (different from heal sound)
3. **Stat Display**: Show stat changes in party selection dialog
4. **Bulk Items**: Create Macaramboni variants (e.g., "Super Macaramboni" for +5 levels)
5. **Merchant Integration**: Add Macaramboni to merchant shop inventories
6. **Rarity System**: Mark Macaramboni as "rare" or "legendary" item

### Code Patterns
The `LevelBoostEffect` pattern can be reused for:
- Stat boost items (permanent ATK/DEF/SPD increases)
- Skill unlock items (grant specific skills)
- Evolution items (change Clawkin form/type)
- Experience items (grant EXP instead of direct level)

## Files Modified

### New Files
- `core/src/main/java/github/dluckycompany/clawkins/item/LevelBoostEffect.java`

### Modified Files
- `core/src/main/java/github/dluckycompany/clawkins/item/ItemFactory.java`
- `core/src/main/java/github/dluckycompany/clawkins/item/Item.java`
- `core/src/main/java/github/dluckycompany/clawkins/ui/PartySelectionDialog.java`
- `core/src/main/java/github/dluckycompany/clawkins/debug/CheatCodeManager.java`

### Assets Used
- `assets/items/68_macncheese_dish.png` (existing)

## Related Documentation
- [Cheat Console Guide](../CHEAT_CONSOLE_GUIDE.md)
- [Item System Handoff](2026-05-11-handover-merchant-shop-system.md)
- [Level System Documentation](game-handoff.md)

---

**Implementation Complete**: All functionality tested and working as expected.
