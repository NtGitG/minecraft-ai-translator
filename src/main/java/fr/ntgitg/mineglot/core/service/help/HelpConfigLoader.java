package fr.ntgitg.mineglot.core.service.help;

import com.google.gson.Gson;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.utils.help.HelpData;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class HelpConfigLoader {

    private final Gson gson;
    private volatile HelpData helpData;

    private HelpConfigLoader() {
        this.gson = new Gson();
    }

    public static HelpConfigLoader getInstance() {
        return SingletonManager.getInstance(HelpConfigLoader.class, HelpConfigLoader::new);
    }

    public static void loadHelp(String langCode) {
        getInstance().loadHelpInternal(langCode);
    }

    public static HelpData getHelpData() {
        return getInstance().getHelpDataInternal();
    }

    public synchronized void loadHelpInternal(String langCode) {
        String code = langCode == null || langCode.trim().isEmpty() ? "fr" : langCode.trim();
        String path = String.format("/assets/mineglot/help_%s.json",
                code.toLowerCase(Locale.ROOT));

        try (InputStream input = HelpConfigLoader.class.getResourceAsStream(path)) {
            if (input == null) {
                helpData = new HelpData();
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                HelpData parsed = gson.fromJson(reader, HelpData.class);
                helpData = parsed != null ? parsed : new HelpData();
            }
        } catch (Exception e) {
            helpData = new HelpData();
        }
    }

    public HelpData getHelpDataInternal() {
        HelpData current = helpData;
        if (current != null) {
            return current;
        }

        synchronized (this) {
            if (helpData == null) {
                loadHelpInternal("fr");
            }
            return helpData;
        }
    }
}
