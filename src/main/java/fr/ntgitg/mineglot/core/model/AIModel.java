package fr.ntgitg.mineglot.core.model;

import com.knuddels.jtokkit.api.EncodingType;

import java.util.Arrays;

public enum AIModel {
    GPT_4O("gpt-4o", "GPT-4o", "openai", EncodingType.CL100K_BASE),
    GPT_4O_MINI("gpt-4o-mini", "GPT-4o-mini", "openai", EncodingType.CL100K_BASE),
    GPT_4_TURBO("gpt-4-turbo", "GPT-4-Turbo", "openai", EncodingType.CL100K_BASE),
    GPT_4("gpt-4", "GPT-4", "openai", EncodingType.CL100K_BASE),
    GPT_3_5_TURBO("gpt-3.5-turbo", "GPT-3.5t", "openai", EncodingType.CL100K_BASE),

    CLAUDE_SONNET_4_6("claude-sonnet-4-6", "Claude Sonnet 4.6", "claude", EncodingType.CL100K_BASE),
    CLAUDE_OPUS_4_8("claude-opus-4-8", "Claude Opus 4.8", "claude", EncodingType.CL100K_BASE);

    private final String modelId;
    private final String displayName;
    private final String engine;
    private final EncodingType encodingType;

    AIModel(String modelId, String displayName, String engine, EncodingType encodingType) {
        this.modelId = modelId;
        this.displayName = displayName;
        this.engine = engine;
        this.encodingType = encodingType;
    }

    public String getModelId() {
        return modelId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEngine() {
        return engine;
    }

    public EncodingType getEncodingType() {
        return encodingType;
    }

    public static AIModel[] getModelsForEngine(String engineName) {
        return Arrays.stream(values()).filter(model -> model.getEngine().equals(engineName))
                .toArray(AIModel[]::new);
    }

    public static AIModel fromModelId(String modelId) {
        return Arrays.stream(values()).filter(model -> model.getModelId().equals(modelId)).findFirst()
                .orElse(null);
    }
}
