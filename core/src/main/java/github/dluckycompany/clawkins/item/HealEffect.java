package github.dluckycompany.clawkins.item;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Item effect that heals a clawkin's HP.
 * Logs HP changes for debugging and UI feedback.
 */
public class HealEffect implements ItemEffect {
    private final int healAmount;

    public HealEffect(int healAmount) {
        this.healAmount = Math.max(1, healAmount);
    }

    @Override
    public boolean apply(Clawkin target) {
        if (target == null) {
            return false;
        }

        int hpBefore = target.getCurrentHp();
        target.heal(healAmount);
        int hpAfter = target.getCurrentHp();

        // Log HP change with target name and values
        System.out.println("[ItemEffect] " + target.getName() + " healed from " + hpBefore + " to " + hpAfter + " HP");
        
        // Return true if healing was applied (HP increased), false if already at max
        return hpAfter > hpBefore;
    }

    @Override
    public String getDescription() {
        return "Restores " + healAmount + " HP";
    }

    public int getHealAmount() {
        return healAmount;
    }
}
