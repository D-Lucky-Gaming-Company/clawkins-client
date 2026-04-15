package github.dluckycompany.clawkins.battle;

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
    private final String enemyDisplayName;
    private final String enemyPortraitPath;
    /** Same label as the player HP bar (clawkin name, id fallback). Updated on clawkin switch. */
    private String allyDisplayName;

    public BattleContext(
            String encounterId,
            String encounterTableId,
            List<BattleUnit> allies,
            List<BattleUnit> enemies,
            List<BattleSkill> playerSkills,
            List<BattleSkill> enemySkills,
            String enemyDisplayName,
            String enemyPortraitPath,
            String allyDisplayName) {
        this.encounterId = encounterId;
        this.encounterTableId = encounterTableId;
        this.allies = new ArrayList<>(allies);
        this.enemies = new ArrayList<>(enemies);
        this.playerSkills = new ArrayList<>(playerSkills);
        this.enemySkills = new ArrayList<>(enemySkills);
        this.enemyDisplayName = enemyDisplayName == null ? "" : enemyDisplayName;
        this.enemyPortraitPath = enemyPortraitPath == null ? "" : enemyPortraitPath.trim();
        this.allyDisplayName = allyDisplayName == null ? "" : allyDisplayName;
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
}
