# Clawkin Independent Class Refactor Handoff

**Date:** 2026-04-06  
**Scope:** Tiled project schema + TMX data + Java property-reading logic  
**Files changed:**
- `assets/maps/test.tiled-project`
- `assets/maps/main.tmx`
- `core/src/main/java/github/kinuseka/testproject/tiled/TiledObjectConfigurator.java`

---

## Why This Change Was Made

Before this refactor, the Tiled `Player` class had all Clawkin data hardcoded as flat members directly inside it:

```
Player class members (old):
  clawkin1Id, clawkin1Name, clawkin1Level, clawkin1Hp, clawkin1Attack, ...
  clawkin1Skill1Name, clawkin1Skill1Power, clawkin1Skill1EffectType, ...
  (×3 clawkins × 3 skills × 6 fields = 54 members just for clawkins)
```

Problems with the old approach:
- Clawkins were permanently part of the `Player` class. You could not reuse the same data shape for enemies or other objects.
- Adding or removing a clawkin slot required editing the Player class definition itself.
- The flat naming convention (`clawkin1X`, `clawkin2X`, `clawkin3X`) was redundant boilerplate.

The new approach makes `Clawkin` a **standalone Tiled class** that any object can carry. You add `clawkin1`, `clawkin2`, `clawkin3` as custom class-type properties directly on any specific map object instance. The `Player` class definition itself contains no clawkin members at all.

---

## What Tiled "Class" vs "Property" Means (Important Context)

In Tiled, there are two distinct concepts:

- **Class**: A reusable type definition registered in the `.tiled-project` file. It has named members with types and default values. When you assign a class to an object, it determines what properties that object category has by default.
- **Property**: An actual value stored on a specific map object. When a property's type is a class, its value is a nested set of fields matching that class's members.

Before this change, `clawkin1Id`, `clawkin1Name`, etc. were **members of the Player class** — meaning every Player object in every map automatically inherited them as default properties.

After this change, `clawkin1`, `clawkin2`, `clawkin3` are **properties of type Clawkin** that you manually add to specific object instances on the map. The Player class knows nothing about clawkins.

---

## The New Clawkin Class Definition

Registered in `assets/maps/test.tiled-project` as class id `10`.

**Class name:** `Clawkin`  
**useAs:** `property`, `object`, `project`

Members (all fields and their defaults):

| Property name            | Type   | Default |
|--------------------------|--------|---------|
| `id`                     | string | `""`    |
| `image_clawkin`          | string | `""`    |
| `name`                   | string | `""`    |
| `level`                  | int    | `1`     |
| `hp`                     | int    | `50`    |
| `attack`                 | int    | `8`     |
| `defense`                | int    | `4`     |
| `speed`                  | int    | `6`     |
| `skill1Name`             | string | `""`    |
| `skill1Power`            | int    | `0`     |
| `skill1EffectType`       | string | `""`    |
| `skill1EffectStat`       | string | `""`    |
| `skill1EffectAmount`     | int    | `0`     |
| `skill1EffectDurationTurns` | int | `0`    |
| `skill2Name`             | string | `""`    |
| `skill2Power`            | int    | `0`     |
| `skill2EffectType`       | string | `""`    |
| `skill2EffectStat`       | string | `""`    |
| `skill2EffectAmount`     | int    | `0`     |
| `skill2EffectDurationTurns` | int | `0`    |
| `skill3Name`             | string | `""`    |
| `skill3Power`            | int    | `0`     |
| `skill3EffectType`       | string | `""`    |
| `skill3EffectStat`       | string | `""`    |
| `skill3EffectAmount`     | int    | `0`     |
| `skill3EffectDurationTurns` | int | `0`   |

---

## The Updated Player Class Definition

The `Player` class (id `2`) in `test.tiled-project` now has only these members:

| Property name     | Type   | Default         |
|-------------------|--------|-----------------|
| `Name`            | string | `"Henry"`       |
| `moveSpeed`       | float  | `3`             |
| `notes`           | string | `""`            |
| `playerAttack`    | int    | `12`            |
| `playerDefense`   | int    | `8`             |
| `playerHp`        | int    | `100`           |
| `playerSkill1Name`| string | `"Slash"`       |
| `playerSkill1Power`| int   | `12`            |
| `playerSkill2Name`| string | `"Heavy Strike"`|
| `playerSkill2Power`| int   | `16`            |
| `playerSkill3Name`| string | `"Quick Jab"`   |
| `playerSkill3Power`| int   | `9`             |
| `playerSpeed`     | int    | `10`            |

No clawkin fields. No clawkin slots. Clean.

---

## How Clawkins Are Now Stored in main.tmx

The `PLAYER` object in `main.tmx` still stores its Player class data inside a `<property name="properties" type="class" propertytype="Player">` block (this is the standard TMX pattern for how Tiled stores class-typed properties on objects). Within that block, the clawkin data is now stored as **nested class properties** of type `Clawkin`:

```xml
<property name="properties" type="class" propertytype="Player">
  <properties>
    <!-- player stats ... -->
    <property name="clawkin1" type="class" propertytype="Clawkin">
      <properties>
        <property name="attack" type="int" value="8"/>
        <property name="defense" type="int" value="4"/>
        <property name="hp" type="int" value="50"/>
        <property name="id" value="clawkin_sweepea"/>
        <property name="level" type="int" value="1"/>
        <property name="name" value="Swee'pea"/>
        <property name="skill1Name" value="Leaf Cut"/>
        <property name="skill1Power" type="int" value="8"/>
        <property name="speed" type="int" value="6"/>
      </properties>
    </property>
    <property name="clawkin2" type="class" propertytype="Clawkin">
      <!-- ... Ginger ... -->
    </property>
    <property name="clawkin3" type="class" propertytype="Clawkin">
      <!-- ... Dart ... -->
    </property>
  </properties>
</property>
```

**Compare to the old flat format (no longer used):**

```xml
<!-- OLD — do not use -->
<property name="clawkin1Name" value="Swee'pea"/>
<property name="clawkin1Hp" type="int" value="50"/>
<property name="clawkin1Attack" type="int" value="8"/>
...
```

---

## How libGDX Reads These Nested Properties

When libGDX's `TmxMapLoader` reads the map, the in-memory layout of the Player object's properties is:

```
object.getProperties()
  └── "properties"  →  MapProperties  (the Player class)
        ├── "Name"         → "Jacob"
        ├── "playerHp"     → 100
        ├── ...
        ├── "clawkin1"  →  MapProperties  (the Clawkin class)
        │     ├── "id"     → "clawkin_sweepea"
        │     ├── "name"   → "Swee'pea"
        │     ├── "hp"     → 50
        │     └── ...
        ├── "clawkin2"  →  MapProperties
        └── "clawkin3"  →  MapProperties
```

The key `"properties"` (string literal) is how Tiled TMX format nests class-type properties on objects. It is **not** a special libGDX concept — it is the property name Tiled uses when serializing a class-typed property attached to an object.

---

## Java Code Changes in TiledObjectConfigurator

### New helper: `getNestedClawkinProperties`

```java
private static MapProperties getNestedClawkinProperties(TiledMapTileMapObject object, int slot) {
    Object playerProps = object.getProperties().get(NESTED_PROPERTIES_KEY); // "properties"
    if (playerProps instanceof MapProperties mp) {
        Object clawkinValue = mp.get("clawkin" + slot);
        if (clawkinValue instanceof MapProperties clawkinProps) {
            return clawkinProps;
        }
    }
    return null;
}
```

This navigates two levels: first retrieves the Player class MapProperties under the `"properties"` key, then retrieves `"clawkin1"` / `"clawkin2"` / `"clawkin3"` from within it.

### New helpers: `getStringFromProps` and `getIntFromProps`

These operate directly on a `MapProperties` object rather than on the `TiledMapTileMapObject`. They are used after retrieving the clawkin MapProperties:

```java
private static String getStringFromProps(MapProperties props, String key, String defaultValue)
private static int getIntFromProps(MapProperties props, String key, int defaultValue)
```

Both return their respective `defaultValue` safely when `props` is `null` (which happens when no clawkin is set on that slot).

### Updated: `buildClawkinFromSlot`

Before (old):
```java
// Read flat prefixed keys like "clawkin1Name", "clawkin1Hp"
String rawName = getStringProperty(tileMapObject, prefix + "Name", "").trim();
int hp = getIntProperty(tileMapObject, prefix + "Hp", 50);
```

After (new):
```java
// Read from nested MapProperties under key "clawkin1"
MapProperties clawkinProps = getNestedClawkinProperties(tileMapObject, slot);
String rawName = getStringFromProps(clawkinProps, "name", "").trim();
int hp = getIntFromProps(clawkinProps, "hp", 50);
```

A slot is considered empty and skipped when both `name` and `id` are blank (or when `clawkinProps` is null, which means the property was not added to that object at all).

### Updated: `parseClawkinSkillMetadata`

Signature changed from `(TiledMapTileMapObject, String prefix, String clawkinName)` to `(MapProperties clawkinProps, String clawkinName)`.

Skill property keys changed from `clawkin1Skill1Name` style to `skill1Name` style (because we are now reading directly from the Clawkin's own MapProperties, not from the parent object with a prefixed namespace).

---

## Fallback Behavior (REMOVED — important for future AI)

~~If a PLAYER object has no clawkin properties, the runtime fell back to a hardcoded starter party.~~

**The `addStarterFallbackClawkins()` method has been deleted** in a subsequent session. If a PLAYER object has no `clawkin1` / `clawkin2` / `clawkin3` properties, the party is empty. An empty party entering an encounter triggers an immediate battle defeat. The party is now 100% data-driven from Tiled with no hidden defaults.

---

## Party Cap

`PlayerBattleState.addClawkinToParty(...)` enforces a hard cap of 3:

```java
public void addClawkinToParty(Clawkin clawkin) {
    if (clawkin != null && party.size() < 3) {
        party.add(clawkin);
    }
}
```

The loader currently iterates slots 1–3. If you want to support more slots, increase both the loop bound in `loadConfiguredPlayerClawkins` and this cap.

---

## Current Limitation: PLAYER Only

The clawkin loading path (`loadConfiguredPlayerClawkins`) is only called inside the `PLAYER` branch of `configureByType`. If you want Enemy objects to carry clawkins (as a party of monster companions, for example), you need to add equivalent logic in the `ENEMY` branch and handle them separately from `playerBattleState`.

---

## How to Author Clawkins in Tiled Going Forward

1. Open your map in Tiled and select a `PLAYER` (or any future supported) object.
2. In the Properties panel, click **+ Add Property**.
3. Set the name to `clawkin1`. Set the type to **class → Clawkin**.
4. Expand the new `clawkin1` property and fill in the fields you need (at minimum `name` or `id`).
5. Repeat for `clawkin2` and `clawkin3` as needed.
6. Slots can be omitted entirely — an absent slot is silently skipped by the loader.
7. Save the map.

The property names `clawkin1`, `clawkin2`, `clawkin3` are fixed string literals that the Java code looks for. The Clawkin class type name (`Clawkin`) is what Tiled uses for the schema — it must match the class registered in `test.tiled-project`.

---

## Validation Checklist

1. Open `main.tmx` in Tiled with the updated `test.tiled-project` loaded — `clawkin1`, `clawkin2`, `clawkin3` should appear as nested Clawkin class properties on the Player object.
2. Run the game and confirm the party panel (`P`) shows the authored clawkins.
3. Add only `clawkin1` of type Clawkin, set a name and id, save, and re-run — confirm only 1 party member loads.
4. Remove all three clawkin properties from the Player object in Tiled, save, and re-run — confirm the party is empty (no fallback party; entering an encounter should immediately defeat).
5. Confirm enemy encounters still work (enemy clawkins are not loaded; this path is unaffected).

---

## Related Handoffs

- `docs/handoffs/2026-04-06-handover-clawkin-battle-integration.md` — covers the subsequent session that added `image_clawkin`, removed the fallback, and wired the battle system to be fully clawkin-driven.
