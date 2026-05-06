# Map Integration Handoff Guide (For Future AI Agents)

This guide is a quick, repeatable checklist for adding new playable maps and wiring map transitions.

## Goal
When a new `.tmx` map is added, it must:
- Appear in Tiled trigger dropdowns (`targetMap`)
- Resolve in runtime (`MapAsset.fromKey`)
- Load correctly when a transition fires

## Files to update
- `core/src/main/java/github/dluckycompany/clawkins/asset/MapAsset.java`
- `assets/maps/test.tiled-project`
- Optional docs update: `docs/development/map-transition-trigger-guide.md`

## Step-by-step checklist

1) Locate new map files
- Check `assets/maps/**` for new `.tmx` files.
- Use stable enum naming (UPPER_SNAKE_CASE) matching existing convention.

2) Register runtime map keys
- In `MapAsset`, add enum entries pointing to the exact map path under `assets/maps/`.
- Example pattern:
  - `NEW_ZONE("folder/new_zone.tmx")`
- Keep paths relative to `maps/` because runtime builds descriptor as `"maps/" + mapPath`.

3) Add trigger dropdown options
- In `assets/maps/test.tiled-project`, find property type `MapIdentifier`.
- Append each new key to `values`.
- Keep key names exactly matching `MapAsset` enum names.

4) Wire triggers in Tiled maps
- For each source trigger object:
  - `ObjectType = MAP_TRANSITION`
  - `targetMap = <MapIdentifier key>`
  - `targetTransitionId = <destination transitionId on target map>`
- Ensure destination map has a trigger object with matching `transitionId`.

5) Verify spawn anchor integrity
- Confirm destination `transitionId` exists and is unique in the destination map.
- Avoid duplicate `transitionId` values on the same map.

6) Sanity test in-game
- Trigger transition from source map.
- Confirm target map loads and player appears at destination trigger.
- Confirm no immediate bounce-back loop (cooldown handles this, but bad trigger overlap can still feel wrong).

## Naming conventions
- For connected zones:
  - `BACKALLEY`, `BACKALLEY_2`, `BACKALLEY_EXIT`, `BACKALLEY_SECRET`
- For transition ids:
  - `source_to_target_context` (example: `field_to_backalley_gate`)
  - `target_from_source_context` (example: `backalley_from_field_gate`)

## Common failure points
- Key added to `MapAsset` but not `MapIdentifier` -> not selectable in Tiled dropdown.
- Key added to `MapIdentifier` but not `MapAsset` -> runtime logs "Invalid targetMap key".
- `targetTransitionId` points to non-existent id -> map loads but spawn anchor lookup fails.
- Using filename in trigger without matching enum key -> transition lookup can fail unless it matches `fromKey` lookup forms.

## Minimal update policy
- Do not refactor unrelated systems when adding maps.
- Prefer additive changes: enum entries + dropdown values + trigger data.
- Keep changes scoped to map integration only.
