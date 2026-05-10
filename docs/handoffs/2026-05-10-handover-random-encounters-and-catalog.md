# Random Encounters & Encounter Catalog — Handover

## Summary

Overworld **step-based random battles** were added on top of the existing encounter pipeline (`EncounterEventBus` → `BattleService` → `BattleStateMachine`). Authorable **name/stat tables** live in `RandomEncounterCatalog`. Field maps gained a **permuted set** of scripted `encounterId` values (`enemy_field_2`–`enemy_field_6`) and one extra placed enemy on `field_5.tmx`.

---

## 1. Random encounter flow (runtime)

### Trigger

- **`RandomEncounterSystem`** (`core/.../system/RandomEncounterSystem.java`) runs as an Ashley system when exploration is enabled (same gating pattern as **`EncounterDetectionSystem`** in `GameScreen.syncSystemStates`).
- It measures **Euclidean distance** moved on the player’s `Transform` each frame (world units: Tiled pixels × `Main.UNIT_SCALE`).
- **`TILES_PER_ROLL`** is **2**: when accumulated distance reaches **2 × tileWorldWidth** (from the active map’s `tilewidth`, default 16), it consumes that chunk and performs **one** probability roll.
- **`tileWorldWidth`** = `map tilewidth × Main.UNIT_SCALE` (for 16×16 maps, one tile ≈ **1.0** world unit).

### Map difficulty → roll chance

- **`MapEncounterDifficulty.tierFor(MapAsset)`** maps the current map to an **`EncounterDifficultyTier`**.
- **`EncounterDifficultyTier.encounterChance()`** is the per–2-tiles probability:
  - **EASY** 1%, **MIDDLE** 7%, **NORMAL** 15%, **INTERMEDIATE** 20%, **HARD** 25%
  - **NONE** → no random encounters (most maps default here).
- **Currently assigned to EASY random combat:** `MapAsset.FIELD`, `FIELD_2`, `FIELD_4` only (see `MapEncounterDifficulty` static block). **`FIELD_3` and `FIELD_5` are not** in that list unless you extend `assignEasy(...)` or add explicit tier entries.

### Battle start (unchanged integration)

- On success, **`RandomEncounterGenerator.randomEncounter(tier, bus)`** builds an **`EncounterEvent`** (`EncounterEventType.START_ENCOUNTER`) and **`EncounterEventBus.publish(...)`**.
- **`BattleService`** already polls the bus and starts the session the same way as tile-triggered enemies.

### Map change / teleports

- **`RandomEncounterSystem.resetTravelLedger()`** clears last position and distance accumulator. It is chained on **`TiledService.setMapChangeConsumer`** in **`GameScreen`** so warps do not create one huge movement step.

### Gating

- System **`setProcessing`** follows **`shouldEnableExploration`** (battle, dialogue, menus, transitions, cheats, etc.), matching scripted encounters.

---

## 2. Random encounter content (catalog + generator)

### `RandomEncounterCatalog`

- **Path:** `core/.../encounter/RandomEncounterCatalog.java`
- **`Definition`** record: `displayName`, `baseHp`, `baseAttack`, `baseDefense`, `baseSpeed`.
- Separate **`List<Definition>`** per tier (**EASY** through **HARD**) with placeholder names and escalating baseline stats.
- **`pickRandom(EncounterDifficultyTier)`** — uniform random row for that tier; missing/empty tier list falls back to **EASY**.
- **`statScaleFor(EncounterDifficultyTier)`** — small extra multipliers (1.00 → 1.16) applied after picking a row (tune globally without editing every definition).
- **`fallbackDefinition()`** — first EASY row; used if `pickRandom` returns null.

### `RandomEncounterGenerator`

- **Path:** `core/.../encounter/RandomEncounterGenerator.java`
- Pulls a **`Definition`**, applies **`statScaleFor`**, integer **jitter**, floor clamps, then attaches a fixed **three-skill set** (Dark Rider–style: attack / heal with `attack[self] * 0.25` / defense buff) with **`skillBonus`** scaling by tier for potencies.
- **`enemyImagePath`** in the event is left **empty** unless extended later.

### Removed / superseded

- **`RandomEnemyCategory`** enum was **removed**; categories are represented by catalog rows and tiers instead.

---

## 3. `GameScreen` wiring

- Constructs **`RandomEncounterGenerator`** and **`RandomEncounterSystem(tiledService, encounterEventBus, generator, battleService)`** after **`TiledService`** exists.
- Map-change consumer ends with **`RandomEncounterSystem.resetTravelLedger`**.
- **`syncSystemStates`** toggles **`RandomEncounterSystem.setProcessing(shouldEnableExploration)`** alongside **`EncounterDetectionSystem`**.

---

## 4. Field maps: scripted `encounterId` shuffle (`enemy_field_2`–`6`)

TMX edits only; **runtime does not** randomize these IDs—they are fixed per object.

| Map        | `encounterId` (roaming ENEMY, excluding `enemy_field_1` on first `field` enemy) |
|-----------|--------------------------------------------------------------------------------|
| `field.tmx` (2nd enemy) | `enemy_field_4` |
| `field_2.tmx`           | `enemy_field_5` |
| `field_3.tmx`           | `enemy_field_2` |
| `field_4.tmx`           | `enemy_field_6` |
| `field_5.tmx` (new object, id 14) | `enemy_field_3` |

- **`enemy_field_1`** remains on the **first** enemy on **`field.tmx`** (The Dark Rider).
- **`field_5.tmx`**: added one **ENEMY** object (same property pattern as other field riders), **`nextobjectid`** bumped; triggers **`objectgroup`** opening tag preserved for valid XML.

---

## 5. Extension checklist for future work

1. **More maps with random combat:** add **`MapAsset` → `EncounterDifficultyTier`** in **`MapEncounterDifficulty`** (e.g. `assignMiddle(...)`) — not all field variants are EASY today.
2. **Catalog:** add or edit **`Definition`** rows per tier; adjust **`statScaleFor`** for a global difficulty pass.
3. **Portraits:** extend **`Definition`** or generator to set **`enemyImagePath`** on **`EncounterEvent`**.
4. **True runtime random `encounterId`:** would require a reserved id or map property and Java-side substitution before **`EncounterZone`** / bus publish (not in TMX alone).
5. **Scripted + random overlap:** both can queue **`EncounterEvent`**; only one battle processes at a time; beware back-to-back queues if the player walks into triggers while random fires.

---

## 6. Primary files

| Area | Files |
|------|--------|
| Step rolls | `RandomEncounterSystem.java` |
| Map → tier | `MapEncounterDifficulty.java`, `EncounterDifficultyTier.java` |
| Tables | `RandomEncounterCatalog.java` |
| Event build | `RandomEncounterGenerator.java` |
| Wiring | `GameScreen.java` |
| Maps | `assets/maps/field.tmx`, `field_2.tmx`, `field_3.tmx`, `field_4.tmx`, `field_5.tmx` |

---

## 7. Related existing docs

- World encounter pipeline and Tiled **`ENEMY`** authoring: `docs/development/readme.md` §3.
- **`encounterId` / dialogue lookup:** `docs/handoffs/2026-03-12-encounterid-enemyname-dialogue-lookup.md`.
