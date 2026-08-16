package fr.ntgitg.mineglot.utils.encoder;

import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import org.junit.Test;

import java.text.Normalizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class LanguageEncoderTest {

    @Test
    public void encodeReturnsEmptyStringForNullEmptyOrMissingLanguage() {
        assertEquals("", LanguageEncoder.encode(null, SupportedLanguage.ENGLISH));
        assertEquals("", LanguageEncoder.encode("", SupportedLanguage.ENGLISH));
        assertEquals("", LanguageEncoder.encode("hello", null));
    }

    @Test
    public void decodeReturnsEmptyStringForNullOrEmptyText() {
        assertEquals("", LanguageEncoder.decode(null, SupportedLanguage.ENGLISH));
        assertEquals("", LanguageEncoder.decode("", SupportedLanguage.ENGLISH));
    }

    @Test
    public void englishAndFrenchRemainStable() {
        assertEquals("Hello Steve", LanguageEncoder.encode("Hello Steve",
                SupportedLanguage.ENGLISH));
        assertEquals("Bonjour Steve", LanguageEncoder.decode("Bonjour Steve",
                SupportedLanguage.FRENCH));
    }

    @Test
    public void rtlLanguagesDoNotCrashOrReturnNull() {
        String arabic = LanguageEncoder.encode("مرحبا بالعالم", SupportedLanguage.ARABIC);
        String hebrew = LanguageEncoder.decode("שלום עולם", SupportedLanguage.HEBREW);

        assertNotNull(arabic);
        assertNotNull(hebrew);
        assertFalse(arabic.isEmpty());
        assertFalse(hebrew.isEmpty());
    }

    @Test
    public void unicodeNormalizationPreservesAccents() {
        String decomposed = "Cafe\u0301 de\u0301ja\u0300 vu";
        String expectedNfc = Normalizer.normalize(decomposed, Normalizer.Form.NFC);

        String encoded = LanguageEncoder.encode(decomposed, SupportedLanguage.FRENCH);
        String decoded = LanguageEncoder.decode(decomposed, SupportedLanguage.FRENCH);

        assertEquals("Café déjà vu", expectedNfc);
        assertEquals(expectedNfc, encoded);
        assertEquals(expectedNfc, decoded);
    }
}
