package github.dluckycompany.clawkins.item;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Concrete ItemEffect implementation for healing items.
 * Restores HP to the target clawkin up to their maximum.
 */
public class HealingEffect implements ItemEffect {
    private final int healAmount;

    public HealingEffect(int healAmount) {
        this.healAmount = Math.max(1, healAmount);
    }

    /**
     * Apply healing to the target clawkin.
     * Returns true if healing was actually applied (target was not at max HP).
     * Returns false if the target is already at max HP.
     * 
     * Does not validate target state beyond HP - that's the responsibility of ItemUseAction.
     *
     * @param target the clawkin to heal
     * @return true if healing was applied, false if target is already at max HP
     */
    @Override
    public boolean apply(Clawkin target) {
        if (target == null) {
            return false;
        }

        // Check if healing is needed
        if (target.getCurrentHp() >= target.getMaxHp()) {
            System.out.println("[HealingEffect] " + target.getName() + " is already at max HP - healing not needed");
            return false;
        }

        int hpBefore = target.getCurrentHp();
        target.heal(healAmount);
        int hpAfter = target.getCurrentHp();

        // Log amount healed for debugging
        int actualHealed = hpAfter - hpBefore;
        System.out.println("[HealingEffect] Applied " + actualHealed + " healing to " + target.getName() + 
                          " (HP: " + hpBefore + " → " + hpAfter + ")");
        return true;
    }

    @Override
    public String getDescription() {
        return "Heals " + healAmount + " HP";
    }
}
