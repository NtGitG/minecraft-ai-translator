package fr.ntgitg.mineglot.utils.encoder;

import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.Normalizer2;
import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.EnumSet;

public final class LanguageEncoder {

    private static final Normalizer2 NFC = Normalizer2.getNFCInstance();

    private static final EnumSet<SupportedLanguage> RTL =
            EnumSet.of(SupportedLanguage.ARABIC, SupportedLanguage.URDU, SupportedLanguage.HEBREW);

    private LanguageEncoder() {
    }

    public static String encode(String text, SupportedLanguage srcLang) {
        if (text == null || text.isEmpty() || srcLang == null) {
            return "";
        }
        try {
            String normalized = NFC.normalize(text);
            return RTL.contains(srcLang) ? reorderRTL(normalized) : normalized;
        } catch (Exception e) {
            ModLogger.error("Encode error", e);
            return "";
        }
    }

    public static String decode(String text, SupportedLanguage tgtLang) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        try {
            String normalized = NFC.normalize(text);
            return RTL.contains(tgtLang) ? reorderRTL(normalized) : normalized;
        } catch (Exception e) {
            ModLogger.error("Decode error", e);
            return "";
        }
    }

    private static String reorderRTL(String input) {
        Bidi bidi = new Bidi(input, Bidi.DIRECTION_RIGHT_TO_LEFT);
        return bidi.writeReordered(Bidi.DO_MIRRORING | Bidi.REMOVE_BIDI_CONTROLS);
    }
}
