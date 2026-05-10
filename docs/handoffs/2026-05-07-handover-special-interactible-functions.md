# Special Interactible Functions (Dialogue-First Pipeline)

## Goal

Add support for object-id based "special interactions" without requiring new assets.

Desired runtime flow:

1. Player interacts with object.
2. Dialogue plays first when available.
3. After dialogue closes, run special logic for matching `ObjectId`.

## What was implemented

- Added `ObjectId -> callback` registry in `InteractionSystem`.
- Added post-dialogue execution queue for special interactions.
- Kept existing interaction behavior unchanged when no callback is registered.
- Added integration hook in `GameScreen` for project-specific callback wiring.

## Runtime contract

When an interactible is triggered:

1. `InteractionSystem` increments interaction count as before.
2. It resolves a special callback by normalized `ObjectId` (trim + lowercase).
3. Dialogue resolution runs as before.
4. If dialogue exists, callback is queued.
5. On dialogue close, queued callback executes exactly once.
6. If dialogue flow is empty, callback executes immediately.

Merchant behavior is unchanged and still short-circuits to merchant UI logic.

## New API surface

`InteractionSystem` now provides:

- `registerSpecialInteraction(String objectId, Consumer<SpecialInteractionContext> callback)`
- `registerSpecialInteractionByGroupId(String groupId, Consumer<SpecialInteractionContext> callback)`
- `registerPreDialogueCheck(String objectId, Predicate<SpecialInteractionContext> preDialogueCheck)`
- `registerPreDialogueCheckByGroupId(String groupId, Predicate<SpecialInteractionContext> preDialogueCheck)`
- `unregisterSpecialInteraction(String objectId)`
- `unregisterSpecialInteractionByGroupId(String groupId)`
- `unregisterPreDialogueCheck(String objectId)`
- `unregisterPreDialogueCheckByGroupId(String groupId)`
- `clearSpecialInteractions()`
- `clearPreDialogueChecks()`

And context payload:

- `SpecialInteractionContext.playerEntity()`
- `SpecialInteractionContext.targetEntity()`
- `SpecialInteractionContext.objectId()`
- `SpecialInteractionContext.objectName()`
- `SpecialInteractionContext.interactionCount()`

## Wiring location

Use this method in `GameScreen`:

- `registerSpecialInteractions()`

This is intentionally the single place to map object ids to game logic.

## Example chest implementation pattern

```java
interactionSystem.registerSpecialInteraction("chest_tutorial_01", context -> {
    playerBattleState.getWallet().addMoney(100);
    hudWallet.updateDisplay();
});
```

Notes:

- If chest should be one-time only, add your own persistence guard (save flag, map state, or inventory marker).
- Keep this logic data-driven by keying from `ObjectId`, not map coordinates.

## Update: Optional `GroupId` support (2026-05-11)

`GroupId` can now be added to interactible or merchant map objects, but it is optional.

- If `GroupId` is missing or blank, behavior remains unchanged.
- `TiledObjectConfigurator` reads `GroupId` (also accepts `groupId`) and stores it in `Interactible`.
- `InteractionSystem` can now register special interactions and pre-dialogue checks by `GroupId`.

Resolution order for callbacks/checks:

1. Match by normalized `ObjectId` first.
2. If no object-level match exists, fallback to normalized `GroupId`.

This keeps object-level behavior precise while enabling grouped behavior for multiple objects.

## Files changed

- `core/src/main/java/github/dluckycompany/clawkins/system/InteractionSystem.java`
- `core/src/main/java/github/dluckycompany/clawkins/GameScreen.java`
- `core/src/main/java/github/dluckycompany/clawkins/component/Interactible.java`
- `core/src/main/java/github/dluckycompany/clawkins/tiled/TiledObjectConfigurator.java`
- `docs/development/readme.md`

## Why this design

- Minimal and backward-compatible.
- No Tiled schema change required.
- Uses existing dialogue system and object ids already present in map contract.
- Future AI can add behavior by registering IDs only, with no core-system rewrite.
