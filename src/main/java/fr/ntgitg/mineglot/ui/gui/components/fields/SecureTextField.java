package fr.ntgitg.mineglot.ui.gui.components.fields;

import fr.ntgitg.mineglot.ui.gui.utils.keyboard.ClipboardManager;
import fr.ntgitg.mineglot.ui.gui.utils.keyboard.KeyboardManager;
import fr.ntgitg.mineglot.ui.gui.utils.keyboard.PlaceholderManager;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

public class SecureTextField {
    private final GuiTextField textField;
    private boolean masked = true;
    private String realText = "";
    private int maxStringLength = 256;
    private final PlaceholderManager placeholderManager = new PlaceholderManager();

    public SecureTextField(int id, int x, int y, int width, int height, FontRenderer fontRenderer) {
        this.textField = new GuiTextField(id, fontRenderer, x, y, width, height);
        this.textField.setMaxStringLength(maxStringLength);
        this.textField.setFocused(true);
    }

    public void setMasked(boolean masked) {
        this.masked = masked;
        updateDisplayedText();
    }

    public boolean isMasked() {
        return masked;
    }

    public void setText(String text) {
        realText = clampToMaxLength(text != null ? text : "");
        updateDisplayedText();
    }

    public String getText() {
        return realText;
    }

    private String clampToMaxLength(String value) {
        if (value.length() <= maxStringLength) {
            return value;
        }
        return value.substring(0, maxStringLength);
    }

    private void updateDisplayedText() {
        int cursor = clampIndex(textField.getCursorPosition());
        int selection = clampIndex(textField.getSelectionEnd());

        if (masked) {
            StringBuilder maskedText = new StringBuilder(realText.length());
            for (int i = 0; i < realText.length(); i++) {
                maskedText.append('*');
            }
            textField.setText(maskedText.toString());
        } else {
            textField.setText(realText);
        }

        textField.setCursorPosition(clampIndex(cursor));
        textField.setSelectionPos(clampIndex(selection));
    }

    public boolean handleKeyTyped(char typedChar, int keyCode) {
        if (KeyboardManager.isCtrlKeyPressed() && keyCode == Keyboard.KEY_V) {
            pasteFromClipboard();
            return true;
        }

        if (KeyboardManager.handleSpecialKeys(textField, keyCode)) {
            return true;
        }

        if (KeyboardManager.handleNavigationKeys(textField, keyCode)) {
            return true;
        }

        if (keyCode == Keyboard.KEY_BACK) {
            deletePreviousChar();
            return true;
        }

        if (keyCode == Keyboard.KEY_DELETE) {
            deleteNextChar();
            return true;
        }

        if (typedChar >= 32 && typedChar < 127) {
            insertAtSelection(String.valueOf(typedChar));
            return true;
        }

        return false;
    }

    private void pasteFromClipboard() {
        String clipboard = ClipboardManager.getFromClipboard();
        if (clipboard == null || clipboard.isEmpty()) {
            return;
        }

        insertAtSelection(clipboard);
    }

    private void deletePreviousChar() {
        if (hasSelection()) {
            insertAtSelection("");
            return;
        }

        int cursor = clampIndex(textField.getCursorPosition());
        if (cursor <= 0) {
            return;
        }

        textField.setSelectionPos(cursor - 1);
        textField.setCursorPosition(cursor);
        insertAtSelection("");
    }

    private void deleteNextChar() {
        if (hasSelection()) {
            insertAtSelection("");
            return;
        }

        int cursor = clampIndex(textField.getCursorPosition());
        if (cursor >= realText.length()) {
            return;
        }

        textField.setSelectionPos(cursor + 1);
        textField.setCursorPosition(cursor);
        insertAtSelection("");
    }

    private boolean hasSelection() {
        return textField.getCursorPosition() != textField.getSelectionEnd();
    }

    private int clampIndex(int index) {
        return Math.max(0, Math.min(index, realText.length()));
    }

    private void insertAtSelection(String insert) {
        int cursor = clampIndex(textField.getCursorPosition());
        int selectionEnd = clampIndex(textField.getSelectionEnd());
        int start = Math.min(cursor, selectionEnd);
        int end = Math.max(cursor, selectionEnd);

        int availableLength = maxStringLength - (realText.length() - (end - start));
        if (availableLength < 0) {
            availableLength = 0;
        }

        String valueToInsert = insert;
        if (valueToInsert.length() > availableLength) {
            valueToInsert = valueToInsert.substring(0, availableLength);
        }

        realText = realText.substring(0, start) + valueToInsert + realText.substring(end);
        updateDisplayedText();

        int newCursor = start + valueToInsert.length();
        textField.setCursorPosition(newCursor);
        textField.setSelectionPos(newCursor);
    }

    public void updateCursorCounter() {
        textField.updateCursorCounter();
    }

    public void drawTextBox(FontRenderer fontRenderer) {
        textField.drawTextBox();
        placeholderManager.drawPlaceholder(textField, fontRenderer);
    }

    public void setFocused(boolean focused) {
        textField.setFocused(focused);
    }

    public boolean isFocused() {
        return textField.isFocused();
    }

    public void setMaxStringLength(int maxLength) {
        if (maxLength <= 0) {
            return;
        }

        maxStringLength = maxLength;
        textField.setMaxStringLength(maxLength);
        setText(realText);
    }

    public void clear() {
        setText("");
    }

    public GuiTextField getInnerTextField() {
        return textField;
    }
}
