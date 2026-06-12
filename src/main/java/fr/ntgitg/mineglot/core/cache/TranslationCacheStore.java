package fr.ntgitg.mineglot.core.cache;

import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.core.storage.TranslationStorage;
import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

final class TranslationCacheStore {

    TranslationStorage.RawCacheResult getRawCacheValue(String text, String targetLang) {
        return TranslationStorage.getRawCacheValue(text, targetLang);
    }

    TranslationStorage.RawCacheResult getRawCacheValueByKey(String key) {
        return TranslationStorage.getRawCacheValueByKey(key);
    }

    CompletableFuture<Void> writeRawAsyncByKey(String key, String translation,
                                               BooleanSupplier shouldWrite) {
        BooleanSupplier writeAllowed = shouldWrite != null ? shouldWrite : () -> true;
        return ThreadManager.runDbAsync(() -> {
            try {
                if (!writeAllowed.getAsBoolean()) {
                    ModLogger.debug("Ecriture RocksDB ignoree pour une cle invalidee: {}", key);
                    return;
                }

                TranslationStorage.putRawCacheValueByKey(key, translation);
                if (!writeAllowed.getAsBoolean()) {
                    TranslationStorage.deleteRawCacheValueByKey(key);
                    ModLogger.debug("Ecriture RocksDB annulee apres invalidation: {}", key);
                }
            } catch (Exception e) {
                ModLogger.error("Ecriture RocksDB (cle precalculee) echouee", e);
            }
        });
    }

    void migrateLegacyAsync(String text, String targetLang, String translation, String unifiedKey) {
        final String legacyKey = TranslationStorage.generateCacheKey(text, targetLang);
        ThreadManager.runDbAsync(() -> {
            try {
                TranslationStorage.putRawCacheValue(text, targetLang, translation);
                ModLogger.debug("Migration RocksDB reussie: {} -> {}", legacyKey, unifiedKey);
            } catch (Exception e) {
                ModLogger.error("Migration RocksDB echouee pour la cle: {}", unifiedKey, e);
            }
        });
    }

    void deleteRawCacheValue(String text, String targetLang) {
        TranslationStorage.deleteRawCacheValue(text, targetLang);
    }

    void deleteRawCacheValueByKey(String key) {
        TranslationStorage.deleteRawCacheValueByKey(key);
    }

    void clearCacheStore() {
        TranslationStorage.clearCacheStore();
    }
}
