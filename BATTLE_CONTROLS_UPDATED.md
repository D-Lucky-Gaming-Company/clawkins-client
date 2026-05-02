# Battle Controls - Complete Reference

## Battle Actions
```
[1] → Attack
[2] → Defend  
[3] → Special
[4] → Item
```

## System Controls
```
[E] → Toggle Inventory (Open/Close)
[X] → Attempt to Run (Shows confirmation)
[Z] / [Space] / [Enter] → Confirm / Advance Dialogue
[Escape] → Cancel Confirmation
```

## Clawkin Selection (NEW!)
```
[↑] → Navigate Selection Up
[↓] → Navigate Selection Down
[Enter] → Confirm Switch to Selected Clawkin
```

---

## Clawkin Selection System

### Visual Indicators

#### Yellow Border Highlight
- Shows which Clawkin you're about to switch to
- Moves with **↑/↓** arrow keys
- Wraps around (top ↔ bottom)

#### Active Clawkin (Brighter Icon)
- Slightly brighter than other icons
- This is the Clawkin currently in battle
- Changes only after confirming switch

#### Knocked Out Clawkin
- Shows grayed "_down" icon variant
- Cannot be selected for switching

### How to Switch Clawkins

1. **Navigate** using **↑/↓** to highlight desired Clawkin
2. **Press Enter** to initiate switch
3. **Confirmation appears**: "Switch to [Name]?\n[Z] Yes  [X] No"
4. **Confirm** with **Z** or **Cancel** with **X**

### Switch Rules
- ✅ Can switch to any alive Clawkin
- ❌ Cannot switch to knocked out Clawkin (HP = 0)
- ❌ Cannot switch to already active Clawkin
- ❌ Cannot switch to empty slot

---

## Slot Layout

```
┌─────────────────┐
│  [Ginger]   ← Top slot (0)
│  [Swee'pea] ← Middle slot (1)
│  [Dart]     ← Bottom slot (2)
└─────────────────┘
```

---

## Confirmation Flows

### Run Confirmation
1. Press **[X]** during battle
2. Prompt: "Are you sure you want to run?"
3. **[Z]** = Yes, attempt to run
4. **[X]** = No, return to battle

### Switch Confirmation
1. Navigate with **[↑/↓]** to desired Clawkin
2. Press **[Enter]**
3. Prompt: "Switch to [Name]?"
4. **[Z]** = Yes, switch Clawkin
5. **[X]** = No, keep current Clawkin

---

## Tips

### Navigation
- **Wrap Around**: Pressing ↑ at top goes to bottom, ↓ at bottom goes to top
- **Visual Feedback**: Yellow border shows your cursor position
- **Independent**: You can navigate without switching

### Strategy
- **Check HP**: Knocked out Clawkins show grayed icons
- **Plan Ahead**: Navigate to backup Clawkin before current one faints
- **Confirmation Safety**: Accidental Enter won't switch immediately

### Efficiency
- **Quick Actions**: Use number keys 1-4 for fast attacks
- **Inventory Access**: Press E anytime to check items
- **Emergency Switch**: Navigate and confirm in 3 key presses

---

## Complete Control Summary

| Key | Action | Confirmation? |
|-----|--------|---------------|
| **1-4** | Battle Actions | No |
| **E** | Toggle Inventory | No |
| **X** | Attempt Run | Yes |
| **↑** | Navigate Up | No |
| **↓** | Navigate Down | No |
| **Enter** | Initiate Switch | Yes |
| **Z/Space** | Confirm | - |
| **Escape** | Cancel | - |

---

## Visual Legend

| Icon State | Meaning |
|------------|---------|
| 🟨 **Yellow Border** | Currently highlighted (cursor) |
| ✨ **Brighter Icon** | Currently active (in battle) |
| 💀 **Grayed Icon** | Knocked out (HP = 0) |
| 🔲 **Normal Icon** | Alive but not active |

---

## Common Scenarios

### Scenario 1: Switch Before Fainting
```
Current: Ginger (Low HP)
Goal: Switch to Dart

1. Press ↓ twice (Ginger → Swee'pea → Dart)
2. Press Enter
3. Confirm: "Switch to Dart?" → Press Z
4. Dart is now active
```

### Scenario 2: Navigate Without Switching
```
Current: Ginger (Active)
Action: Just looking at options

1. Press ↓ (highlight moves to Swee'pea)
2. Press ↓ (highlight moves to Dart)
3. Press ↑ (highlight moves back to Swee'pea)
4. Don't press Enter → No switch occurs
```

### Scenario 3: Cancel Switch
```
Current: Ginger (Active)
Action: Changed mind

1. Press ↓ (highlight Swee'pea)
2. Press Enter
3. Prompt: "Switch to Swee'pea?"
4. Press X (cancel)
5. Ginger remains active
```

---

## Keyboard Layout Reference

```
┌─────┬─────┬─────┬─────┐
│  1  │  2  │  3  │  4  │  ← Battle Actions
└─────┴─────┴─────┴─────┘

        ┌─────┐
        │  ↑  │              ← Navigate Up
    ┌───┼─────┼───┐
    │ ← │  ↓  │ → │          ← Navigate Down
    └───┴─────┴───┘

┌─────────────────────────┐
│        Enter            │  ← Confirm Switch
└─────────────────────────┘

┌─────┐  ┌─────┐  ┌─────┐
│  E  │  │  X  │  │  Z  │  ← Inventory / Run / Confirm
└─────┘  └─────┘  └─────┘
```

---

## Important Notes

- **No Mouse Input**: All battle controls are keyboard-only
- **Confirmation Required**: Both running and switching require confirmation
- **Visual Feedback**: Always check the yellow border to see your selection
- **Safe Navigation**: You can explore options without committing
- **Persistent State**: Your selection persists across battle turns
