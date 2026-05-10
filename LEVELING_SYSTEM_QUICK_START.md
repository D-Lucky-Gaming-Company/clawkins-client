# Clawkin Leveling System - Quick Start Guide

## For Developers

### Creating a New Clawkin with Leveling Support

```java
// Use the factory for pre-configured Clawkins
Clawkin sweepea = ClawkinFactory.createSweepeaLevel5();
Clawkin ginger = ClawkinFactory.createGingerLevel5();
Clawkin dart = ClawkinFactory.createDartLevel5();

// Add to party
playerBattleState.addClawkinToParty(sweepea);
```

### Customizing Growth Rates

```java
// Create custom growth profile
StatGrowth customGrowth = new StatGrowth(
    "clawkin_custom",
    60,  // Base HP at level 5
    40,  // Base ATK at level 5
    30,  // Base DEF at level 5
    35,  // Base SPEED at level 5
    StatGrowth.GrowthRate.FAST,      // HP growth
    StatGrowth.GrowthRate.VERY_FAST, // ATK growth
    StatGrowth.GrowthRate.MODERATE,  // DEF growth
    StatGrowth.GrowthRate.FAST       // SPEED growth
);
```

### Adding Custom Skills

```java
// In SkillUnlockSystem.java, add to static initializer:
List<SkillUnlock> customSkills = new ArrayList<>();
customSkills.add(new SkillUnlock(5, createCustomSkill1()));
customSkills.add(new SkillUnlock(10, createCustomSkill2()));
customSkills.add(new SkillUnlock(20, createCustomSkill3()));
SKILL_UNLOCKS.put("clawkin_custom", customSkills);
```

### Manually Awarding EXP (for testing)

```java
// Get the active Clawkin's data
ClawkinData data = playerBattleState.getActiveClawkinData();

// Award EXP
List<LevelUpResult> results = ExpManager.awardExp(data, 500);

// Check if leveled up
if (!results.isEmpty()) {
    System.out.println("Leveled up!");
    for (LevelUpResult result : results) {
        System.out.println("Level " + result.getOldLevel() + " → " + result.getNewLevel());
    }
}
```

### Checking Clawkin Stats

```java
Clawkin clawkin = playerBattleState.getActiveClawkin();
ClawkinData data = playerBattleState.getActiveClawkinData();

System.out.println("Level: " + data.getLevel());
System.out.println("EXP: " + data.getCurrentExp());
System.out.println("EXP to next level: " + data.getExpToNextLevel());
System.out.println("Progress: " + (data.getExpProgressToNextLevel() * 100) + "%");

System.out.println("HP: " + data.getCurrentHp() + "/" + data.getMaxHp());
System.out.println("ATK: " + data.getAttack());
System.out.println("DEF: " + data.getDefense());
System.out.println("SPEED: " + data.getSpeed());
```

## For Testers

### Testing Level-Up Flow

1. **Start a battle** with Swee'pea at Level 5
2. **Win the battle** (defeat the enemy)
3. **Check console logs** for EXP award messages
4. **Win more battles** until you see "Leveled up!" in logs
5. **Verify level-up message** appears after victory dialogue
6. **Check stats** increased correctly

### Expected Behavior

#### After Winning a Battle:
1. Victory message: "Victory! [Enemy] was defeated."
2. Press Z/Space/Enter to continue
3. **If leveled up**: Level-up message appears
4. Press Z/Space/Enter to dismiss
5. Return to overworld

#### Level-Up Message Should Show:
- Old level → New level
- HP increase (with +gain)
- ATK increase (with +gain)
- DEF increase (with +gain)
- SPEED increase (with +gain)
- New skills (if any unlocked)

### Testing Checklist

- [ ] EXP is awarded after battle victory
- [ ] Level-up occurs when enough EXP is gained
- [ ] Stats increase correctly on level-up
- [ ] Current HP increases when max HP increases
- [ ] Skills unlock at correct levels
- [ ] Multiple level-ups work (if enough EXP)
- [ ] Level-up message displays correctly
- [ ] Max level (30) prevents further leveling
- [ ] Party members retain their levels between battles

### Console Log Examples

**Battle Victory:**
```
BattleService: Battle victory! Awarding 80 EXP
ExpManager: Awarding 80 EXP to Swee'pea (Level 5)
```

**Level-Up:**
```
ExpManager: Swee'pea leveled up 1 time(s)!
ExpManager:   Level 5 → 6
ExpManager:   HP: 55 → 62 (+7)
ExpManager:   ATK: 35 → 38 (+3)
ExpManager:   DEF: 50 → 55 (+5)
ExpManager:   SPEED: 20 → 21 (+1)
```

**Skill Unlock:**
```
ExpManager:   New skills unlocked:
ExpManager:     - Claw & Chomp
```

## Common Issues

### Issue: No EXP awarded after victory
**Solution**: Check that battle phase is `VICTORY` and `closeBattleSession()` is called.

### Issue: Level-up message doesn't appear
**Solution**: Check `BattleOverlay` dialogue flow includes `LEVEL_UP_DISPLAY` phase.

### Issue: Stats don't increase
**Solution**: Verify `StatGrowth` is correctly configured for the Clawkin ID.

### Issue: Skills don't unlock
**Solution**: Check `SkillUnlockSystem` has skills defined for the Clawkin ID.

### Issue: Multiple Clawkins share EXP
**Solution**: This is not implemented yet. Only the active Clawkin gains EXP.

## Quick Reference

### EXP Needed for Levels 5-10
- Level 5 → 6: 275 EXP
- Level 6 → 7: 325 EXP
- Level 7 → 8: 375 EXP
- Level 8 → 9: 425 EXP
- Level 9 → 10: 475 EXP

### Swee'pea Stat Growth (Per Level)
- HP: +7
- ATK: +3
- DEF: +5
- SPEED: +1.5 (rounded)

### Ginger Stat Growth (Per Level)
- HP: +3
- ATK: +5
- DEF: +2
- SPEED: +4

### Dart Stat Growth (Per Level)
- HP: +3
- ATK: +5
- DEF: +2
- SPEED: +4

### Swee'pea Skill Unlocks
- Level 5: Heavy Paw, Stretch & Nap
- Level 10: Claw & Chomp
- Level 20: Silent Sovereign Execution

### Ginger Skill Unlocks
- Level 5: Quick Strike, Guard
- Level 10: Power Slash
- Level 20: Blazing Fury

### Dart Skill Unlocks
- Level 5: Swift Strike, Agility Up
- Level 10: Rapid Assault
- Level 20: Lightning Barrage

## Next Steps

1. **Test the system** with Swee'pea
2. **Add Ginger and Dart** to the party
3. **Customize growth rates** if needed
4. **Add more skills** to the unlock system
5. **Implement save/load** for persistence
6. **Add evolution system** (future)

## Support

For questions or issues, refer to:
- `LEVELING_SYSTEM_IMPLEMENTATION.md` - Full technical documentation
- Console logs - Detailed EXP and level-up information
- `ClawkinFactory.java` - Example Clawkin creation
- `StatGrowth.java` - Growth rate definitions
