package github.dluckycompany.clawkins.item;

import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.character.LevelSystem;

/**
 * Item effect that boosts a Clawkin's level by 1.
 * This effect requires party selection UI to choose which Clawkin to boost.
 * 
 * NOTE: This effect should NOT be applied directly via apply().
 * Instead, the InventoryUI should detect this effect type and show
 * a party selection dialog, then call applyLevelBoost() on the selected Clawkin.
 */
public class LevelBoostEffect implements ItemEffect {
    private final int levelBoost;

    public LevelBoostEffect(int levelBoost) {
        this.levelBoost = Math.max(1, levelBoost);
    }

    @Override
    public boolean apply(Clawkin target) {
        // This method should not be called directly for level boost items.
        // The InventoryUI should handle party selection first.
        // However, we implement it for completeness.
        if (target == null) {
            return false;
        }

        return applyLevelBoost(target);
    }

    /**
     * Apply level boost to the target Clawkin.
     * Increases the Clawkin's level by the boost amount (capped at MAX_LEVEL).
     * 
     * @param target the Clawkin to boost
     * @return true if the level was boosted, false if already at max level
     */
    public boolean applyLevelBoost(Clawkin target) {
        if (target == null) {
            return false;
        }

        int currentLevel = target.getLevel();
        int newLevel = Math.min(LevelSystem.MAX_LEVEL, currentLevel + levelBoost);

        // Check if already at max level
        if (currentLevel >= LevelSystem.MAX_LEVEL) {
            System.out.println("[LevelBoostEffect] " + target.getName() + " is already at max level (" + LevelSystem.MAX_LEVEL + ")");
            return false;
        }

        // Sync stats to new level (this handles HP, ATK, DEF, SPD growth)
        target.syncStatsToSharedExperienceLevel(newLevel);

        System.out.println("[LevelBoostEffect] " + target.getName() + " leveled up from " + currentLevel + " to " + newLevel);
        return true;
    }

    @Override
    public String getDescription() {
        return "Boosts level by " + levelBoost;
    }

    public int getLevelBoost() {
        return levelBoost;
    }

    /**
     * Check if this effect requires party selection.
     * Used by InventoryUI to determine if party selection dialog should be shown.
     * 
     * @return true (level boost always requires party selection)
     */
    public boolean requiresPartySelection() {
        return true;
    }
}
