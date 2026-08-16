package fr.ntgitg.mineglot.core.model.base;

import java.io.IOException;

public interface ApiKeyValidator {

    void validateKey(String key);

    void testApiKey(String key) throws IOException;
}
