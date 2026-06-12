package fr.ntgitg.mineglot.events.chat;

import fr.ntgitg.mineglot.core.command.translate.translate_chat.services.ChatSelectionDecorator;
import fr.ntgitg.mineglot.core.command.translate.translate_chat.services.ChatSelectionService;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatSelectionEventHandler {

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event.isCanceled())
            return;

        if (!ChatSelectionService.getInstance().isSelecting())
            return;

        IChatComponent message = event.message;
        ChatSelectionDecorator.makeSelectable(message);
    }
}
