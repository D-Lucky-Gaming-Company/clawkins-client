# Map Asset Naming Convention Sync (2026-05-08)

## Context

Map identifiers were standardized so enum names consistently mirror `.tmx` filenames using uppercase snake case.

Examples:

- `nurse_interior.tmx` -> `NURSE_INTERIOR`
- `nurse_interior 2.tmx` -> `NURSE_INTERIOR_2`
- `shop_interior 3.tmx` -> `SHOP_INTERIOR_3`
- `cave_entrance.tmx` -> `CAVE_ENTRANCE`

## Files Updated

- `core/src/main/java/github/dluckycompany/clawkins/asset/MapAsset.java`
- `core/src/main/java/github/dluckycompany/clawkins/asset/MapAssetName.java`
- `core/src/main/java/github/dluckycompany/clawkins/GameScreen.java`
- `assets/maps/test.tiled-project`

## Canonical MapIdentifier Enum Set

`NURSE_INTERIOR`, `NURSE_INTERIOR_2`, `NURSE_INTERIOR_3`, `NURSE_INTERIOR_4`, `COTTAGE_SAMPLE`, `SHOP_INTERIOR`, `SHOP_INTERIOR_2`, `SHOP_INTERIOR_3`, `MOUNTAIN_1`, `MOUNTAIN_2`, `MOUNTAIN_3`, `MOUNTAIN_4`, `MOUNTAIN_5`, `CAVE_ENTRANCE`, `CAVE_1`, `CAVE_2`, `CAVE_3`, `FIELD`, `FIELD_2`, `FIELD_3`, `FIELD_4`, `FIELD_5`, `FIELD_SECRET`, `MANSION_MAZE`, `MANSION_GARDEN`, `MANSION_EXIT`, `BACKALLEY_1`, `BACKALLEY_2`, `BACKALLEY_3`, `BACKALLEY_4`, `BACKALLEY_EXIT`, `BACKALLEY_SECRET`, `TEST_WORLD`

## Tiled Project Alignment

`assets/maps/test.tiled-project` was updated to:

- use the canonical `MapIdentifier` values above
- set `MapTransitionTrigger.targetMap` default to `COTTAGE_SAMPLE`

## Compatibility Notes

`MapAsset.fromKey(...)` keeps legacy aliases so old transition/save keys continue to resolve:

- `NURSE_INTERIOR_1` -> `NURSE_INTERIOR`
- `SHOP` / `SHOP_1` -> `SHOP_INTERIOR`
- `SHOP_2` -> `SHOP_INTERIOR_2`
- `SHOP_3` -> `SHOP_INTERIOR_3`
- `MOUNTAIN` -> `MOUNTAIN_1`
- `CAVE` -> `CAVE_ENTRANCE`
- `COTTAGE` -> `COTTAGE_SAMPLE`
- `BACKALLEY` -> `BACKALLEY_1`

## Guidance For Future Changes

When adding a new `.tmx`:

1. Add a `MapAsset` enum that matches filename semantics exactly (uppercase snake case).
2. Add corresponding `MapAssetName` entry for area/variation display text.
3. Add the same value to `MapIdentifier` in `assets/maps/test.tiled-project`.
4. Keep alias entries only for backward compatibility when renaming existing identifiers.
