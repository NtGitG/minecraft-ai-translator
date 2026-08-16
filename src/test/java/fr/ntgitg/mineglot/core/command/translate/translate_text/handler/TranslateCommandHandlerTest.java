package fr.ntgitg.mineglot.core.command.translate.translate_text.handler;

import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TranslateCommandHandlerTest {

    @Test
    public void emptyCommandShowsHelpfulUsageInsteadOfConfigError() throws Exception {
        FakeCommandSender sender = new FakeCommandSender();

        TranslateCommandHandler.handleTranslateCommand(sender, new String[0]);

        assertEquals(1, sender.messages.size());
        String message = sender.messages.get(0);
        assertTrue(message.contains("Aucun texte a traduire"));
        assertTrue(message.contains("/trs <texte>"));
        assertTrue(message.contains("/translate <texte>"));
        assertFalse(message.contains("configuration"));
        assertFalse(message.contains("Arguments invalides"));
    }

    @Test
    public void whitespaceOnlyTextShowsHelpfulUsageBeforeReadingConfig() throws Exception {
        FakeCommandSender sender = new FakeCommandSender();

        TranslateCommandHandler.handleTranslateCommand(sender, new String[]{"   "});

        assertEquals(1, sender.messages.size());
        assertTrue(sender.messages.get(0).contains("Aucun texte a traduire"));
    }

    @Test
    public void canBuildMessageAfterPrivateTargetArgument() {
        String message = TranslateCommandHandler.buildMessageFromArgs(
                new String[]{"msg", "Steve", "Bonjour", "tout", "le", "monde"}, 2);

        assertEquals("Bonjour tout le monde", message);
    }

    @Test
    public void publicTextWithMultipleWordsNeverUsesImplicitPlayerDetection() {
        AtomicInteger resolverCalls = new AtomicInteger();

        TranslateCommandHandler.TranslationCommandRequest request =
                TranslateCommandHandler.resolveTranslationRequest(
                        new String[]{"Steve", "Bonjour", "tout", "le", "monde"}, target -> {
                            resolverCalls.incrementAndGet();
                            return target;
                        });

        assertFalse(request.hasError());
        assertFalse(request.isPrivateMessage());
        assertEquals("Steve Bonjour tout le monde", request.getText());
        assertEquals(0, resolverCalls.get());
    }

    @Test
    public void explicitMsgWithKnownRecipientBuildsPrivateRequest() {
        TranslateCommandHandler.TranslationCommandRequest request =
                TranslateCommandHandler.resolveTranslationRequest(
                        new String[]{"msg", "Steve", "Bonjour", "tout", "le", "monde"},
                        target -> "Steve");

        assertFalse(request.hasError());
        assertTrue(request.isPrivateMessage());
        assertEquals("Steve", request.getTargetPlayer());
        assertEquals("Bonjour tout le monde", request.getText());
    }

    @Test
    public void explicitMsgIsCaseInsensitive() {
        TranslateCommandHandler.TranslationCommandRequest request =
                TranslateCommandHandler.resolveTranslationRequest(
                        new String[]{"MSG", "Steve", "Salut"}, target -> "Steve");

        assertFalse(request.hasError());
        assertTrue(request.isPrivateMessage());
        assertEquals("Salut", request.getText());
    }

    @Test
    public void unknownExplicitRecipientFailsClosedAndCannotBeDispatched() {
        TranslateCommandHandler.TranslationCommandRequest request =
                TranslateCommandHandler.resolveTranslationRequest(
                        new String[]{"msg", "Ghost", "message", "secret"}, target -> null);
        AtomicInteger dispatchCalls = new AtomicInteger();

        boolean dispatched = TranslateCommandHandler.dispatchTranslation(null, request,
                (senderName, targetPlayer, text) -> dispatchCalls.incrementAndGet());

        assertTrue(request.hasError());
        assertFalse(request.isPrivateMessage());
        assertEquals("translation.command.recipient_not_found", request.getErrorKey());
        assertArrayEquals(new Object[]{"Ghost"}, request.getErrorArgs());
        assertFalse(dispatched);
        assertEquals(0, dispatchCalls.get());
    }

    @Test
    public void incompleteMsgSyntaxFailsBeforeResolvingRecipient() {
        AtomicInteger resolverCalls = new AtomicInteger();
        TranslateCommandHandler.TargetPlayerResolver resolver = target -> {
            resolverCalls.incrementAndGet();
            return target;
        };

        TranslateCommandHandler.TranslationCommandRequest missingRecipient =
                TranslateCommandHandler.resolveTranslationRequest(new String[]{"msg"}, resolver);
        TranslateCommandHandler.TranslationCommandRequest missingText =
                TranslateCommandHandler.resolveTranslationRequest(
                        new String[]{"msg", "Steve"}, resolver);

        assertTrue(missingRecipient.hasError());
        assertEquals("translation.command.missing_text", missingRecipient.getErrorKey());
        assertTrue(missingText.hasError());
        assertEquals("translation.command.missing_text", missingText.getErrorKey());
        assertEquals(0, resolverCalls.get());
    }

    private static final class FakeCommandSender implements ICommandSender {
        private final List<String> messages = new ArrayList<>();

        @Override
        public String getName() {
            return "TestPlayer";
        }

        @Override
        public IChatComponent getDisplayName() {
            return new ChatComponentText(getName());
        }

        @Override
        public void addChatMessage(IChatComponent component) {
            messages.add(component.getUnformattedText());
        }

        @Override
        public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
            return true;
        }

        @Override
        public BlockPos getPosition() {
            return new BlockPos(0, 0, 0);
        }

        @Override
        public Vec3 getPositionVector() {
            return new Vec3(0, 0, 0);
        }

        @Override
        public World getEntityWorld() {
            return null;
        }

        @Override
        public Entity getCommandSenderEntity() {
            return null;
        }

        @Override
        public boolean sendCommandFeedback() {
            return false;
        }

        @Override
        public void setCommandStat(CommandResultStats.Type type, int amount) {
        }
    }
}
