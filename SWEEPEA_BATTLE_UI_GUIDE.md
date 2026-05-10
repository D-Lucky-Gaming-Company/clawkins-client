# Swee'pea Battle UI Guide

## What You'll See in Battle

### **Starting Battle (Level 5)**

```
┌─────────────────────────────────────────────────────────┐
│                    BATTLE SCREEN                        │
│                                                         │
│  Swee'pea                              Enemy            │
│  HP: 55/55 ████████████████            HP: 100/100     │
│  LV 5                                                   │
│  EXP: 0 / 50 ░░░░░░░░░░░░░░░░                         │
│                                                         │
│                                                         │
│  [🐾]           [😴]           [🔒]                    │
│  [1] Heavy Paw  [2] Stretch    [3] Unlocks             │
│                     & Nap          at Lv 10             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

### **After Using Stretch & Nap**

```
┌─────────────────────────────────────────────────────────┐
│                    BATTLE SCREEN                        │
│                                                         │
│  Swee'pea                              Enemy            │
│  HP: 55/55 ████████████████            HP: 85/100      │
│  LV 5                                                   │
│  EXP: 5 / 50 ██░░░░░░░░░░░░░░          (Round 1 EXP)  │
│                                                         │
│  Message: "Swee'pea uses Stretch & Nap and             │
│            recovered 8 HP. Defense UP for 2 turns!"    │
│                                                         │
│  [🐾]           [😴]           [🔒]                    │
│  [1] Heavy Paw  [2] Stretch    [3] Unlocks             │
│                     & Nap (2)      at Lv 10             │
│                     ↑ ON COOLDOWN                       │
└─────────────────────────────────────────────────────────┘
```

---

### **Level 10 - New Skill Unlocked!**

```
┌─────────────────────────────────────────────────────────┐
│                    BATTLE SCREEN                        │
│                                                         │
│  Swee'pea                              Enemy            │
│  HP: 90/90 ████████████████            HP: 120/120     │
│  LV 10                                                  │
│  EXP: 15 / 50 ██████░░░░░░░░░░                        │
│                                                         │
│  🎉 NEW SKILL UNLOCKED: Claw & Chomp! 🎉               │
│                                                         │
│  [🐾]           [😴]           [🩸]                    │
│  [1] Heavy Paw  [2] Stretch    [3] Claw &              │
│                     & Nap          Chomp                │
│                                    ↑ NOW AVAILABLE!     │
└─────────────────────────────────────────────────────────┘
```

---

### **Using Claw & Chomp**

```
┌─────────────────────────────────────────────────────────┐
│                    BATTLE SCREEN                        │
│                                                         │
│  Swee'pea                              Enemy            │
│  HP: 90/90 ████████████████            HP: 95/120      │
│  LV 10                                  🩸 BLEEDING     │
│  EXP: 20 / 50 ████████░░░░░░░░                        │
│                                                         │
│  Message: "Swee'pea uses Claw & Chomp and deals        │
│            25 damage to Enemy. Enemy is bleeding!"     │
│                                                         │
│  [🐾]           [😴]           [🩸]                    │
│  [1] Heavy Paw  [2] Stretch    [3] Claw &              │
│                     & Nap          Chomp (4)            │
│                                    ↑ 4 TURN COOLDOWN   │
└─────────────────────────────────────────────────────────┘
```

---

### **Level 20 - Ultimate Unlocked!**

```
┌─────────────────────────────────────────────────────────┐
│                    BATTLE SCREEN                        │
│                                                         │
│  Swee'pea                              Boss             │
│  HP: 160/160 ██████████████████        HP: 250/250     │
│  LV 20                                                  │
│  EXP: 35 / 50 ██████████████░░                        │
│                                                         │
│  🎉 ULTIMATE UNLOCKED: Silent Sovereign Execution! 🎉  │
│                                                         │
│  [🐾]           [😴]           [👑]                    │
│  [1] Heavy Paw  [2] Stretch    [3] Silent              │
│                     & Nap          Sovereign            │
│                                    Execution            │
│                                    ↑ ULTIMATE SKILL!    │
└─────────────────────────────────────────────────────────┘
```

---

## Skill Button States

### ✅ **Available (White)**
```
[1] Heavy Paw
```
- Skill is unlocked
- Not on cooldown
- Can be used this turn
- Button clickable

### ⏳ **On Cooldown (Blue-Gray)**
```
[2] Stretch & Nap (2)
```
- Skill was recently used
- Number shows turns remaining
- Cannot be used yet
- Button disabled

### 🔒 **Locked (Gray)**
```
[3] Unlocks at Lv 10
```
- Skill not yet unlocked
- Shows required level
- Cannot be used
- Button disabled

---

## Skill Progression Timeline

### **Level 5** (Starting)
- ✅ Heavy Paw (15 damage, no cooldown)
- ✅ Stretch & Nap (heal 15% HP, +1 DEF, 2 turn cooldown)
- 🔒 Claw & Chomp (Unlocks at Lv 10)

### **Level 10** (Mid-Game)
- ✅ Heavy Paw
- ✅ Stretch & Nap
- ✅ Claw & Chomp (25 damage + bleed, 4 turn cooldown)

### **Level 20** (Late-Game)
- ✅ Heavy Paw
- ✅ Stretch & Nap
- ✅ Silent Sovereign Execution (50+ damage, 5 turn cooldown)

---

## Battle Messages You'll See

### **Skill Usage:**
```
"Swee'pea uses Heavy Paw and deals 15 damage to Enemy."
"Swee'pea uses Stretch & Nap and recovered 8 HP. Defense UP for 2 turns!"
"Swee'pea uses Claw & Chomp and deals 25 damage to Enemy. Enemy is bleeding!"
"Swee'pea uses Silent Sovereign Execution and deals 85 damage to Enemy."
```

### **Item Usage:**
```
"Swee'pea used Potion!"
"Swee'pea used Fish Snack!"
"Swee'pea used Bandage!"
```

### **Round EXP:**
```
(After each round completes)
+5 EXP granted automatically
EXP bar fills slightly
```

### **Level-Up:**
```
"Swee'pea leveled up!"
"Level 9 → 10"
"HP: 85 → 90 (+5)"
"ATK: 32 → 35 (+3)"
"DEF: 95 → 100 (+5)"
"SPEED: 33 → 34 (+1)"
```

---

## Quick Reference Card

### **Swee'pea's Skills**

| Skill | Level | Damage | Effect | Cooldown |
|-------|-------|--------|--------|----------|
| Heavy Paw | 5 | 15 | Physical damage | None |
| Stretch & Nap | 5 | - | Heal 15% HP, +1 DEF | 2 turns |
| Claw & Chomp | 10 | 25 | Damage + Bleed | 4 turns |
| Silent Sovereign | 20 | 50+ | Ultimate damage | 5 turns |

### **Battle Tips**

1. **Heavy Paw** - Use for consistent damage, no cooldown
2. **Stretch & Nap** - Use when HP below 70%, grants defense
3. **Claw & Chomp** - Use on high HP enemies, bleed ignores defense
4. **Silent Sovereign** - Save for finishing blows or tough enemies

### **EXP Gains**

- **Per Round:** +5 EXP (even if you lose!)
- **Per Victory:** +100 EXP (~2 levels)
- **Per Level:** Requires 50 EXP

### **Progression Speed**

- 1 battle = ~2 levels + round EXP
- 5 battles = ~10 levels (reach Claw & Chomp!)
- 10 battles = ~20 levels (reach Ultimate!)

---

## Color Guide

### Button Colors:
- **White** = Available, ready to use
- **Blue-Gray** = On cooldown, wait X turns
- **Gray** = Locked, need higher level

### HP Bar Colors:
- **Green** = Healthy (70-100%)
- **Yellow** = Injured (30-70%)
- **Red** = Critical (0-30%)

### EXP Bar:
- **Blue** = Current EXP progress
- **Gray** = Remaining EXP needed

---

## Keyboard Shortcuts

- **1** = Use Skill 1 (Heavy Paw)
- **2** = Use Skill 2 (Stretch & Nap)
- **3** = Use Skill 3 (Claw & Chomp / Ultimate)
- **4** = Open Items
- **E** = Open Inventory
- **X** = Flee (wild battles only)
- **↑/↓** = Navigate Clawkin selection
- **Enter** = Confirm Clawkin switch

---

## What's New (After Update)

✨ **Skill Names Now Display!**
- See actual skill names instead of generic labels
- Cooldown timers show remaining turns
- Lock status shows unlock requirements

✨ **Round EXP System!**
- Gain 5 EXP after every round
- Progress even in difficult battles
- No more grinding required

✨ **Faster Leveling!**
- Only 50 EXP per level (was 75-1225)
- ~2 levels per victory
- Reach powerful skills faster

✨ **Item Turn Cost!**
- Using items now ends your turn
- Adds strategic depth
- Choose wisely: attack or heal?

---

**Enjoy your battles with Swee'pea!** 🐾
