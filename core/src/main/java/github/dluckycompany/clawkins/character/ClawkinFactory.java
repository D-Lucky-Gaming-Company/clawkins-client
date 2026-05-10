package github.dluckycompany.clawkins.character;

import github.dluckycompany.clawkins.battle.BattleSkill;

import java.util.List;

/**
 * Factory for creating Clawkins with proper leveling system integration.
 * Provides pre-configured starter Clawkins with correct stats and skills.
 */
public class ClawkinFactory {
    
    /**
     * Creates Swee'pea at level 5 with proper stats and skills.
     * Swee'pea is a tanky bruiser with high HP and DEF.
     * 
     * Starting stats at Level 5:
     * - HP: 55
     * - ATK: 35
     * - DEF: 50
     * - SPEED: 20
     * 
     * Starting skills:
     * - Heavy Paw (Level 5)
     * - Stretch & Nap (Level 5)
     * 
     * @return Swee'pea Clawkin at level 5
     */
    public static Clawkin createSweepeaLevel5() {
        StatGrowth growth = StatGrowth.createSweepeaGrowth();
        
        int level = 5;
        int hp = growth.calculateHpAtLevel(level);
        int attack = growth.calculateAttackAtLevel(level);
        int defense = growth.calculateDefenseAtLevel(level);
        int speed = growth.calculateSpeedAtLevel(level);
        
        List<BattleSkill> skills = SkillUnlockSystem.getAllSkillsUpToLevel("clawkin_sweepea", level);
        
        Clawkin.SummaryProfile profile = new Clawkin.SummaryProfile(
            "Clawkin",
            "Tanky Bruiser",
            "The Silent Sovereign",
            "Swee'pea is a powerful tank with exceptional defense and sustain. " +
            "While slow, Swee'pea can outlast most opponents through sheer durability.",
            hp,
            attack,
            defense,
            speed,
            "Exceptional HP pool for sustained battles",
            "Moderate attack power with heavy strikes",
            "Outstanding defense - hard to take down",
            "Low speed - acts last in most battles"
        );
        
        return new Clawkin(
            "clawkin_sweepea",
            "Swee'pea",
            "ui/Clawkin_02.png",
            "ui/battle_ui/Sprites/icons/Sweepea'_icon.png",
            level,
            hp,
            attack,
            defense,
            speed,
            skills,
            profile
        );
    }
    
    /**
     * Creates Ginger at level 5 with proper stats and skills.
     * Ginger is a fast attacker with high speed and offense.
     * 
     * Starting stats at Level 5:
     * - HP: 35
     * - ATK: 45
     * - DEF: 25
     * - SPEED: 60
     * 
     * @return Ginger Clawkin at level 5
     */
    public static Clawkin createGingerLevel5() {
        StatGrowth growth = StatGrowth.createGingerGrowth();
        
        int level = 5;
        int hp = growth.calculateHpAtLevel(level);
        int attack = growth.calculateAttackAtLevel(level);
        int defense = growth.calculateDefenseAtLevel(level);
        int speed = growth.calculateSpeedAtLevel(level);
        
        List<BattleSkill> skills = SkillUnlockSystem.getAllSkillsUpToLevel("clawkin_ginger", level);
        
        Clawkin.SummaryProfile profile = new Clawkin.SummaryProfile(
            "Clawkin",
            "Fast Attacker",
            "The Swift Blade",
            "Ginger is a lightning-fast attacker who strikes first with devastating power. " +
            "Fragile but deadly, Ginger excels at overwhelming opponents with speed and aggression.",
            hp,
            attack,
            defense,
            speed,
            "Low HP - high risk, high reward",
            "Exceptional attack power - hits hard",
            "Low defense - vulnerable to counterattacks",
            "Outstanding speed - almost always strikes first"
        );
        
        return new Clawkin(
            "clawkin_ginger",
            "Ginger",
            "ui/Clawkin_01.png",
            "ui/battle_ui/Sprites/icons/Ginger_icon.png",
            level,
            hp,
            attack,
            defense,
            speed,
            skills,
            profile
        );
    }
    
    /**
     * Creates Dart at level 5 with proper stats and skills.
     * Dart is a fast attacker with low defense.
     * 
     * @return Dart Clawkin at level 5
     */
    public static Clawkin createDartLevel5() {
        StatGrowth growth = StatGrowth.createDartGrowth();
        
        int level = 5;
        int hp = growth.calculateHpAtLevel(level);
        int attack = growth.calculateAttackAtLevel(level);
        int defense = growth.calculateDefenseAtLevel(level);
        int speed = growth.calculateSpeedAtLevel(level);
        
        List<BattleSkill> skills = SkillUnlockSystem.getAllSkillsUpToLevel("clawkin_dart", level);
        
        Clawkin.SummaryProfile profile = new Clawkin.SummaryProfile(
            "Clawkin",
            "Speed Attacker",
            "The Swift Striker",
            "Dart is a lightning-fast attacker who strikes first and hits hard. " +
            "Fragile but deadly, Dart excels at overwhelming opponents quickly.",
            hp,
            attack,
            defense,
            speed,
            "Lower HP - glass cannon",
            "High attack power",
            "Low defense - vulnerable",
            "Exceptional speed - strikes first"
        );
        
        return new Clawkin(
            "clawkin_dart",
            "Dart",
            "ui/Clawkin_03.png",
            "ui/battle_ui/Sprites/icons/Dart_icon.png",
            level,
            hp,
            attack,
            defense,
            speed,
            skills,
            profile
        );
    }
}
