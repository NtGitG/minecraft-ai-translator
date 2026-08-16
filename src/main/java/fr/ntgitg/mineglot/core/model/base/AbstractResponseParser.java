package fr.ntgitg.mineglot.core.model.base;

import fr.ntgitg.mineglot.core.service.SingletonManager;

public abstract class AbstractResponseParser extends BaseAIResponseParser {

    protected AbstractResponseParser() {
        super();
    }

    protected static <T extends AbstractResponseParser> T getInstance(Class<T> parserClass) {
        return SingletonManager.getInstance(parserClass, () -> {
            try {
                return parserClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Erreur lors de la création de l'instance " + parserClass.getSimpleName(), e);
            }
        });
    }
}
