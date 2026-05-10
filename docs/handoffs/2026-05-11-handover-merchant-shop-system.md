# Merchant Shop System - Complete Implementation Handoff

**Date**: 2026-05-11  
**Status**: ✅ Complete and Functional  
**Feature**: Dialogue-First Merchant Shop with BUY/SELL Functionality

---

## Overview

The Merchant Shop System provides a complete two-panel UI for buying items from merchants and selling items from the player's inventory. The system integrates with the existing dialogue system, requiring players to interact with merchant NPCs and complete dialogue before the shop UI opens.

### Key Features

- **Dialogue-First Interaction**: Merchants trigger dialogue before opening shop
- **Two-Mode Operation**: BUY mode (merchant's items) and SELL mode (player's items)
- **Quantity Selection**: Graphical dialogs for selecting purchase/sell quantities
- **Infinite Stock Support**: Merchants can have infinite or limited stock
- **Wallet Integration**: Real-time money display and transaction handling
- **Keyboard + Mouse Navigation**: Full input support with audio feedback
- **Modal Overlays**: Proper z-ordering and input blocking for dialogs

---

## Architecture

### Core Components

1. **MerchantShopUI.java** - Main shop interface with two-panel layout
2. **BuyQuantityDialog.java** - Modal dialog for selecting purchase quantity
3. **SellQuantityDialog.java** - Modal dialog for selecting sell quantity
4. **MerchantInventory.java** - Merchant stock and pricing management
5. **InteractionSystem.java** - Special interaction registration for merchants
6. **GameScreen.java** - Rendering pipeline with full blackout overlay

### File Locations

```
core/src/main/java/github/dluckycompany/clawkins/
├── ui/
│   ├── MerchantShopUI.java
│   ├── BuyQuantityDialog.java
│   └── SellQuantityDialog.java
├── item/
│   └── MerchantInventory.java
├── system/
│   └── InteractionSystem.java
└── GameScreen.java

assets/
└── dialogue/
    └── merchants.json
```

---

## Merchant Registration

### Special Interaction Setup

Merchants are registered as special interactions in `GameScreen.java`:

```java
// Register merchant shops (dialogue-first, then shop opens)
interactionSystem.registerSpecialInteraction("shop_01", () -> {
    dialogueSystem.startDialogue("merchants", () -> {
        openMerchantShop("Merchant", MerchantInventory.createDefaultInventory());
    });
});

interactionSystem.registerSpecialInteraction("shop_02", () -> {
    dialogueSystem.startDialogue("merchants", () -> {
        openMerchantShop("Merchant", MerchantInventory.createDefaultInventory());
    });
});
```

### Tiled Map Setup

In Tiled, create merchant NPCs with:

- **Object Layer**: "Interactibles"
- **Object Type**: "npc" or "merchant"
- **Custom Property**: `objectId` = `"shop_01"` or `"shop_02"`

### Dialogue Configuration

Merchants use `assets/dialogue/merchants.json`:

```json
{
  "dialogueId": "merchants",
  "interactions": [
    {
      "level": 0,
      "lines": [
        "Welcome to my shop!",
        "I have many wares for sale.",
        "Take a look around.",
        "Let me know if you need anything."
      ]
    },
    {
      "level": 1,
      "lines": ["Back again? Take a look!"]
    }
  ]
}
```

**Interaction Levels**:

- Level 0: First visit (4 lines)
- Level 1+: Subsequent visits (1 line)

---

## MerchantInventory System

### Creating Merchant Stock

```java
// Infinite stock merchant (default)
MerchantInventory inventory = new MerchantInventory(true);
inventory.addItem(ItemFactory.BASIC_POTION, -1, 50);  // -1 = infinite
inventory.addItem(ItemFactory.FULL_HEAL, -1, 200);
inventory.addItem(ItemFactory.ATTACK_BOOST, -1, 100);
inventory.addItem(ItemFactory.DEFENSE_BOOST, -1, 100);
inventory.addItem(ItemFactory.REVIVE, -1, 300);

// Limited stock merchant
MerchantInventory limitedInventory = new MerchantInventory(false);
limitedInventory.addItem(ItemFactory.RARE_SWORD, 3, 500);  // Only 3 in stock
```

### Stock Representation

- **Infinite Stock**: `-1` in `stock` map
- **Limited Stock**: Positive integer (decreases on purchase)
- **Out of Stock**: `0` (item disappears from BUY list)

### Pricing

```java
// Custom pricing
inventory.addItem(item, quantity, buyPrice);

// Default pricing (2x sell price)
inventory.addItem(item, quantity);
```

---

## UI Layout and Design

### Color Palette

```java
PANEL_LEFT_BG = #E7DAC7    // Warm Beige (detail panel)
PANEL_RIGHT_BG = #C19253   // Warm Tan (item list)
PANEL_DIVIDER = #1F1A13    // Dark Coffee (borders)
TEXT_PRIMARY = #1F1A13     // Dark Brown (text)
ROW_SELECTED_BG = #ECCD61  // Warm Yellow (selected)
ROW_HOVER_BG = #CFA66B     // Light Tan (hover)
BUTTON_BG = #C19253        // Tan (buttons)
```

### Layout Structure

```
┌─────────────────────────────────────────────────────────┐
│ ← BACK                    MERCHANT'S SHOP    Money: 500 │
├──────────────────┬──────────────────────────────────────┤
│ DETAILS [BEIGE]  │ ITEMS [TAN]                          │
│                  │ [ BUY ] [ SELL ]                     │
│ Item Name        │ ┌──────────────────────────────────┐ │
│ Description      │ │ [Icon] Potion                    │ │
│ Type: Potion     │ │ [Icon] Full Heal                 │ │
│ Price: 50 gold   │ │ [Icon] Attack Boost              │ │
│                  │ │ [Icon] Defense Boost             │ │
│ [ BUY ] [ SELL ] │ │ [Icon] Revive                    │ │
│                  │ └──────────────────────────────────┘ │
└──────────────────┴──────────────────────────────────────┘
```

### Rounded Corners

All UI elements use `RoundedPanelDrawable` with NinePatch:

- Main panels: 12px radius
- Item rows: 8px radius
- Buttons: 8px radius
- Dialogs: 12px radius with 2px black stroke

---

## Shop Modes

### BUY Mode (Default)

**Display**:

- Shows merchant's inventory
- No quantity displayed on item rows (selected during purchase)
- Price shown in detail panel

**Flow**:

1. Player selects item
2. Detail panel shows item info + buy price
3. Player clicks BUY button
4. `BuyQuantityDialog` appears
5. Player selects quantity (limited by money and stock)
6. Confirm purchase
7. Transaction executes:
   - `wallet.removeMoney(totalPrice)`
   - `playerInventory.addItem(item, quantity)`
   - `merchantInventory.removeStock(item, quantity)` (if limited stock)
8. Wallet display refreshes immediately
9. Success popup shows
10. Item list refreshes (if out of stock)

### SELL Mode

**Display**:

- Shows player's inventory
- Quantity displayed as "xN" on each row
- Sell price shown in detail panel (50% of buy price)

**Flow**:

1. Player selects item
2. Detail panel shows item info + sell price
3. Player clicks SELL button
4. `SellQuantityDialog` appears
5. Player selects quantity (limited by owned quantity)
6. Confirm sale
7. Transaction executes:
   - `playerInventory.removeItem(item, quantity)`
   - `wallet.addMoney(totalPrice)`
8. Wallet display refreshes immediately
9. Success popup shows
10. Item list refreshes

---

## Quantity Dialogs

### BuyQuantityDialog

**Features**:

- Graphical +/- buttons (minus.png, plus.png)
- Centered quantity display
- Dynamic total price calculation
- Affordability checking: `maxAffordable = money / price`
- Stock checking: `maxStock = merchantInventory.getQuantity(item)`
- Max quantity: `Math.min(maxAffordable, maxStock)`

**Infinite Stock Handling**:

```java
int maxStock = merchantInventory.getQuantity(selectedItem);

// Convert -1 to Integer.MAX_VALUE for dialog
if (maxStock == -1) {
    maxStock = Integer.MAX_VALUE;
}

BuyQuantityDialog dialog = new BuyQuantityDialog(
    selectedItem, maxAffordable, maxStock, buyPrice, ...
);
```

**Keyboard Controls**:

- `A` / `Left Arrow`: Decrease quantity
- `D` / `Right Arrow`: Increase quantity
- `Enter` / `Z` / `Space`: Confirm
- `X` / `Escape`: Cancel

### SellQuantityDialog

**Features**:

- Same UI as BuyQuantityDialog
- Max quantity: Player's owned quantity
- Shows sell price (50% of buy price)

---

## Wallet Integration

### Real-Time Display

```java
// Store wallet label reference
private Label walletLabel;

// Create label in buildLayout()
walletLabel = new Label("Money: " + wallet.getMoney(), ...);

// Refresh after every transaction
private void refreshWalletDisplay() {
    if (walletLabel != null && wallet != null) {
        walletLabel.setText("Money: " + wallet.getMoney());
    }
}
```

### Transaction Safety

```java
// BUY transaction
int totalPrice = quantity * buyPrice;
if (wallet.getMoney() >= totalPrice) {
    wallet.removeMoney(totalPrice);
    playerInventory.addItem(item, quantity);
    merchantInventory.removeStock(item, quantity);
    refreshWalletDisplay();
    showSuccessDialog("Bought " + quantity + "x " + item.getName() + "\nfor " + totalPrice + " gold!");
}

// SELL transaction
int totalPrice = quantity * sellPrice;
playerInventory.removeItem(item, quantity);
wallet.addMoney(totalPrice);
refreshWalletDisplay();
showSuccessDialog("Sold " + quantity + "x " + item.getName() + "\nfor " + totalPrice + " gold!");
```

---

## Rendering Pipeline

### Full Blackout Overlay

The merchant shop uses a fully opaque black background to prevent game world bleed-through:

```java
// GameScreen.java
private void renderFullBlackoutOverlay() {
    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    shapeRenderer.setProjectionMatrix(inventoryStage.getCamera().combined);
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    shapeRenderer.setColor(0f, 0f, 0f, 1.0f);  // Fully opaque black
    shapeRenderer.rect(0, 0, VIRTUAL_UI_WIDTH, VIRTUAL_UI_HEIGHT);
    shapeRenderer.end();
    Gdx.gl.glDisable(GL20.GL_BLEND);
}

// Render call
if (!isBattleActive && merchantShopVisible && merchantShopUI != null) {
    renderFullBlackoutOverlay();  // Full blackout instead of dimming
    renderUIWithViewport(inventoryStage, uiDelta);
}
```

### Viewport Behavior

- Uses `FitViewport` for proper aspect ratio handling
- Fixed central panel with dark side-fill
- Works in fullscreen and windowed mode
- Matches InventoryUI layout strategy

---

## Tab Highlighting

### Dynamic Background Updates

```java
// Store tab button references
private TextButton buyTabRef;
private TextButton sellTabRef;

// Update highlights when mode changes
private void updateTabHighlights() {
    if (buyTabRef != null && sellTabRef != null) {
        buyTabRef.getStyle().up = RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(
            currentMode == ShopMode.BUY ? ROW_SELECTED_BG : BUTTON_BG, 8, 1, 0.85f);

        sellTabRef.getStyle().up = RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(
            currentMode == ShopMode.SELL ? ROW_SELECTED_BG : BUTTON_BG, 8, 1, 0.85f);
    }
}

// Call on mode change
buyTabRef.addListener(new ClickListener() {
    @Override
    public void clicked(InputEvent event, float x, float y) {
        if (currentMode != ShopMode.BUY) {
            currentMode = ShopMode.BUY;
            audioService.playSound(SoundEffect.UI_SELECT);
            updateTabHighlights();  // Update visual feedback
            populateItemList();
        }
    }
});
```

---

## Audio Integration

### Sound Effects

```java
// Reuses existing AudioService and SoundEffect enum
audioService.playSound(SoundEffect.UI_SELECT);  // Confirm actions
audioService.playSound(SoundEffect.UI_BACK);    // Cancel actions
audioService.playSound(SoundEffect.UI_HOVER);   // Item hover
```

### Audio Triggers

- **Item Selection**: `UI_SELECT`
- **Tab Switch**: `UI_SELECT`
- **Buy/Sell Confirm**: `UI_SELECT`
- **Cancel/Back**: `UI_BACK`
- **Item Hover**: `UI_HOVER`

---

## Keyboard Navigation

### List Navigation

```java
public boolean handleNavigationKey(int keycode) {
    // Priority: Active dialogs first
    if (activeBuyDialog != null) {
        return activeBuyDialog.handleNavigationKey(keycode);
    }
    if (activeSellDialog != null) {
        return activeSellDialog.handleNavigationKey(keycode);
    }

    // List navigation
    switch (keycode) {
        case Input.Keys.W:
        case Input.Keys.UP:
            navigateUp();
            return true;
        case Input.Keys.S:
        case Input.Keys.DOWN:
            navigateDown();
            return true;
        case Input.Keys.ENTER:
        case Input.Keys.Z:
        case Input.Keys.SPACE:
            if (actionMode) {
                executeAction();  // BUY or SELL
            } else {
                actionMode = true;
            }
            return true;
        case Input.Keys.X:
        case Input.Keys.ESCAPE:
            if (actionMode) {
                actionMode = false;
            } else {
                closeMerchantShop();
            }
            return true;
    }
    return false;
}
```

---

## Success Popup

### Layout and Styling

```java
private void showSuccessDialog(String message) {
    Dialog successDialog = new Dialog("", createWindowStyle()) {
        @Override
        protected void result(Object object) {
            // Close dialog on any button press
        }
    };

    successDialog.setModal(true);
    successDialog.setMovable(false);
    successDialog.setResizable(false);

    // Content table with proper padding
    Table contentTable = successDialog.getContentTable();
    contentTable.pad(30);  // Prevent text touching borders

    Label messageLabel = new Label(message, new Label.LabelStyle(font, TEXT_PRIMARY));
    messageLabel.setFontScale(1.5f);
    messageLabel.setWrap(true);
    messageLabel.setAlignment(Align.center);
    contentTable.add(messageLabel).width(300).center();

    // Button table with spacing
    TextButton okButton = new TextButton("OK", getRoundedButtonStyle());
    okButton.getLabel().setFontScale(1.4f);
    successDialog.button(okButton);
    successDialog.getButtonTable().pad(20);
    successDialog.getButtonTable().padTop(15);

    successDialog.show(stage);
}
```

---

## Bug Fixes Applied

### 1. Infinite Stock Display (FIXED)

**Problem**: BUY tab showed "x-1" for infinite stock items.

**Solution**: Remove quantity display entirely from BUY mode. Quantity is selected during purchase prompt.

```java
// Only show quantity in SELL mode
if (currentMode == ShopMode.SELL) {
    String quantityText = "x" + quantity;
    Label quantityLabel = new Label(quantityText, ...);
    textContainer.add(quantityLabel).right().padLeft(10f);
}
// BUY mode: no quantity label
```

### 2. Quantity Dialog Max Value (FIXED)

**Problem**: BuyQuantityDialog received `-1` as maxStock, preventing +/- buttons from working.

**Solution**: Convert `-1` to `Integer.MAX_VALUE` before passing to dialog.

```java
int maxStock = merchantInventory.getQuantity(selectedItem);
if (maxStock == -1) {
    maxStock = Integer.MAX_VALUE;  // Effectively infinite
}
```

### 3. Background Bleed-Through (FIXED)

**Problem**: Game world visible on left/right sides of merchant UI.

**Solution**: Use fully opaque black overlay instead of semi-transparent dimming.

```java
shapeRenderer.setColor(0f, 0f, 0f, 1.0f);  // Alpha = 1.0 (fully opaque)
```

### 4. Tab Highlighting (FIXED)

**Problem**: BUY/SELL tabs didn't update visual highlight when clicked.

**Solution**: Store tab references and dynamically update backgrounds on mode change.

### 5. Wallet Refresh (FIXED)

**Problem**: Money display didn't update after transactions.

**Solution**: Store wallet label reference and call `refreshWalletDisplay()` after every transaction.

---

## Testing Checklist

### BUY Mode

- [x] BUY tab highlighted on initial open
- [x] Items displayed without quantity
- [x] Clicking item shows detail panel with buy price
- [x] Clicking BUY opens quantity dialog
- [x] +/- buttons work correctly
- [x] Cannot buy beyond affordable amount
- [x] Cannot buy beyond stock (if limited)
- [x] Total price updates dynamically
- [x] Confirm executes transaction
- [x] Wallet updates immediately
- [x] Success popup shows with proper layout
- [x] Item list refreshes if out of stock

### SELL Mode

- [x] SELL tab highlights when clicked
- [x] Items displayed with "xN" quantities
- [x] Clicking item shows detail panel with sell price
- [x] Clicking SELL opens quantity dialog
- [x] +/- buttons work correctly
- [x] Cannot sell more than owned
- [x] Total price updates dynamically
- [x] Confirm executes transaction
- [x] Wallet updates immediately
- [x] Success popup shows
- [x] Item list refreshes

### Edge Cases

- [x] Buy with exact money (works)
- [x] Buy with insufficient money (shows error)
- [x] Buy when merchant out of stock (shows error)
- [x] Sell last item (works, item disappears)
- [x] Cancel buy dialog (no money deducted)
- [x] Cancel sell dialog (no money added)
- [x] Infinite stock doesn't decrease
- [x] Limited stock decreases correctly

### UI/UX

- [x] Background fully black (no game world visible)
- [x] Tab highlights update correctly
- [x] Wallet display updates immediately
- [x] Success popup readable with proper padding
- [x] Rounded corners on all elements
- [x] Keyboard navigation works
- [x] Mouse navigation works
- [x] Audio feedback on all actions
- [x] Fullscreen mode works
- [x] Windowed mode works

---

## Future Enhancements (Not Implemented)

- Bulk buy discount system
- Merchant reputation/haggling
- Item preview in quantity dialog
- Animated coin counter
- Merchant-specific dialogue after purchase
- Merchant restock system for limited stock
- Rare item rotation system
- Quest-locked items

---

## Compatibility

- ✅ Works with existing save/load system
- ✅ Compatible with battle context flag
- ✅ Works with infinite stock merchants
- ✅ Works with limited stock merchants
- ✅ Keyboard and mouse input both supported
- ✅ Reuses existing AudioService
- ✅ Reuses existing Wallet system
- ✅ Reuses existing Inventory system
- ✅ Integrates with dialogue system

---

## Performance Notes

- No performance impact - reuses existing dialog framework
- Wallet refresh is O(1) - just setText()
- Success dialog uses standard LibGDX Dialog class
- No new textures loaded (reuses existing button assets)
- Item list refresh is O(n) where n = number of items

---

## Code Ownership

**Primary Files**:

- `MerchantShopUI.java` - Main shop interface
- `BuyQuantityDialog.java` - Buy quantity selection
- `MerchantInventory.java` - Stock and pricing management

**Modified Files**:

- `GameScreen.java` - Added merchant shop rendering and registration
- `InteractionSystem.java` - Removed old merchant short-circuit

**Asset Files**:

- `assets/dialogue/merchants.json` - Merchant dialogue

---

## Summary

The Merchant Shop System is a complete, polished feature that integrates seamlessly with the existing game architecture. It provides a dialogue-first interaction pattern, two-mode operation (BUY/SELL), quantity selection dialogs, real-time wallet updates, and proper modal overlays. All bugs have been fixed, and the system is ready for production use.

**Key Achievements**:

- ✅ Dialogue-first merchant interaction
- ✅ Two-panel layout with rounded corners
- ✅ BUY and SELL modes with tab highlighting
- ✅ Quantity selection dialogs with graphical buttons
- ✅ Infinite and limited stock support
- ✅ Real-time wallet display updates
- ✅ Success popups with proper layout
- ✅ Full blackout overlay (no background bleed)
- ✅ Keyboard and mouse navigation
- ✅ Audio feedback integration
- ✅ Comprehensive error handling

**Status**: Production-ready ✅
