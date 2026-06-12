package fr.ntgitg.mineglot.ui.gui.screens.api;

import fr.ntgitg.mineglot.core.apikey.ApiKeyCoordinator;
import fr.ntgitg.mineglot.ui.gui.base.AbstractGui;
import fr.ntgitg.mineglot.ui.gui.components.fields.SecureTextField;
import fr.ntgitg.mineglot.ui.gui.components.progressbar.HoldToConfirmBar;
import fr.ntgitg.mineglot.ui.gui.screens.config.ConfigGui;
import fr.ntgitg.mineglot.ui.gui.utils.button.ButtonType;
import fr.ntgitg.mineglot.ui.gui.utils.button.CustomButtonFactory;
import fr.ntgitg.mineglot.ui.gui.utils.sound.SoundManager;
import fr.ntgitg.mineglot.ui.gui.utils.title.TitleManager;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class ApiKeyGui extends AbstractGui {

    private static final int FIELD_WIDTH = 200;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MAX_API_KEY_LENGTH = 256;
    private static final int PROGRESS_BAR_WIDTH = 100;
    private static final int PROGRESS_BAR_HEIGHT = 4;
    private static final int HOLD_DURATION_MS = 5000; // 5 secondes

    private static final int WARNING_COLOR = 0xFFFF0000; // Rouge clair (alpha=255, rouge=255, vert=0,

    private SecureTextField apiKeyField;
    private HoldToConfirmBar holdBar;

    private boolean showApiKey = false;
    private boolean isTesting = false;
    private float deltaTime = 0.0f;

    private static final int CLEAR_BUTTON_ID = ButtonType.API_CLEAR.getId();
    private static final int SHOW_HIDE_BUTTON_ID = ButtonType.API_SHOW_HIDE.getId();
    private static final int SAVE_BUTTON_ID = ButtonType.API_SAVE.getId();

    private final String currentEngine;

    public ApiKeyGui(GuiScreen parentScreen) {
        super(parentScreen);
        this.currentEngine = getConfigurationManager().getCurrentEngine();
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();

        addBackButton();
        setupApiKeyField();
        setupButtons();
        setupHoldBar();
        updateButtonTexts();
    }

    private void setupApiKeyField() {
        int y = getCenterY() - 30;
        apiKeyField = new SecureTextField(0, getCenterX() - FIELD_WIDTH / 2, y,
                FIELD_WIDTH - BUTTON_WIDTH - 8, FIELD_HEIGHT, getUIManager().getFontRenderer());
        apiKeyField.setMaxStringLength(MAX_API_KEY_LENGTH);
        reloadApiKeyFromConfig();
        apiKeyField.setMasked(!showApiKey);
        apiKeyField.setFocused(true);
    }

    private void reloadApiKeyFromConfig() {
        if (apiKeyField != null) {
            String existingApiKey = getConfigurationManager().getApiKey(currentEngine);
            apiKeyField.setText(existingApiKey != null ? existingApiKey : "");
        } else {
            ModLogger.warn("reloadApiKeyFromConfig: apiKeyField est null");
        }
    }

    private void setupButtons() {
        int y = getCenterY() - 30;
        buttonList.add(CustomButtonFactory.createApiShowHide(getCenterX() + FIELD_WIDTH / 2 - 60, y,
                getShowHideButtonText(), btn -> {
                }));
        int yButtons = y + FIELD_HEIGHT + 16;
        buttonList.add(CustomButtonFactory.createApiSave(getCenterX() - 68, yButtons,
                I18nManager.getMessage("button.save"), btn -> {
                }));
        buttonList.add(CustomButtonFactory.createApiClear(getCenterX() + 8, yButtons,
                I18nManager.getMessage("button.clear"), btn -> {
                }));
    }

    private void setupHoldBar() {
        int yChamp = getCenterY() - 30;
        int yButtons = yChamp + FIELD_HEIGHT + 16;
        int x = getCenterX() - PROGRESS_BAR_WIDTH / 2;
        int y = yButtons + BUTTON_HEIGHT + 10;
        holdBar = new HoldToConfirmBar(x, y, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, HOLD_DURATION_MS,
                this::handleClearButton);
    }

    private void updateButtonTexts() {
        for (GuiButton button : buttonList) {
            if (button.id == SHOW_HIDE_BUTTON_ID) {
                button.displayString = getShowHideButtonText();
            } else if (button.id == CLEAR_BUTTON_ID) {
                button.displayString = I18nManager.getMessage("button.clear");
            } else if (button.id == SAVE_BUTTON_ID) {
                button.displayString = I18nManager.getMessage("button.save");
            }
        }
    }

    private String getShowHideButtonText() {
        return showApiKey ? I18nManager.getMessage("button.hide")
                : I18nManager.getMessage("button.show");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        playButtonSound();

        if (button.id == CLEAR_BUTTON_ID) {
            return;
        }

        if (button.id == BACK_BUTTON_ID) {
            handleBackButton();
        } else if (button.id == SHOW_HIDE_BUTTON_ID) {
            toggleApiKeyVisibility(button);
        } else if (button.id == SAVE_BUTTON_ID) {
            handleSaveButton(button);
        }
    }

    private void toggleApiKeyVisibility(GuiButton button) {
        showApiKey = !showApiKey;
        button.displayString = getShowHideButtonText();
        apiKeyField.setMasked(!showApiKey);
    }

    private void handleSaveButton(GuiButton button) {
        if (isTesting) {
            MessageService.sendError(mc.thePlayer, "api_key.please_wait");
            SoundManager.playClick();
            return;
        }

        apiKeyField.updateCursorCounter();

        String apiKey = apiKeyField.getText().trim();
        ApiKeyCoordinator.validateAndSaveApiKey(currentEngine, apiKey, () -> {
                    reloadApiKeyFromConfig();
                    startApiKeyTest(button);
                }, // onSuccess
                errorMsg -> SoundManager.playClick() // onError
        );
    }

    private void startApiKeyTest(GuiButton button) {
        String apiKey = apiKeyField.getText().trim();
        isTesting = true;
        button.enabled = false;
        MessageService.sendInfo(mc.thePlayer, "api_key.testing");

        ApiKeyCoordinator.testApiKeyAsync(currentEngine, apiKey, () -> handleTestSuccess(button), // onSuccess
                errorKey -> handleTestError(errorKey, button) // onError
        );
    }

    private void handleTestSuccess(GuiButton button) {
        mc.addScheduledTask(() -> {
            ModLogger.info("Clé API validée avec succès pour le moteur: {}", currentEngine);
            SoundManager.playClick();
            isTesting = false;
            button.enabled = true;
        });
    }

    private void handleTestError(String errorKey, GuiButton button) {
        mc.addScheduledTask(() -> {
            ModLogger.debug("Test de cle API échoué ({}): {}", currentEngine, errorKey);
            SoundManager.playClick();
            isTesting = false;
            button.enabled = true;
        });
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        apiKeyField.updateCursorCounter();
        if (holdBar != null)
            holdBar.update(deltaTime);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiButton button : buttonList) {
            if (button.id == CLEAR_BUTTON_ID && button.mousePressed(mc, mouseX, mouseY)) {
                if (holdBar != null)
                    holdBar.startHold();
                return;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (holdBar != null)
            holdBar.stopHold();
    }

    private void handleClearButton() {
        apiKeyField.setText("");
        ApiKeyCoordinator.clearApiKey(currentEngine, SoundManager::playClick, // onSuccess
                errorMsg -> SoundManager.playClick() // onError
        );
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (apiKeyField.isFocused()) {
            if (apiKeyField.handleKeyTyped(typedChar, keyCode)) {
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        deltaTime = partialTicks * 0.05f;
        drawApiKeyField();
        if (holdBar != null) {
            holdBar.render();
            getUIManager().drawCenteredText("§c" + I18nManager.getMessage("api_key.hold_to_clear"),
                    getCenterX(), holdBar.getY() + holdBar.getHeight() + 10, WARNING_COLOR);
        }
    }

    private void drawApiKeyField() {
        apiKeyField.drawTextBox(getUIManager().getFontRenderer());
    }

    @Override
    protected String getTitle() {
        return TitleManager.getApiKeyTitle(currentEngine);
    }

    @Override
    protected void onBackButtonClicked() {
        if (getParentScreen() instanceof ConfigGui) {
            ModLogger.debug("Reinitialisation de ConfigGui apres retour depuis ApiKeyGui");
            ((ConfigGui) getParentScreen()).initGui(); // Force la mise à jour de l'interface parente
        }
    }
}
