package fr.ntgitg.mineglot.utils.text;

import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TranslationTextPreparerTest {

    @Test
    public void prepareCleansMinecraftColorCodesBeforeEncoding() {
        assertEquals("Hello", TranslationTextPreparer.prepare("\u00A7aHello",
                SupportedLanguage.ENGLISH));
    }

    @Test
    public void prepareWithAutoSourceOnlyCleansText() {
        assertEquals("Bonjour", TranslationTextPreparer.prepare("\u00A7cBonjour",
                (SupportedLanguage) null));
    }
}
