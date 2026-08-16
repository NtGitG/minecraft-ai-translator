package fr.ntgitg.mineglot.core.service;

import fr.ntgitg.mineglot.utils.log.ModLogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public final class SingletonManager {

    private static final ConcurrentMap<Class<?>, Object> INSTANCES = new ConcurrentHashMap<>();

    private SingletonManager() {
        throw new AssertionError("Utility class");
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<T> clazz, Supplier<T> constructor) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz cannot be null");
        }
        if (constructor == null) {
            throw new IllegalArgumentException("constructor cannot be null");
        }

        try {
            Object existing = INSTANCES.get(clazz);
            if (existing != null) {
                return (T) existing;
            }

            T created = constructor.get();
            if (created == null) {
                throw new IllegalStateException("Constructor returned null for "
                        + clazz.getSimpleName());
            }

            Object raced = INSTANCES.putIfAbsent(clazz, created);
            return (T) (raced != null ? raced : created);
        } catch (RuntimeException e) {
            ModLogger.error("Erreur creation singleton: {}", clazz.getSimpleName(), e);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getExistingInstance(Class<T> clazz) {
        return (T) INSTANCES.get(clazz);
    }

    public static boolean hasInstance(Class<?> clazz) {
        return INSTANCES.containsKey(clazz);
    }

    public static boolean removeInstance(Class<?> clazz) {
        Object removed = INSTANCES.remove(clazz);
        if (removed == null) {
            return false;
        }
        ModLogger.debug("Singleton supprime: {}", clazz.getSimpleName());
        return true;
    }

    public static void clearAll() {
        int count = INSTANCES.size();
        INSTANCES.clear();
        ModLogger.info("Singletons nettoyes: {}", count);
    }

    public static int getInstanceCount() {
        return INSTANCES.size();
    }

    public static void debugInstances() {
        ModLogger.debug("Singletons actifs: {}", INSTANCES.size());
        INSTANCES.keySet().forEach(clazz -> ModLogger.debug("- {}", clazz.getSimpleName()));
    }
}
