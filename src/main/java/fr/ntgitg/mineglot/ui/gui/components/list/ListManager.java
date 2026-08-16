package fr.ntgitg.mineglot.ui.gui.components.list;

import fr.ntgitg.mineglot.ui.core.UIManager;
import fr.ntgitg.mineglot.ui.gui.rendering.GuiRenderUtils;
import net.minecraft.client.gui.FontRenderer;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class ListManager {
    private final int selectedBackground;
    private final int unselectedBackground;
    private final int hoverBackground;
    private final int textPadding;

    private final UIManager uiManager = UIManager.getInstance();

    private BiFunction<String, Integer, Integer> colorProvider;
    private BiPredicate<String, Integer> selectionProvider;

    public ListManager() {
        this(0x8000FF00, 0x80000000, 0x80404040, 10);
    }

    public ListManager(int selectedBackground, int unselectedBackground, int hoverBackground,
                       int textPadding) {
        this.selectedBackground = selectedBackground;
        this.unselectedBackground = unselectedBackground;
        this.hoverBackground = hoverBackground;
        this.textPadding = textPadding;
        this.colorProvider = (text, idx) -> 0xFFFFFF;
        this.selectionProvider = (text, idx) -> false;
    }

    public void setColorProvider(BiFunction<String, Integer, Integer> provider) {
        this.colorProvider = Objects.requireNonNull(provider, "Color provider cannot be null");
    }

    public void setSelectionProvider(BiPredicate<String, Integer> provider) {
        this.selectionProvider = Objects.requireNonNull(provider, "Selection provider cannot be null");
    }

    private void internalDraw(String text, int x, int y, int width, int height, int backgroundColor,
                              String colorCode, FontRenderer fontRenderer, int textColor) {
        GuiRenderUtils.drawRect(x, y, x + width, y + height, backgroundColor);
        String display = text;
        int maxWidth = width - 2 * textPadding;
        if (fontRenderer.getStringWidth(display) > maxWidth) {
            display = fontRenderer.trimStringToWidth(display, maxWidth) + "...";
        }
        String displayText = colorCode + display;
        uiManager.drawCenteredText(displayText, x + width / 2,
                y + (height / 2) - (fontRenderer.FONT_HEIGHT / 2), textColor);
    }

    public void drawListItemWithHover(String text, int x, int y, int width, int height,
                                      boolean isSelected, boolean isHovered, FontRenderer fontRenderer, int idx) {
        int bg = isSelected ? selectedBackground : (isHovered ? hoverBackground : unselectedBackground);
        String code = isSelected ? "\u00A7a" : (isHovered ? "\u00A7f" : "\u00A77");
        internalDraw(text, x, y, width, height, bg, code, fontRenderer, colorProvider.apply(text, idx));
    }

    public void drawList(List<String> items, int x, int y, int width, int itemHeight,
                         int spacing, int scrollOffset, int visibleItems, int selectedIndex, int hoveredIndex,
                         FontRenderer fontRenderer) {
        if (items == null || items.isEmpty()) {
            return;
        }

        int maxOffset = Math.max(0, items.size() - visibleItems);
        int clampedOffset = Math.min(Math.max(0, scrollOffset), maxOffset);
        int count = Math.min(visibleItems, items.size() - clampedOffset);

        for (int i = 0; i < count; i++) {
            int idx = i + clampedOffset;
            int itemY = y + i * (itemHeight + spacing);
            boolean selected = idx == selectedIndex || selectionProvider.test(items.get(idx), idx);
            boolean hovered = idx == hoveredIndex;
            drawListItemWithHover(items.get(idx), x, itemY, width, itemHeight, selected, hovered,
                    fontRenderer, idx);
        }
    }

    public int calculateHoveredIndex(int mouseX, int mouseY, int x, int y, int width, int itemHeight,
                                     int spacing, int scrollOffset, int visibleItems, int totalItems) {
        int listHeight = visibleItems * (itemHeight + spacing);
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY >= y + listHeight) {
            return -1;
        }

        int line = (mouseY - y) / (itemHeight + spacing);
        int idx = scrollOffset + line;
        return (idx >= 0 && idx < totalItems) ? idx : -1;
    }
}
