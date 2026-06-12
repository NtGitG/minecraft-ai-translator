package fr.ntgitg.mineglot.core.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class ModelRegistry {
    private static final List<String> ENGINES = Arrays.stream(AIModel.values())
            .map(AIModel::getEngine).distinct().collect(Collectors.toList());

    private ModelRegistry() {
    }

    public static List<String> getAvailableEngines() {
        return Collections.unmodifiableList(ENGINES);
    }

    public static boolean isEngineSupported(String engine) {
        return ENGINES.contains(engine);
    }
}
