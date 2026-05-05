# TeamViewer Action Menu Handoff

Date: 2026-04-07  
Status: Active / Current

## Feature Summary

TeamViewer supports per-slot actions with three options:

- `CANCEL`
- `SWITCH`
- `SUMMARY`

Current behavior is fully wired. `SUMMARY` is no longer a placeholder.

## Player Controls

- Open TeamViewer with `P`.
- Select slot with `W/S` or arrow keys.
- Press `ENTER` (or `Z`/`SPACE`) to open action menu.
- Action menu navigation:
  - `A/D` or left/right to switch option highlight.
  - `ENTER` (or `Z`/`SPACE`) to confirm.
  - `ESC` (or `X`) to close action menu.

Footer prompt now says: `Press Enter to select action`.

## Action Outcomes

- `CANCEL`: closes menu only.
- `SWITCH`: sets selected clawkin as active fighter and persists to battle state.
- `SUMMARY`: opens standalone Summary screen for the selected clawkin.

## Integration Details

### TeamViewer

File: `core/src/main/java/github/kinuseka/testproject/ui/TeamViewerScreen.java`

- Action menu state machine and highlighting.
- Execution branch for `CANCEL` / `SWITCH` / `SUMMARY`.
- Summary callback API:
  - `setOnSummaryRequested(Runnable callback)`

### GameScreen

File: `core/src/main/java/github/kinuseka/testproject/GameScreen.java`

- Callback wiring:
  - `teamViewerScreen.setOnSummaryRequested(this::openSummaryFromTeamViewer)`
- Summary lifecycle:
  - `openSummaryFromTeamViewer()`
  - `closeSummaryToTeamViewer()`

### Battle State Persistence

Files:

- `core/src/main/java/github/kinuseka/testproject/battle/PlayerBattleState.java`
- `core/src/main/java/github/kinuseka/testproject/battle/BattleService.java`

Switch persists selected active clawkin index; battle startup prioritizes that slot if alive.

## Validation

- Build check:
  - `./gradlew.bat core:compileJava`
- Manual flow:
  1. Open TeamViewer.
  2. Confirm footer reads `Press Enter to select action`.
  3. Select `SUMMARY` and confirm Summary screen opens.
  4. Press `ESC` from Summary and confirm return to TeamViewer.

## Known Notes

- A legacy temporary hotkey (`Y`) exists in `GameScreen` while TeamViewer is visible; primary route is now action-menu `SUMMARY`.
- Existing style warnings in unrelated files are non-blocking.
