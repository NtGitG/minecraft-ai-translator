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
                new String[]{"Steve", "Bonjour", "tout", "le", "monde"}, 1);

        assertEquals("Bonjour tout le monde", message);
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
