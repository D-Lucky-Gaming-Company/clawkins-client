# Skill Stat Scale Developer Guide

This guide describes the current skill magnitude model used by both Clawkin and Enemy skill authoring.

## 1. Goal

The old model split skill strength across multiple overlapping fields (`Power`, `EffectAmount`, `EffectStat`).
The new model uses one base number plus one formula string:

- `skillXEffectBaseStat` (integer)
- `skillXEffectStatScale` (string expression)

Runtime computes final magnitude as:

`magnitude = max(1, round(effectBaseStat + evaluate(effectStatScale)))`

If `effectStatScale` is blank, battle logic uses an internal fallback based on effect type.

## 2. Authoring Fields

Each skill slot (`skill1`, `skill2`, `skill3`) uses:

- `skillXName` (string)
- `skillXEffectType` (`damage`, `heal`, `attack`, `defense`)
- `skillXEffectBaseStat` (int)
- `skillXEffectStatScale` (string)
- `skillXEffectDurationTurns` (int)

These fields exist on:

- `Clawkin` class in Tiled project schema
- `Enemy` class in Tiled project schema

The enum dropdown for `skillXEffectType` comes from `SkillEffectType` in `assets/maps/test.tiled-project`.

## 3. Formula Language (`skillXEffectStatScale`)

Supported operators:

- `+`, `-`, `*`, `/`
- Parentheses: `(` and `)`

Supported stat identifiers:

- `attack`, `atk`
- `defense`, `def`
- `speed`, `spd`

Side selectors:

- Implicit self: `attack` is treated as `attack[self]`
- Explicit self: `attack[self]`
- Explicit enemy: `attack[enemy]`

Examples:

- `attack[self]`
- `defense[self] * 0.5`
- `attack[self] - defense[enemy] * 0.25`
- `(attack[self] + speed[self]) / 2`
- `5 * 5`

Notes:

- Unknown identifiers evaluate to `0`.
- Parse errors evaluate to `0` and log an error.
- Division by zero resolves that division branch to `0`.

## 4. Effect-Type Behavior in Battle

Current behavior in `BattleStateMachine`:

- `DAMAGE`: deals `max(1, offense - defense[target])` where `offense` is resolved skill magnitude.
- `HEAL`: restores HP by resolved magnitude.
- `ATTACK`: adds temporary attack boost of `max(1, magnitude / 4)` for `effectDurationTurns`.
- `DEFENSE`: adds temporary defense boost of `max(1, magnitude / 4)` for `effectDurationTurns`.

If a skill is missing or malformed, runtime still clamps outcome to at least `1` when applying effects that require positive magnitude.

## 5. Recommended Authoring Patterns

Baseline patterns:

- Physical hit:
  - `skillXEffectType = damage`
  - `skillXEffectBaseStat = 0`
  - `skillXEffectStatScale = attack[self]`
- Strong hit with flat bonus:
  - `skillXEffectType = damage`
  - `skillXEffectBaseStat = 6`
  - `skillXEffectStatScale = attack[self] * 1.2`
- Defensive stance:
  - `skillXEffectType = defense`
  - `skillXEffectBaseStat = 4`
  - `skillXEffectStatScale = defense[self]`
  - `skillXEffectDurationTurns = 2`
- Recovery move:
  - `skillXEffectType = heal`
  - `skillXEffectBaseStat = 3`
  - `skillXEffectStatScale = defense[self] * 0.8`

## 6. Backward Compatibility

Parser compatibility still supports old key `skillXEffectStat` as a fallback source for scale text when `skillXEffectStatScale` is missing.

Migration target:

- Prefer `skillXEffectBaseStat` + `skillXEffectStatScale` only.
- Do not add new data using the legacy `skillXEffectStat` key.

## 7. Runtime Reference Points

If you need to debug behavior, check these files:

- `core/src/main/java/github/kinuseka/testproject/tiled/TiledObjectConfigurator.java`
  - Parsing from Tiled properties into `BattleSkill`.
- `core/src/main/java/github/kinuseka/testproject/battle/BattleSkill.java`
  - Canonical skill metadata model.
- `core/src/main/java/github/kinuseka/testproject/battle/BattleStateMachine.java`
  - Formula evaluation and per-effect application.

## 8. Quick Validation Checklist

After editing Tiled values:

1. Confirm each skill has a valid `skillXEffectType`.
2. Confirm formulas use only supported identifiers/operators.
3. Run `core:compileJava`.
4. Trigger a battle and verify logs print expected `base` and `scale` values.
5. Confirm observed effect magnitude matches expected rough math.