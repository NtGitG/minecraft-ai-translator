package fr.ntgitg.mineglot.core.model.token;

import java.util.Map;

public interface StatsStorage {

    Map<String, ModelStats> loadStats();

    void saveStats(Map<String, ModelStats> stats);

    class ModelStats {
        private final long inputTokens;
        private final long outputTokens;

        public ModelStats(long inputTokens, long outputTokens) {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
        }

        public long getInputTokens() {
            return inputTokens;
        }

        public long getOutputTokens() {
            return outputTokens;
        }
    }
}
