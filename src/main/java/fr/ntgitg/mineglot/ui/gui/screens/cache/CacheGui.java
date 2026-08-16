package fr.ntgitg.mineglot.ui.gui.screens.cache;

import fr.ntgitg.mineglot.core.cache.TranslationCache;
import fr.ntgitg.mineglot.core.service.ThreadManager;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import fr.ntgitg.mineglot.core.storage.DatabaseOperations;
import fr.ntgitg.mineglot.ui.gui.base.AbstractGui;
import fr.ntgitg.mineglot.ui.gui.utils.button.ButtonType;
import fr.ntgitg.mineglot.ui.gui.utils.button.CustomButtonFactory;
import fr.ntgitg.mineglot.ui.gui.utils.title.TitleManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CacheGui extends AbstractGui {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int STATS_Y_OFFSET = 50;
    private static final int BUTTON_Y_OFFSET = 24;
    private static final int LINE_HEIGHT = 15;
    private static final long ROCKSDB_SIZE_REFRESH_MS = 2_000L;

    private final AtomicBoolean rocksDBSizeRefreshInProgress = new AtomicBoolean(false);
    private volatile long cachedRocksDBSize = -1L;
    private volatile long lastRocksDBSizeRefreshMs;

    public CacheGui(GuiScreen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();

        if (!getCacheServices().isOperational()) {
            ModLogger.warn("Service de cache non operationnel dans CacheGui");
        }

        buttonList.add(CustomButtonFactory.createClearCache(getCenterX() - BUTTON_WIDTH / 2,
                getUIManager().getBackButtonY(getCenterY(), GUI_HEIGHT, BUTTON_HEIGHT) - BUTTON_Y_OFFSET,
                I18nManager.getMessage("cache.clear"), btn -> {
                }));

        addBackButton();
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        try {
            if (!getCacheServices().isOperational()) {
                drawCenteredTextLine(I18nManager.getMessage("cache.service_unavailable"),
                        getCenterY() - STATS_Y_OFFSET);
                return;
            }

            long cacheSize = getCacheServices().getCacheSize();
            long rocksDBSize = getCachedRocksDBSize();

            TranslationCache cache = getCacheServices().getTranslationCache();
            double hitRate = cache.getHitRate() * 100;
            double memoryUsage = cache.getMemoryBytes() / 1024.0 / 1024.0;

            int y = getCenterY() - STATS_Y_OFFSET;
            y = drawCenteredTextLine(I18nManager.getMessage("cache.guava_format", cacheSize), y);

            if (rocksDBSize >= 0) {
                y = drawCenteredTextLine(I18nManager.getMessage("cache.rocksdb_format", rocksDBSize), y);
            } else {
                y = drawCenteredTextLine(I18nManager.getMessage("cache.rocksdb_error"), y);
            }

            y = drawCenteredTextLine(I18nManager.getMessage("cache.success_rate_format", hitRate), y);
            y = drawCenteredTextLine(I18nManager.getMessage("cache.memory_format", memoryUsage), y);
            drawCenteredTextLine(I18nManager.getMessage("cache.hit_miss_format", cache.getHitCount(),
                    cache.getMissCount()), y);
        } catch (Exception e) {
            handleConfigError(e, "affichage des statistiques du cache");
            drawCenteredTextLine(I18nManager.getMessage("cache.display_error"),
                    getCenterY() - STATS_Y_OFFSET);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        playButtonSound();

        if (button.id == BACK_BUTTON_ID) {
            super.actionPerformed(button);
            return;
        }

        if (button.id == ButtonType.CLEAR_CACHE.getId()) {
            handleClearCacheAction();
        }
    }

    @Override
    protected String getTitle() {
        return TitleManager.getCacheTitle();
    }

    private int drawCenteredTextLine(String text, int y) {
        getUIManager().drawCenteredText(text, getCenterX(), y, WHITE_COLOR);
        return y + LINE_HEIGHT;
    }

    private long getCachedRocksDBSize() {
        refreshRocksDBSizeIfNeeded();
        return cachedRocksDBSize;
    }

    private void refreshRocksDBSizeIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastRocksDBSizeRefreshMs < ROCKSDB_SIZE_REFRESH_MS) {
            return;
        }

        if (!getDatabaseService().isOperational()) {
            cachedRocksDBSize = -1L;
            lastRocksDBSizeRefreshMs = now;
            return;
        }

        if (!rocksDBSizeRefreshInProgress.compareAndSet(false, true)) {
            return;
        }

        lastRocksDBSizeRefreshMs = now;
        ThreadManager.runDbAsync(() -> {
            try {
                cachedRocksDBSize = DatabaseOperations.getSize();
            } catch (Exception e) {
                cachedRocksDBSize = -1L;
                ModLogger.warn("Impossible de rafraichir la taille RocksDB", e);
            } finally {
                rocksDBSizeRefreshInProgress.set(false);
            }
        }).exceptionally(error -> {
            cachedRocksDBSize = -1L;
            rocksDBSizeRefreshInProgress.set(false);
            ModLogger.warn("Planification du rafraichissement RocksDB impossible", error);
            return null;
        });
    }

    private void handleClearCacheAction() {
        try {
            if (!getCacheServices().isOperational()) {
                MessageService.sendError(mc.thePlayer, "cache.service_unavailable");
                return;
            }

            getCacheServices().clearCache();
            cachedRocksDBSize = 0L;
            lastRocksDBSizeRefreshMs = System.currentTimeMillis();

            getMetricsManager().resetAllModelStats();
            MessageService.sendSuccess(mc.thePlayer, "cache.clear_success");
        } catch (Exception e) {
            handleConfigError(e, "nettoyage du cache");
        }
    }
}
