package fr.ntgitg.mineglot.ui.hud.core;

public final class HUDConfig {

    public static final class Widgets {
        public static final boolean DEFAULT_SHOW_SOURCE = true; // Afficher la langue source par défaut
        public static final boolean DEFAULT_SHOW_TARGET = true; // Afficher la langue cible par défaut
        public static final boolean DEFAULT_SHOW_MODEL = true; // Afficher le modèle IA par défaut

        public static final boolean SHOW_HUD_BACKGROUND = true; // true = fond PNG, false = HUD
    }

    private HUDConfig() {
        throw new UnsupportedOperationException("Cette classe ne doit pas être instanciée");
    }
}
