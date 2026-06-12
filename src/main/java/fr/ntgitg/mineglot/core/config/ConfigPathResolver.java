package fr.ntgitg.mineglot.core.config;

import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ConfigPathResolver {

    private ConfigPathResolver() {
    }

    public static String getDefaultDbPath() {
        return getConfigDirPath().resolve("mineglot").resolve("translations").toString();
    }

    public static Path getConfigDirPath() {
        try {
            if (FMLCommonHandler.instance().getSide().isClient()) {
                return Loader.instance().getConfigDir().toPath();
            }

            return FMLCommonHandler.instance().getMinecraftServerInstance().getDataDirectory()
                    .toPath().resolve("config");
        } catch (Exception e) {
            ModLogger.warn("Impossible de determiner le chemin config, fallback utilise");
            return Paths.get("config");
        }
    }

    public static void ensureDbDirectories(String dbPath) {
        try {
            Path path = Paths.get(dbPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                ModLogger.info("Repertoire DB cree: {}", path);
            }
        } catch (Exception e) {
            ModLogger.error("Impossible de creer le repertoire DB: {}", dbPath, e);
        }
    }
}
