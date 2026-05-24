package github.dluckycompany.clawkins.encounter;

import github.dluckycompany.clawkins.battle.BattleSkill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EncounterEvent {
    private final EncounterEventType type;
    private final String encounterId;
    private final String encounterTableId;
    private final int enemyLevel;
    private final int enemyHp;
    private final int enemyAttack;
    private final int enemyDefense;
    private final int enemySpeed;
    private final List<BattleSkill> enemySkills;
    /** Display name for HUD (from map). */
    private final String enemyName;
    /** Optional portrait path relative to assets/internal (same convention as Clawkin.image_clawkin). */
    private final String enemyImagePath;
    /** True for roaming field trainers ({@link github.dluckycompany.clawkins.component.FieldTrainerWalkSprite}). */
    private final boolean roamingTrainer;

    public EncounterEvent(
            EncounterEventType type,
            String encounterId,
            String encounterTableId,
            int enemyLevel,
            int enemyHp,
            int enemyAttack,
            int enemyDefense,
            int enemySpeed,
            List<BattleSkill> enemySkills,
            String enemyName,
            String enemyImagePath) {
        this(
                type,
                encounterId,
                encounterTableId,
                enemyLevel,
                enemyHp,
                enemyAttack,
                enemyDefense,
                enemySpeed,
                enemySkills,
                enemyName,
                enemyImagePath,
                false
        );
    }

    public EncounterEvent(
            EncounterEventType type,
            String encounterId,
            String encounterTableId,
            int enemyLevel,
            int enemyHp,
            int enemyAttack,
            int enemyDefense,
            int enemySpeed,
            List<BattleSkill> enemySkills,
            String enemyName,
            String enemyImagePath,
            boolean roamingTrainer) {
        this.type = type;
        this.encounterId = encounterId;
        this.encounterTableId = encounterTableId;
        this.enemyLevel = enemyLevel;
        this.enemyHp = enemyHp;
        this.enemyAttack = enemyAttack;
        this.enemyDefense = enemyDefense;
        this.enemySpeed = enemySpeed;
        this.enemySkills = new ArrayList<>(enemySkills == null ? List.of() : enemySkills);
        this.enemyName = enemyName == null ? "" : enemyName;
        this.enemyImagePath = enemyImagePath == null ? "" : enemyImagePath.trim();
        this.roamingTrainer = roamingTrainer;
    }

    public EncounterEvent(
            EncounterEventType type,
            String encounterId,
            String encounterTableId,
            int enemyHp,
            int enemyAttack,
            int enemyDefense,
            int enemySpeed,
            List<BattleSkill> enemySkills,
            String enemyName,
            String enemyImagePath) {
        this(type, encounterId, encounterTableId, 1, enemyHp, enemyAttack, enemyDefense, enemySpeed, enemySkills, enemyName, enemyImagePath);
    }

    public EncounterEventType getType() {
        return type;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public String getEncounterTableId() {
        return encounterTableId;
    }

    public int getEnemyLevel() {
        return enemyLevel;
    }

    public int getEnemyHp() {
        return enemyHp;
    }

    public int getEnemyAttack() {
        return enemyAttack;
    }

    public int getEnemyDefense() {
        return enemyDefense;
    }

    public int getEnemySpeed() {
        return enemySpeed;
    }

    public List<BattleSkill> getEnemySkills() {
        return Collections.unmodifiableList(enemySkills);
    }

    public String getEnemyName() {
        return enemyName;
    }

    public String getEnemyImagePath() {
        return enemyImagePath;
    }

    public boolean isRoamingTrainer() {
        return roamingTrainer;
    }
}
