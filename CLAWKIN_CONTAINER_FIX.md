# Clawkin Container Alignment Fix

## Problem
The Clawkin icons were not properly aligned inside the container slots. They appeared misaligned or outside the intended boundaries because the container and icons were being positioned independently as separate actors on the stage.

## Root Cause
The original implementation added the container and icon table as separate actors to the stage:
```java
stage.addActor(clawkinContainer);  // Container as separate actor
stage.addActor(clawkinIconTable);  // Icons as separate actor
```

This caused:
- ❌ Icons positioned independently from container
- ❌ No parent-child relationship
- ❌ Manual positioning required for both
- ❌ Icons could float outside container
- ❌ Difficult to maintain alignment

## Solution
Restructured the UI to use proper parent-child relationships with Scene2D's **Stack** widget:

### New Architecture
```
clawkinWrapper (Table)
    └── clawkinStack (Stack)
        ├── clawkinContainer (Image) ← Background layer
        └── clawkinIconTable (Table) ← Foreground layer (icons)
```

### Key Changes

#### 1. Added Stack Widget
```java
import com.badlogic.gdx.scenes.scene2d.ui.Stack;

// New fields
private Stack clawkinStack;  // Layers container and icons
private Table clawkinWrapper; // Positions the stack on screen
```

#### 2. Restructured buildClawkinContainer()
```java
private void buildClawkinContainer() {
    // Create container background
    clawkinContainer = new Image(...);
    clawkinContainer.setScaling(Scaling.stretch);
    
    // Create icon table
    clawkinIconTable = new Table();
    clawkinIconTable.center(); // Center icons within table
    
    // Stack layers: container background + icons foreground
    clawkinStack = new Stack();
    clawkinStack.add(clawkinContainer); // Layer 0: Background
    clawkinStack.add(clawkinIconTable); // Layer 1: Icons
    
    // Wrapper table for screen positioning
    clawkinWrapper = new Table();
    clawkinWrapper.setFillParent(true);
    clawkinWrapper.left().center(); // Left side, vertically centered
    clawkinWrapper.add(clawkinStack); // Add stack to wrapper
}
```

#### 3. Updated buildWidgets()
```java
// OLD: Added container and icons separately
stage.addActor(clawkinContainer);
stage.addActor(clawkinIconTable);

// NEW: Add only the wrapper (contains everything)
stage.addActor(clawkinWrapper);
```

#### 4. Rewrote positionClawkinContainer()
```java
private void positionClawkinContainer() {
    float containerW = MathUtils.clamp(w * 0.08f, 60f, 100f);
    float containerH = MathUtils.clamp(h * 0.4f, 150f, 300f);
    
    // Update wrapper positioning
    clawkinWrapper.clearChildren();
    clawkinWrapper.left().center();
    clawkinWrapper.pad(10f, 10f, 0f, 0f); // 10px from edges
    
    // Set stack size (container + icons share same bounds)
    clawkinWrapper.add(clawkinStack).size(containerW, containerH);
    
    // Rebuild icon table with proper sizing
    clawkinIconTable.clearChildren();
    clawkinIconTable.center();
    
    if (currentParty != null) {
        int partySize = currentParty.size();
        float availableHeight = containerH * 0.85f; // 85% of container
        float iconSlotHeight = availableHeight / partySize;
        float iconSize = Math.min(
            iconSlotHeight * 0.8f,  // 80% of slot height
            containerW * 0.65f      // or 65% of container width
        );
        
        // Add icons with proper spacing
        for (Clawkin clawkin : currentParty) {
            Image icon = createClawkinIcon(clawkin);
            clawkinIconTable.add(icon)
                .size(iconSize, iconSize)
                .pad(iconSlotHeight * 0.1f) // 10% padding
                .center()
                .row();
        }
    }
}
```

#### 5. Simplified updateClawkinContainer()
```java
public void updateClawkinContainer(List<Clawkin> party) {
    this.currentParty = party;
    positionClawkinContainer(); // Rebuilds icons with proper layout
}
```

## How It Works Now

### Parent-Child Hierarchy
```
Stage
  └── clawkinWrapper (Table, fillParent=true)
      └── clawkinStack (Stack, sized to container dimensions)
          ├── clawkinContainer (Image, background)
          └── clawkinIconTable (Table, foreground)
              ├── Icon 1 (Ginger)
              ├── Icon 2 (Dart)
              └── Icon 3 (Swee'pea)
```

### Layering
1. **Stack** ensures container and icons share the same bounds
2. **Container** (Image) fills the stack as background
3. **Icon Table** (Table) fills the stack as foreground
4. **Icons** are centered within the table using `.center()`

### Sizing Logic

#### Container Size
```java
containerW = clamp(screenWidth * 0.08f, 60f, 100f)
containerH = clamp(screenHeight * 0.4f, 150f, 300f)
```

#### Icon Slot Calculation
```java
availableHeight = containerH * 0.85f  // Use 85% of container
iconSlotHeight = availableHeight / partySize
```

#### Icon Size
```java
iconSize = min(
    iconSlotHeight * 0.8f,  // 80% of slot height
    containerW * 0.65f      // 65% of container width
)
```

#### Icon Padding
```java
padding = iconSlotHeight * 0.1f  // 10% of slot height
```

### Alignment

#### Horizontal Alignment
- **Wrapper**: `.left()` - Anchored to left side of screen
- **Icon Table**: `.center()` - Icons centered within table
- **Icons**: `.center()` - Each icon centered in its cell

#### Vertical Alignment
- **Wrapper**: `.center()` - Container vertically centered on screen
- **Icon Table**: `.center()` - Icons centered within container
- **Icons**: Distributed evenly with padding

## Benefits of New Structure

### ✅ Proper Parent-Child Relationship
- Icons are children of the stack
- Container and icons share same bounds
- No independent positioning needed

### ✅ Automatic Alignment
- Stack ensures icons stay within container
- Table layout handles icon distribution
- Center alignment built into layout

### ✅ Responsive Behavior
- Single size calculation for stack
- Icons scale proportionally
- Maintains alignment at all resolutions

### ✅ No Manual Positioning
- Layout-based positioning only
- No hardcoded coordinates
- Scene2D handles all positioning

### ✅ Clean Code
- Single actor added to stage (wrapper)
- Clear hierarchy
- Easy to maintain

## Visual Result

### Before (Broken)
```
┌─────────────┐
│             │
│             │  [Icon] ← Floating outside
│             │
│             │     [Icon] ← Misaligned
│             │
└─────────────┘
  [Icon] ← Outside container
```

### After (Fixed)
```
┌─────────────┐
│             │
│   [Icon]    │ ← Centered in slot 1
│             │
│   [Icon]    │ ← Centered in slot 2
│             │
│   [Icon]    │ ← Centered in slot 3
│             │
└─────────────┘
```

## Testing Checklist

- ✅ Icons appear inside container bounds
- ✅ Icons centered horizontally in container
- ✅ Icons distributed evenly vertically
- ✅ No icons floating outside container
- ✅ Proper spacing between icons
- ✅ Icons scale with container size
- ✅ Alignment maintained on screen resize
- ✅ Works with 1, 2, or 3 party members
- ✅ Health state changes work correctly
- ✅ No flickering or visual artifacts

## Code Comparison

### Old Approach (Broken)
```java
// Separate actors, independent positioning
stage.addActor(clawkinContainer);
stage.addActor(clawkinIconTable);

// Manual positioning required
clawkinContainer.setPosition(x, y);
clawkinIconTable.setPosition(x, y); // Must match!
```

### New Approach (Fixed)
```java
// Single wrapper, hierarchical structure
stage.addActor(clawkinWrapper);

// Layout-based positioning
clawkinWrapper.left().center();
clawkinWrapper.add(clawkinStack).size(w, h);
```

## Files Modified

**BattleHud.java**
- Added `Stack` import
- Added `clawkinStack` and `clawkinWrapper` fields
- Rewrote `buildClawkinContainer()` to use Stack
- Rewrote `positionClawkinContainer()` with proper layout
- Simplified `updateClawkinContainer()`
- Updated `buildWidgets()` to add only wrapper

## Technical Details

### Stack Widget
Scene2D's `Stack` widget layers actors on top of each other:
- All children share the same bounds
- Children are drawn in order (first = bottom, last = top)
- Perfect for background + foreground layering

### Table Layout
Scene2D's `Table` widget provides flexible layout:
- `.center()` centers content
- `.add(actor).size(w, h)` sets cell size
- `.pad(p)` adds padding
- `.row()` moves to next row

### Wrapper Pattern
Using a wrapper table for positioning:
- Wrapper handles screen positioning (`.left().center()`)
- Stack handles layering (container + icons)
- Icon table handles icon layout (vertical distribution)

## Conclusion

The fix restructures the Clawkin container to use proper Scene2D parent-child relationships with Stack and Table widgets. Icons are now properly contained within the container bounds, centered in their slots, and maintain alignment across all screen sizes.

**Status**: ✅ Fixed and Verified  
**Build**: ✅ Passing  
**Alignment**: ✅ Perfect  
**Responsiveness**: ✅ Maintained
