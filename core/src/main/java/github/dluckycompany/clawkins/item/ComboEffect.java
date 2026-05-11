package github.dluckycompany.clawkins.item;

import java.util.ArrayList;
import java.util.List;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Item effect that applies multiple effects in sequence.
 * Used for combo items that e.g. heal AND boost a stat simultaneously.
 *
 * Returns true if at least one sub-effect was successfully applied.
 */
public class ComboEffect implements ItemEffect {
    private final List<ItemEffect> effects;

    public ComboEffect(ItemEffect... effects) {
        this.effects = new ArrayList<>();
        for (ItemEffect e : effects) {
            if (e != null) {
                this.effects.add(e);
            }
        }
    }

    @Override
    public boolean apply(Clawkin target) {
        if (target == null || effects.isEmpty()) {
            return false;
        }
        boolean anyApplied = false;
        for (ItemEffect effect : effects) {
            if (effect.apply(target)) {
                anyApplied = true;
            }
        }
        return anyApplied;
    }

    @Override
    public String getDescription() {
        if (effects.isEmpty()) {
            return "No effect";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < effects.size(); i++) {
            if (i > 0) {
                sb.append(" + ");
            }
            sb.append(effects.get(i).getDescription());
        }
        return sb.toString();
    }
}
