package fr.ntgitg.mineglot.ui.gui.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public abstract class AbstractScrollableListGui<T> extends AbstractGui {
    protected List<T> allItems;
    protected ScrollableListComponent<T> listComponent;
    protected int visibleItemCount;
    protected int listSpacing = 4;
    protected int buttonWidth = 200;
    protected int buttonHeight = 20;

    public AbstractScrollableListGui(GuiScreen parent, List<T> allItems, int visibleItemCount) {
        super(parent);
        this.allItems = (allItems != null) ? allItems : new java.util.ArrayList<T>();
        this.visibleItemCount = visibleItemCount;
    }

    protected ScrollableListComponent.SelectionMode getSelectionMode() {
        return ScrollableListComponent.SelectionMode.SINGLE;
    }

    protected Set<Integer> getInitialSelectedIndices() {
        return Collections.emptySet();
    }

    protected void refreshList() {
        if (listComponent != null) {
            String query =
                    listComponent.getSearchField() != null ? listComponent.getSearchField().getText() : "";

            listComponent.setItems(allItems);
            if (isMultipleSelectionMode()) {
                listComponent.setSelectedIndices(getInitialSelectedIndices());
            } else if (isSingleSelectionMode()) {
                T current = getSelectedItem();
                if (current != null) {
                    listComponent.setSelectedValue(current);
                }
            }

            if (listComponent.getSearchField() != null && !query.isEmpty()) {
                listComponent.getSearchField().setText(query);
                listComponent.handleSearchFieldInput('\0', 0, null);
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        addBackButton();

        listComponent = new ScrollableListComponent.Builder<T>().items(allItems)
                .nameExtractor(this::getDisplayName)
                .position(getCenterX() - buttonWidth / 2, getListStartY()).size(buttonWidth, buttonHeight)
                .spacing(listSpacing).visibleItems(visibleItemCount).selectionMode(getSelectionMode()) // <--
                .tooltipProvider(idx -> getTooltip(getObjectAtIndex(idx)))
                .clickListener(idx -> onSelect(getObjectAtIndex(idx))).withSearchField(fontRendererObj)
                .selectedOnTop(false).build();

        if (isMultipleSelectionMode()) {
            listComponent.setSelectedIndices(getInitialSelectedIndices());
        } else if (isSingleSelectionMode()) {
            T current = getSelectedItem();
            if (current != null) {
                listComponent.setSelectedValue(current);
            }
        }
    }

    @Override
    public void onResize(Minecraft mc, int width, int height) {
        super.onResize(mc, width, height);
        initGui();
    }

    protected int getListStartY() {
        return getCenterY() - GUI_HEIGHT / 2 + 70;
    }

    protected T getObjectAtIndex(int idx) {
        List<T> displayItems = listComponent.getDisplayItems();
        if (idx < 0 || idx >= displayItems.size())
            return null;
        return displayItems.get(idx);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (listComponent != null) {
            Predicate<String> selectedMatcher = null;
            if (isSingleSelectionMode()) {
                selectedMatcher = displayName -> {
                    T selected = getSelectedItem();
                    if (selected == null) {
                        return false;
                    }
                    return getDisplayName(selected).equals(displayName);
                };
            }

            listComponent.handleSearchFieldInput(typedChar, keyCode, selectedMatcher);

            if (isSingleSelectionMode()) {
                T current = getSelectedItem();
                if (current != null) {
                    listComponent.setSelectedValue(current);
                }
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (listComponent != null) {
            listComponent.handleClick(mouseX, mouseY);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = org.lwjgl.input.Mouse.getDWheel();
        if (listComponent != null) {
            listComponent.handleMouseInput(dWheel);
            if (org.lwjgl.input.Mouse.isButtonDown(0)) {
                int mouseX = org.lwjgl.input.Mouse.getX() * this.width / mc.displayWidth;
                int mouseY =
                        this.height - org.lwjgl.input.Mouse.getY() * this.height / mc.displayHeight - 1;
                listComponent.handleMouseDrag(mouseX, mouseY);
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (listComponent != null) {
            listComponent.handleMouseRelease();
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        if (listComponent != null) {
            listComponent.draw(mouseX, mouseY, fontRendererObj);
            if (org.lwjgl.input.Mouse.isButtonDown(0)) {
                listComponent.handleMouseDrag(mouseX, mouseY);
            }
        }
    }

    protected abstract String getDisplayName(T item);

    protected abstract void onSelect(T item);

    protected abstract List<String> getTooltip(T item);

    protected abstract T getSelectedItem();

    private boolean isSingleSelectionMode() {
        return getSelectionMode() == ScrollableListComponent.SelectionMode.SINGLE;
    }

    private boolean isMultipleSelectionMode() {
        return getSelectionMode() == ScrollableListComponent.SelectionMode.MULTIPLE;
    }
}
