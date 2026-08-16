package fr.ntgitg.mineglot.ui.gui.screens.update;

import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.core.update.ReleaseInfo;
import fr.ntgitg.mineglot.core.update.ReleasePageOpener;
import fr.ntgitg.mineglot.ui.gui.base.AbstractGui;
import fr.ntgitg.mineglot.ui.gui.utils.button.CustomButtonFactory;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public final class UpdateAvailableGui extends AbstractGui {
    static final int DOWNLOAD_BUTTON_ID = 7201;
    static final int LATER_BUTTON_ID = 7202;
    static final int LINK_CONFIRMATION_ID = 7203;
    private static final int ERROR_COLOR = 0xFF5555;

    private final ReleaseInfo releaseInfo;
    private final String currentVersion;
    private final Runnable dismissAction;
    private final ReleaseLinkOpener releaseLinkOpener;
    private final UpdateScreenNavigator screenNavigator;
    private final UpdateLinkConfirmationFactory linkConfirmationFactory;

    private boolean linkOpenFailed;

    public UpdateAvailableGui(GuiScreen parentScreen, ReleaseInfo releaseInfo,
                              String currentVersion, Runnable dismissAction) {
        this(parentScreen, releaseInfo, currentVersion, dismissAction,
                ReleasePageOpener::open, null,
                (callback, url, id) -> new GuiConfirmOpenLink(callback, url, id, true));
    }

    UpdateAvailableGui(GuiScreen parentScreen, ReleaseInfo releaseInfo,
                       String currentVersion, Runnable dismissAction,
                       ReleaseLinkOpener releaseLinkOpener,
                       UpdateScreenNavigator screenNavigator,
                       UpdateLinkConfirmationFactory linkConfirmationFactory) {
        super(parentScreen);
        if (releaseInfo == null) {
            throw new IllegalArgumentException("releaseInfo cannot be null");
        }
        if (releaseLinkOpener == null) {
            throw new IllegalArgumentException("releaseLinkOpener cannot be null");
        }
        if (linkConfirmationFactory == null) {
            throw new IllegalArgumentException("linkConfirmationFactory cannot be null");
        }
        this.releaseInfo = releaseInfo;
        this.currentVersion = currentVersion;
        this.dismissAction = dismissAction;
        this.releaseLinkOpener = releaseLinkOpener;
        this.screenNavigator = screenNavigator;
        this.linkConfirmationFactory = linkConfirmationFactory;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();

        int buttonX = getCenterX() - BUTTON_WIDTH / 2;
        int firstButtonY = getCenterY() + 34;
        buttonList.add(CustomButtonFactory.createGeneric(DOWNLOAD_BUTTON_ID, buttonX,
                firstButtonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                I18nManager.getMessage("gui.update.download"), button -> {
                }));
        buttonList.add(CustomButtonFactory.createGeneric(LATER_BUTTON_ID, buttonX,
                firstButtonY + 25, BUTTON_WIDTH, BUTTON_HEIGHT,
                I18nManager.getMessage("gui.update.later"), button -> {
                }));
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        int lineY = getCenterY() - 48;
        drawCentered(I18nManager.getMessage("gui.update.available"), lineY, WHITE_COLOR);
        drawCentered(I18nManager.getMessage("gui.update.current_version", currentVersion),
                lineY + 16, WHITE_COLOR);
        drawCentered(I18nManager.getMessage("gui.update.latest_version",
                releaseInfo.getDisplayVersion()), lineY + 30, WHITE_COLOR);
        drawCentered(I18nManager.getMessage("gui.update.browser_notice"),
                lineY + 48, WHITE_COLOR);

        if (linkOpenFailed) {
            drawCentered(I18nManager.getMessage("gui.update.open_failed"),
                    lineY + 63, ERROR_COLOR);
        }
    }

    private void drawCentered(String text, int y, int color) {
        getUIManager().drawCenteredText(text, getCenterX(), y, color);
    }

    @Override
    protected String getTitle() {
        return I18nManager.getMessage("gui.update.title");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == DOWNLOAD_BUTTON_ID) {
            linkOpenFailed = false;
            showScreen(linkConfirmationFactory.create(this,
                    releaseInfo.getReleasePageUrl(), LINK_CONFIRMATION_ID));
            return;
        }
        if (button.id == LATER_BUTTON_ID) {
            dismissAndReturn();
            return;
        }
        super.actionPerformed(button);
    }

    @Override
    public void confirmClicked(boolean openLink, int id) {
        if (id != LINK_CONFIRMATION_ID) {
            showScreen(this);
            return;
        }

        if (!openLink) {
            showScreen(this);
            return;
        }

        if (releaseLinkOpener.open(releaseInfo.getReleasePageUri())) {
            dismissAndReturn();
        } else {
            linkOpenFailed = true;
            showScreen(this);
        }
    }

    @Override
    protected void handleBackButton() {
        dismissAndReturn();
    }

    private void dismissAndReturn() {
        dismissForSession();
        showScreen(getParentScreen());
    }

    private void dismissForSession() {
        if (dismissAction != null) {
            dismissAction.run();
        }
    }

    private void showScreen(GuiScreen screen) {
        if (screenNavigator != null) {
            screenNavigator.show(screen);
        } else {
            mc.displayGuiScreen(screen);
        }
    }
}
