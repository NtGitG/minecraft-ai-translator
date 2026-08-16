package fr.ntgitg.mineglot.ui.gui.utils.mouse;

public final class MouseUtils {

    private MouseUtils() {
    }

    public static boolean isMouseInArea(int mouseX, int mouseY, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return false;
        }

        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
