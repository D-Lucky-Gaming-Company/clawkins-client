# Interactible Dialogue System

## Summary

Implemented a full map-driven interactible object flow with dialogue and optional collision:

- New object type in Tiled: `Interactible`
- Player can interact only when facing object and pressing interaction key.
- Dialogue box supports top/bottom positioning with bottom default.
- Interactible collision defaults to true when not specified.
- Movement now respects solid interactible objects.

## Tiled Contract

Object requirements:

- Layer: `objects`
- Object name: `Interactible`

Supported custom properties:

- `ObjectName` (string) → default `"Object"`
- `ObjectText` (string) → default `"..."`
- `ObjectTextInteracted` (string, optional)
  - If missing/blank, system keeps showing `ObjectText` after first interaction.
- `hasCollision` (bool or string `"True"/"False"`)
  - Default: `true` when missing.
- `DialoguePosition` (`TOP` or `BOTTOM`)
  - Default: `BOTTOM` when missing/invalid.

## Interaction Behavior

Interaction keys:

- `Z`
- `SPACE`
- `ENTER`

Rules:

- Player must be facing the object and within interaction range.
- First interaction shows `ObjectText`.
- Subsequent interactions show:
  - `ObjectTextInteracted` if defined/non-blank,
  - otherwise `ObjectText`.
- Interaction while dialogue is open closes the dialogue.

## Collision Behavior

- `MoveSystem` now checks solid interactible entities in addition to map tile collision.
- Interactibles with `hasCollision=true` block movement.
- Interactibles with `hasCollision=false` are pass-through.

## Files Added

- `core/src/main/java/github/kinuseka/testproject/component/Interactible.java`
- `core/src/main/java/github/kinuseka/testproject/system/InteractionSystem.java`
- `core/src/main/java/github/kinuseka/testproject/ui/DialogueOverlay.java`

## Files Updated

- `core/src/main/java/github/kinuseka/testproject/tiled/TiledObjectConfigurator.java`
  - Added `Interactible` case and custom property parsing/defaults.
  - Robust boolean parse for `hasCollision`.
- `core/src/main/java/github/kinuseka/testproject/system/MoveSystem.java`
  - Added entity-level blocking check for solid interactibles.
- `core/src/main/java/github/kinuseka/testproject/GameScreen.java`
  - Registered `InteractionSystem`.
  - Added `DialogueOverlay` render + dispose.
  - Interaction system processing tied to exploration state.
- `docs/development/readme.md`
  - Added dedicated Interactible + Dialogue developer section.

## Validation

- `core:compileJava` succeeds.
- No new linter issues in changed files.

## Notes

- Dialogue rendering currently uses a simple runtime overlay (shape + bitmap font), no custom skin assets required.
- This integrates cleanly with current battle pause behavior (interaction processing disabled while battle session is active).
