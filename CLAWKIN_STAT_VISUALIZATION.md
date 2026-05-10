# Clawkin Stat Visualization

## Level 5 Starting Stats (Visual)

### Swee'pea - Tanky Bruiser
```
HP:    ███████████ (55)  ⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛
ATK:   ███████ (35)      ⬛⬛⬛⬛⬛⬛⬛
DEF:   ██████████ (50)   ⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛
SPEED: ████ (20)         ⬛⬛⬛⬛
```

### Ginger - Fast Attacker
```
HP:    ███████ (35)      ⬛⬛⬛⬛⬛⬛⬛
ATK:   █████████ (45)    ⬛⬛⬛⬛⬛⬛⬛⬛⬛
DEF:   █████ (25)        ⬛⬛⬛⬛⬛
SPEED: ████████████ (60) ⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛
```

### Dart - Speed Attacker
```
HP:    █████████ (45)    ⬛⬛⬛⬛⬛⬛⬛⬛⬛
ATK:   █████████ (45)    ⬛⬛⬛⬛⬛⬛⬛⬛⬛
DEF:   █████ (25)        ⬛⬛⬛⬛⬛
SPEED: ████████ (40)     ⬛⬛⬛⬛⬛⬛⬛⬛
```

## Level 30 Max Stats (Visual)

### Swee'pea - Tanky Bruiser
```
HP:    ██████████████████████████████████████████████ (230)
ATK:   ██████████████████████ (110)
DEF:   ███████████████████████████████████ (175)
SPEED: ███████████ (57)
```

### Ginger - Fast Attacker
```
HP:    ██████████████████████ (110)
ATK:   ██████████████████████████████████ (170)
DEF:   ███████████████ (75)
SPEED: ████████████████████████████████ (160)
```

### Dart - Speed Attacker
```
HP:    ████████████████████████ (120)
ATK:   ██████████████████████████████████ (170)
DEF:   ███████████████ (75)
SPEED: ████████████████████████████ (140)
```

## Stat Distribution Radar Chart (Level 30)

### Swee'pea
```
         HP (230)
            *
           /|\
          / | \
         /  |  \
    DEF /   |   \ ATK
   (175)*   |   *(110)
        \   |   /
         \  |  /
          \ | /
           \|/
            *
        SPEED (57)
```

### Ginger
```
         HP (110)
            *
           /|\
          / | \
         /  |  \
    DEF /   |   \ ATK
    (75)*   |   *(170)
        \   |   /
         \  |  /
          \ | /
           \|/
            *
        SPEED (160)
```

### Dart
```
         HP (120)
            *
           /|\
          / | \
         /  |  \
    DEF /   |   \ ATK
    (75)*   |   *(170)
        \   |   /
         \  |  /
          \ | /
           \|/
            *
        SPEED (140)
```

## Stat Comparison by Category

### HP (Survivability)
```
Swee'pea: ████████████████████████████████████████████████ 230
Dart:     ████████████████████████ 120
Ginger:   ██████████████████████ 110
```

### ATK (Damage Output)
```
Ginger:   ██████████████████████████████████ 170
Dart:     ██████████████████████████████████ 170
Swee'pea: ██████████████████████ 110
```

### DEF (Damage Reduction)
```
Swee'pea: ███████████████████████████████████ 175
Ginger:   ███████████████ 75
Dart:     ███████████████ 75
```

### SPEED (Turn Order)
```
Ginger:   ████████████████████████████████ 160
Dart:     ████████████████████████████ 140
Swee'pea: ███████████ 57
```

## Role Triangle

```
        TANK
      (Swee'pea)
         /\
        /  \
       /    \
      /      \
     /        \
    /          \
   /            \
  /              \
 /________________\
SPEED            BALANCED
(Ginger)         (Dart)
```

## Stat Total Comparison (Level 30)

```
Swee'pea: ████████████████████████████████████████████████████████ 572
Ginger:   ███████████████████████████████████████████████ 515
Dart:     ██████████████████████████████████████████████ 505
```

## Growth Rate Comparison

### HP Growth
```
Swee'pea (EXTREME):   +7/level  ███████
Ginger (MODERATE):    +3/level  ███
Dart (MODERATE):      +3/level  ███
```

### ATK Growth
```
Ginger (VERY_FAST):   +5/level  █████
Dart (VERY_FAST):     +5/level  █████
Swee'pea (MODERATE):  +3/level  ███
```

### DEF Growth
```
Swee'pea (VERY_FAST): +5/level  █████
Ginger (SLOW):        +2/level  ██
Dart (SLOW):          +2/level  ██
```

### SPEED Growth
```
Ginger (FAST):        +4/level  ████
Dart (FAST):          +4/level  ████
Swee'pea (VERY_SLOW): +1.5/level █
```

## Battle Scenario Simulation

### Turn 1 (All Level 30)
```
1. Ginger acts first  (SPEED 160) → Deals 170 damage
2. Dart acts second   (SPEED 140) → Deals 170 damage
3. Swee'pea acts last (SPEED 57)  → Deals 110 damage
```

### Damage Taken (vs 100 ATK enemy)
```
Swee'pea: Takes 1 damage   (100 - 175 DEF = 1 minimum)
Ginger:   Takes 25 damage  (100 - 75 DEF = 25)
Dart:     Takes 25 damage  (100 - 75 DEF = 25)
```

### Survival Time (vs 50 damage/turn)
```
Swee'pea: 230 HP ÷ 1 damage  = 230 turns (DEF negates most damage)
Dart:     120 HP ÷ 25 damage = 4.8 turns
Ginger:   110 HP ÷ 25 damage = 4.4 turns
```

## Playstyle Visualization

### Swee'pea - Defensive Tank
```
┌─────────────────────────────────┐
│  DEFENSE FIRST                  │
│  ┌───────────────────────────┐  │
│  │ High HP + High DEF        │  │
│  │ Outlast opponents         │  │
│  │ Sustain with healing      │  │
│  │ Slow but steady           │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

### Ginger - Glass Cannon
```
┌─────────────────────────────────┐
│  OFFENSE FIRST                  │
│  ┌───────────────────────────┐  │
│  │ Strike first, strike hard │  │
│  │ Overwhelm before counter  │  │
│  │ High risk, high reward    │  │
│  │ Speed is survival         │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

### Dart - Balanced Aggressor
```
┌─────────────────────────────────┐
│  BALANCED APPROACH              │
│  ┌───────────────────────────┐  │
│  │ Fast but not fragile      │  │
│  │ Strong offense            │  │
│  │ Moderate survivability    │  │
│  │ Flexible tactics          │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

## Recommended Team Compositions

### 1. Balanced Team
```
┌──────────┬──────────┬──────────┐
│ Swee'pea │  Ginger  │   Dart   │
│  (Tank)  │ (Speed)  │ (Speed)  │
└──────────┴──────────┴──────────┘
Coverage: Tank + 2 Attackers
```

### 2. Speed Team
```
┌──────────┬──────────┬──────────┐
│  Ginger  │   Dart   │   Dart   │
│ (Speed)  │ (Speed)  │ (Speed)  │
└──────────┴──────────┴──────────┘
Coverage: Maximum offense
```

### 3. Tank Team
```
┌──────────┬──────────┬──────────┐
│ Swee'pea │ Swee'pea │  Ginger  │
│  (Tank)  │  (Tank)  │ (Speed)  │
└──────────┴──────────┴──────────┘
Coverage: High survivability
```

## Summary

### Swee'pea
- **Best at:** Tanking, Sustain, Defense
- **Worst at:** Speed, First Strike
- **Playstyle:** Defensive, Patient, Endurance

### Ginger
- **Best at:** Speed, First Strike, Damage
- **Worst at:** Survivability, Defense
- **Playstyle:** Aggressive, High-Risk, Burst

### Dart
- **Best at:** Balanced Speed/Damage
- **Worst at:** Specialization
- **Playstyle:** Flexible, Adaptive, Reliable

Each Clawkin offers a unique strategic approach to battles!
