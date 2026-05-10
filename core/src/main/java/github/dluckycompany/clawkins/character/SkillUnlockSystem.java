package github.dluckycompany.clawkins.character;

import com.badlogic.gdx.Gdx;
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
     * - Heavy Paw (Level 4) - Basic attack
     * - Stretch & Nap (Level 4) - Heal/sustain
     * - Claw & Chomp (Level 10) - Stronger attack
     * - Silent Sovereign Execution (Level 20) - Ultimate attack
     */
    private static final Map<String, List<SkillUnlock>> SKILL_UNLOCKS = new HashMap<>();
    
    static {
        // Swee'pea skill progression
        List<SkillUnlock> sweepeaSkills = new ArrayList<>();
        sweepeaSkills.add(new SkillUnlock(4, createHeavyPaw()));
        sweepeaSkills.add(new SkillUnlock(4, createStretchAndNap()));
        sweepeaSkills.add(new SkillUnlock(10, createClawAndChomp()));
        sweepeaSkills.add(new SkillUnlock(20, createSilentSovereignExecution()));
        SKILL_UNLOCKS.put("clawkin_sweepea", sweepeaSkills);
        
        // Dart skill progression
        List<SkillUnlock> dartSkills = new ArrayList<>();
        dartSkills.add(new SkillUnlock(4, createWaterfowlFlurry()));
        dartSkills.add(new SkillUnlock(4, createPounceStep()));
        dartSkills.add(new SkillUnlock(10, createBucklerPawParry()));
        dartSkills.add(new SkillUnlock(20, createMoonlightGreatClaw()));
        SKILL_UNLOCKS.put("clawkin_dart", dartSkills);
        
        // Ginger skill progression
        List<SkillUnlock> gingerSkills = new ArrayList<>();
        gingerSkills.add(new SkillUnlock(4, createStaticScratch()));
        gingerSkills.add(new SkillUnlock(4, createStretchAndFlex()));
        gingerSkills.add(new SkillUnlock(10, createTheZoomies()));
        gingerSkills.add(new SkillUnlock(20, createCinderSlash()));
        SKILL_UNLOCKS.put("clawkin_ginger", gingerSkills);
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
            Gdx.app.log("SkillUnlockSystem", "normalizeClawkinId: null ID, defaulting to clawkin_ginger");
            return "clawkin_ginger"; // Default
        }
        
        String normalized = clawkinId.toLowerCase().trim();
        Gdx.app.log("SkillUnlockSystem", "normalizeClawkinId: input='" + clawkinId + "', normalized='" + normalized + "'");
        
        if (normalized.contains("sweepea") || normalized.contains("swee")) {
            Gdx.app.log("SkillUnlockSystem", "  -> Matched clawkin_sweepea");
            return "clawkin_sweepea";
        } else if (normalized.contains("dart")) {
            Gdx.app.log("SkillUnlockSystem", "  -> Matched clawkin_dart");
            return "clawkin_dart";
        } else if (normalized.contains("ginger")) {
            Gdx.app.log("SkillUnlockSystem", "  -> Matched clawkin_ginger");
            return "clawkin_ginger";
        }
        
        Gdx.app.log("SkillUnlockSystem", "  -> No match, defaulting to clawkin_ginger");
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
            "A slow, weighted slap delivered with the full mass of a sleepy cat.",
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
            "Swee'pea lazily stretches before briefly dozing off mid-battle.",
            "Restores 15% of Max HP and grants +1 DEF for 2 turns",
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
            "A stubborn bite that refuses to let go, slowly draining the opponent.",
            "Deals 25 Physical Damage. Inflicts Bleed (5% Max HP damage per turn for 2 turns)",
            "4 turn cooldown"
        );
    }
    
    private static BattleSkill createSilentSovereignExecution() {
        return new BattleSkill(
            "Silent Sovereign Execution",
            BattleSkill.EffectType.DAMAGE,
            60,                    // Base damage (ultimate)
            "attack[self]",        // Scales with attack
            0,                     // No duration (instant)
            6,                     // 6 turn cooldown
            "A calculated, silent strike that punishes weakened enemies with lethal precision.",
            "Deals 60 Physical Damage. Ignores 25% of enemy DEF. If target HP is below 30%, damage is increased by 1.5×",
            "6 turn cooldown"
        );
    }
    
    // ============ Ginger Skill Definitions ============
    
    private static BattleSkill createStaticScratch() {
        return new BattleSkill(
            "Static Scratch",
            BattleSkill.EffectType.DAMAGE,
            15,                    // Base damage (15 physical damage)
            "",                    // No scaling - flat damage
            0,                     // No duration (instant)
            0,                     // No cooldown
            "Ginger rubs its fur rapidly, building a static charge before swiping.",
            "Deals 15 Physical Damage. 10% chance to inflict Stun (enemy skips turn).",
            "No cooldown"
        );
    }
    
    private static BattleSkill createStretchAndFlex() {
        return new BattleSkill(
            "Stretch & Flex",
            BattleSkill.EffectType.ATTACK,
            1,                     // Base boost value
            "speed[self]+attack[self]", // Boosts both speed and attack
            2,                     // Lasts 2 turns
            2,                     // 2 turn cooldown
            "A quick, limbering stretch that readies Ginger for high-speed maneuvers.",
            "Grants +1 SPEED and +1 ATK for 2 turns.",
            "2 turn cooldown"
        );
    }
    
    private static BattleSkill createTheZoomies() {
        return new BattleSkill(
            "The Zoomies",
            BattleSkill.EffectType.DAMAGE,
            30,                    // Base damage (30 physical damage)
            "",                    // No scaling - flat damage
            2,                     // Evasion buff lasts 2 turns
            4,                     // 4 turn cooldown
            "Pure, unadulterated chaotic energy makes Ginger an erratic, blurring target.",
            "Deals 30 Physical Damage. Increases EVASION (30% dodge chance) for 2 turns.",
            "4 turn cooldown"
        );
    }
    
    private static BattleSkill createCinderSlash() {
        return new BattleSkill(
            "Cinder-Slash",
            BattleSkill.EffectType.DAMAGE,
            80,                    // Base damage (ultimate)
            "attack[self]",        // Scales with attack
            0,                     // No duration (instant)
            6,                     // 6 turn cooldown
            "A devastating swipe fueled by molten rage, splashing heat onto all nearby foes.",
            "Deals 80 Physical Damage. Ignores Evasion buffs. 20% chance to inflict Burn (damage over time).",
            "6 turn cooldown"
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
    
    // ============ Dart Skill Definitions ============
    
    private static BattleSkill createWaterfowlFlurry() {
        return new BattleSkill(
            "Waterfowl Flurry",
            BattleSkill.EffectType.DAMAGE,
            10,                    // Base damage (10 × 3 hits)
            "attack[self]",        // Scales with attack
            0,                     // No duration (instant)
            0,                     // No cooldown
            "Dart unleashes a rapid sequence of precision slashes aimed at weak points.",
            "Deals 10 Physical Damage × 3 hits. Grants +10% Crit Rate on the next turn",
            "No cooldown"
        );
    }
    
    private static BattleSkill createPounceStep() {
        return new BattleSkill(
            "Pounce Step",
            BattleSkill.EffectType.DAMAGE,
            20,                    // Base damage
            "attack[self]",        // Scales with attack
            1,                     // Speed buff lasts 1 turn
            2,                     // 2 turn cooldown
            "Dart lunges forward, strikes, then immediately retreats to reposition.",
            "Deals 20 Physical Damage. Grants +1 SPEED for 1 turn. If this attack critically hits, the enemy skips their next turn",
            "2 turn cooldown"
        );
    }
    
    private static BattleSkill createBucklerPawParry() {
        return new BattleSkill(
            "Buckler Paw (Parry)",
            BattleSkill.EffectType.PARRY,
            0,
            "defense[self]",
            1,
            3,
            "Dart enters a focused stance, waiting for the exact moment to deflect and retaliate.",
            "Deflects incoming damage based on incoming hit plus DEF. Failure can cause reduced chip damage.",
            "3 turn cooldown (reduced to 1 turn on failed parry)"
        );
    }
    
    private static BattleSkill createMoonlightGreatClaw() {
        return new BattleSkill(
            "Moonlight Great-Claw",
            BattleSkill.EffectType.DAMAGE,
            80,                    // Base damage (ultimate)
            "attack[self]",        // Scales with attack
            0,                     // No duration (instant)
            5,                     // 5 turn cooldown
            "Dart channels spectral energy into a devastating, sweeping execution strike.",
            "Deals 80 Physical Damage. If this attack defeats an enemy, Dart immediately gains another turn",
            "5 turn cooldown"
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
