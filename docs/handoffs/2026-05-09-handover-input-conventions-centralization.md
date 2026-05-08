# Handover: Centralized Input Conventions (2026-05-09)

## Why this was added
Menu/dialogue screens were repeatedly re-implementing the same key mappings (`Z/Enter/Space/A` for interact and `X/Escape/Backspace/B` for cancel), which caused drift and duplicated code.

## Source of truth
Use:

- `core/src/main/java/github/dluckycompany/clawkins/input/InputConventions.java`

This class now owns shared keyboard/controller conventions for:

- interact (just-pressed and keycode forms)
- cancel/unselect (just-pressed and keycode forms)
- menu directional navigation (`up/down/left/right`)

## Current canonical mapping
- **Interact / Confirm**: `Enter`, `Numpad Enter`, `Z`, `Space`, `Button A`
- **Cancel / Unselect / Back**: `X`, `Escape`, `Backspace`, `Button B`
- **Menu Up**: `W`, `Up`, `DPad Up`
- **Menu Down**: `S`, `Down`, `DPad Down`
- **Menu Left**: `A`, `Left`, `DPad Left`
- **Menu Right**: `D`, `Right`, `DPad Right`

## Integration rule for future work
When implementing new menu/dialogue/key navigation:

1. **Do not** hardcode these key sets in screen classes.
2. Call `InputConventions` helpers instead.
3. If mappings need to change, update only `InputConventions` so all consumers inherit it.

## Files already migrated in this pass
- `MainMenuScreen`
- `MainSideMenuOverlay`
- `TeamViewerScreen`
- `GameScreen` (prompt navigation/confirm flow)
- `InventoryUI`
- `PartySelectionDialog`
- `CharacterSetupScreen`
- `InteractionSystem`
- `BattleOverlay`
