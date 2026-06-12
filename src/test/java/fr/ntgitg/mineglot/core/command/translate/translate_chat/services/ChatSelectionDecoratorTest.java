package fr.ntgitg.mineglot.core.command.translate.translate_chat.services;

import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ChatSelectionDecoratorTest {

    @Test
    public void makeSelectableAddsTranslationClickCommand() {
        ChatComponentText message = new ChatComponentText("Steve: Bonjour");

        boolean selectable = ChatSelectionDecorator.makeSelectable(message);

        assertTrue(selectable);
        ClickEvent clickEvent = message.getChatStyle().getChatClickEvent();
        assertNotNull(clickEvent);
        assertEquals(ClickEvent.Action.RUN_COMMAND, clickEvent.getAction());
        assertEquals("/translation Steve: Bonjour", clickEvent.getValue());
    }

    @Test
    public void makeSelectableDecoratesSiblingTextToo() {
        ChatComponentText message = new ChatComponentText("Steve: ");
        IChatComponent sibling = new ChatComponentText("Bonjour");
        message.appendSibling(sibling);

        boolean selectable = ChatSelectionDecorator.makeSelectable(message);

        assertTrue(selectable);
        assertEquals("/translation Steve: Bonjour",
                sibling.getChatStyle().getChatClickEvent().getValue());
    }

    @Test
    public void makeSelectableIgnoresCommands() {
        ChatComponentText message = new ChatComponentText("/spawn");

        boolean selectable = ChatSelectionDecorator.makeSelectable(message);

        assertFalse(selectable);
        assertEquals(null, message.getChatStyle().getChatClickEvent());
    }
}
