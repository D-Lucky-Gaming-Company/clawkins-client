package github.dluckycompany.clawkins.item;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Item effect that revives a fainted clawkin with partial HP.
 */
public class ReviveEffect implements ItemEffect {
    private final int hpRestored;

    public ReviveEffect(int hpRestored) {
        this.hpRestored = Math.max(1, hpRestored);
    }

    @Override
    public boolean apply(Clawkin target) {
        if (target == null) {
            return false;
        }
        // Revive only works on fainted clawkins
        if (target.isDead()) {
            target.heal(hpRestored);
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Revives and restores " + hpRestored + " HP";
    }

    public int getHpRestored() {
        return hpRestored;
    }
}
