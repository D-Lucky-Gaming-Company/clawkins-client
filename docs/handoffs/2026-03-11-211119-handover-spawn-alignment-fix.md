# Spawn Alignment Fix

## What was reported

Map objects were spawning offset from the position where they were placed in Tiled.

## Root cause

`TiledObjectConfigurator.addTransform(...)` applied an unconditional vertical offset:

- `y -= h`

That forced every tile object to shift downward by its full sprite height, causing placement mismatch relative to Tiled coordinates.

## Fix applied

Updated object transform creation to use Tiled coordinates directly (no forced Y subtraction).

### File changed

- `core/src/main/java/github/kinuseka/testproject/tiled/TiledObjectConfigurator.java`

## Impact

- Encounter objects, enemies, and other map-spawned tile objects now align to where they are placed in Tiled.
- Existing objects that were manually compensated in Tiled for the old offset may need repositioning.

## Validation

- `core:compileJava` passes.
- No lint errors in modified file.
