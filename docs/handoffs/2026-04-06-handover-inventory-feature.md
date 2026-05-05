# Inventory Feature Handoff

## Summary

This handoff covers the current full-screen inventory implementation opened with `E`.

The inventory is implemented as a separate cached screen and pauses world updates while active.

## Player-Facing Behavior

- Open from world with `E`.
- Inventory open is blocked during battle, dialogue, and map transitions.
- Uses full-screen `InventoryScreen`.
- `ESC` and back button return to `GameScreen`.
- `USE` opens party selection and applies item use.
- `DROP` opens quantity picker with plus/minus/exit buttons.
- Keyboard controls:
  - Item list navigation supports `WASD` and arrow keys.
  - `ENTER` mirrors click flow for item/action confirmation.
  - Inventory keyboard flow is two-step:
    1. `ENTER` on item list enters action mode.
    2. `A/LEFT` and `D/RIGHT` choose `USE` or `DROP`, then `ENTER` activates.
- Party target dialog controls:
  - `W/S` or `UP/DOWN` moves Clawkin highlight.
  - `ENTER` confirms selected Clawkin (same as click).
  - `ESC` cancels/close dialog.

## Runtime Integration

### Entry Point

- `GameScreen.toggleInventory()` calls `game.setScreen(InventoryScreen.class)`.

### Pause/Input Flow

- `InventoryScreen.show()` pauses `GameScreen` via `previousGameScreen.setPaused(true)`.
- `InventoryScreen.hide()` resumes via `previousGameScreen.setPaused(false)`.
- Inventory screen stores previous input processor on show and restores it on hide.

### Battle Safety

- `GameScreen.render()` force-closes inventory-related UI state when a battle starts.

## UI Architecture

### Main Classes

- `core/src/main/java/github/kinuseka/testproject/ui/InventoryScreen.java`
- `core/src/main/java/github/kinuseka/testproject/ui/InventoryUI.java`

### Layout

- Root full-screen table.
- Header with back button/title/wallet text.
- Two-column body:
  - left: details panel + `USE`/`DROP`
  - right: scrollable item list
- Styled with rounded panels and tan/beige palette.

### Modal/Refresh Model

- `PartySelectionDialog` is used for use-target selection.
- `DropQuantityDialog` is used for quantity selection when dropping.
- `refreshItemList()` repopulates rows and invalidates layout hierarchy after changes.
- Inventory now tracks keyboard action mode (item list vs `USE/DROP`) and button highlight state.
- `PartySelectionDialog` now supports keyboard navigation + enter confirm and a shared close callback path.

## Related Files

- `core/src/main/java/github/kinuseka/testproject/GameScreen.java`
- `core/src/main/java/github/kinuseka/testproject/ui/InventoryScreen.java`
- `core/src/main/java/github/kinuseka/testproject/ui/InventoryUI.java`
- `core/src/main/java/github/kinuseka/testproject/ui/InventoryOverlay.java`

## Known Caveats

- `InventoryOverlay` still exists as a separate UI path with its own dialog/target stages.
- If inventory and overlay paths are mixed, input processor ownership must be explicit.

## Validation Checklist

1. Press `E` and verify inventory opens.
2. Verify world gameplay is paused while inventory is visible.
3. Press `ESC` or click back and verify return to world.
4. Use `WASD`/arrows to navigate items and verify row highlight updates.
5. Press `ENTER` on an item and verify action mode engages (`USE/DROP` selection).
6. Use `A/LEFT` and `D/RIGHT` to switch `USE`/`DROP`, then press `ENTER` to activate.
7. On `USE`, verify party dialog appears; navigate targets with keyboard and confirm with `ENTER`.
8. On `DROP`, verify quantity dialog appears and list refreshes after confirm/cancel.
9. Start battle and verify inventory UI does not remain visible.
