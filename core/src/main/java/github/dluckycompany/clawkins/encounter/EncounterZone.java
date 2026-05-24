package github.dluckycompany.clawkins.encounter;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import github.dluckycompany.clawkins.battle.BattleSkill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data component describing an encounter zone in the world.
 */
public class EncounterZone implements Component {
    public static final ComponentMapper<EncounterZone> MAPPER = ComponentMapper.getFor(EncounterZone.class);

    private final String encounterId;
    private final String enemyName;
    private final String encounterTableId;
    private final boolean oneShot;
    private final int enemyLevel;
    private final int enemyHp;
    private final int enemyAttack;
    private final int enemyDefense;
    private final int enemySpeed;
    private final List<BattleSkill> enemySkills;
    /** Optional battle HUD portrait path (assets-relative). */
    private final String enemyImagePath;
    /** Roaming field trainer ({@link github.dluckycompany.clawkins.component.FieldTrainerWalkSprite}). */
    private final boolean roamingTrainer;

    public EncounterZone(
            String encounterId,
            String enemyName,
            String encounterTableId,
            boolean oneShot,
            int enemyLevel,
            int enemyHp,
            int enemyAttack,
            int enemyDefense,
            int enemySpeed,
            List<BattleSkill> enemySkills,
            String enemyImagePath) {
        this(
                encounterId,
                enemyName,
                encounterTableId,
                oneShot,
                enemyLevel,
                enemyHp,
                enemyAttack,
                enemyDefense,
                enemySpeed,
                enemySkills,
                enemyImagePath,
                false
        );
    }

    public EncounterZone(
            String encounterId,
            String enemyName,
            String encounterTableId,
            boolean oneShot,
            int enemyLevel,
            int enemyHp,
            int enemyAttack,
            int enemyDefense,
            int enemySpeed,
            List<BattleSkill> enemySkills,
            String enemyImagePath,
            boolean roamingTrainer) {
        this.encounterId = encounterId;
        this.enemyName = enemyName;
        this.encounterTableId = encounterTableId;
        this.oneShot = oneShot;
        this.enemyLevel = enemyLevel;
        this.enemyHp = enemyHp;
        this.enemyAttack = enemyAttack;
        this.enemyDefense = enemyDefense;
        this.enemySpeed = enemySpeed;
        this.enemySkills = new ArrayList<>(enemySkills == null ? List.of() : enemySkills);
        this.enemyImagePath = enemyImagePath == null ? "" : enemyImagePath.trim();
        this.roamingTrainer = roamingTrainer;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public String getEnemyName() {
        return enemyName;
    }

    public String getEncounterTableId() {
        return encounterTableId;
    }

    public boolean isOneShot() {
        return oneShot;
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

    public String getEnemyImagePath() {
        return enemyImagePath;
    }

    public boolean isRoamingTrainer() {
        return roamingTrainer;
    }
}
