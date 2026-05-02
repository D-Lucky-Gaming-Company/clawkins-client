# Battle Inventory Integration - Flow Diagram

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Main.java                               │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              Screen Cache System                          │  │
│  │  • GameScreen (cached)                                    │  │
│  │  • InventoryScreen (cached)                               │  │
│  │  • setScreen(Class) → retrieves cached instance           │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────┴─────────────────────┐
        ↓                                           ↓
┌──────────────────┐                    ┌──────────────────────┐
│   GameScreen     │                    │  InventoryScreen     │
│                  │                    │                      │
│  • BattleOverlay │                    │  • Stage (800x600)   │
│  • PlayerState   │                    │  • InventoryUI       │
│  • isPaused      │                    │  • previousGameScreen│
└──────────────────┘                    └──────────────────────┘
        │                                           ↑
        ↓                                           │
┌──────────────────┐                                │
│  BattleOverlay   │                                │
│                  │                                │
│  • BattleHud     │────────────────────────────────┘
│  • Main game     │    openInventoryScreen()
│  • inBattle      │
└──────────────────┘
        │
        ↓
┌──────────────────┐
│   BattleHud      │
│                  │
│  • inventoryBtn  │ ← Click triggers callback
│  • fleeBtn       │
│  • Stage         │
└──────────────────┘
```

## Interaction Flow

### Opening Inventory from Battle

```
┌─────────────────────────────────────────────────────────────────┐
│ Step 1: Player clicks Inventory button                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 2: BattleHud.inventoryBtn.onClick()                       │
│         → calls onInventory callback                            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 3: BattleOverlay.openInventoryScreen()                    │
│         → game.setScreen(InventoryScreen.class)                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 4: LibGDX calls InventoryScreen.show()                    │
│         → previousGameScreen.setPaused(true)                    │
│         → stage.clear()                                         │
│         → inventoryUI.buildLayout()                             │
│         → Gdx.input.setInputProcessor(stage)                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Result: Inventory screen visible, battle paused                │
│         Input: InventoryScreen.stage                            │
│         Battle state: Preserved in background                   │
└─────────────────────────────────────────────────────────────────┘
```

### Closing Inventory and Returning to Battle

```
┌─────────────────────────────────────────────────────────────────┐
│ Step 1: Player presses ESC or clicks Back button               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 2: InventoryScreen.returnToGameScreen()                   │
│         → previousGameScreen.setPaused(false)                   │
│         → battleOverlay.resumeFromInventory()                   │
│         → game.setScreen(GameScreen.class)                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 3: BattleOverlay.resumeFromInventory()                    │
│         → if (inBattle) battleHud.show()                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 4: BattleHud.show()                                        │
│         → Gdx.input.setInputProcessor(stage)                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 5: LibGDX calls InventoryScreen.hide()                    │
│         → stage.clear()                                         │
│         → Gdx.input.setInputProcessor(previousInputProcessor)   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Result: Battle screen visible, battle resumed                  │
│         Input: BattleHud.stage                                  │
│         Battle state: Exactly as before inventory opened        │
└─────────────────────────────────────────────────────────────────┘
```

## Input Processor State Machine

```
                    ┌──────────────────┐
                    │  Battle Active   │
                    │                  │
                    │  Input:          │
                    │  BattleHud.stage │
                    └────────┬─────────┘
                             │
                Inventory    │    ESC/Back
                button       │    pressed
                clicked      │
                             │
                    ┌────────▼─────────┐
                    │ Inventory Open   │
                    │                  │
                    │  Input:          │
                    │  Inventory.stage │
                    └────────┬─────────┘
                             │
                             │ resumeFromInventory()
                             │ + battleHud.show()
                             │
                    ┌────────▼─────────┐
                    │  Battle Active   │
                    │                  │
                    │  Input:          │
                    │  BattleHud.stage │
                    └──────────────────┘
                             │
                             │ Can repeat
                             │ indefinitely
                             ↓
```

## State Preservation

```
┌─────────────────────────────────────────────────────────────────┐
│                    Battle State (Preserved)                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  BattleService                                                  │
│  ├─ BattleStateMachine                                          │
│  │  ├─ Current Phase (PLAYER_TURN, ENEMY_TURN, etc.)           │
│  │  ├─ Turn Counter                                             │
│  │  └─ Battle Log                                               │
│  │                                                              │
│  └─ BattleContext                                               │
│     ├─ Allies (HP, stats, status)                               │
│     ├─ Enemies (HP, stats, status)                              │
│     ├─ Skills                                                   │
│     └─ Encounter Info                                           │
│                                                                 │
│  PlayerBattleState                                              │
│  ├─ Active Clawkin                                              │
│  ├─ Party                                                       │
│  ├─ Inventory ← Can be modified in InventoryScreen             │
│  └─ Wallet                                                      │
│                                                                 │
│  BattleHud (Visual State)                                       │
│  ├─ Player HP Bar                                               │
│  ├─ Enemy HP Bar                                                │
│  ├─ Button States                                               │
│  └─ Portraits                                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
         ↑                                           ↑
         │                                           │
    Preserved                                   Preserved
    during                                      during
    inventory                                   inventory
         │                                           │
         └───────────────────┬───────────────────────┘
                             │
                    No state corruption
                    No data loss
                    Exact resume point
```

## Screen Lifecycle

```
GameScreen.show()
    ↓
BattleOverlay.init()
    ↓
BattleHud created
    ↓
Battle starts
    ↓
┌─────────────────────────────────────┐
│  Battle Loop (GameScreen.render())  │
│  ├─ engine.update()                 │
│  ├─ battleOverlay.update()          │
│  └─ battleOverlay.render()          │
│     └─ battleHud.render()           │
└─────────────────────────────────────┘
    │
    │ Inventory button clicked
    ↓
InventoryScreen.show()
    ↓
┌─────────────────────────────────────┐
│ Inventory Loop (Inventory.render()) │
│  ├─ stage.act()                     │
│  └─ stage.draw()                    │
└─────────────────────────────────────┘
    │
    │ ESC/Back pressed
    ↓
InventoryScreen.hide()
    ↓
BattleOverlay.resumeFromInventory()
    ↓
BattleHud.show()
    ↓
┌─────────────────────────────────────┐
│  Battle Loop (GameScreen.render())  │
│  ├─ engine.update()                 │
│  ├─ battleOverlay.update()          │
│  └─ battleOverlay.render()          │
│     └─ battleHud.render()           │
└─────────────────────────────────────┘
    │
    │ Can repeat cycle
    ↓
```

## Memory Management

```
┌─────────────────────────────────────────────────────────────────┐
│                    Screen Cache (Main.java)                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  GameScreen (Single Instance)                                   │
│  ├─ Created once in GameScreen constructor                      │
│  ├─ Cached in Main.screenCache                                  │
│  ├─ show() called on screen switch                              │
│  ├─ hide() called on screen switch                              │
│  └─ dispose() called only on app exit                           │
│                                                                 │
│  InventoryScreen (Single Instance)                              │
│  ├─ Created once in GameScreen constructor                      │
│  ├─ Cached in Main.screenCache                                  │
│  ├─ show() called on screen switch                              │
│  │  └─ stage.clear() + rebuild UI                               │
│  ├─ hide() called on screen switch                              │
│  │  └─ stage.clear() + restore input                            │
│  └─ dispose() called only on app exit                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

Result:
✅ No memory leaks
✅ No duplicate instances
✅ Efficient resource usage
✅ Fast screen transitions (no recreation)
```

## Error Prevention

```
┌─────────────────────────────────────────────────────────────────┐
│                    Safeguards Implemented                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Null Checks                                                 │
│     if (game == null) return;                                   │
│     if (battleHud == null) return;                              │
│     if (previousGameScreen == null) return;                     │
│                                                                 │
│  2. State Validation                                            │
│     if (inBattle && battleHud != null) { ... }                  │
│     Only restore input if battle is actually active             │
│                                                                 │
│  3. Stage Cleanup                                               │
│     stage.clear() in show() and hide()                          │
│     Prevents duplicate actors                                   │
│                                                                 │
│  4. Input Processor Restoration                                 │
│     Save previous processor before switching                    │
│     Restore on return                                           │
│                                                                 │
│  5. Pause State Management                                      │
│     setPaused(true) when opening inventory                      │
│     setPaused(false) when closing inventory                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

**Visual Summary**: The system uses a clean screen-switching architecture with proper state preservation, input management, and resource cleanup. The battle state remains completely intact while the inventory is open, and the transition is seamless and repeatable.
