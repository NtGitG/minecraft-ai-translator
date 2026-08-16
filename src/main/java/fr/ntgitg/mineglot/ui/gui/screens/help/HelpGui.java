package fr.ntgitg.mineglot.ui.gui.screens.help;

import fr.ntgitg.mineglot.core.service.help.HelpConfigLoader;
import fr.ntgitg.mineglot.ui.gui.base.AbstractReadOnlyListGui;
import fr.ntgitg.mineglot.ui.gui.utils.sound.SoundManager;
import fr.ntgitg.mineglot.ui.gui.utils.title.TitleManager;
import fr.ntgitg.mineglot.ui.gui.utils.tooltip.TooltipManager;
import fr.ntgitg.mineglot.utils.help.HelpData;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;

public class HelpGui extends AbstractReadOnlyListGui<HelpData.HelpCommand> {

    public HelpGui(GuiScreen parentScreen) {
        super(parentScreen, loadHelpCommands(), 4);
    }

    private static List<HelpData.HelpCommand> loadHelpCommands() {
        try {
            HelpData helpData = HelpConfigLoader.getHelpData();
            if (helpData != null && helpData.commands != null && !helpData.commands.isEmpty()) {
                return helpData.commands;
            }
            ModLogger.warn("Donnees d'aide vides ou nulles, utilisation du fallback");
            return createFallbackCommands();
        } catch (Exception e) {
            ModLogger.warn("Impossible de charger les donnees d'aide - fallback utilise");
            return createFallbackCommands();
        }
    }

    private static List<HelpData.HelpCommand> createFallbackCommands() {
        List<HelpData.HelpCommand> fallback = new java.util.ArrayList<>();

        addFallbackCommand(fallback, "/translate <texte>", "Traduit instantanement un texte");
        addFallbackCommand(fallback, "/trs <texte>", "Alias rapide de /translate");
        addFallbackCommand(fallback, "/trs msg <joueur> <texte>",
                "Traduit et envoie un message prive en toute securite");
        addFallbackCommand(fallback, "/translation [texte]",
                "Traduit un texte ou active la selection de message");
        addFallbackCommand(fallback, "/trs-clear", "Supprime la derniere traduction du cache");
        addFallbackCommand(fallback, "/transexport [daily|weekly|monthly]",
                "Exporte vos statistiques personnelles d'utilisation");
        addFallbackCommand(fallback, "/author", "Affiche les informations sur l'auteur");

        return fallback;
    }

    private static void addFallbackCommand(List<HelpData.HelpCommand> commands, String usage,
                                           String description) {
        HelpData.HelpCommand command = new HelpData.HelpCommand();
        command.usage = usage;
        command.description = description;
        commands.add(command);
    }

    @Override
    protected String getDisplayName(HelpData.HelpCommand cmd) {
        return cmd != null ? cmd.usage : "\u00A7c[Commande invalide]";
    }

    @Override
    protected void onSelect(HelpData.HelpCommand cmd) {
        if (cmd == null) {
            ModLogger.warn("Tentative de selection d'une commande nulle");
            return;
        }

        SoundManager.playClick();
    }

    @Override
    protected List<String> getTooltip(HelpData.HelpCommand cmd) {
        if (cmd == null) {
            return java.util.Collections
                    .singletonList(TooltipManager.COLOR_WARNING + "[Commande invalide]");
        }

        return TooltipManager.buildTooltipLines(cmd.usage,
                TooltipManager.COLOR_CONTENT
                        + (cmd.description != null ? cmd.description : "Pas de description"));
    }

    @Override
    protected String getTitle() {
        return TitleManager.getHelpTitle();
    }
}
