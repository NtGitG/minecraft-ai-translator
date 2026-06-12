package fr.ntgitg.mineglot.ui.gui.base;

import net.minecraft.client.gui.GuiScreen;

import java.util.List;

public abstract class AbstractReadOnlyListGui<T> extends AbstractScrollableListGui<T> {

    public AbstractReadOnlyListGui(GuiScreen parent, List<T> allItems, int visibleItemCount) {
        super(parent, allItems, visibleItemCount);
    }

    @Override
    protected final ScrollableListComponent.SelectionMode getSelectionMode() {
        return ScrollableListComponent.SelectionMode.NONE;
    }

    @Override
    protected final T getSelectedItem() {
        return null;
    }
}
