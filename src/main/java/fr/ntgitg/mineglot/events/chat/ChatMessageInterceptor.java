package fr.ntgitg.mineglot.events.chat;

import fr.ntgitg.mineglot.core.chat.ChatMessageParser;
import fr.ntgitg.mineglot.core.chat.ChatMessageParser.ParsedChat;
import fr.ntgitg.mineglot.core.chat.ChatTranslationFilter;
import fr.ntgitg.mineglot.core.translation.TranslationOrchestrator;
import fr.ntgitg.mineglot.core.service.chat.ChatMessageManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.concurrent.CompletableFuture;

public class ChatMessageInterceptor {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChat(ClientChatReceivedEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!ChatTranslationFilter.canTranslateAnyChatMessage()) {
            return;
        }

        if (event.message == null) {
            ModLogger.warn("Event message was null, chat interception aborted");
            return;
        }

        String rawMessage = event.message.getUnformattedText();
        ParsedChat parsed = ChatMessageParser.extract(event);

        if (parsed == null
                || !ChatTranslationFilter.needsTranslation(parsed.pseudo, parsed.message, rawMessage)) {
            return;
        }

        ChatMessageManager chatMessageManager = ChatMessageManager.getInstance();
        ChatMessageManager.PendingMessage pending =
                chatMessageManager.registerPendingMessage(parsed.message, parsed.pseudo);

        event.setCanceled(true);
        chatMessageManager.displayOriginalMessage(event.message, pending.getChatLineId());

        try {
            // La detection de langue est resolue une seule fois en aval (apres le
            // fast-path cache), pour eviter une double detection Lingua sur cache hit.
            CompletableFuture<Void> future = TranslationOrchestrator.translateAsync(
                    parsed.pseudo,
                    parsed.message,
                    true,
                    false
            );

            future.thenRun(() -> {
                chatMessageManager.removePendingMessage(pending.getId());
                ModLogger.debug("Traduction reussie, message original supprime: ID={}",
                        pending.getId());
            }).exceptionally(throwable -> {
                ModLogger.error("Traduction echouee, message original conserve: ID={}",
                        pending.getId(), throwable);
                chatMessageManager.releasePendingMessage(pending.getId());
                return null;
            });
        } catch (Exception e) {
            ModLogger.error("Erreur lors du lancement de la traduction", e);
            chatMessageManager.releasePendingMessage(pending.getId());
        }
    }
}
