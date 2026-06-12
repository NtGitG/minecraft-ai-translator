package fr.ntgitg.mineglot.core.model.openai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.ntgitg.mineglot.core.model.base.AbstractResponseParser;

public final class OpenAIResponseParser extends AbstractResponseParser {

    public OpenAIResponseParser() {
        super();
    }

    public static OpenAIResponseParser getInstance() {
        return AbstractResponseParser.getInstance(OpenAIResponseParser.class);
    }

    @Override
    protected String extractTranslation(JsonObject json) {
        // 1) Responses API format: output -> content -> text.value OR output as string
        if (json.has("output")) {
            try {
                if (json.get("output").isJsonPrimitive()) {
                    String outStr = json.get("output").getAsString();
                    if (outStr != null && !outStr.isEmpty()) {
                        return outStr.trim();
                    }
                }

                JsonArray output = json.getAsJsonArray("output");
                if (output != null && output.size() > 0) {
                    for (int o = 0; o < output.size(); o++) {
                        JsonObject obj = output.get(o).getAsJsonObject();

                        // If output_text is a primitive string
                        if (obj.has("output_text") && obj.get("output_text").isJsonPrimitive()) {
                            String s = obj.get("output_text").getAsString();
                            if (s != null && !s.isEmpty()) {
                                return s.trim();
                            }
                        }

                        // Format: { "type": "output_text", "output_text": { "content": ["..."] } }
                        if (obj.has("output_text")) {
                            JsonObject ot = obj.getAsJsonObject("output_text");
                            if (ot.has("content") && ot.get("content").isJsonArray()) {
                                JsonArray arr = ot.getAsJsonArray("content");
                                for (int i = 0; i < arr.size(); i++) {
                                    String s = arr.get(i).getAsString();
                                    if (s != null && !s.isEmpty()) {
                                        return s.trim();
                                    }
                                }
                            }
                        }

                        if (obj.has("content")) {
                            JsonArray content = obj.getAsJsonArray("content");
                            if (content != null && content.size() > 0) {
                                for (int i = 0; i < content.size(); i++) {
                                    JsonObject item = content.get(i).getAsJsonObject();
                                    if (item.has("text")) {
                                        // text can be an object { value: "..." } or a primitive string
                                        if (item.get("text").isJsonObject()) {
                                            JsonObject text = item.getAsJsonObject("text");
                                            if (text.has("value")) {
                                                String val = text.get("value").getAsString();
                                                if (val != null && !val.isEmpty()) {
                                                    return val.trim();
                                                }
                                            }
                                        } else if (item.get("text").isJsonPrimitive()) {
                                            String val = item.get("text").getAsString();
                                            if (val != null && !val.isEmpty()) {
                                                return val.trim();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // fallback to legacy parsing
            }
        }

        // 2) Convenience field some clients expose
        if (json.has("output_text")) {
            try {
                return json.get("output_text").getAsString().trim();
            } catch (Exception ignored) {
            }
        }

        // 3) Legacy Chat Completions format
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            return null;
        }

        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        if (message == null || !message.has("content")) {
            return null;
        }

        return message.get("content").getAsString().trim();
    }
}
