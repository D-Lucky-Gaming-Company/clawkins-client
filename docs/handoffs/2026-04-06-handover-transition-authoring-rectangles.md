# Transition Authoring Decision (Rectangle Zones)

## Decision
Transition points are now authored as rectangle objects in Tiled, not object templates.

## Reason
- Rectangle objects allow dynamic trigger sizing per map context.
- Easier to cover doors, hallways, map borders, and irregular entry strips.
- Reduces template maintenance overhead.

## Scope
Applies to map transition authoring across:
- `assets/maps/main.tmx`
- `assets/maps/nurse_interior.tmx`
- `assets/maps/cottage_sample.tmx`

## Authoring Notes
- Place transition zones on the `objects` layer as rectangle objects.
- Add transition properties on each rectangle object.
- Keep IDs and links consistent between source and destination maps.
- Rectangle object bounds are the trigger area; no separate trigger width/height properties are used.

## Runtime Implication
Current runtime parser (`TiledService`) accepts only tile map objects and throws on non-tile map objects in `objects` layer. Before transition logic is implemented, parsing must be extended to support rectangle objects.

## Deep-Dive References
- Beginner/developer authoring guide:
	- `docs/development/map-transition-trigger-guide.md`
- Runtime implementation handoff for future AI/dev work:
	- `docs/handoffs/2026-04-06-handover-map-transition-runtime-deep-dive.md`
