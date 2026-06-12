package fr.ntgitg.mineglot.ui.hud.widgets.components;

import fr.ntgitg.mineglot.ui.hud.core.HUDConstants;
import fr.ntgitg.mineglot.ui.hud.widgets.base.BaseHUDWidget;

public class SimpleHUDWidget extends BaseHUDWidget {

    public SimpleHUDWidget(String label, String content, int customWidth) {
        super(label, content, HUDConstants.HUD_TEXT_COLOR, customWidth);
    }

    public SimpleHUDWidget(String label, String content) {
        this(label, content, HUDConstants.WIDGET_WIDTH);
    }
}
