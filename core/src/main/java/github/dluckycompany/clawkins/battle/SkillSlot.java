package github.dluckycompany.clawkins.battle;

/**
 * Represents a skill slot with lock/unlock state.
 * Combines a BattleSkill with its unlock requirements and current state.
 */
public class SkillSlot {
    private final BattleSkill skill;
    private final int unlockLevel;
    private final int slotIndex; // 0, 1, 2 for skill slots 1, 2, 3
    
    public SkillSlot(BattleSkill skill, int unlockLevel, int slotIndex) {
        this.skill = skill;
        this.unlockLevel = Math.max(1, unlockLevel);
        this.slotIndex = slotIndex;
    }
    
    /**
     * Checks if this skill is unlocked at the given level.
     * 
     * @param currentLevel The Clawkin's current level
     * @return true if the skill is unlocked
     */
    public boolean isUnlocked(int currentLevel) {
        return currentLevel >= unlockLevel;
    }
    
    /**
     * Checks if this skill is locked at the given level.
     * 
     * @param currentLevel The Clawkin's current level
     * @return true if the skill is locked
     */
    public boolean isLocked(int currentLevel) {
        return currentLevel < unlockLevel;
    }
    
    /**
     * Gets the number of levels remaining until unlock.
     * 
     * @param currentLevel The Clawkin's current level
     * @return Levels remaining (0 if already unlocked)
     */
    public int getLevelsUntilUnlock(int currentLevel) {
        return Math.max(0, unlockLevel - currentLevel);
    }
    
    /**
     * Gets a display string for the lock state.
     * 
     * @param currentLevel The Clawkin's current level
     * @return Display string like "Unlocks at Lv. 10" or empty if unlocked
     */
    public String getLockDisplayText(int currentLevel) {
        if (isUnlocked(currentLevel)) {
            return "";
        }
        return "Unlocks at Lv. " + unlockLevel;
    }
    
    public BattleSkill getSkill() {
        return skill;
    }
    
    public int getUnlockLevel() {
        return unlockLevel;
    }
    
    public int getSlotIndex() {
        return slotIndex;
    }
    
    /**
     * Gets the skill name, or "Locked" if skill is null.
     */
    public String getDisplayName() {
        return skill != null ? skill.getName() : "Locked";
    }
}
