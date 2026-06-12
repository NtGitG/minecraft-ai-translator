package fr.ntgitg.mineglot.ui.gui.utils.tooltip;

import fr.ntgitg.mineglot.ui.core.LayoutCalculator;
import fr.ntgitg.mineglot.ui.core.LayoutCalculator.ResolutionInfo;
import fr.ntgitg.mineglot.ui.core.UIManager;
import fr.ntgitg.mineglot.ui.gui.rendering.GuiRenderUtils;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public final class TooltipManager {

    private static final int TOOLTIP_PADDING = 8;
    private static final int TOOLTIP_BACKGROUND = 0xC0000000;
    private static final int TOOLTIP_BORDER = 0x80000000;
    private static final float TOOLTIP_Z_LEVEL = 300.0F;

    public static final String COLOR_TITLE = "\u00A76";
    public static final String COLOR_CONTENT = "\u00A77";
    public static final String COLOR_WARNING = "\u00A7c";
    public static final String COLOR_INFO = "\u00A7b";
    public static final String COLOR_HIGHLIGHT = "\u00A7e";

    private TooltipManager() {
    }

    public static List<String> buildTooltipLines(String title, String... contentLines) {
        List<String> lines = new ArrayList<>();
        lines.add(COLOR_TITLE + "\u00A7l" + (title != null ? title : ""));
        lines.add("");

        if (contentLines == null) {
            return lines;
        }

        for (String line : contentLines) {
            if (line != null && !line.isEmpty()) {
                lines.add(line);
            }
        }

        return lines;
    }

    public static void drawTooltipImproved(String title, List<String> content, int mouseX, int mouseY,
                                           FontRenderer fontRenderer) {
        if (content == null || content.isEmpty()) {
            return;
        }

        ResolutionInfo resolution = LayoutCalculator.getResolutionInfo();
        int screenWidth = resolution.getWidth();
        int screenHeight = resolution.getHeight();

        int maxWidth = 0;
        int lineCount = content.size();
        if (title != null) {
            String displayTitle = title.contains("\u00A7") ? title : COLOR_TITLE + title;
            int titleWidth = fontRenderer.getStringWidth(displayTitle);
            maxWidth = titleWidth;
            lineCount += 1;
        }
        for (String line : content) {
            String displayLine = line.contains("\u00A7") ? line : COLOR_CONTENT + line;
            int lineWidth = fontRenderer.getStringWidth(displayLine);
            if (lineWidth > maxWidth) {
                maxWidth = lineWidth;
            }
        }

        int tooltipWidth = maxWidth + TOOLTIP_PADDING * 2;
        int tooltipHeight = (fontRenderer.FONT_HEIGHT + 2) * lineCount + TOOLTIP_PADDING;

        int x = mouseX + 12;
        int y = mouseY - 12;

        if (x + tooltipWidth + 2 > screenWidth) {
            x = screenWidth - tooltipWidth - 2;
        }
        if (y + tooltipHeight + 2 > screenHeight) {
            y = screenHeight - tooltipHeight - 2;
        }
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, TOOLTIP_Z_LEVEL);

        GuiRenderUtils.drawRectWithBorder(x - 2, y - 2, x + tooltipWidth + 2, y + tooltipHeight + 2,
                TOOLTIP_BACKGROUND, TOOLTIP_BORDER);

        int textY = y + TOOLTIP_PADDING / 2;

        UIManager uiManager = UIManager.getInstance();

        if (title != null) {
            String displayTitle = title.contains("\u00A7") ? title : COLOR_TITLE + title;
            uiManager.drawTextWithShadow(displayTitle, x + TOOLTIP_PADDING, textY, 0xFFFFFF);
            textY += fontRenderer.FONT_HEIGHT + 2;
        }

        for (String line : content) {
            String displayLine = line.contains("\u00A7") ? line : COLOR_CONTENT + line;
            uiManager.drawTextNoShadow(displayLine, x + TOOLTIP_PADDING, textY, 0xFFFFFF);
            textY += fontRenderer.FONT_HEIGHT + 2;
        }

        GL11.glPopMatrix();
    }
}
