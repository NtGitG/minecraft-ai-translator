package fr.ntgitg.mineglot.core.command.translate.translate_chat.services;

import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.List;

public final class ChatSelectionDecorator {

    private static final String TRANSLATION_COMMAND = "/translation ";

    private ChatSelectionDecorator() {
    }

    public static int decorateCurrentChatHistory() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.ingameGUI == null) {
            return 0;
        }

        try {
            GuiNewChat chat = minecraft.ingameGUI.getChatGUI();
            int decorated = decorateChatLines(getChatLines(chat, "chatLines", "field_146252_h"));
            decorated += decorateChatLines(getChatLines(chat, "drawnChatLines", "field_146253_i"));
            return decorated;
        } catch (RuntimeException e) {
            ModLogger.warn("Impossible de rendre l'historique du chat cliquable", e);
            return 0;
        }
    }

    public static boolean makeSelectable(IChatComponent message) {
        if (message == null) {
            return false;
        }

        String originalText = message.getUnformattedText();
        if (!ValidationService.isNotEmpty(originalText) || originalText.startsWith("/")) {
            return false;
        }

        applySelectionClick(message, originalText);
        return true;
    }

    private static void applySelectionClick(IChatComponent component, String originalText) {
        ChatStyle currentStyle = component.getChatStyle();
        ChatStyle selectableStyle = currentStyle == null
                ? new ChatStyle()
                : currentStyle.createShallowCopy();

        selectableStyle.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                TRANSLATION_COMMAND + originalText));
        selectableStyle.setUnderlined(false);
        component.setChatStyle(selectableStyle);

        for (IChatComponent sibling : component.getSiblings()) {
            applySelectionClick(sibling, originalText);
        }
    }

    private static int decorateChatLines(List<?> lines) {
        if (lines == null || lines.isEmpty()) {
            return 0;
        }

        int decorated = 0;
        for (Object line : lines) {
            if (line instanceof ChatLine
                    && makeSelectable(((ChatLine) line).getChatComponent())) {
                decorated++;
            }
        }
        return decorated;
    }

    private static List<?> getChatLines(GuiNewChat chat, String deobfuscatedName,
                                        String srgName) {
        return ReflectionHelper.getPrivateValue(GuiNewChat.class, chat, deobfuscatedName, srgName);
    }
}
