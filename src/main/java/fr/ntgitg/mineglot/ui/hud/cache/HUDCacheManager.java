package fr.ntgitg.mineglot.ui.hud.cache;

import fr.ntgitg.mineglot.ui.core.TextRenderer;
import fr.ntgitg.mineglot.ui.hud.core.HUDConstants;
import fr.ntgitg.mineglot.ui.hud.factory.HUDWidgetFactory;
import fr.ntgitg.mineglot.ui.hud.widgets.base.BaseHUDWidget;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public class HUDCacheManager {

    private List<WidgetPosition> cachedPositions;
    private List<CachedWidgetRender> cachedRenders;
    private final HUDWidgetFactory widgetFactory;

    public HUDCacheManager(HUDWidgetFactory widgetFactory) {
        this.widgetFactory = widgetFactory;
        this.cachedPositions = calculateWidgetPositions();
        this.cachedRenders = createCachedRenders();
    }

    private List<WidgetPosition> calculateWidgetPositions() {
        List<BaseHUDWidget> widgets = widgetFactory.createWidgets();
        List<WidgetPosition> positions = new ArrayList<>();

        if (widgets.isEmpty()) {
            return positions;
        }

        int startX = HUDConstants.HUD_POSITION_X + HUDConstants.HUD_INNER_PADDING_LEFT;

        int hudCenterY = HUDConstants.HUD_POSITION_Y + (HUDConstants.HUD_HEIGHT / 2);
        int startY = hudCenterY - (HUDConstants.WIDGET_HEIGHT / 2);

        int currentX = startX;

        for (BaseHUDWidget widget : widgets) {
            positions.add(new WidgetPosition(widget, currentX, startY));
            currentX += widget.getWidth() + HUDConstants.WIDGET_SPACING;
        }

        return positions;
    }

    private List<CachedWidgetRender> createCachedRenders() {
        List<CachedWidgetRender> renders = new ArrayList<>();

        for (WidgetPosition widgetPos : cachedPositions) {
            renders.add(new CachedWidgetRender(widgetPos));
        }

        return renders;
    }

    public List<CachedWidgetRender> getCachedRenders() {
        return cachedRenders;
    }

    public void invalidateAndRecreateCache() {
        widgetFactory.invalidateCache();

        this.cachedPositions = calculateWidgetPositions();
        this.cachedRenders = createCachedRenders();
        ModLogger.debug("HUDCacheManager - widgets recréés");
    }

    public static class WidgetPosition {
        private final BaseHUDWidget widget;
        private final int x;
        private final int y;

        public WidgetPosition(BaseHUDWidget widget, int x, int y) {
            this.widget = widget;
            this.x = x;
            this.y = y;
        }

        public BaseHUDWidget getWidget() {
            return widget;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    public static class CachedWidgetRender {
        private final WidgetPosition widgetPos;
        private final String label;
        private final String content;
        private final int textColor;

        public CachedWidgetRender(WidgetPosition widgetPos) {
            this.widgetPos = widgetPos;
            BaseHUDWidget widget = widgetPos.getWidget();
            this.label = widget.getLabel();
            this.content = widget.getContent();
            this.textColor = HUDConstants.HUD_TEXT_COLOR;
        }

        public void render(FontRenderer fontRenderer) {
            if (fontRenderer == null) {
                return;
            }

            int x = widgetPos.getX();
            int y = widgetPos.getY();
            int textY = TextRenderer.getCenteredY(y, HUDConstants.WIDGET_HEIGHT, fontRenderer);

            if (!label.isEmpty()) {
                TextRenderer.drawHUDText(fontRenderer, label, x, textY,
                        HUDConstants.WIDGET_TEXT_SCALE, textColor);
            }

            if (!content.isEmpty()) {
                int contentX = x + HUDConstants.LABEL_WIDTH + HUDConstants.LABEL_CONTENT_SPACING;
                TextRenderer.drawHUDText(fontRenderer, content, contentX, textY,
                        HUDConstants.WIDGET_TEXT_SCALE, textColor);
            }
        }
    }
}
