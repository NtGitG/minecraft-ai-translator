package fr.ntgitg.mineglot.core.model.claude;

import com.google.gson.JsonObject;
import fr.ntgitg.mineglot.core.model.base.AbstractResponseParser;

public final class ClaudeResponseParser extends AbstractResponseParser {

    public ClaudeResponseParser() {
        super();
    }

    public static ClaudeResponseParser getInstance() {
        return AbstractResponseParser.getInstance(ClaudeResponseParser.class);
    }

    @Override
    protected String extractTranslation(JsonObject json) {
        JsonObject content = json.getAsJsonArray("content").get(0).getAsJsonObject();
        if (content == null || !content.has("text")) {
            return null;
        }
        return content.get("text").getAsString().trim();
    }
}
