package fr.ntgitg.mineglot.core.model.base;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AIRequestBuilderTest {

    @Test
    public void openAIResponsesBuilderDoesNotMutatePreparedInput() {
        String preparedText = "\u00A7aPrepared input";
        String body = BaseAIRequestBuilder.buildOpenAIResponsesRequestBody(
                preparedText, "en", "ar", "gpt-4o", 100, 0.1);

        JsonObject json = new JsonParser().parse(body).getAsJsonObject();
        String inputText = json.getAsJsonArray("input")
                .get(0).getAsJsonObject()
                .getAsJsonArray("content")
                .get(0).getAsJsonObject()
                .get("text").getAsString();

        assertEquals(preparedText, inputText);
    }

    @Test
    public void claudeBuilderUsesAnthropicMessagesShapeWithoutMutatingInput() {
        String preparedText = "\u00A7bPrepared chat input";
        String body = BaseAIRequestBuilder.buildClaudeRequestBody(
                preparedText, "en", "ar", "claude-sonnet-4-6", 100);

        JsonObject json = new JsonParser().parse(body).getAsJsonObject();
        assertEquals("claude-sonnet-4-6", json.get("model").getAsString());
        assertEquals(100, json.get("max_tokens").getAsInt());
        assertEquals("Source is English. Translate into Arabic. If already Arabic, "
                + "return unchanged. No questions. Reply with translation only.",
                json.get("system").getAsString());
        assertFalse(json.has("temperature"));

        assertEquals(1, json.getAsJsonArray("messages").size());
        assertEquals("user", json.getAsJsonArray("messages")
                .get(0).getAsJsonObject()
                .get("role").getAsString());
        String inputText = json.getAsJsonArray("messages")
                .get(0).getAsJsonObject()
                .get("content").getAsString();

        assertEquals(preparedText, inputText);
    }
}
