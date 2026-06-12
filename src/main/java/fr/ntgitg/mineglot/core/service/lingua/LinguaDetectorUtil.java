package fr.ntgitg.mineglot.core.service.lingua;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import fr.ntgitg.mineglot.core.model.SupportedLanguage;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class LinguaDetectorUtil {

    private static final double MIN_RELATIVE_DISTANCE = 0.20;

    private final Object lock = new Object();
    private volatile LanguageDetector detector;
    private volatile EnumSet<Language> supportedModels;

    private LinguaDetectorUtil() {
    }

    private static LinguaDetectorUtil getInstance() {
        return SingletonManager.getInstance(LinguaDetectorUtil.class, LinguaDetectorUtil::new);
    }

    public static void initialize() {
        getInstance().initializeInternal();
    }

    public static Optional<String> detectIso(String text) {
        return getInstance().detectIsoInternal(text);
    }

    private void initializeInternal() {
        if (detector != null) {
            return;
        }

        synchronized (lock) {
            if (detector != null) {
                return;
            }

            Set<String> isoCodes = Arrays.stream(SupportedLanguage.values())
                    .map(SupportedLanguage::getCode)
                    .filter(code -> !"auto".equalsIgnoreCase(code))
                    .collect(Collectors.toSet());

            supportedModels = Arrays.stream(Language.values())
                    .filter(lang -> isoCodes.contains(lang.getIsoCode639_1().toString()))
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Language.class)));

            try {
                detector = LanguageDetectorBuilder
                        .fromLanguages(supportedModels.toArray(new Language[0]))
                        .withLowAccuracyMode()
                        .withMinimumRelativeDistance(MIN_RELATIVE_DISTANCE)
                        .build();
            } catch (Exception ex) {
                ModLogger.error("Lingua initialisation failed", ex);
                throw new IllegalStateException("Cannot initialise Lingua", ex);
            }

            ModLogger.info("Lingua initialised ({} models)", supportedModels.size());
        }
    }

    private Optional<String> detectIsoInternal(String text) {
        if (!ValidationService.isNotEmpty(text)) {
            return Optional.empty();
        }

        if (detector == null) {
            ModLogger.debug("Lingua detector not initialised yet -> initialising now");
            initializeInternal();
        }

        Language currentDetectorLanguage = detector.detectLanguageOf(text.toLowerCase(Locale.ROOT));
        if (currentDetectorLanguage == Language.UNKNOWN) {
            return Optional.empty();
        }

        ModLogger.debug("Lingua detect: '{}' -> {}", text,
                currentDetectorLanguage.getIsoCode639_1());
        return Optional.of(currentDetectorLanguage.getIsoCode639_1().toString());
    }
}
