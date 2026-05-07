# isTrippable Interactible Handover

## Goal

Add a map-driven flag that auto-triggers interactibles when the player enters their bounds.

## New property

- Name: `isTrippable`
- Type: `bool`
- Default: `false`
- Location: Interactible class in `assets/maps/test.tiled-project`

## Runtime behavior

- `isTrippable=false`:
  - Existing behavior remains: player uses interaction key near/facing object.
- `isTrippable=true`:
  - No keypress needed.
  - Interaction starts when player enters interactible bounds.
  - Trigger is zone-enter based (fires once per entry, not every frame while inside).

## Notes

- If `hasCollision=true`, player usually cannot enter the bounds, so trippable behavior will not occur in practice.
- Dialogue-first flow remains unchanged:
  - Dialogue runs first when present.
  - Special interaction callback runs after dialogue closes.

## Files changed

- `assets/maps/test.tiled-project`
- `core/src/main/java/github/dluckycompany/clawkins/component/Interactible.java`
- `core/src/main/java/github/dluckycompany/clawkins/tiled/TiledObjectConfigurator.java`
- `core/src/main/java/github/dluckycompany/clawkins/system/InteractionSystem.java`
- `docs/development/readme.md`
