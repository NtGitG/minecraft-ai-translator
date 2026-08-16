package fr.ntgitg.mineglot.features.signs;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SignTranslationHandlerTest {

    @Test
    public void buildSignTextKeepsVisibleLineSeparation() {
        IChatComponent[] lines = {
                new ChatComponentText("Market"),
                new ChatComponentText("  Buy now  "),
                new ChatComponentText(""),
                new ChatComponentText("Floor 2")
        };

        assertEquals("Market | Buy now | Floor 2",
                SignTranslationHandler.buildSignText(lines));
    }

    @Test
    public void buildSignTextIgnoresEmptyLines() {
        IChatComponent[] lines = {
                null,
                new ChatComponentText(" "),
                new ChatComponentText("")
        };

        assertEquals("", SignTranslationHandler.buildSignText(lines));
    }
}
