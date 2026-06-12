package fr.ntgitg.mineglot.ui.gui.utils.button;

import fr.ntgitg.mineglot.ui.gui.utils.sound.SoundManagerAdapter;

public final class CustomButtonFactory {

    private CustomButtonFactory() {
    }
    public static CustomButton create(ButtonType type, int x, int y, String text,
                                      ButtonListener handler) {
        return create(type, x, y, type.getDefaultWidth(), type.getDefaultHeight(), text, handler);
    }

    public static CustomButton create(ButtonType type, int x, int y, int width, int height,
                                      String text, ButtonListener handler) {
        CustomButton button = new CustomButton(type.getId(), x, y, width, height, type.getTexture(), 0,
                0, width * 2, height * 2, new SoundManagerAdapter());
        button.displayString = text;
        button.withEventHandler(handler);
        return button;
    }

    public static CustomButton createGeneric(int id, int x, int y, int width, int height, String text,
                                             ButtonListener handler) {
        CustomButton button = new CustomButton(id, x, y, width, height, ButtonType.GENERIC.getTexture(),
                0, 0, width * 2, height * 2, new SoundManagerAdapter());
        button.displayString = text;
        button.withEventHandler(handler);
        return button;
    }

    public static CustomButton createClearCache(int x, int y, String text, ButtonListener handler) {
        return create(ButtonType.CLEAR_CACHE, x, y, text, handler);
    }

    public static CustomButton createApiSave(int x, int y, String text, ButtonListener handler) {
        return create(ButtonType.API_SAVE, x, y, text, handler);
    }

    public static CustomButton createApiShowHide(int x, int y, String text, ButtonListener handler) {
        return create(ButtonType.API_SHOW_HIDE, x, y, text, handler);
    }

    public static CustomButton createApiClear(int x, int y, String text, ButtonListener handler) {
        return create(ButtonType.API_CLEAR, x, y, text, handler);
    }

    public static CustomButton createApiBack(int x, int y, String text, ButtonListener handler) {
        return create(ButtonType.API_BACK, x, y, text, handler);
    }
}
