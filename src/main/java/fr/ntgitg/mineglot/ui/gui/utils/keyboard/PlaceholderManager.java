package fr.ntgitg.mineglot.ui.gui.utils.keyboard;

import fr.ntgitg.mineglot.ui.core.UIManager;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

public class PlaceholderManager {
    private static final String PLACEHOLDER_TEXT = "Rechercher";

    private int cachedTextWidth = -1;
    private FontRenderer lastFontRenderer = null;

    private final UIManager uiManager = UIManager.getInstance();

    public void drawPlaceholder(GuiTextField field, FontRenderer fontRenderer) {
        if (field == null || fontRenderer == null) {
            return;
        }

        String text = field.getText();
        if (text != null && !text.isEmpty()) {
            return; // Ne pas afficher si le champ contient du texte
        }

        if (fontRenderer != lastFontRenderer) {
            invalidateCache();
            lastFontRenderer = fontRenderer;
        }

        if (cachedTextWidth == -1) {
            cachedTextWidth = uiManager.getTextWidth(PLACEHOLDER_TEXT);
        }

        int fieldCenterX = field.xPosition + field.width / 2;
        int fieldCenterY = field.yPosition + (field.height - 8) / 2;
        int startX = fieldCenterX - cachedTextWidth / 2;

        uiManager.drawPlaceholder(PLACEHOLDER_TEXT, startX, fieldCenterY);
    }

    public void invalidateCache() {
        cachedTextWidth = -1;
        lastFontRenderer = null;
    }
}
