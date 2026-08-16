package fr.ntgitg.mineglot.core.model;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class AIModelTest {

    @Test
    public void claudeModelsUseActiveAnthropicModelIds() {
        AIModel[] models = AIModel.getModelsForEngine("claude");

        assertArrayEquals(new AIModel[]{
                AIModel.CLAUDE_SONNET_4_6,
                AIModel.CLAUDE_OPUS_4_8
        }, models);
        assertEquals("claude-sonnet-4-6", models[0].getModelId());
        assertEquals("claude-opus-4-8", models[1].getModelId());
    }
}
