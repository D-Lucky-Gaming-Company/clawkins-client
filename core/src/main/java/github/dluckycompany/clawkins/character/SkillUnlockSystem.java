package github.dluckycompany.clawkins.character;

import github.dluckycompany.clawkins.battle.BattleSkill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages skill unlocking based on Clawkin level.
 * Each Clawkin has a predefined skill progression.
 */
public class SkillUnlockSystem {
    
    /**
     * Represents a skill unlock entry with level requirement.
     */
    public static class SkillUnlockEntry {
        private final BattleSkill skill;
        private final int unlockLevel;
        
        public SkillUnlockEntry(BattleSkill skill, int unlockLevel) {
            this.skill = skill;
            this.unlockLevel = unlockLevel;
        }
        
        public BattleSkill getSkill() {
            return skill;
        }
        
        public int getUnlockLevel() {
            return unlockLevel;
        }
    }
    
    /**
     * Defines when skills unlock for Swee'pea.
     * 
     * Swee'pea's Skills:
     * - Heavy Paw (Level 5) - Basic attack
     * - Stretch & Nap (Level 5) - Heal/sustain
     * - Claw & Chomp (Level 10) - Stronger attack
     * - Silent Sovereign Execution (Level 20) - Ultimate attack
     */
    private static final Map<String, List<SkillUnlock>> SKILL_UNLOCKS = new HashMap<>();
    
    static {
        // Swee'pea skill progression
        List<SkillUnlock> sweepeaSkills = new ArrayList<>();
        sweepeaSkills.add(new SkillUnlock(5, createHeavyPaw()));
        sweepeaSkills.add(new SkillUnlock(5, createStretchAndNap()));
        sweepeaSkills.add(new SkillUnlock(10, createClawAndChomp()));
        sweepeaSkills.add(new SkillUnlock(20, createSilentSovereignExecution()));
        SKILL_UNLOCKS.put("clawkin_sweepea", sweepeaSkills);
        
        // Ginger skill progression (placeholder - can be customized later)
        List<SkillUnlock> gingerSkills = new ArrayList<>();
        gingerSkills.add(new SkillUnlock(5, createBasicAttack("Quick Strike")));
        gingerSkills.add(new SkillUnlock(5, createBasicDefense("Guard")));
        gingerSkills.add(new SkillUnlock(10, createMediumAttack("Power Slash")));
        gingerSkills.add(new SkillUnlock(20, createStrongAttack("Blazing Fury")));
        SKILL_UNLOCKS.put("clawkin_ginger", gingerSkills);
        
        // Dart skill progression (placeholder - can be customized later)
        List<SkillUnlock> dartSkills = new ArrayList<>();
        dartSkills.add(new SkillUnlock(5, createBasicAttack("Swift Strike")));
        dartSkills.add(new SkillUnlock(5, createSpeedBoost("Agility Up")));
        dartSkills.add(new SkillUnlock(10, createMediumAttack("Rapid Assault")));
        dartSkills.add(new SkillUnlock(20, createStrongAttack("Lightning Barrage")));
        SKILL_UNLOCKS.put("clawkin_dart", dartSkills);
    }
    
    /**
     * Gets all skills that should be unlocked at a specific level.
     * 
     * @param clawkinId The Clawkin's ID
     * @param level The level to check
     * @return List of skills unlocked at that level
     */
    public static List<BattleSkill> getSkillsUnlockedAtLevel(String clawkinId, int level) {
        List<BattleSkill> unlockedSkills = new ArrayList<>();
        
        String normalizedId = normalizeClawkinId(clawkinId);
        List<SkillUnlock> skillProgression = SKILL_UNLOCKS.get(normalizedId);
        
        if (skillProgression == null) {
            return unlockedSkills;
        }
        
        for (SkillUnlock unlock : skillProgression) {
            if (unlock.unlockLevel == level) {
                unlockedSkills.add(unlock.skill);
            }
        }
        
        return unlockedSkills;
    }
    
    /**
     * Gets all skills that should be available up to a specific level.
     * 
     * @param clawkinId The Clawkin's ID
     * @param level The current level
     * @return List of all skills available at that level
     */
    public static List<BattleSkill> getAllSkillsUpToLevel(String clawkinId, int level) {
        List<BattleSkill> availableSkills = new ArrayList<>();
        
        String normalizedId = normalizeClawkinId(clawkinId);
        List<SkillUnlock> skillProgression = SKILL_UNLOCKS.get(normalizedId);
        
        if (skillProgression == null) {
            return availableSkills;
        }
        
        for (SkillUnlock unlock : skillProgression) {
            if (unlock.unlockLevel <= level) {
                availableSkills.add(unlock.skill);
            }
        }
        
        return availableSkills;
    }
    
    /**
     * Gets all skill unlock entries for a Clawkin (with level requirements).
     * 
     * @param clawkinId The Clawkin's ID
     * @return List of all skill unlock entries
     */
    public static List<SkillUnlockEntry> getAllSkillUnlockEntries(String clawkinId) {
        List<SkillUnlockEntry> entries = new ArrayList<>();
        
        String normalizedId = normalizeClawkinId(clawkinId);
        List<SkillUnlock> skillProgression = SKILL_UNLOCKS.get(normalizedId);
        
        if (skillProgression == null) {
            return entries;
        }
        
        for (SkillUnlock unlock : skillProgression) {
            entries.add(new SkillUnlockEntry(unlock.skill, unlock.unlockLevel));
        }
        
        return entries;
    }
    
    /**
     * Gets skill unlock entries that are unlocked at a specific level.
     * 
     * @param clawkinId The Clawkin's ID
     * @param level The level to check
     * @return List of skill unlock entries for that level
     */
    public static List<SkillUnlockEntry> getSkillUnlockEntriesAtLevel(String clawkinId, int level) {
        List<SkillUnlockEntry> entries = new ArrayList<>();
        
        String normalizedId = normalizeClawkinId(clawkinId);
        List<SkillUnlock> skillProgression = SKILL_UNLOCKS.get(normalizedId);
        
        if (skillProgression == null) {
            return entries;
        }
        
        for (SkillUnlock unlock : skillProgression) {
            if (unlock.unlockLevel == level) {
                entries.add(new SkillUnlockEntry(unlock.skill, unlock.unlockLevel));
            }
        }
        
        return entries;
    }
    
    /**
     * Checks if a skill should be unlocked at a specific level.
     * 
     * @param clawkinId The Clawkin's ID
     * @param level The level to check
     * @return True if any skills unlock at this level
     */
    public static boolean hasSkillUnlockAtLevel(String clawkinId, int level) {
        return !getSkillsUnlockedAtLevel(clawkinId, level).isEmpty();
    }
    
    private static String normalizeClawkinId(String clawkinId) {
        if (clawkinId == null) {
            return "clawkin_ginger"; // Default
        }
        
        String normalized = clawkinId.toLowerCase().trim();
        
        if (normalized.contains("sweepea") || normalized.contains("swee")) {
            return "clawkin_sweepea";
        } else if (normalized.contains("dart")) {
            return "clawkin_dart";
        } else if (normalized.contains("ginger")) {
            return "clawkin_ginger";
        }
        
        return "clawkin_ginger"; // Default
    }
    
    // ============ Swee'pea Skill Definitions ============
    
    private static BattleSkill createHeavyPaw() {
        return new BattleSkill(
            "Heavy Paw",
            BattleSkill.EffectType.DAMAGE,
            15,                    // Base damage (15 physical damage)
            "",                    // No scaling - flat damage
            0,                     // No duration (instant)
            0,                     // No cooldown
            "A powerful swipe with heavy paws.",
            "Deals 15 Physical Damage",
            "No cooldown"
        );
    }
    
    private static BattleSkill createStretchAndNap() {
        return new BattleSkill(
            "Stretch & Nap",
            BattleSkill.EffectType.HEAL,
            0,                     // Base heal (calculated as 15% of Max HP)
            "maxhp[self] * 0.15",  // Heals 15% of Max HP
            2,                     // Grants +1 DEF for 2 turns
            2,                     // 2 turn cooldown
            "Takes a quick rest to recover health.",
            "Restores 15% Max HP, +1 DEF for 2 turns",
            "2 turn cooldown"
        );
    }
    
    private static BattleSkill createClawAndChomp() {
        return new BattleSkill(
            "Claw & Chomp",
            BattleSkill.EffectType.BLEED,
            25,                    // Base damage (25 physical damage)
            "",                    // No scaling - flat damage
            2,                     // Bleed lasts 2 turns
            4,                     // 4 turn cooldown
            "A fierce combination of claws and bite.",
            "Deals 25 Physical Damage, inflicts Bleed (5% Max HP/turn for 2 turns)",
            "4 turn cooldown"
        );
    }
    
    private static BattleSkill createSilentSovereignExecution() {
        return new BattleSkill(
            "Silent Sovereign Execution",
            BattleSkill.EffectType.DAMAGE,
            50,                    // Base damage (ultimate)
            "attack[self]",        // Scales with attack
            0,                     // No duration (instant)
            5,                     // 5 turn cooldown
            "An overwhelming display of royal power.",
            "Deals massive physical damage",
            "Scales with ATK"
        );
    }
    
    // ============ Generic Skill Templates ============
    
    private static BattleSkill createBasicAttack(String name) {
        return new BattleSkill(
            name,
            BattleSkill.EffectType.DAMAGE,
            12,
            "attack[self]",
            0,
            0,
            "A basic attack.",
            "Deals physical damage",
            "Scales with ATK"
        );
    }
    
    private static BattleSkill createMediumAttack(String name) {
        return new BattleSkill(
            name,
            BattleSkill.EffectType.DAMAGE,
            22,
            "attack[self]",
            0,
            2,
            "A stronger attack.",
            "Deals heavy physical damage",
            "Scales with ATK"
        );
    }
    
    private static BattleSkill createStrongAttack(String name) {
        return new BattleSkill(
            name,
            BattleSkill.EffectType.DAMAGE,
            45,
            "attack[self]",
            0,
            4,
            "A powerful ultimate attack.",
            "Deals massive physical damage",
            "Scales with ATK"
        );
    }
    
    private static BattleSkill createBasicDefense(String name) {
        return new BattleSkill(
            name,
            BattleSkill.EffectType.DEFENSE,
            "defense[self]",
            2
        );
    }
    
    private static BattleSkill createSpeedBoost(String name) {
        return new BattleSkill(
            name,
            BattleSkill.EffectType.ATTACK,
            "speed[self]",
            3
        );
    }
    
    /**
     * Internal class to represent a skill unlock.
     */
    private static class SkillUnlock {
        final int unlockLevel;
        final BattleSkill skill;
        
        SkillUnlock(int unlockLevel, BattleSkill skill) {
            this.unlockLevel = unlockLevel;
            this.skill = skill;
        }
    }
}
