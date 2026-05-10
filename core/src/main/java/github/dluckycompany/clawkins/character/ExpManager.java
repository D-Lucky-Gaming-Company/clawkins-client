package github.dluckycompany.clawkins.character;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages EXP distribution and level-up processing for Clawkins.
 * Handles post-battle EXP rewards and stat updates.
 */
public class ExpManager {
    
    private static final String TAG = "ExpManager";
    
    /**
     * Awards EXP to a Clawkin after battle victory.
     * Processes all level-ups and returns results.
     * 
     * @param clawkinData The Clawkin to award EXP to
     * @param expAmount Amount of EXP to award
     * @return List of level-up results (empty if no level-ups occurred)
     */
    public static List<LevelUpResult> awardExp(ClawkinData clawkinData, int expAmount) {
        if (clawkinData == null || expAmount <= 0) {
            return List.of();
        }
        
        Gdx.app.log(TAG, "Awarding " + expAmount + " EXP to " + clawkinData.getName() + 
                    " (Level " + clawkinData.getLevel() + ")");
        
        List<LevelUpResult> results = clawkinData.grantExp(expAmount);
        
        if (!results.isEmpty()) {
            Gdx.app.log(TAG, clawkinData.getName() + " leveled up " + results.size() + " time(s)!");
            for (LevelUpResult result : results) {
                Gdx.app.log(TAG, "  Level " + result.getOldLevel() + " → " + result.getNewLevel());
                Gdx.app.log(TAG, "  HP: " + result.getOldMaxHp() + " → " + result.getNewMaxHp() + 
                           " (+" + result.getHpGain() + ")");
                Gdx.app.log(TAG, "  ATK: " + result.getOldAttack() + " → " + result.getNewAttack() + 
                           " (+" + result.getAttackGain() + ")");
                Gdx.app.log(TAG, "  DEF: " + result.getOldDefense() + " → " + result.getNewDefense() + 
                           " (+" + result.getDefenseGain() + ")");
                Gdx.app.log(TAG, "  SPEED: " + result.getOldSpeed() + " → " + result.getNewSpeed() + 
                           " (+" + result.getSpeedGain() + ")");
                
                if (result.hasNewSkills()) {
                    Gdx.app.log(TAG, "  New skills unlocked:");
                    for (var skill : result.getNewlyUnlockedSkills()) {
                        Gdx.app.log(TAG, "    - " + skill.getName());
                    }
                }
            }
        }
        
        return results;
    }
    
    /**
     * Calculates EXP reward for defeating an enemy.
     * 
     * @param enemyLevel Enemy's level (estimated from stats if not available)
     * @param enemyMaxHp Enemy's max HP
     * @param isWildBattle True if wild encounter, false if trainer battle
     * @return EXP reward amount
     */
    public static int calculateExpReward(int enemyLevel, int enemyMaxHp, boolean isWildBattle) {
        // If enemy level is unknown, estimate from HP
        if (enemyLevel <= 0) {
            enemyLevel = estimateLevelFromHp(enemyMaxHp);
        }
        
        return LevelSystem.calculateExpReward(enemyLevel, isWildBattle);
    }
    
    /**
     * Calculates EXP reward for completing a battle round.
     * Grants EXP after each round regardless of outcome.
     * 
     * @return EXP reward per round
     */
    public static int calculateRoundExpReward() {
        return LevelSystem.calculateRoundExpReward();
    }
    
    /**
     * Estimates an enemy's level based on their max HP.
     * Used when enemy level is not explicitly set.
     * 
     * @param maxHp Enemy's max HP
     * @return Estimated level
     */
    private static int estimateLevelFromHp(int maxHp) {
        // Random encounters now scale from a lower HP floor and a wider level range,
        // so use a softer estimate to avoid always collapsing to level 1.
        int estimatedLevel = 1 + Math.max(0, (maxHp - 24) / 7);
        return Math.min(estimatedLevel, LevelSystem.MAX_LEVEL);
    }
    
    /**
     * Formats a list of level-up results into a single display message.
     * 
     * @param clawkinName Name of the Clawkin
     * @param results List of level-up results
     * @return Formatted message for display
     */
    public static String formatLevelUpMessage(String clawkinName, List<LevelUpResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // If multiple level-ups, show summary first
        if (results.size() > 1) {
            LevelUpResult first = results.get(0);
            LevelUpResult last = results.get(results.size() - 1);
            sb.append(clawkinName).append(" leveled up!\n");
            sb.append("Level ").append(first.getOldLevel());
            sb.append(" → ").append(last.getNewLevel()).append("\n\n");
        } else {
            LevelUpResult result = results.get(0);
            sb.append(clawkinName).append(" leveled up!\n");
            sb.append("Level ").append(result.getOldLevel());
            sb.append(" → ").append(result.getNewLevel()).append("\n\n");
        }
        
        // Show cumulative stat gains
        int totalHpGain = 0;
        int totalAtkGain = 0;
        int totalDefGain = 0;
        int totalSpdGain = 0;
        List<String> allNewSkills = new ArrayList<>();
        
        for (LevelUpResult result : results) {
            totalHpGain += result.getHpGain();
            totalAtkGain += result.getAttackGain();
            totalDefGain += result.getDefenseGain();
            totalSpdGain += result.getSpeedGain();
            
            for (var skill : result.getNewlyUnlockedSkills()) {
                allNewSkills.add(skill.getName());
            }
        }
        
        LevelUpResult lastResult = results.get(results.size() - 1);
        
        sb.append("Stats:\n");
        sb.append("HP: ").append(lastResult.getNewMaxHp());
        if (totalHpGain > 0) {
            sb.append(" (+").append(totalHpGain).append(")");
        }
        sb.append("\n");
        
        sb.append("ATK: ").append(lastResult.getNewAttack());
        if (totalAtkGain > 0) {
            sb.append(" (+").append(totalAtkGain).append(")");
        }
        sb.append("\n");
        
        sb.append("DEF: ").append(lastResult.getNewDefense());
        if (totalDefGain > 0) {
            sb.append(" (+").append(totalDefGain).append(")");
        }
        sb.append("\n");
        
        sb.append("SPEED: ").append(lastResult.getNewSpeed());
        if (totalSpdGain > 0) {
            sb.append(" (+").append(totalSpdGain).append(")");
        }
        sb.append("\n");
        
        if (!allNewSkills.isEmpty()) {
            sb.append("\nNew Skills:\n");
            for (String skillName : allNewSkills) {
                sb.append("• ").append(skillName).append("\n");
            }
        }
        
        return sb.toString();
    }
}
