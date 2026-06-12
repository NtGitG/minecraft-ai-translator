package fr.ntgitg.mineglot.core.cache;

import fr.ntgitg.mineglot.core.service.AbstractService;
import fr.ntgitg.mineglot.core.service.SingletonManager;

public class CacheServiceFacade extends AbstractService {
    private TranslationCache translationCache;

    CacheServiceFacade() {
        super("Cache"); // Appel au constructeur parent
    }

    public static CacheServiceFacade getInstance() {
        return SingletonManager.getInstance(CacheServiceFacade.class, CacheServiceFacade::new);
    }

    @Override
    protected void doStart() throws Exception {
        this.translationCache = TranslationCache.getInstance();
    }

    @Override
    protected void doStop() throws Exception {
        if (translationCache != null) {
            translationCache.clearMemoryCache();
        }
    }

    public void clearCache() {
        translationCache.clearCache();
    }

    public long getCacheSize() {
        return translationCache.getCacheSize();
    }

    public TranslationCache getTranslationCache() {
        return translationCache;
    }
}
