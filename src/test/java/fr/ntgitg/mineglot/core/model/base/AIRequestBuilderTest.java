package fr.ntgitg.mineglot.core.model.base;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
    public void chatBuilderDoesNotMutatePreparedInput() {
        String preparedText = "\u00A7bPrepared chat input";
        String body = BaseAIRequestBuilder.buildRequestBody(
                preparedText, "en", "ar", "claude-3-sonnet-20240229", 100, 0.1);

        JsonObject json = new JsonParser().parse(body).getAsJsonObject();
        String inputText = json.getAsJsonArray("messages")
                .get(1).getAsJsonObject()
                .get("content").getAsString();

        assertEquals(preparedText, inputText);
    }
}
