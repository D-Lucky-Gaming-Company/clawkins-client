package github.dluckycompany.clawkins.item;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Item effect that temporarily boosts a clawkin's stat (ATK, DEF, or SPD).
 */
public class StatBoostEffect implements ItemEffect {
    public enum StatType {
        ATTACK, DEFENSE, SPEED
    }

    private final StatType stat;
    private final int boostAmount;
    private final int durationTurns;

    public StatBoostEffect(StatType stat, int boostAmount, int durationTurns) {
        this.stat = stat;
        this.boostAmount = Math.max(1, boostAmount);
        this.durationTurns = Math.max(1, durationTurns);
    }

    @Override
    public boolean apply(Clawkin target) {
        if (target == null) {
            return false;
        }
        target.addStatBoost(stat, boostAmount, durationTurns);
        return true;
    }

    @Override
    public String getDescription() {
        String statName = stat.toString().toLowerCase();
        return "Boosts " + statName + " by " + boostAmount + " for " + durationTurns + " turns";
    }

    public StatType getStat() {
        return stat;
    }

    public int getBoostAmount() {
        return boostAmount;
    }

    public int getDurationTurns() {
        return durationTurns;
    }
}
