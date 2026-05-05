# Battle HUD Corner Buttons - Usage Guide

## Overview
Two new UI buttons have been added to the Battle HUD:
- **Inventory Button** (bottom-left corner, green bag icon)
- **Run/Flee Button** (bottom-right corner, red icon)

## Features

### Responsive Layout
- Both buttons automatically scale with screen resolution
- Maintain proper padding from screen edges (16-24px)
- Stay anchored to their respective corners regardless of screen size
- Button size: 48-80px (scales between 8-14% of screen dimensions)

### Visual Feedback
Both buttons include enhanced hover and click feedback:
- **Hover**: Scale up to 110% of original size
- **Click**: Scale down to 95% with slight alpha reduction (0.8)
- **Release**: Return to hover state
- Smooth visual transitions for better user experience

### Inventory Button
- **Location**: Bottom-left corner
- **Asset**: `ui/battle_ui/Sprites/Inventory_Button.png`
- **Function**: Opens the inventory screen

### Run/Flee Button
- **Location**: Bottom-right corner
- **Asset**: `ui/battle_ui/Sprites/Flee_Button.png`
- **Function**: Attempts to flee from battle
- **Restriction**: Only enabled during wild battles
- **Visual State**: Grayed out when disabled (trainer battles)

## Implementation Details

### Setting Up Callbacks

```java
// In your BattleOverlay or GameScreen class:
battleHud.setOnInventory(() -> {
    // Open inventory screen
    System.out.println("Opening inventory...");
    // TODO: Implement inventory screen transition
});

battleHud.setOnFlee(() -> {
    // Handle flee attempt
    System.out.println("Attempting to flee...");
    // TODO: Implement flee logic (success rate, animations, etc.)
});
```

### Configuring Battle Type

```java
// Set whether this is a wild battle (enables/disables flee button)
battleHud.setWildBattle(true);  // Wild battle - flee button enabled
battleHud.setWildBattle(false); // Trainer battle - flee button disabled
```

### Triggering Actions Programmatically

```java
// Trigger inventory action
battleHud.triggerInventory();

// Trigger flee action (only works if isWildBattle == true)
battleHud.triggerFlee();
```

## Layout Structure

The buttons are positioned using Scene2D Table layout:

```
┌────────────────────────────────────────────┐
│  [HP Bar]                      [HP Bar]    │  ← Top corners
│                                            │
│  [PLAYER]                      [ENEMY]     │  ← Center
│                                            │
│         [ATTACK] [DEFEND] [ITEM] [SPECIAL] │  ← Center-bottom
│                                            │
│  [INV]                            [FLEE]   │  ← Bottom corners
└────────────────────────────────────────────┘
```

## Technical Notes

### Responsive Sizing
```java
float cornerButtonSize = MathUtils.clamp(
    Math.min(worldW * 0.08f, worldH * 0.14f), 
    48f,  // minimum size
    80f   // maximum size
);

float cornerPad = MathUtils.clamp(
    Math.min(worldW * 0.015f, worldH * 0.025f), 
    16f,  // minimum padding
    24f   // maximum padding
);
```

### Button State Management
- Buttons are created in `buildWidgets()`
- Layout is applied in `applyResponsiveLayout()`
- Flee button state updates automatically when `setWildBattle()` is called
- Disabled buttons show visual feedback (gray color, no interaction)

## TODO Items

### Inventory Button
- [ ] Implement inventory screen transition
- [ ] Add inventory state management
- [ ] Handle item selection and usage in battle

### Flee Button
- [ ] Implement flee success rate calculation
- [ ] Add flee animation
- [ ] Handle post-flee state (return to overworld, etc.)
- [ ] Add sound effects for flee attempts

## Asset Requirements

Ensure these assets exist in your project:
- `assets/ui/battle_ui/Sprites/Inventory_Button.png`
- `assets/ui/battle_ui/Sprites/Flee_Button.png`

Both assets are already present in the project.

## Example Integration

```java
public class BattleOverlay {
    private BattleHud battleHud;
    
    public void startBattle(boolean isWildBattle) {
        // Configure battle type
        battleHud.setWildBattle(isWildBattle);
        
        // Set up callbacks
        battleHud.setOnInventory(() -> openInventory());
        battleHud.setOnFlee(() -> handleFleeAttempt());
        
        // Show the HUD
        battleHud.show();
    }
    
    private void openInventory() {
        // Pause battle
        // Show inventory UI
        // Handle item selection
    }
    
    private void handleFleeAttempt() {
        // Calculate flee success rate
        // Play animation
        // If successful, end battle and return to overworld
        // If failed, continue battle
    }
}
```

## Testing

To test the buttons:
1. Start a battle
2. Hover over the corner buttons to see scale animation
3. Click to trigger callbacks
4. Toggle `setWildBattle()` to see flee button enable/disable

## Notes
- The flee button automatically checks `isWildBattle` before executing
- Both buttons use the same responsive layout system as other HUD elements
- Button textures are properly disposed in the `dispose()` method
- All layout calculations use viewport-relative values for true responsiveness
