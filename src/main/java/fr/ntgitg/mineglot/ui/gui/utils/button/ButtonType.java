package fr.ntgitg.mineglot.ui.gui.utils.button;

import net.minecraft.util.ResourceLocation;

public enum ButtonType {
    GENERIC(10, "textures/gui/button_background.png", 200, 20), BUY_ME_COFFEE(999,
            "textures/gui/buy_me_coffee_button.png", 200, 20), BACK(0, "textures/gui/back_button.png",
            200, 20), CLEAR_CACHE(1, "textures/gui/clear_cache_button.png", 200, 20), API_SAVE(2,
            "textures/gui/save_button.png", 60, 20), API_SHOW_HIDE(3,
            "textures/gui/show_hide_button.png", 60,
            20), API_CLEAR(4, "textures/gui/clear_button.png", 60, 20), API_BACK(5,
            "textures/gui/back_button.png", 60,
            20), MINEGLOT_GPT(31415, "textures/gui/mineglot_button.png", 200, 20),
    GITHUB(888, "textures/gui/github_button.png", 200, 20);

    private final int id;
    private final String texturePath;
    private final int defaultWidth;
    private final int defaultHeight;

    ButtonType(int id, String texturePath, int defaultWidth, int defaultHeight) {
        this.id = id;
        this.texturePath = texturePath;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
    }

    public int getId() {
        return id;
    }

    public ResourceLocation getTexture() {
        return new ResourceLocation("mineglot", texturePath);
    }

    public int getDefaultWidth() {
        return defaultWidth;
    }

    public int getDefaultHeight() {
        return defaultHeight;
    }
}
