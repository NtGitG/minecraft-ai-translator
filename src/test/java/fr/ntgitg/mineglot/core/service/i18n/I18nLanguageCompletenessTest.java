package fr.ntgitg.mineglot.core.service.i18n;

import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class I18nLanguageCompletenessTest {

    private static final String[] LANG_FILES = {
            "fr_FR.lang",
            "en_US.lang",
            "ja_JP.lang"
    };

    private static final Pattern STRING_LITERAL =
            Pattern.compile("\"([A-Za-z0-9_.-]+)\"");
    private static final Pattern I18N_KEY_PREFIX = Pattern.compile(
            "^(error|translation|command|api_key|api|cache|language|model|config|help|author|"
                    + "sign|target|player|service|chat|export|menu|gui|button)\\.");

    @Test
    public void allJavaI18nKeysExistInEveryLanguageFile() throws Exception {
        Set<String> javaKeys = collectJavaI18nKeys();

        for (String langFile : LANG_FILES) {
            Properties translations = loadTranslations(langFile);
            Set<String> missing = new HashSet<>();

            for (String key : javaKeys) {
                if (!translations.containsKey(key)) {
                    missing.add(key);
                }
            }

            assertTrue(langFile + " missing i18n keys: " + missing, missing.isEmpty());
        }
    }

    @Test
    public void genericCommandErrorMessageDoesNotExposeUnusedFormatter() throws Exception {
        for (String langFile : LANG_FILES) {
            Properties translations = loadTranslations(langFile);
            String value = translations.getProperty("command.error.general");

            assertNotNull("Missing command.error.general in " + langFile, value);
            assertFalse(langFile + " command.error.general must not contain %s",
                    value.contains("%s"));
        }
    }

    private static Set<String> collectJavaI18nKeys() throws Exception {
        final Set<String> keys = new HashSet<>();
        Path sourceRoot = Paths.get("src", "main", "java");

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> collectKeysFromFile(path, keys));
        }

        return keys;
    }

    private static void collectKeysFromFile(Path path, Set<String> keys) {
        try {
            String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            Matcher matcher = STRING_LITERAL.matcher(source);
            while (matcher.find()) {
                String candidate = matcher.group(1);
                if (I18N_KEY_PREFIX.matcher(candidate).find()) {
                    keys.add(candidate);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot scan " + path, e);
        }
    }

    private static Properties loadTranslations(String langFile) throws Exception {
        String resourcePath = "assets/mineglot/lang/" + langFile;
        InputStream stream = I18nLanguageCompletenessTest.class.getClassLoader()
                .getResourceAsStream(resourcePath);
        assertNotNull("Missing language resource " + resourcePath
                + " from " + Arrays.toString(LANG_FILES), stream);

        Properties properties = new Properties();
        try (InputStreamReader reader =
                     new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}
