package github.dluckycompany.clawkins.battle;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class BattleUnit {
    public enum StatType {
        ATTACK,
        DEFENSE,
        SPEED
    }

    private final String id;
    private int hp;
    private final int maxHp;
    private final int baseAttack;
    private final int baseDefense;
    private final int baseSpeed;
    private final Map<StatType, TimedBoost> temporaryBoosts = new EnumMap<>(StatType.class);
    private final Map<String, Integer> skillCooldowns = new HashMap<>();
    private BleedEffect bleedEffect = null;

    public BattleUnit(String id, int hp, int attack, int defense, int speed) {
        this.id = id;
        this.hp = hp;
        this.maxHp = hp;
        this.baseAttack = Math.max(1, attack);
        this.baseDefense = Math.max(0, defense);
        this.baseSpeed = Math.max(1, speed);
    }

    public String getId() {
        return id;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getAttack() {
        return Math.max(1, baseAttack + getBoostAmount(StatType.ATTACK));
    }

    public int getDefense() {
        return Math.max(0, baseDefense + getBoostAmount(StatType.DEFENSE));
    }

    public int getSpeed() {
        return Math.max(1, baseSpeed + getBoostAmount(StatType.SPEED));
    }

    public void addTemporaryBoost(StatType stat, int amount, int durationTurns) {
        if (stat == null || amount <= 0 || durationTurns <= 0) {
            return;
        }

        TimedBoost existing = temporaryBoosts.get(stat);
        if (existing == null) {
            temporaryBoosts.put(stat, new TimedBoost(amount, durationTurns));
            return;
        }

        existing.amount = Math.max(existing.amount, amount);
        existing.turnsRemaining = Math.max(existing.turnsRemaining, durationTurns);
    }

    public void tickTemporaryBoosts() {
        temporaryBoosts.values().removeIf(boost -> {
            boost.turnsRemaining--;
            return boost.turnsRemaining <= 0;
        });
    }

    public void applyBleed(int damagePerTurn, int durationTurns) {
        if (damagePerTurn <= 0 || durationTurns <= 0) {
            return;
        }
        // Replace existing bleed if new one is stronger or has longer duration
        if (bleedEffect == null || damagePerTurn >= bleedEffect.damagePerTurn) {
            bleedEffect = new BleedEffect(damagePerTurn, durationTurns);
        }
    }

    public int tickBleed() {
        if (bleedEffect == null) {
            return 0;
        }
        
        int damage = bleedEffect.damagePerTurn;
        bleedEffect.turnsRemaining--;
        
        if (bleedEffect.turnsRemaining <= 0) {
            bleedEffect = null;
        }
        
        return damage;
    }

    public boolean hasBleed() {
        return bleedEffect != null;
    }

    public int getBleedTurnsRemaining() {
        return bleedEffect != null ? bleedEffect.turnsRemaining : 0;
    }

    public void setSkillCooldown(String skillName, int turns) {
        if (skillName == null || turns <= 0) {
            return;
        }
        skillCooldowns.put(skillName, turns);
    }

    public int getSkillCooldown(String skillName) {
        return skillCooldowns.getOrDefault(skillName, 0);
    }

    public boolean isSkillOnCooldown(String skillName) {
        return getSkillCooldown(skillName) > 0;
    }

    public void tickCooldowns() {
        skillCooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
    }

    private int getBoostAmount(StatType stat) {
        TimedBoost boost = temporaryBoosts.get(stat);
        return boost == null ? 0 : boost.amount;
    }

    private static final class TimedBoost {
        int amount;
        int turnsRemaining;

        TimedBoost(int amount, int turnsRemaining) {
            this.amount = amount;
            this.turnsRemaining = turnsRemaining;
        }
    }

    private static final class BleedEffect {
        int damagePerTurn;
        int turnsRemaining;

        BleedEffect(int damagePerTurn, int turnsRemaining) {
            this.damagePerTurn = damagePerTurn;
            this.turnsRemaining = turnsRemaining;
        }
    }
}
