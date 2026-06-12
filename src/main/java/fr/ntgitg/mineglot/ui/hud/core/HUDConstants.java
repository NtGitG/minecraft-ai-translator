package fr.ntgitg.mineglot.ui.hud.core;

public final class HUDConstants {

    public static final boolean HUD_ENABLED = true; // Mettre false pour désactiver complètement le

    public static final int HUD_POSITION_X = 10; // Position X fixe (pixels depuis la gauche)
    public static final int HUD_POSITION_Y = 10; // Position Y fixe (pixels depuis le haut)

    public static final int HUD_TEXT_COLOR = 0xFFFFFFFF; // Couleur du texte (blanc)

    public static final int HUD_WIDTH = 200; // Largeur réduite pour moins d'encombrement
    public static final int HUD_HEIGHT = 24; // Hauteur réduite pour moins d'encombrement
    public static final int WIDGET_WIDTH = 30; // Largeur réduite pour moins d'encombrement
    public static final int WIDGET_HEIGHT = 16; // Hauteur réduite pour moins d'encombrement
    public static final int WIDGET_SPACING = 2; // Espacement réduit pour moins d'encombrement

    public static final int HUD_INNER_PADDING_LEFT = 50;

    public static final float WIDGET_TEXT_SCALE = 1.0f; // Échelle normale du texte
    public static final int LABEL_WIDTH = 10; // Largeur fixe pour les labels "S:", "C:", "M:"
    public static final int LABEL_CONTENT_SPACING = 0; // Pas d'espacement - directement collé

    private HUDConstants() {
        throw new UnsupportedOperationException("Cette classe ne doit pas être instanciée");
    }
}
