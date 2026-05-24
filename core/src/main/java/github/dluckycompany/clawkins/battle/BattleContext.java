package github.dluckycompany.clawkins.battle;

import github.dluckycompany.clawkins.character.Clawkin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BattleContext {
    private final String encounterId;
    private final String encounterTableId;
    private final List<BattleUnit> allies;
    private final List<BattleUnit> enemies;
    private final List<BattleSkill> playerSkills;
    private final List<BattleSkill> enemySkills;
    private final int enemyLevel;
    private final String enemyDisplayName;
    private final String enemyPortraitPath;
    /** Same label as the player HP bar (clawkin name, id fallback). Updated on clawkin switch. */
    private String allyDisplayName;
    /** Roaming field trainer ({@link github.dluckycompany.clawkins.component.FieldTrainerWalkSprite}). */
    private final boolean roamingTrainer;
    /** Manages skill unlock state and validation */
    private SkillManager skillManager;
    /** Active party clawkin mirrored for item stat boosts during battle. */
    private Clawkin activeAllyClawkin;
    /** Called at end of enemy turn (when player buffs tick). */
    private Runnable onPlayerTurnEnd;

    public BattleContext(
            String encounterId,
            String encounterTableId,
            List<BattleUnit> allies,
            List<BattleUnit> enemies,
            List<BattleSkill> playerSkills,
            List<BattleSkill> enemySkills,
            int enemyLevel,
            String enemyDisplayName,
            String enemyPortraitPath,
            String allyDisplayName) {
        this(
                encounterId,
                encounterTableId,
                allies,
                enemies,
                playerSkills,
                enemySkills,
                enemyLevel,
                enemyDisplayName,
                enemyPortraitPath,
                allyDisplayName,
                false
        );
    }

    public BattleContext(
            String encounterId,
            String encounterTableId,
            List<BattleUnit> allies,
            List<BattleUnit> enemies,
            List<BattleSkill> playerSkills,
            List<BattleSkill> enemySkills,
            int enemyLevel,
            String enemyDisplayName,
            String enemyPortraitPath,
            String allyDisplayName,
            boolean roamingTrainer) {
        this.encounterId = encounterId;
        this.encounterTableId = encounterTableId;
        this.allies = new ArrayList<>(allies);
        this.enemies = new ArrayList<>(enemies);
        this.playerSkills = new ArrayList<>(playerSkills);
        this.enemySkills = new ArrayList<>(enemySkills);
        this.enemyLevel = enemyLevel;
        this.enemyDisplayName = enemyDisplayName == null ? "" : enemyDisplayName;
        this.enemyPortraitPath = enemyPortraitPath == null ? "" : enemyPortraitPath.trim();
        this.allyDisplayName = allyDisplayName == null ? "" : allyDisplayName;
        this.roamingTrainer = roamingTrainer;
        this.skillManager = null; // Set externally
    }

    public String getEncounterId() {
        return encounterId;
    }

    public String getEncounterTableId() {
        return encounterTableId;
    }

    public List<BattleUnit> getAllies() {
        return Collections.unmodifiableList(allies);
    }

    public void replaceFirstAlly(BattleUnit unit) {
        if (unit != null && !allies.isEmpty()) {
            allies.set(0, unit);
        }
    }

    public List<BattleUnit> getEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    public List<BattleSkill> getPlayerSkills() {
        return Collections.unmodifiableList(playerSkills);
    }

    public void replacePlayerSkills(List<BattleSkill> skills) {
        playerSkills.clear();
        if (skills != null) {
            playerSkills.addAll(skills);
        }
    }

    public List<BattleSkill> getEnemySkills() {
        return Collections.unmodifiableList(enemySkills);
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    /** HUD label for the enemy (from map {@code enemyName}). */
    public String getEnemyDisplayName() {
        return enemyDisplayName;
    }

    /** HUD / log label for the active ally (clawkin name, not internal id). */
    public String getAllyDisplayName() {
        return allyDisplayName;
    }

    public void setAllyDisplayName(String allyDisplayName) {
        this.allyDisplayName = allyDisplayName == null ? "" : allyDisplayName;
    }

    /** Optional HUD portrait path (from map {@code enemyImagePath} / {@code image_clawkin}). */
    public String getEnemyPortraitPath() {
        return enemyPortraitPath;
    }

    public boolean isRoamingTrainer() {
        return roamingTrainer;
    }
    
    /**
     * Gets the skill manager for this battle context.
     * 
     * @return SkillManager or null if not set
     */
    public SkillManager getSkillManager() {
        return skillManager;
    }
    
    /**
     * Sets the skill manager for this battle context.
     * 
     * @param skillManager The skill manager to use
     */
    public void setSkillManager(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public Clawkin getActiveAllyClawkin() {
        return activeAllyClawkin;
    }

    public void setActiveAllyClawkin(Clawkin activeAllyClawkin) {
        this.activeAllyClawkin = activeAllyClawkin;
    }

    public void setOnPlayerTurnEnd(Runnable onPlayerTurnEnd) {
        this.onPlayerTurnEnd = onPlayerTurnEnd;
    }

    public void runPlayerTurnEndHooks() {
        if (onPlayerTurnEnd != null) {
            onPlayerTurnEnd.run();
        }
    }
}
