package fr.ntgitg.mineglot.core.service.error;

public enum ErrorType {

    PLAYER("Erreur joueur"),

    CONFIG("Erreur configuration"),

    TRANSLATION("Erreur traduction"),

    API("Erreur API"),

    UI("Erreur interface utilisateur"),

    RENDERING("Erreur de rendu"),

    SYSTEM("Erreur système"),

    DATABASE("Erreur base de données");

    private final String displayName;

    ErrorType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
