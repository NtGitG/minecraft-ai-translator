package fr.ntgitg.mineglot.core.translation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TranslationRendererTest {

    @Test
    public void buildsPrivateMessageCommandWithTranslatedText() {
        assertEquals("/msg Steve Bonjour tout le monde",
                TranslationRenderer.buildPrivateMessageCommand("Steve",
                        "Bonjour tout le monde"));
    }
}
