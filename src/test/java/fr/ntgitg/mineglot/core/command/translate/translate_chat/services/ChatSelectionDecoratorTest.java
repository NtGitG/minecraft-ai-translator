package fr.ntgitg.mineglot.core.command.translate.translate_chat.services;

import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ChatSelectionDecoratorTest {

    @After
    public void restoreDecoratedMessages() {
        ChatSelectionDecorator.restoreOriginalClicks();
    }

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
        assertNull(message.getChatStyle().getChatClickEvent());
    }

    @Test
    public void restoreRecoversRootLinkAndInheritedSiblingLink() {
        ClickEvent link = new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com");
        ChatComponentText message = new ChatComponentText("Documentation: ");
        message.getChatStyle().setChatClickEvent(link).setUnderlined(true);
        IChatComponent sibling = new ChatComponentText("example.com");
        message.appendSibling(sibling);

        assertSame(link, sibling.getChatStyle().getChatClickEvent());

        ChatSelectionDecorator.makeSelectable(message);
        ChatSelectionDecorator.restoreOriginalClicks();

        assertSame(link, message.getChatStyle().getChatClickEvent());
        assertSame(link, sibling.getChatStyle().getChatClickEvent());
        assertTrue(message.getChatStyle().getUnderlined());
        assertTrue(sibling.getChatStyle().getUnderlined());
    }

    @Test
    public void repeatedDecorationDoesNotReplaceOriginalClickSnapshot() {
        ClickEvent link = new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com");
        ChatComponentText message = linkedMessage(link);

        ChatSelectionDecorator.makeSelectable(message);
        ClickEvent firstSelectionClick = message.getChatStyle().getChatClickEvent();
        ChatSelectionDecorator.makeSelectable(message);

        assertSame(firstSelectionClick, message.getChatStyle().getChatClickEvent());

        ChatSelectionDecorator.restoreOriginalClicks();
        assertSame(link, message.getChatStyle().getChatClickEvent());
    }

    @Test
    public void restoreWorksAcrossTwoSelectionCycles() {
        ClickEvent link = new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com");
        ChatComponentText message = linkedMessage(link);

        ChatSelectionDecorator.makeSelectable(message);
        ChatSelectionDecorator.restoreOriginalClicks();
        ChatSelectionDecorator.makeSelectable(message);
        ChatSelectionDecorator.restoreOriginalClicks();

        assertSame(link, message.getChatStyle().getChatClickEvent());
    }

    @Test
    public void restoreRecognizesMinecraftShallowStyleCopies() {
        ClickEvent link = new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com");
        ChatComponentText source = linkedMessage(link);
        ChatSelectionDecorator.makeSelectable(source);

        ChatComponentText drawnCopy = new ChatComponentText("example.com");
        drawnCopy.setChatStyle(source.getChatStyle().createShallowCopy());

        assertSame(source.getChatStyle().getChatClickEvent(),
                drawnCopy.getChatStyle().getChatClickEvent());
        assertEquals(1, ChatSelectionDecorator.restoreClickOverrides(drawnCopy));
        assertSame(link, drawnCopy.getChatStyle().getChatClickEvent());
    }

    @Test
    public void restorePreservesUnrelatedStyleChangesFromOtherMods() {
        ClickEvent link = new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com");
        ChatComponentText message = linkedMessage(link);
        ChatSelectionDecorator.makeSelectable(message);

        message.getChatStyle().setBold(true).setItalic(true);
        ChatSelectionDecorator.restoreOriginalClicks();

        assertSame(link, message.getChatStyle().getChatClickEvent());
        assertTrue(message.getChatStyle().getBold());
        assertTrue(message.getChatStyle().getItalic());
    }

    @Test
    public void restoreDoesNotOverwriteClickChangedByAnotherMod() {
        ClickEvent link = new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com");
        ClickEvent external = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/external");
        ChatComponentText message = linkedMessage(link);
        ChatSelectionDecorator.makeSelectable(message);

        ChatStyle externalStyle = message.getChatStyle().createShallowCopy();
        externalStyle.setChatClickEvent(external);
        message.setChatStyle(externalStyle);
        ChatSelectionDecorator.restoreOriginalClicks();

        assertSame(external, message.getChatStyle().getChatClickEvent());
    }

    @Test
    public void disablingSelectionServiceRestoresOriginalClick() {
        ClickEvent link = new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com");
        ChatComponentText message = linkedMessage(link);
        ChatSelectionDecorator.makeSelectable(message);

        ChatSelectionService.getInstance().setSelecting(false, false);

        assertSame(link, message.getChatStyle().getChatClickEvent());
    }

    private static ChatComponentText linkedMessage(ClickEvent clickEvent) {
        ChatComponentText message = new ChatComponentText("example.com");
        message.getChatStyle().setChatClickEvent(clickEvent).setUnderlined(true);
        return message;
    }
}
