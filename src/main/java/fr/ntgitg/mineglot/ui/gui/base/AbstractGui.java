
package fr.ntgitg.mineglot.ui.gui.base;

import fr.ntgitg.mineglot.core.cache.CacheServiceFacade;
import fr.ntgitg.mineglot.core.config.ConfigService;
import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.player.PlayerNameManager;
import fr.ntgitg.mineglot.core.storage.DatabaseService;
import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.monitoring.metrics.MetricsManager;
import fr.ntgitg.mineglot.ui.core.LayoutCalculator;
import fr.ntgitg.mineglot.ui.core.UIManager;
import fr.ntgitg.mineglot.ui.gui.core.GuiConstants;
import fr.ntgitg.mineglot.ui.gui.rendering.GuiBackground;
import fr.ntgitg.mineglot.ui.gui.utils.button.ButtonType;
import fr.ntgitg.mineglot.ui.gui.utils.button.CustomButtonFactory;
import fr.ntgitg.mineglot.ui.gui.utils.sound.SoundManager;
import fr.ntgitg.mineglot.core.service.error.ErrorManager;
import fr.ntgitg.mineglot.core.service.error.ErrorType;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public abstract class AbstractGui extends GuiScreen {
    protected static final int GUI_WIDTH = GuiConstants.DEFAULT_GUI_WIDTH;
    protected static final int GUI_HEIGHT = GuiConstants.DEFAULT_GUI_HEIGHT;
    protected static final int TITLE_Y_OFFSET = GuiConstants.TITLE_Y_OFFSET;

    protected static final int BUTTON_WIDTH = GuiConstants.BUTTON_WIDTH;
    protected static final int BUTTON_HEIGHT = GuiConstants.BUTTON_HEIGHT;
    protected static final int BACK_BUTTON_ID = GuiConstants.BACK_BUTTON_ID;

    protected static final int WHITE_COLOR = GuiConstants.WHITE_COLOR;

    protected static final int KEY_ESCAPE = GuiConstants.KEY_ESCAPE;

    private int centerX;
    private int centerY;
    private GuiScreen parentScreen;

    private final UIManager uiManager = UIManager.getInstance();
    private final ConfigService configService = ConfigService.getInstance();
    private final CacheServiceFacade cacheServices = CacheServiceFacade.getInstance();
    private final PlayerNameManager playerNameManager = PlayerNameManager.getInstance();
    private final ConfigurationManager configurationManager = ConfigurationManager.getInstance();
    private final DatabaseService databaseService = DatabaseService.getInstance();
    private final MetricsManager metricsManager = MetricsManager.getInstance();

    public AbstractGui(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    protected int getCenterX() {
        return centerX;
    }

    protected int getCenterY() {
        return centerY;
    }

    protected void setCenterX(int centerX) {
        this.centerX = centerX;
    }

    protected void setCenterY(int centerY) {
        this.centerY = centerY;
    }

    protected GuiScreen getParentScreen() {
        return parentScreen;
    }

    protected UIManager getUIManager() {
        return uiManager;
    }

    protected ConfigService getConfigService() {
        return configService;
    }

    protected CacheServiceFacade getCacheServices() {
        return cacheServices;
    }

    protected PlayerNameManager getPlayerNameManager() {
        return playerNameManager;
    }

    protected ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    protected DatabaseService getDatabaseService() {
        return databaseService;
    }

    protected MetricsManager getMetricsManager() {
        return metricsManager;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        getUIManager().initialize();

        int[] center = LayoutCalculator.calculateScreenCenter();
        centerX = center[0];
        centerY = center[1];

        addBackButton();
    }

    protected void addBackButton() {
        int y = LayoutCalculator.getBackButtonY(getCenterY(), GUI_HEIGHT, BUTTON_HEIGHT);
        GuiButton backButton = CustomButtonFactory.create(ButtonType.BACK,
                getCenterX() - BUTTON_WIDTH / 2, y, "§f" + I18nManager.getMessage("gui.back"), btn -> {
                });
        buttonList.add(backButton);
    }

    protected void handleBackButton() {
        onBackButtonClicked();
        mc.displayGuiScreen(getParentScreen());
    }

    protected void onBackButtonClicked() {
    }

    protected void playButtonSound() {
        SoundManager.playClick();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GuiBackground.drawModernBackground(getCenterX(), getCenterY(), GUI_WIDTH, GUI_HEIGHT);

        String title = getTitle();
        getUIManager().drawTextWithShadow(title, getCenterX() - getUIManager().getTextWidth(title) / 2,
                getCenterY() - GUI_HEIGHT / 2 + TITLE_Y_OFFSET, WHITE_COLOR);

        drawContent(mouseX, mouseY, partialTicks);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    protected abstract void drawContent(int mouseX, int mouseY, float partialTicks);

    protected abstract String getTitle();

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BACK_BUTTON_ID) {
            handleBackButton();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == KEY_ESCAPE) { // Échap
            handleBackButton();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    protected boolean validateConfigService(ConfigService configService, String context) {
        if (configService == null || !configService.isOperational()) {
            MessageService.sendError(mc.thePlayer, "service.config_unavailable");
            ModLogger.warn("ConfigService non opérationnel dans " + context);
            return false;
        }
        return true;
    }

    protected void handleError(Exception e, String operation, ErrorType errorType) {
        ErrorManager.handleError(e, errorType, mc.thePlayer);

    }

    protected void handleApiError(Exception e, String operation) {
        handleError(e, operation, ErrorType.API);
    }

    protected void handleConfigError(Exception e, String operation) {
        handleError(e, operation, ErrorType.CONFIG);
    }

    protected void handleTranslationError(Exception e, String operation) {
        handleError(e, operation, ErrorType.TRANSLATION);
    }

    protected void handlePlayerError(Exception e, String operation) {
        handleError(e, operation, ErrorType.PLAYER);
    }

    protected boolean validateTranslationText(String text, String context) {
        ValidationService.ValidationResult result = ValidationService.validateTranslationText(text);
        if (!result.isValid()) {
            String errorKey = result.getErrorKey() != null
                    ? result.getErrorKey()
                    : "translation.error.general";
            MessageService.sendError(mc.thePlayer, errorKey);
            ModLogger.warn("Texte invalide dans " + context + ": " + result.getErrorMessage());
            return false;
        }
        return true;
    }

}
