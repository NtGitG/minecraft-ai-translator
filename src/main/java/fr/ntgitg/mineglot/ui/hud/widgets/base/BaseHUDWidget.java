package fr.ntgitg.mineglot.ui.hud.widgets.base;

import fr.ntgitg.mineglot.ui.hud.core.HUDConstants;

public abstract class BaseHUDWidget {
    protected final String label;
    protected final String content;
    protected final int textColor;
    protected final int width;

    protected BaseHUDWidget(String label, String content, int textColor) {
        this(label, content, textColor, HUDConstants.WIDGET_WIDTH);
    }

    protected BaseHUDWidget(String label, String content, int textColor, int width) {
        this.label = label != null ? label : "";
        this.content = content != null ? content : "";
        this.textColor = textColor;
        this.width = width > 0 ? width : HUDConstants.WIDGET_WIDTH;
    }

    protected BaseHUDWidget(String text, int textColor) {
        this(text, textColor, HUDConstants.WIDGET_WIDTH);
    }

    protected BaseHUDWidget(String text, int textColor, int width) {
        if (text != null && text.contains(":")) {
            String[] parts = text.split(":", 2);
            this.label = parts[0] + ":";
            this.content = parts.length > 1 ? parts[1].trim() : "";
        } else {
            this.label = "";
            this.content = text != null ? text : "";
        }
        this.textColor = textColor;
        this.width = width > 0 ? width : HUDConstants.WIDGET_WIDTH;
    }

    public String getLabel() {
        return label;
    }

    public String getContent() {
        return content;
    }

    public String getText() {
        if (label.isEmpty()) {
            return content;
        }
        if (content.isEmpty()) {
            return label;
        }
        return label + " " + content;
    }

    public int getWidth() {
        return width;
    }
}
