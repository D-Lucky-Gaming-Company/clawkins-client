package github.dluckycompany.clawkins.character;

import github.dluckycompany.clawkins.battle.BattleSkill;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a level-up event.
 * Contains information about stat increases and newly unlocked skills.
 */
public class LevelUpResult {
    
    private final int oldLevel;
    private final int newLevel;
    
    private final int oldMaxHp;
    private final int newMaxHp;
    
    private final int oldAttack;
    private final int newAttack;
    
    private final int oldDefense;
    private final int newDefense;
    
    private final int oldSpeed;
    private final int newSpeed;
    
    private final List<BattleSkill> newlyUnlockedSkills;
    
    public LevelUpResult(
        int oldLevel,
        int newLevel,
        int oldMaxHp,
        int newMaxHp,
        int oldAttack,
        int newAttack,
        int oldDefense,
        int newDefense,
        int oldSpeed,
        int newSpeed,
        List<BattleSkill> newlyUnlockedSkills
    ) {
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.oldMaxHp = oldMaxHp;
        this.newMaxHp = newMaxHp;
        this.oldAttack = oldAttack;
        this.newAttack = newAttack;
        this.oldDefense = oldDefense;
        this.newDefense = newDefense;
        this.oldSpeed = oldSpeed;
        this.newSpeed = newSpeed;
        this.newlyUnlockedSkills = new ArrayList<>(newlyUnlockedSkills == null ? List.of() : newlyUnlockedSkills);
    }
    
    public int getOldLevel() {
        return oldLevel;
    }
    
    public int getNewLevel() {
        return newLevel;
    }
    
    public int getLevelsGained() {
        return newLevel - oldLevel;
    }
    
    public int getOldMaxHp() {
        return oldMaxHp;
    }
    
    public int getNewMaxHp() {
        return newMaxHp;
    }
    
    public int getHpGain() {
        return newMaxHp - oldMaxHp;
    }
    
    public int getOldAttack() {
        return oldAttack;
    }
    
    public int getNewAttack() {
        return newAttack;
    }
    
    public int getAttackGain() {
        return newAttack - oldAttack;
    }
    
    public int getOldDefense() {
        return oldDefense;
    }
    
    public int getNewDefense() {
        return newDefense;
    }
    
    public int getDefenseGain() {
        return newDefense - oldDefense;
    }
    
    public int getOldSpeed() {
        return oldSpeed;
    }
    
    public int getNewSpeed() {
        return newSpeed;
    }
    
    public int getSpeedGain() {
        return newSpeed - oldSpeed;
    }
    
    public List<BattleSkill> getNewlyUnlockedSkills() {
        return new ArrayList<>(newlyUnlockedSkills);
    }
    
    public boolean hasNewSkills() {
        return !newlyUnlockedSkills.isEmpty();
    }
    
    /**
     * Formats the level-up result as a readable string.
     * 
     * @return Formatted level-up message
     */
    public String formatLevelUpMessage() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("LEVEL UP!\n");
        sb.append("Level ").append(oldLevel).append(" → ").append(newLevel).append("\n\n");
        
        sb.append("Stat Increases:\n");
        sb.append("HP: ").append(oldMaxHp).append(" → ").append(newMaxHp);
        sb.append(" (+").append(getHpGain()).append(")\n");
        
        sb.append("ATK: ").append(oldAttack).append(" → ").append(newAttack);
        sb.append(" (+").append(getAttackGain()).append(")\n");
        
        sb.append("DEF: ").append(oldDefense).append(" → ").append(newDefense);
        sb.append(" (+").append(getDefenseGain()).append(")\n");
        
        sb.append("SPEED: ").append(oldSpeed).append(" → ").append(newSpeed);
        sb.append(" (+").append(getSpeedGain()).append(")\n");
        
        if (hasNewSkills()) {
            sb.append("\nNew Skills Unlocked:\n");
            for (BattleSkill skill : newlyUnlockedSkills) {
                sb.append("• ").append(skill.getName()).append("\n");
            }
        }
        
        return sb.toString();
    }
}
