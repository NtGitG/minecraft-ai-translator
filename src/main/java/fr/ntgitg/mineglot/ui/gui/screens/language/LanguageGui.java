package fr.ntgitg.mineglot.ui.gui.screens.language;

import fr.ntgitg.mineglot.ui.gui.utils.title.TitleManager;
import net.minecraft.client.gui.GuiScreen;

public class LanguageGui extends AbstractLanguageSelectionGui {

    public LanguageGui(GuiScreen parentScreen) {
        super(parentScreen, true, "language.selected", "selection de la langue cible",
                TitleManager::getLanguageTitle);
    }
}
