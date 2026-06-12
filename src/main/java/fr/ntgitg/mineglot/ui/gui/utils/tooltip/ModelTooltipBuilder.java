package fr.ntgitg.mineglot.ui.gui.utils.tooltip;

import fr.ntgitg.mineglot.monitoring.metrics.MetricsManager;

import java.util.ArrayList;
import java.util.List;

public final class ModelTooltipBuilder {

    private ModelTooltipBuilder() {
    }

    public static List<String> buildTooltip(String modelId) {
        List<String> lines = new ArrayList<>();

        if (modelId == null || modelId.trim().isEmpty()) {
            lines.add(TooltipManager.COLOR_WARNING + "Model unavailable");
            return lines;
        }

        MetricsManager.ModelStats stats = MetricsManager.getInstance().getModelStats(modelId);
        long totalTokens = stats.inputTokens + stats.outputTokens;

        lines.add(TooltipManager.COLOR_TITLE + "\u00A7l" + modelId);
        lines.add("");

        lines.add(TooltipManager.COLOR_HIGHLIGHT + "\u00A7lUsage:");
        lines.add(TooltipManager.COLOR_CONTENT + "- Prompt: " + stats.inputTokens + " tokens");
        lines.add(TooltipManager.COLOR_CONTENT + "- Completion: " + stats.outputTokens + " tokens");
        lines.add(TooltipManager.COLOR_CONTENT + "- Total: " + totalTokens + " tokens");
        lines.add("");

        lines.add(TooltipManager.COLOR_HIGHLIGHT + "\u00A7lBilling:");
        lines.add(TooltipManager.COLOR_INFO + "Check your API provider dashboard for real costs");

        return lines;
    }
}
