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

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ChatSelectionDecorator {

    private static final String TRANSLATION_COMMAND = "/translation ";
    private static final Map<IChatComponent, ClickOverride> COMPONENT_OVERRIDES =
            new IdentityHashMap<>();
    private static final Map<ClickEvent, ClickEvent> ORIGINAL_CLICKS =
            new IdentityHashMap<>();

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

    public static synchronized boolean makeSelectable(IChatComponent message) {
        if (message == null) {
            return false;
        }

        String originalText = message.getUnformattedText();
        if (!ValidationService.isNotEmpty(originalText) || originalText.startsWith("/")) {
            return false;
        }

        List<DecorationTarget> targets = new ArrayList<>();
        collectDecorationTargets(message, targets,
                Collections.newSetFromMap(new IdentityHashMap<>()));

        for (DecorationTarget target : targets) {
            ClickEvent selectionClick = new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    TRANSLATION_COMMAND + originalText);
            ClickOverride override = new ClickOverride(selectionClick);
            COMPONENT_OVERRIDES.put(target.component, override);
            ORIGINAL_CLICKS.put(selectionClick, target.originalClick);
        }

        for (DecorationTarget target : targets) {
            applySelectionClick(target.component,
                    COMPONENT_OVERRIDES.get(target.component).selectionClick);
        }
        return true;
    }

    /**
     * Restores every click action overridden by selection mode, including the shallow style
     * copies created by Minecraft when it wraps messages into {@code drawnChatLines}.
     */
    public static synchronized int restoreOriginalClicks() {
        if (ORIGINAL_CLICKS.isEmpty() && COMPONENT_OVERRIDES.isEmpty()) {
            return 0;
        }

        Set<IChatComponent> visited =
                Collections.newSetFromMap(new IdentityHashMap<>());
        int restored = restoreCurrentChatHistory(visited);

        // Also restore messages which have not reached the GUI yet, or have already scrolled out.
        for (IChatComponent component : new ArrayList<>(COMPONENT_OVERRIDES.keySet())) {
            restored += restoreClickOverrides(component, visited);
        }

        COMPONENT_OVERRIDES.clear();
        ORIGINAL_CLICKS.clear();
        return restored;
    }

    static synchronized int restoreClickOverrides(IChatComponent component) {
        return restoreClickOverrides(component,
                Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static void collectDecorationTargets(IChatComponent component,
                                                 List<DecorationTarget> targets,
                                                 Set<IChatComponent> visited) {
        if (component == null || !visited.add(component)) {
            return;
        }

        if (!COMPONENT_OVERRIDES.containsKey(component)) {
            ClickEvent originalClick = component.getChatStyle().getChatClickEvent();
            if (ORIGINAL_CLICKS.containsKey(originalClick)) {
                originalClick = ORIGINAL_CLICKS.get(originalClick);
            }
            targets.add(new DecorationTarget(component, originalClick));
        }

        for (IChatComponent sibling : component.getSiblings()) {
            collectDecorationTargets(sibling, targets, visited);
        }
    }

    private static void applySelectionClick(IChatComponent component,
                                            ClickEvent selectionClick) {
        ChatStyle currentStyle = component.getChatStyle();
        ChatStyle selectableStyle = currentStyle == null
                ? new ChatStyle()
                : currentStyle.createShallowCopy();

        selectableStyle.setChatClickEvent(selectionClick);
        component.setChatStyle(selectableStyle);
    }

    private static int restoreCurrentChatHistory(Set<IChatComponent> visited) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.ingameGUI == null) {
            return 0;
        }

        try {
            GuiNewChat chat = minecraft.ingameGUI.getChatGUI();
            int restored = restoreChatLines(
                    getChatLines(chat, "chatLines", "field_146252_h"), visited);
            restored += restoreChatLines(
                    getChatLines(chat, "drawnChatLines", "field_146253_i"), visited);
            return restored;
        } catch (RuntimeException e) {
            ModLogger.warn("Impossible de restaurer les clics originaux du chat", e);
            return 0;
        }
    }

    private static int restoreChatLines(List<?> lines, Set<IChatComponent> visited) {
        if (lines == null || lines.isEmpty()) {
            return 0;
        }

        int restored = 0;
        for (Object line : lines) {
            if (line instanceof ChatLine) {
                restored += restoreClickOverrides(
                        ((ChatLine) line).getChatComponent(), visited);
            }
        }
        return restored;
    }

    private static int restoreClickOverrides(IChatComponent component,
                                             Set<IChatComponent> visited) {
        if (component == null || !visited.add(component)) {
            return 0;
        }

        int restored = 0;
        ChatStyle currentStyle = component.getChatStyle();
        ClickEvent currentClick = currentStyle == null
                ? null
                : currentStyle.getChatClickEvent();

        if (ORIGINAL_CLICKS.containsKey(currentClick)) {
            ChatStyle restoredStyle = currentStyle == null
                    ? new ChatStyle()
                    : currentStyle.createShallowCopy();
            restoredStyle.setChatClickEvent(ORIGINAL_CLICKS.get(currentClick));
            component.setChatStyle(restoredStyle);
            restored++;
        }

        for (IChatComponent sibling : component.getSiblings()) {
            restored += restoreClickOverrides(sibling, visited);
        }
        return restored;
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

    private static final class DecorationTarget {
        private final IChatComponent component;
        private final ClickEvent originalClick;

        private DecorationTarget(IChatComponent component, ClickEvent originalClick) {
            this.component = component;
            this.originalClick = originalClick;
        }
    }

    private static final class ClickOverride {
        private final ClickEvent selectionClick;

        private ClickOverride(ClickEvent selectionClick) {
            this.selectionClick = selectionClick;
        }
    }
}
