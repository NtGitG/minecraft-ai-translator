package fr.ntgitg.mineglot.ui.gui.components.scrollbar;

public final class ScrollManager {
    private static final int SCROLL_BAR_PADDING = 3;
    private static final int SCROLL_BAR_BASE_WIDTH = 6;

    private ScrollManager() {
    }

    public static void drawScrollBar(int x, int y, int totalHeight, int totalItems, int visibleItems,
                                     int scrollOffset) {
        if (totalItems <= visibleItems || totalItems <= 0 || visibleItems <= 0)
            return;

        int barHeight = getScrollBarHeight(totalHeight, totalItems, visibleItems);
        int barY = getScrollBarY(y, totalHeight, barHeight, scrollOffset, totalItems, visibleItems);

        ScrollBarRenderer.render(x + SCROLL_BAR_PADDING, y, totalHeight, barY, barHeight);
    }

    public static int handleMouseWheel(int dWheel, int scrollOffset, int totalItems,
                                       int visibleItems) {
        if (dWheel == 0 || totalItems <= visibleItems)
            return scrollOffset;
        int maxScroll = Math.max(0, totalItems - visibleItems);
        return dWheel < 0 ? Math.min(maxScroll, scrollOffset + 1) : Math.max(0, scrollOffset - 1);
    }

    public static int handleScrollBarDrag(int mouseY, int startY, int totalHeight, int barHeight,
                                          int totalItems, int visibleItems) {
        if (visibleItems <= 0 || totalItems <= visibleItems)
            return 0;
        int scrollArea = totalHeight - barHeight;
        if (scrollArea <= 0)
            return 0;

        float relativeY = Math.max(0f, Math.min((mouseY - startY), scrollArea));
        float percent = relativeY / scrollArea;
        int maxScroll = Math.max(0, totalItems - visibleItems);
        return Math.round(percent * maxScroll);
    }

    public static int getScrollBarY(int startY, int totalHeight, int barHeight, int scrollOffset,
                                    int totalItems, int visibleItems) {
        int maxScroll = Math.max(0, totalItems - visibleItems);
        if (maxScroll == 0)
            return startY;
        int scrollArea = totalHeight - barHeight;
        float percent = (float) scrollOffset / maxScroll;
        return startY + (int) (percent * scrollArea);
    }

    public static int getScrollBarHeight(int totalHeight, int totalItems, int visibleItems) {
        int minHeight = 20; // Hauteur minimale en pixels
        int maxHeight = totalHeight - 10; // Hauteur maximale (avec une petite marge)

        float ratio = (float) visibleItems / totalItems;
        int calculatedHeight = (int) (ratio * totalHeight);

        return Math.min(maxHeight, Math.max(minHeight, calculatedHeight));
    }

    public static boolean isMouseOverScrollBar(int mouseX, int mouseY, int x, int y, int totalHeight,
                                               int totalItems, int visibleItems, int scrollOffset) {
        if (totalItems <= visibleItems || totalItems <= 0 || visibleItems <= 0)
            return false;
        int barHeight = getScrollBarHeight(totalHeight, totalItems, visibleItems);
        int barY = getScrollBarY(y, totalHeight, barHeight, scrollOffset, totalItems, visibleItems);
        int barX = x + SCROLL_BAR_PADDING;
        int barW = getScrollBarWidth();
        int clickableLeft = barX - SCROLL_BAR_PADDING;
        int clickableRight = barX + barW + SCROLL_BAR_PADDING;
        return mouseX >= clickableLeft && mouseX < clickableRight && mouseY >= barY
                && mouseY < barY + barHeight;
    }

    public static int handleScrollAreaClick(int mouseX, int mouseY, int x, int y, int totalHeight,
                                            int totalItems, int visibleItems, int scrollOffset) {
        int barX = x + SCROLL_BAR_PADDING;
        int barW = getScrollBarWidth();
        if (mouseX < barX - SCROLL_BAR_PADDING || mouseX > barX + barW + SCROLL_BAR_PADDING)
            return -1;

        int barHeight = getScrollBarHeight(totalHeight, totalItems, visibleItems);
        int barY = getScrollBarY(y, totalHeight, barHeight, scrollOffset, totalItems, visibleItems);

        if (mouseY < barY) {
            return Math.max(0, scrollOffset - visibleItems);
        } else if (mouseY > barY + barHeight) {
            int maxScroll = Math.max(0, totalItems - visibleItems);
            return Math.min(maxScroll, scrollOffset + visibleItems);
        }
        return -1;
    }

    private static int getScrollBarWidth() {
        return SCROLL_BAR_BASE_WIDTH; // ou remplacez par la largeur que vous souhaitez
    }
}
