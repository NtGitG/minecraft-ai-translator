package fr.ntgitg.mineglot.core.model;

import fr.ntgitg.mineglot.core.model.claude.ClaudeResponseParser;
import fr.ntgitg.mineglot.core.model.openai.OpenAIResponseParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AIResponseParserTest {

    private static final String ORIGINAL_TEXT = "original text";
    private static final String TARGET_LANG = "en";

    @Test
    public void openAIParsesResponsesApiOutputText() {
        String json = "{"
                + "\"output\":[{"
                + "\"type\":\"message\","
                + "\"content\":[{\"type\":\"output_text\",\"text\":\"Hello Steve\"}]"
                + "}]"
                + "}";

        String result = new OpenAIResponseParser()
                .parseTranslation(json, ORIGINAL_TEXT, TARGET_LANG);

        assertEquals("Hello Steve", result);
    }

    @Test
    public void openAIParsesLegacyChatCompletion() {
        String json = "{"
                + "\"choices\":[{"
                + "\"message\":{\"content\":\"Hello from legacy chat\"}"
                + "}]"
                + "}";

        String result = new OpenAIResponseParser()
                .parseTranslation(json, ORIGINAL_TEXT, TARGET_LANG);

        assertEquals("Hello from legacy chat", result);
    }

    @Test
    public void openAIParsesDirectOutputText() {
        String json = "{\"output_text\":\"Hello direct field\"}";

        String result = new OpenAIResponseParser()
                .parseTranslation(json, ORIGINAL_TEXT, TARGET_LANG);

        assertEquals("Hello direct field", result);
    }

    @Test
    public void claudeParsesContentText() {
        String json = "{"
                + "\"content\":[{\"type\":\"text\",\"text\":\"Hello Claude\"}]"
                + "}";

        String result = new ClaudeResponseParser()
                .parseTranslation(json, ORIGINAL_TEXT, TARGET_LANG);

        assertEquals("Hello Claude", result);
    }

    @Test
    public void parsersReturnOriginalTextForEmptyResponses() {
        assertEquals(ORIGINAL_TEXT,
                new OpenAIResponseParser().parseTranslation("{}", ORIGINAL_TEXT, TARGET_LANG));
        assertEquals(ORIGINAL_TEXT,
                new ClaudeResponseParser().parseTranslation("{}", ORIGINAL_TEXT, TARGET_LANG));
    }
}
