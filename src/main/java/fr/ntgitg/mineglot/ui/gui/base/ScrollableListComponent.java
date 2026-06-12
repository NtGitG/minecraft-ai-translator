package fr.ntgitg.mineglot.ui.gui.base;

import fr.ntgitg.mineglot.ui.gui.components.list.ListManager;
import fr.ntgitg.mineglot.ui.gui.components.scrollbar.ScrollManager;
import fr.ntgitg.mineglot.ui.gui.utils.keyboard.PlaceholderManager;
import fr.ntgitg.mineglot.ui.gui.utils.mouse.MouseUtils;
import fr.ntgitg.mineglot.ui.gui.utils.tooltip.TooltipManager;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class ScrollableListComponent<T> {
    private static final int SCROLLBAR_X_OFFSET = 3;

    public enum SelectionMode {
        SINGLE, MULTIPLE, NONE
    }

    private List<T> items;
    private List<T> displayItems;
    private List<String> displayNames;
    private Function<T, String> nameExtractor;

    private int x;
    private int y;
    private int width;
    private int itemHeight;
    private int spacing;
    private int visibleItems;

    private int scrollOffset;
    private boolean needsFiltering = true;
    private boolean selectedOnTop;
    private boolean isDraggingScrollBar;
    private int scrollDragOffsetY;

    private final ListManager listManager = new ListManager();

    private TooltipProvider tooltipProvider;
    private ClickListener clickListener;
    private PlaceholderManager placeholderManager;
    private GuiTextField searchField;

    private ScrollableListSelectionState<T> selectionState;
    private ScrollableListFilterEngine<T> filterEngine;

    public interface TooltipProvider {
        List<String> getTooltip(int index);
    }

    public interface ClickListener {
        void onClick(int index);
    }

    public static class Builder<T> {
        private List<T> items;
        private Function<T, String> nameExtractor;
        private int x;
        private int y;
        private int width;
        private int itemHeight = 20;
        private int spacing = 2;
        private int visibleItems = 5;
        private SelectionMode selectionMode = SelectionMode.SINGLE;
        private TooltipProvider tooltipProvider;
        private ClickListener clickListener;
        private boolean withSearchField;
        private FontRenderer fontRenderer;
        private GuiTextField externalSearchField;
        private boolean selectedOnTop;

        public Builder<T> items(List<T> items) {
            this.items = items;
            return this;
        }

        public Builder<T> nameExtractor(Function<T, String> nameExtractor) {
            this.nameExtractor = nameExtractor;
            return this;
        }

        public Builder<T> position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder<T> size(int width, int itemHeight) {
            this.width = width;
            this.itemHeight = itemHeight;
            return this;
        }

        public Builder<T> spacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        public Builder<T> visibleItems(int visibleItems) {
            this.visibleItems = visibleItems;
            return this;
        }

        public Builder<T> selectionMode(SelectionMode mode) {
            this.selectionMode = mode;
            return this;
        }

        public Builder<T> tooltipProvider(TooltipProvider provider) {
            this.tooltipProvider = provider;
            return this;
        }

        public Builder<T> clickListener(ClickListener listener) {
            this.clickListener = listener;
            return this;
        }

        public Builder<T> withSearchField(FontRenderer fontRenderer) {
            this.withSearchField = true;
            this.fontRenderer = fontRenderer;
            return this;
        }

        public Builder<T> withExternalSearchField(GuiTextField searchField) {
            this.externalSearchField = searchField;
            return this;
        }

        public Builder<T> selectedOnTop(boolean value) {
            this.selectedOnTop = value;
            return this;
        }

        public ScrollableListComponent<T> build() {
            Objects.requireNonNull(items, "items cannot be null");
            Objects.requireNonNull(nameExtractor, "nameExtractor cannot be null");

            if (withSearchField && externalSearchField != null) {
                throw new IllegalStateException("Cannot use internal and external search field together");
            }

            ScrollableListComponent<T> component = new ScrollableListComponent<>();
            component.items = new ArrayList<>(items);
            component.displayItems = new ArrayList<>(items);
            component.displayNames = new ArrayList<>();
            component.nameExtractor = nameExtractor;
            component.filterEngine = new ScrollableListFilterEngine<>(nameExtractor);
            component.selectionState = new ScrollableListSelectionState<>(selectionMode);

            component.x = x;
            component.y = y;
            component.width = width;
            component.itemHeight = itemHeight;
            component.spacing = spacing;
            component.visibleItems = visibleItems;
            component.tooltipProvider = tooltipProvider;
            component.clickListener = clickListener;
            component.selectedOnTop = selectedOnTop;
            component.placeholderManager = new PlaceholderManager();

            if (withSearchField) {
                Objects.requireNonNull(fontRenderer, "fontRenderer cannot be null with internal search field");
                component.initializeSearchField(fontRenderer);
            } else if (externalSearchField != null) {
                component.searchField = externalSearchField;
            }

            component.updateFilter();
            return component;
        }
    }

    private ScrollableListComponent() {
    }

    private void initializeSearchField(FontRenderer fontRenderer) {
        searchField = new GuiTextField(0, fontRenderer, x, y - 24, width, 20);
        searchField.setMaxStringLength(32);
        searchField.setFocused(true);
    }

    public void setSelectedOnTop(boolean value) {
        selectedOnTop = value;
        needsFiltering = true;
        updateFilter();
    }

    public void updateFilter() {
        if (items == null) {
            displayItems = new ArrayList<>();
            displayNames = new ArrayList<>();
            scrollOffset = 0;
            needsFiltering = false;
            return;
        }

        String query = getSearchQuery();
        displayItems = filterEngine.filter(items, query);

        if (selectedOnTop) {
            displayItems = filterEngine.reorderSelectedOnTop(displayItems, selectionState.getMode(),
                    selectionState.getSelectedValue(), selectionState.getSelectedValues());
        }

        selectionState.onFilterUpdated(displayItems);
        displayNames = filterEngine.extractNames(displayItems);
        scrollOffset = clampScrollOffset(scrollOffset, displayItems.size());
        needsFiltering = false;
    }

    public synchronized void draw(int mouseX, int mouseY, FontRenderer fontRenderer) {
        if (searchField != null) {
            searchField.drawTextBox();
            placeholderManager.drawPlaceholder(searchField, fontRenderer);
        }

        if (needsFiltering) {
            updateFilter();
        }

        listManager.setSelectionProvider((text, idx) -> selectionState.isSelected(idx, displayItems));

        int hoveredIdx = listManager.calculateHoveredIndex(mouseX, mouseY, x, y, width, itemHeight,
                spacing, scrollOffset, visibleItems, displayItems.size());

        listManager.drawList(displayNames, x, y, width, itemHeight, spacing,
                scrollOffset, visibleItems, selectionState.getSelectedIndex(), hoveredIdx, fontRenderer);

        if (displayItems.size() > visibleItems) {
            ScrollManager.drawScrollBar(x + width + SCROLLBAR_X_OFFSET, y, getTotalHeight(), displayItems.size(),
                    visibleItems, scrollOffset);
        }

        drawTooltip(hoveredIdx, mouseX, mouseY, fontRenderer);
    }

    public void handleMouseInput(int dWheel) {
        scrollOffset = ScrollManager.handleMouseWheel(dWheel, scrollOffset, displayItems.size(), visibleItems);
    }

    public void handleClick(int mouseX, int mouseY) {
        if (displayItems.size() > visibleItems) {
            int scrollBarX = x + width + SCROLLBAR_X_OFFSET;
            int totalHeight = getTotalHeight();
            int barHeight = ScrollManager.getScrollBarHeight(totalHeight, displayItems.size(),
                    visibleItems);
            int barY = ScrollManager.getScrollBarY(y, totalHeight, barHeight, scrollOffset,
                    displayItems.size(), visibleItems);

            if (ScrollManager.isMouseOverScrollBar(mouseX, mouseY, scrollBarX, y, totalHeight,
                    displayItems.size(), visibleItems, scrollOffset)) {
                isDraggingScrollBar = true;
                scrollDragOffsetY = Math.max(0, Math.min(barHeight - 1, mouseY - barY));
                return;
            }

            int areaScrollOffset = ScrollManager.handleScrollAreaClick(mouseX, mouseY, scrollBarX, y,
                    totalHeight, displayItems.size(), visibleItems, scrollOffset);
            if (areaScrollOffset >= 0) {
                scrollOffset = areaScrollOffset;
                isDraggingScrollBar = false;
                scrollDragOffsetY = 0;
                return;
            }
        }

        if (searchField != null && MouseUtils.isMouseInArea(mouseX, mouseY, searchField.xPosition,
                searchField.yPosition, searchField.width, searchField.height)) {
            searchField.mouseClicked(mouseX, mouseY, 0);
            needsFiltering = true;
            return;
        }

        int clickedIndex = listManager.calculateHoveredIndex(mouseX, mouseY, x, y, width, itemHeight,
                spacing, scrollOffset, visibleItems, displayItems.size());

        if (clickedIndex == -1) {
            return;
        }

        T value = displayItems.get(clickedIndex);
        selectionState.onItemClicked(clickedIndex, value);

        if (clickListener != null) {
            clickListener.onClick(clickedIndex);
        }
    }

    public void handleMouseRelease() {
        isDraggingScrollBar = false;
        scrollDragOffsetY = 0;
    }

    public void handleMouseDrag(int mouseX, int mouseY) {
        if (!isDraggingScrollBar || displayItems.size() <= visibleItems) {
            return;
        }

        int barHeight = ScrollManager.getScrollBarHeight(getTotalHeight(), displayItems.size(), visibleItems);
        int barTopY = mouseY - scrollDragOffsetY;
        scrollOffset = ScrollManager.handleScrollBarDrag(barTopY, y, getTotalHeight(), barHeight,
                displayItems.size(), visibleItems);
    }

    public int getSelectedIndex() {
        return selectionState.getSelectedIndex();
    }

    public Set<Integer> getSelectedIndices() {
        return selectionState.getSelectedIndices(displayItems);
    }

    public synchronized void setItems(List<T> items) {
        this.items = items == null ? new ArrayList<T>() : new ArrayList<>(items);
        needsFiltering = true;
        updateFilter();
    }

    public synchronized void setSelectedIndex(int idx) {
        selectionState.setSelectedIndex(idx, displayItems);
    }

    public synchronized void setSelectedIndices(Set<Integer> indices) {
        selectionState.setSelectedIndices(indices, displayItems);
    }

    public GuiTextField getSearchField() {
        return searchField;
    }

    public List<T> getDisplayItems() {
        return Collections.unmodifiableList(displayItems);
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int offset) {
        scrollOffset = clampScrollOffset(offset, displayItems.size());
    }

    public boolean isSelected(int idx) {
        return selectionState.isSelected(idx, displayItems);
    }

    public synchronized void setSelectedValue(T value) {
        selectionState.setSelectedValue(value, displayItems);
    }

    public void handleSearchFieldInput(char typedChar, int keyCode,
                                       java.util.function.Predicate<String> isSelected) {
        if (searchField == null) {
            return;
        }

        boolean changed = searchField.textboxKeyTyped(typedChar, keyCode);
        if (!changed && !searchField.getText().isEmpty()) {
            return;
        }

        needsFiltering = true;
        updateFilter();
        syncSelectionWithFilteredList(isSelected);
    }

    public void syncSelectionWithFilteredList(java.util.function.Predicate<String> isSelected) {
        selectionState.syncSelectionWithFilteredList(displayItems, nameExtractor, isSelected);
    }

    private void drawTooltip(int hoveredIdx, int mouseX, int mouseY, FontRenderer fontRenderer) {
        if (hoveredIdx == -1 || tooltipProvider == null) {
            return;
        }

        List<String> tooltipLines = tooltipProvider.getTooltip(hoveredIdx);
        if (tooltipLines == null || tooltipLines.isEmpty()) {
            return;
        }

        String title = tooltipLines.get(0);
        List<String> content = tooltipLines.size() > 1
                ? tooltipLines.subList(1, tooltipLines.size())
                : Collections.emptyList();

        TooltipManager.drawTooltipImproved(title, content, mouseX + 8, mouseY + 8, fontRenderer);
    }

    private int getTotalHeight() {
        return visibleItems * (itemHeight + spacing);
    }

    private int clampScrollOffset(int offset, int itemCount) {
        int maxOffset = Math.max(0, itemCount - visibleItems);
        return Math.min(Math.max(0, offset), maxOffset);
    }

    private String getSearchQuery() {
        if (searchField == null) {
            return "";
        }
        return searchField.getText().toLowerCase(Locale.ROOT);
    }
}
