# EarthBound Font Style Tuning

## Summary

Applied EarthBound-like readability styling to runtime UI text that already uses `assets/font/earthbound-dialogue-gold.otf`.

This keeps the same font asset but improves the visual feel using generator parameters (outline, shadow, spacing, kerning).

## Files Updated

- `core/src/main/java/github/kinuseka/testproject/ui/DialogueOverlay.java`
- `core/src/main/java/github/kinuseka/testproject/battle/BattleOverlay.java`

## Changes

Both overlays now apply a shared style profile via local helper method `applyEarthboundStyle(...)`:

- `size`: unchanged intent (`24` body/battle, `26` dialogue title)
- `kerning = true`
- `spaceX = 1`
- `borderWidth = 1.8f`
- `borderColor = Color.BLACK`
- `shadowOffsetX = 1`
- `shadowOffsetY = -1`
- `shadowColor = rgba(0,0,0,0.7)`

## Why

- Outline + shadow improve legibility over map/background colors.
- Slight spacing/kerning adjustments move closer to classic JRPG dialogue readability.
- Keeps implementation minimal and localized to existing overlay constructors.

## Validation

- `./gradlew.bat core:compileJava` succeeds.
- No new linter issues on touched files.
