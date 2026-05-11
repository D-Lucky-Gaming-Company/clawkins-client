package github.dluckycompany.clawkins.battle;

import github.dluckycompany.clawkins.character.SkillUnlockSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages skill slots and unlock state for a Clawkin in battle.
 * Provides validation and state checking for skill usage.
 */
public class SkillManager {
    private final String clawkinId;
    private final int currentLevel;
    private final List<SkillSlot> skillSlots;
    
    /**
     * Creates a SkillManager for a Clawkin.
     * 
     * @param clawkinId The Clawkin's ID
     * @param currentLevel The Clawkin's current level
     */
    public SkillManager(String clawkinId, int currentLevel) {
        this.clawkinId = clawkinId;
        this.currentLevel = currentLevel;
        this.skillSlots = new ArrayList<>();
        
        // Load skill slots from SkillUnlockSystem
        loadSkillSlots();
    }
    
    /**
     * Loads all skill slots for this Clawkin from the SkillUnlockSystem.
     */
    private void loadSkillSlots() {
        List<SkillUnlockSystem.SkillUnlockEntry> entries = 
            SkillUnlockSystem.getAllSkillUnlockEntries(clawkinId);
        
        int slotIndex = 0;
        for (SkillUnlockSystem.SkillUnlockEntry entry : entries) {
            if (slotIndex >= 3) {
                break; // Only support 3 skill slots for now
            }
            skillSlots.add(new SkillSlot(entry.getSkill(), entry.getUnlockLevel(), slotIndex));
            slotIndex++;
        }
    }
    
    /**
     * Gets the skill slot at the given index.
     * 
     * @param slotIndex Slot index (0, 1, 2)
     * @return SkillSlot or null if invalid index
     */
    public SkillSlot getSkillSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= skillSlots.size()) {
            return null;
        }
        return skillSlots.get(slotIndex);
    }
    
    /**
     * Gets all skill slots.
     * 
     * @return List of all skill slots
     */
    public List<SkillSlot> getAllSkillSlots() {
        return new ArrayList<>(skillSlots);
    }
    
    /**
     * Gets only the unlocked skills for battle use.
     * 
     * @return List of unlocked BattleSkills
     */
    public List<BattleSkill> getUnlockedSkills() {
        List<BattleSkill> unlocked = new ArrayList<>();
        for (SkillSlot slot : skillSlots) {
            if (slot.isUnlocked(currentLevel) && slot.getSkill() != null) {
                unlocked.add(slot.getSkill());
            }
        }
        return unlocked;
    }
    
    /**
     * Validates if a skill can be used in battle.
     * Checks both unlock state and cooldown.
     * 
     * @param slotIndex Slot index (0, 1, 2)
     * @param playerUnit The player's BattleUnit (for cooldown check)
     * @return true if the skill can be used
     */
    public boolean canUseSkill(int slotIndex, BattleUnit playerUnit) {
        SkillSlot slot = getSkillSlot(slotIndex);
        if (slot == null || slot.getSkill() == null) {
            return false;
        }
        
        // Check if skill is unlocked
        if (slot.isLocked(currentLevel)) {
            return false;
        }
        
        // Check cooldown
        if (playerUnit != null && playerUnit.isSkillOnCooldown(slot.getSkill().getName())) {
            return false;
        }
        
        return true;
    }

    /**
     * True when the slot has an unlocked skill that cannot be used solely because it is on cooldown.
     */
    public boolean isSkillBlockedOnlyByCooldown(int slotIndex, BattleUnit playerUnit) {
        SkillSlot slot = getSkillSlot(slotIndex);
        if (slot == null || slot.getSkill() == null || slot.isLocked(currentLevel)) {
            return false;
        }
        return playerUnit != null && playerUnit.isSkillOnCooldown(slot.getSkill().getName());
    }
    
    /**
     * Gets a validation message for why a skill cannot be used.
     * 
     * @param slotIndex Slot index (0, 1, 2)
     * @param playerUnit The player's BattleUnit (for cooldown check)
     * @return Error message, or empty string if skill can be used
     */
    public String getSkillValidationMessage(int slotIndex, BattleUnit playerUnit) {
        SkillSlot slot = getSkillSlot(slotIndex);
        if (slot == null || slot.getSkill() == null) {
            return "No skill in this slot.";
        }
        
        // Check if skill is locked
        if (slot.isLocked(currentLevel)) {
            return slot.getLockDisplayText(currentLevel);
        }
        
        // Check cooldown
        if (playerUnit != null && playerUnit.isSkillOnCooldown(slot.getSkill().getName())) {
            int cooldown = playerUnit.getSkillCooldown(slot.getSkill().getName());
            return slot.getSkill().getName() + " is on cooldown (" + cooldown + " turns remaining).";
        }
        
        return ""; // Skill can be used
    }
    
    /**
     * Checks if any new skills were unlocked at the current level.
     * 
     * @return List of newly unlocked skills
     */
    public List<BattleSkill> getNewlyUnlockedSkills() {
        List<BattleSkill> newlyUnlocked = new ArrayList<>();
        for (SkillSlot slot : skillSlots) {
            if (slot.getUnlockLevel() == currentLevel && slot.getSkill() != null) {
                newlyUnlocked.add(slot.getSkill());
            }
        }
        return newlyUnlocked;
    }
    
    /**
     * Gets the number of unlocked skills.
     * 
     * @return Count of unlocked skills
     */
    public int getUnlockedSkillCount() {
        int count = 0;
        for (SkillSlot slot : skillSlots) {
            if (slot.isUnlocked(currentLevel)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Gets the number of locked skills.
     * 
     * @return Count of locked skills
     */
    public int getLockedSkillCount() {
        return skillSlots.size() - getUnlockedSkillCount();
    }
    
    public String getClawkinId() {
        return clawkinId;
    }
    
    public int getCurrentLevel() {
        return currentLevel;
    }
}
