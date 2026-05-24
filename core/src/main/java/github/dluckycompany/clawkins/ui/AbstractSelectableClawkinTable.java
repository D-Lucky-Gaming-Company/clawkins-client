package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Selectable table row bound to a party {@link Clawkin}.
 */
public abstract class AbstractSelectableClawkinTable extends AbstractSelectableTable implements SelectableClawkinEntry {

    protected final Clawkin clawkin;

    protected AbstractSelectableClawkinTable(
            Clawkin clawkin,
            Drawable selectedBackground,
            Drawable normalBackground) {
        super(selectedBackground, normalBackground);
        this.clawkin = clawkin;
    }

    @Override
    public Clawkin getClawkin() {
        return clawkin;
    }

    /**
     * Creates the HP display widget shown inside this selectable row.
     */
    protected abstract ClawkinHpDisplay createInnerCard();

    /**
     * Adds the inner card widget to this row's layout.
     */
    protected abstract void mountInnerCard(ClawkinHpDisplay card);
}
