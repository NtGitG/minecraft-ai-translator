package fr.ntgitg.mineglot.core.model.base;

import org.junit.Test;

public class NetworkApiKeyTesterTest {

    private static final String FAKE_KEY_BODY = "abcdefghijklmnopqrstuvwxyz";

    @Test
    public void openAIValidatorAcceptsSkKeys() {
        NetworkApiKeyTester.createOpenAI().validateKey("sk-" + FAKE_KEY_BODY);
    }

    @Test
    public void openAIValidatorAcceptsProjectKeys() {
        NetworkApiKeyTester.createOpenAI().validateKey("sk-proj-" + FAKE_KEY_BODY);
    }

    @Test
    public void claudeValidatorAcceptsAnthropicKeys() {
        NetworkApiKeyTester.createClaude().validateKey("sk-ant-" + FAKE_KEY_BODY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void openAIValidatorRejectsUnknownPrefixes() {
        NetworkApiKeyTester.createOpenAI().validateKey("bad-12345678901234567890");
    }
}
