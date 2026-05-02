# Highlight Positioning System - Technical Explanation

## Overview
The yellow border highlight moves between individual slots (top, middle, bottom) as the player navigates with arrow keys.

---

## Slot Layout

```
Container (300px height example)
┌─────────────────────────┐
│  ╔═══════════════════╗  │ ← Slot 0 (Top) - Ginger
│  ║   [Ginger Icon]   ║  │   Y position: 0-100px from top
│  ╚═══════════════════╝  │
├─────────────────────────┤
│  ┌───────────────────┐  │ ← Slot 1 (Middle) - Swee'pea
│  │ [Swee'pea Icon]   │  │   Y position: 100-200px from top
│  └───────────────────┘  │
├─────────────────────────┤
│  ┌───────────────────┐  │ ← Slot 2 (Bottom) - Dart
│  │   [Dart Icon]     │  │   Y position: 200-300px from top
│  └───────────────────┘  │
└─────────────────────────┘

Legend:
╔═══╗ = Yellow highlight (currently on Slot 0)
┌───┐ = No highlight
```

---

## Coordinate System

### Stack Coordinates (LibGDX)
The highlight is positioned inside a `Stack` widget, which uses **center-based coordinates**:

```
        Y = +150 (top of container)
              ↑
              │
              │
Y = 0 ────────┼──────── (center of container)
              │
              │
              ↓
        Y = -150 (bottom of container)
```

### Slot Positions
For a 300px tall container with 3 slots:
- **Slot height** = 300 / 3 = 100px
- **Slot 0 (Top)**: Center at Y = +100 (from container center)
- **Slot 1 (Middle)**: Center at Y = 0 (at container center)
- **Slot 2 (Bottom)**: Center at Y = -100 (from container center)

---

## Position Calculation

### Formula
```java
// Distance from container top to slot center
float slotCenterFromTop = (highlightedClawkinIndex + 0.5f) * slotHeight;

// Convert to Stack coordinates (relative to container center)
float highlightY = (containerH / 2f) - slotCenterFromTop;
```

### Example Calculation (300px container)
```
slotHeight = 300 / 3 = 100px
containerH / 2 = 150px (center position)

Slot 0 (index = 0):
  slotCenterFromTop = (0 + 0.5) * 100 = 50px
  highlightY = 150 - 50 = +100px ✓

Slot 1 (index = 1):
  slotCenterFromTop = (1 + 0.5) * 100 = 150px
  highlightY = 150 - 150 = 0px ✓

Slot 2 (index = 2):
  slotCenterFromTop = (2 + 0.5) * 100 = 250px
  highlightY = 150 - 250 = -100px ✓
```

---

## Highlight Sizing

### Size Relative to Slot
```java
float highlightW = slotWidth * 0.90f;   // 90% of slot width
float highlightH = slotHeight * 0.80f;  // 80% of slot height
```

### Why Smaller?
- **Padding**: Creates visual space between highlight and slot edges
- **Clarity**: Makes it clear which slot is selected
- **Aesthetics**: Prevents highlight from touching container borders

### Example (100px slot)
```
Slot dimensions: 100px × 100px
Highlight dimensions: 90px × 80px

Visual:
┌─────────────────────┐ ← Slot boundary (100px)
│  ╔═══════════════╗  │ ← Highlight (90px × 80px)
│  ║               ║  │   5px padding on each side
│  ║               ║  │   10px padding top/bottom
│  ╚═══════════════╝  │
└─────────────────────┘
```

---

## Movement Behavior

### Navigation Flow
```
Initial State (Slot 0):
┌─────────────────────────┐
│  ╔═══════════════════╗  │ ← Highlight here
│  ║   [Ginger]        ║  │
│  ╚═══════════════════╝  │
│  ┌───────────────────┐  │
│  │   [Swee'pea]      │  │
│  └───────────────────┘  │
│  ┌───────────────────┐  │
│  │   [Dart]          │  │
│  └───────────────────┘  │
└─────────────────────────┘

Press ↓ (Down Arrow):
┌─────────────────────────┐
│  ┌───────────────────┐  │
│  │   [Ginger]        │  │
│  └───────────────────┘  │
│  ╔═══════════════════╗  │ ← Highlight moved here
│  ║   [Swee'pea]      ║  │
│  ╚═══════════════════╝  │
│  ┌───────────────────┐  │
│  │   [Dart]          │  │
│  └───────────────────┘  │
└─────────────────────────┘

Press ↓ again:
┌─────────────────────────┐
│  ┌───────────────────┐  │
│  │   [Ginger]        │  │
│  └───────────────────┘  │
│  ┌───────────────────┐  │
│  │   [Swee'pea]      │  │
│  └───────────────────┘  │
│  ╔═══════════════════╗  │ ← Highlight moved here
│  ║   [Dart]          ║  │
│  ╚═══════════════════╝  │
└─────────────────────────┘

Press ↓ again (wraps to top):
┌─────────────────────────┐
│  ╔═══════════════════╗  │ ← Highlight wraps back
│  ║   [Ginger]        ║  │
│  ╚═══════════════════╝  │
│  ┌───────────────────┐  │
│  │   [Swee'pea]      │  │
│  └───────────────────┘  │
│  ┌───────────────────┐  │
│  │   [Dart]          │  │
│  └───────────────────┘  │
└─────────────────────────┘
```

---

## Code Flow

### When Arrow Key Pressed
```
User presses ↓
    ↓
BattleOverlay.update()
    ↓
BattleHud.moveSelectionDown()
    ↓
highlightedClawkinIndex++ (with wrapping)
    ↓
updateSelectionHighlight()
    ↓
Calculate new Y position
    ↓
selectionHighlight.setPosition(x, y)
    ↓
Visual update (highlight moves to new slot)
```

### Update Frequency
- **Only when needed**: Highlight position recalculates only when:
  1. Arrow key pressed (selection changes)
  2. Screen resized
  3. Container repositioned
- **Not per-frame**: Efficient, no performance impact

---

## Responsive Behavior

### Different Screen Sizes

#### Small Screen (150px container)
```
slotHeight = 150 / 3 = 50px

Slot 0: Y = +75 - 25 = +50px
Slot 1: Y = +75 - 75 = 0px
Slot 2: Y = +75 - 125 = -50px
```

#### Large Screen (450px container)
```
slotHeight = 450 / 3 = 150px

Slot 0: Y = +225 - 75 = +150px
Slot 1: Y = +225 - 225 = 0px
Slot 2: Y = +225 - 375 = -150px
```

### Scaling
- Container size: `MathUtils.clamp(h * 0.4f, 150f, 300f)`
- Highlight scales proportionally with container
- Maintains consistent visual appearance across resolutions

---

## Layering in Stack

### Z-Order (bottom to top)
```
Layer 3 (Top):    clawkinIconTable (icons)
Layer 2 (Middle): selectionHighlight (yellow border)
Layer 1 (Bottom): clawkinContainer (background)
```

### Why This Order?
1. **Container first**: Provides background
2. **Highlight second**: Sits behind icons but above background
3. **Icons last**: Always visible on top

### Visual Result
```
┌─────────────────────────┐ ← Container background
│  ╔═══════════════════╗  │ ← Highlight border
│  ║   [Icon Image]    ║  │ ← Icon on top
│  ╚═══════════════════╝  │
└─────────────────────────┘
```

---

## Key Implementation Details

### Centering in Stack
```java
selectionHighlight.setPosition(
    -(highlightW / 2f),           // Center horizontally
    highlightY - (highlightH / 2f) // Center vertically in slot
);
```

### Why Negative X?
- Stack uses center-based coordinates
- X = 0 is the horizontal center
- To center a 90px wide highlight:
  - Left edge at X = -45px
  - Right edge at X = +45px
  - Center at X = 0

### Slot Index Mapping
```java
highlightedClawkinIndex:
  0 → Top slot (Ginger)
  1 → Middle slot (Swee'pea)
  2 → Bottom slot (Dart)
```

---

## Troubleshooting

### Highlight Not Moving?
- Check `highlightedClawkinIndex` is updating
- Verify `updateSelectionHighlight()` is called
- Ensure highlight is visible: `setVisible(true)`

### Highlight in Wrong Position?
- Verify container dimensions match calculation
- Check Stack coordinate system (center-based)
- Confirm slot height calculation: `containerH / 3`

### Highlight Wrong Size?
- Check responsive sizing: `0.90f * slotWidth`
- Verify container size clamping
- Ensure highlight texture is square (128×128)

---

## Summary

The highlight system:
- ✅ **Moves between individual slots** (not fixed to container)
- ✅ **Uses center-based Stack coordinates** for positioning
- ✅ **Calculates position dynamically** based on slot index
- ✅ **Scales responsively** with screen size
- ✅ **Updates only when needed** (efficient)
- ✅ **Wraps around** (top ↔ bottom)
- ✅ **Properly layered** (behind icons, above background)

The formula `highlightY = (containerH / 2f) - slotCenterFromTop` ensures the highlight moves to the correct slot every time!
