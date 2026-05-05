# Team Viewer Feature Handoff

## Summary

This handoff covers the full-screen Team Viewer implementation opened with `P`.

The viewer renders a 3-slot party panel using Scene2D tables and uses explicit input multiplexer routing for keyboard and mouse.

## Player-Facing Behavior

- Open from world with `P`.
- Team viewer open is blocked during battle, dialogue, and map transitions.
- Displays exactly 3 party slots.
- Keyboard controls:
  - `W` / `A` / `S` / `D` and arrow keys cycle selected slot
  - `ENTER` confirms the currently highlighted card (same path as mouse select)
  - `ESC` closes viewer
- Mouse controls:
  - hover updates selection
  - click selects card and triggers callback
- Footer back button closes viewer.

## Runtime Integration

### Entry Point

- `GameScreen.toggleTeamViewer()` creates `TeamViewerScreen` and mounts it on `inventoryStage`.

### Pause/Input Flow

- Opening sets `teamViewerVisible = true` and `isPaused = true`.
- Closing sets `teamViewerVisible = false` and `isPaused = false`.
- `TeamViewerScreen` constructs an `InputMultiplexer` with:
  1. `Stage` first (UI click handling)
  2. `TeamViewerScreen` second (keyboard handling)
- `GameScreen` assigns `Gdx.input.setInputProcessor(teamViewerScreen.getInputMultiplexer())` when opening.

### Close Behavior (Critical)

- `exitTeamViewer()` closes immediately.
- It restores prior input processor first, then triggers `onBackPressed` callback.
- This avoids deferred-close failures when `delta == 0` (paused world).

### Battle Safety

- `GameScreen.render()` force-clears team viewer visibility when a battle begins.

## UI Architecture

### Main Classes

- `core/src/main/java/github/kinuseka/testproject/ui/TeamViewerScreen.java`
- `core/src/main/java/github/kinuseka/testproject/ui/ClawkinCard.java`

### Layout

- Full-screen dark occlusion root.
- Tan card area with 3 vertical cards.
- Footer row with back button and selection text.

### Card Model (`ClawkinCard`)

- Handles both real party members and null ghost slots.
- Displays mapped Clawkin portraits, metadata, and HP bar.
- Current portrait mapping:
  - Clawkin 1: Ginger (`Clawkin_01`)
  - Clawkin 2: Swee'pea (`Clawkin_02`)
  - Clawkin 3: Dart (`Clawkin_03`)
- Portrait loading uses path fallbacks (`ui/` first, then map entity paths).
- HP bar uses dynamic proportional width and threshold colors:
  - green: >50%
  - yellow: <=50%
  - red: <=20%
- Proportional fill now uses Scene2D cell width updates (not actor `setSize`) so values like `25/50` render at exactly 50% width.

## Related Files

- `core/src/main/java/github/kinuseka/testproject/GameScreen.java`
- `core/src/main/java/github/kinuseka/testproject/ui/TeamViewerScreen.java`
- `core/src/main/java/github/kinuseka/testproject/ui/ClawkinCard.java`

## Known Caveats

- Viewer currently renders through `inventoryStage` in `GameScreen`.
- In `GameScreen.toggleTeamViewer()`, close path sets input processor to `null`; if more overlays are layered in the future, this may require restoring a specific processor instead.

## Validation Checklist

1. Press `P` and verify team viewer opens.
2. Verify world gameplay is paused while viewer is visible.
3. Verify all 3 slots render (including empty slots).
4. Use `WASD` and arrow keys and verify highlight + footer text updates.
5. Click cards and verify selection callback behavior for non-empty slots.
6. Press `ENTER` and verify keyboard selection callback behavior for non-empty slots.
7. Press `ESC` and verify immediate close.
8. Verify HP bars are proportional to current HP (example: `25/50` is half bar).
9. Start battle and verify viewer is force-closed.
