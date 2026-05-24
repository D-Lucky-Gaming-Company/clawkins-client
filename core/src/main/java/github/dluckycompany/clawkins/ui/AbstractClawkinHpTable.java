package github.dluckycompany.clawkins.ui;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Base table widget bound to a single {@link Clawkin} for HP display implementations.
 */
public abstract class AbstractClawkinHpTable extends AbstractUiTable implements ClawkinHpDisplay {

    protected final Clawkin clawkin;

    protected AbstractClawkinHpTable(Clawkin clawkin) {
        this.clawkin = clawkin;
    }

    @Override
    public Clawkin getClawkin() {
        return clawkin;
    }

    @Override
    public boolean isEmpty() {
        return clawkin == null;
    }

    @Override
    public abstract void updateHp();
}
