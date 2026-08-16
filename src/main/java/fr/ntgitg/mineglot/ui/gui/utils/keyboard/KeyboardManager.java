package fr.ntgitg.mineglot.ui.gui.utils.keyboard;

import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

public final class KeyboardManager {

    private KeyboardManager() {
    }

    public static boolean isCtrlKeyPressed() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    public static boolean handleSpecialKeys(GuiTextField textField, int keyCode) {
        if (!isCtrlKeyPressed()) {
            return false;
        }

        switch (keyCode) {
            case Keyboard.KEY_A:
                selectAllText(textField);
                return true;

            case Keyboard.KEY_C:
                copySelectedText(textField);
                return true;

            case Keyboard.KEY_V:
                pasteToTextField(textField);
                return true;

            default:
                return false;
        }
    }

    public static boolean handleNavigationKeys(GuiTextField textField, int keyCode) {
        switch (keyCode) {
            case Keyboard.KEY_LEFT:
                moveCursorLeft(textField);
                return true;

            case Keyboard.KEY_RIGHT:
                moveCursorRight(textField);
                return true;

            case Keyboard.KEY_HOME:
                textField.setCursorPosition(0);
                return true;

            case Keyboard.KEY_END:
                textField.setCursorPosition(textField.getText().length());
                return true;

            default:
                return false;
        }
    }

    private static void selectAllText(GuiTextField textField) {
        int textLength = textField.getText().length();
        textField.setCursorPosition(textLength);
        textField.setSelectionPos(0);
    }

    private static void copySelectedText(GuiTextField textField) {
        String selected = textField.getSelectedText();
        if (selected != null && !selected.isEmpty()) {
            ClipboardManager.copyToClipboard(selected);
        }
    }

    private static void pasteToTextField(GuiTextField textField) {
        String clipboard = ClipboardManager.getFromClipboard();
        if (clipboard != null) {
            textField.writeText(clipboard);
        }
    }

    private static void moveCursorLeft(GuiTextField textField) {
        int cursorPos = textField.getCursorPosition();
        if (cursorPos > 0) {
            textField.setCursorPosition(cursorPos - 1);
        }
    }

    private static void moveCursorRight(GuiTextField textField) {
        int cursorPos = textField.getCursorPosition();
        int textLength = textField.getText().length();
        if (cursorPos < textLength) {
            textField.setCursorPosition(cursorPos + 1);
        }
    }
}
