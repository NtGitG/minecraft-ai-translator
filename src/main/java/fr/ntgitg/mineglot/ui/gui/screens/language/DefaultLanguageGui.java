package fr.ntgitg.mineglot.ui.gui.screens.language;

import fr.ntgitg.mineglot.ui.gui.utils.title.TitleManager;
import net.minecraft.client.gui.GuiScreen;

public class DefaultLanguageGui extends AbstractLanguageSelectionGui {

    public DefaultLanguageGui(GuiScreen parentScreen) {
        super(parentScreen, false, "language.default_set", "selection de la langue par defaut",
                TitleManager::getDefaultLanguageTitle);
    }
}
