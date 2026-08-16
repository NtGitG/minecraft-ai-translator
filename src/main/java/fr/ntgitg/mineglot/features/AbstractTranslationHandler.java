package fr.ntgitg.mineglot.features;

import fr.ntgitg.mineglot.core.service.ThreadManager;

import java.util.concurrent.TimeUnit;

public abstract class AbstractTranslationHandler<T> {

    protected static final long TRANSLATION_COOLDOWN_MS = 10_000; // 10 seconds

    protected volatile T lastTarget = null;
    protected volatile long lastTranslationTime = 0;

    protected AbstractTranslationHandler(String threadBaseName) {
    }

    protected boolean isOnCooldown(T current) {
        if (!isSame(current, lastTarget)) {
            return false;
        }
        long now = System.currentTimeMillis();
        return now - lastTranslationTime < TRANSLATION_COOLDOWN_MS;
    }

    protected void updateLastTarget(T current) {
        this.lastTarget = copyOf(current);
        this.lastTranslationTime = System.currentTimeMillis();
    }

    protected void schedule(Runnable r, long delay, TimeUnit unit) {
        ThreadManager.getFeatureExecutor().schedule(r, delay, unit);
    }

    public void shutdown() {
        lastTarget = null;
    }

    protected abstract boolean isSame(T a, T b);

    protected T copyOf(T target) {
        return target;
    }
}
