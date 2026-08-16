package fr.ntgitg.mineglot.core.model.base;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProviderApiEndpointsTest {

    @Test
    public void openAIEndpointsAreCentralized() {
        assertEquals("https://api.openai.com/v1/responses",
                ProviderApiEndpoints.OPENAI_RESPONSES_URL);
        assertEquals("https://api.openai.com/v1/models",
                ProviderApiEndpoints.OPENAI_MODELS_URL);
    }

    @Test
    public void claudeEndpointsAreCentralized() {
        assertEquals("https://api.anthropic.com/v1/messages",
                ProviderApiEndpoints.CLAUDE_MESSAGES_URL);
        assertEquals("https://api.anthropic.com/v1/models",
                ProviderApiEndpoints.CLAUDE_MODELS_URL);
    }
}
