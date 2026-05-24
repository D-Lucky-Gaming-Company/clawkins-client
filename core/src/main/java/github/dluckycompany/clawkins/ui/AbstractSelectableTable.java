package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Base table that toggles between normal and selected background drawables.
 */
public abstract class AbstractSelectableTable extends AbstractUiTable implements SelectableUi {

    private boolean selected;
    private final Drawable selectedBackground;
    private final Drawable normalBackground;

    protected AbstractSelectableTable(Drawable selectedBackground, Drawable normalBackground) {
        this.selectedBackground = selectedBackground;
        this.normalBackground = normalBackground;
        setBackground(normalBackground);
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
        setBackground(selected ? selectedBackground : normalBackground);
        onSelectionChanged(selected);
    }

    /**
     * Invoked after the selection background changes so subclasses can add extra feedback.
     */
    protected abstract void onSelectionChanged(boolean selected);

    @Override
    public boolean isSelected() {
        return selected;
    }
}
