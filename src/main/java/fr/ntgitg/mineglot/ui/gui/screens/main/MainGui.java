package fr.ntgitg.mineglot.ui.gui.screens.main;

import fr.ntgitg.mineglot.core.author.GitHubManager;
import fr.ntgitg.mineglot.core.donation.DonationManager;
import fr.ntgitg.mineglot.ui.gui.base.AbstractReadOnlyListGui;
import fr.ntgitg.mineglot.ui.gui.components.scrollbar.ScrollManager;
import fr.ntgitg.mineglot.ui.gui.screens.api.ApiKeyGui;
import fr.ntgitg.mineglot.ui.gui.screens.cache.CacheGui;
import fr.ntgitg.mineglot.ui.gui.screens.config.ConfigGui;
import fr.ntgitg.mineglot.ui.gui.screens.help.HelpGui;
import fr.ntgitg.mineglot.ui.gui.screens.language.DefaultLanguageGui;
import fr.ntgitg.mineglot.ui.gui.screens.language.LanguageGui;
import fr.ntgitg.mineglot.ui.gui.screens.model.ModelGui;
import fr.ntgitg.mineglot.ui.gui.screens.target.SimpleTargetPlayersGui;
import fr.ntgitg.mineglot.ui.gui.utils.button.ButtonListener;
import fr.ntgitg.mineglot.ui.gui.utils.button.ButtonType;
import fr.ntgitg.mineglot.ui.gui.utils.button.CustomButtonFactory;
import fr.ntgitg.mineglot.ui.gui.utils.title.TitleManager;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MainGui extends AbstractReadOnlyListGui<MainGui.MenuItem> {
    private static final int BUTTON_SPACING = 24;
    private static final int BUTTONS_PER_PAGE = 5;
    private static final int GITHUB_BUTTON_ID = 888;
    private static final int DONATION_BUTTON_ID = 999;
    private static final int SCROLLBAR_MARGIN = 3;
    private static final int UNSET_SCROLL_OFFSET = Integer.MIN_VALUE;

    private int lastRenderedScrollOffset = UNSET_SCROLL_OFFSET;
    private int lastRenderedItemCount = -1;
    private boolean buttonsDirty = true;

    public static class MenuItem {
        private final String labelKey;
        private final int buttonId;
        private final Function<GuiScreen, GuiScreen> targetGuiFactory;

        public MenuItem(String labelKey, int buttonId, Function<GuiScreen, GuiScreen> targetGuiFactory) {
            this.labelKey = labelKey;
            this.buttonId = buttonId;
            this.targetGuiFactory = targetGuiFactory;
        }

        public String getDisplayName() {
            if (buttonId == DONATION_BUTTON_ID) {
                return "";
            }
            if (buttonId == GITHUB_BUTTON_ID) {
                return "";
            }
            return "§f" + I18nManager.getMessage(labelKey);
        }

        public int getButtonId() {
            return buttonId;
        }

        public GuiScreen createGui(GuiScreen parent) {
            if (targetGuiFactory == null) {
                return null;
            }
            try {
                return targetGuiFactory.apply(parent);
            } catch (RuntimeException e) {
                return null;
            }
        }
    }

    private static List<MenuItem> createMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("menu.language", 1, LanguageGui::new));
        items.add(new MenuItem("menu.default_language", 2, DefaultLanguageGui::new));
        items.add(new MenuItem("menu.target_players", 3, SimpleTargetPlayersGui::new));
        items.add(new MenuItem("menu.configuration", 4, ConfigGui::new));
        items.add(new MenuItem("menu.api_key", 5, ApiKeyGui::new));
        items.add(new MenuItem("menu.model", 6, ModelGui::new));
        items.add(new MenuItem("menu.help", 7, HelpGui::new));
        items.add(new MenuItem("menu.cache_stats", 9, CacheGui::new));
        items.add(new MenuItem("menu.github", GITHUB_BUTTON_ID, null));
        items.add(new MenuItem("menu.donate", DONATION_BUTTON_ID, null));
        return items;
    }

    public MainGui(GuiScreen parentScreen) {
        super(parentScreen, createMenuItems(), BUTTONS_PER_PAGE);
    }

    @Override
    public void initGui() {
        int[] center = getUIManager().getScreenCenter();
        setCenterX(center[0]);
        setCenterY(center[1]);

        buttonList.clear();

        addBackButton();

        int listStartY = getMainListStartY();
        listComponent = new fr.ntgitg.mineglot.ui.gui.base.ScrollableListComponent.Builder<MenuItem>()
                .items(allItems).nameExtractor(this::getDisplayName)
                .position(getCenterX() - buttonWidth / 2, listStartY).size(buttonWidth, buttonHeight)
                .spacing(listSpacing).visibleItems(visibleItemCount).selectionMode(getSelectionMode()) // Mode
                .tooltipProvider(idx -> getTooltip(getObjectAtIndex(idx))).selectedOnTop(false).build();

        markButtonsDirty();
        updateCustomButtonsIfNeeded(true);
    }

    @Override
    protected void refreshList() {
        if (listComponent != null) {
            listComponent.setItems(allItems);
        }
        markButtonsDirty();
    }

    private void markButtonsDirty() {
        buttonsDirty = true;
    }

    private void updateCustomButtonsIfNeeded(boolean force) {
        if (listComponent == null) {
            return;
        }

        int scrollOffset = listComponent.getScrollOffset();
        int itemCount = allItems.size();
        if (!force && !buttonsDirty
                && scrollOffset == lastRenderedScrollOffset
                && itemCount == lastRenderedItemCount) {
            return;
        }

        setupCustomButtons(scrollOffset);
        lastRenderedScrollOffset = scrollOffset;
        lastRenderedItemCount = itemCount;
        buttonsDirty = false;
    }

    private void setupCustomButtons(int scrollOffset) {
        buttonList.removeIf(button -> button.id != BACK_BUTTON_ID);
        int startY = getMainListStartY();
        for (int i = 0; i < BUTTONS_PER_PAGE && i + scrollOffset < allItems.size(); i++) {
            MenuItem item = allItems.get(i + scrollOffset);
            GuiButton button;
            if (item.getButtonId() == DONATION_BUTTON_ID) {
                button = CustomButtonFactory.create(ButtonType.BUY_ME_COFFEE,
                        getCenterX() - ButtonType.BUY_ME_COFFEE.getDefaultWidth() / 2,
                        startY + (i * BUTTON_SPACING), "", new ButtonListener() {
                            @Override
                            public void onButtonClick(
                                    fr.ntgitg.mineglot.ui.gui.utils.button.CustomButton clickedButton) {
                                DonationManager.openDonationLink();
                            }
                        });
            } else if (item.getButtonId() == GITHUB_BUTTON_ID) {
                button = CustomButtonFactory.create(ButtonType.GITHUB,
                        getCenterX() - ButtonType.GITHUB.getDefaultWidth() / 2, startY + (i * BUTTON_SPACING),
                        "", new ButtonListener() {
                            @Override
                            public void onButtonClick(
                                    fr.ntgitg.mineglot.ui.gui.utils.button.CustomButton clickedButton) {
                                openGitHubProfile();
                            }
                        });
            } else {
                button = CustomButtonFactory.createGeneric(item.getButtonId(),
                        getCenterX() - BUTTON_WIDTH / 2, startY + (i * BUTTON_SPACING), BUTTON_WIDTH,
                        BUTTON_HEIGHT, item.getDisplayName(), btn -> {
                        });
            }
            buttonList.add(button);
        }
    }

    @Override
    protected void onSelect(MenuItem item) {
        playButtonSound();

        if (item.getButtonId() == DONATION_BUTTON_ID || item.getButtonId() == GITHUB_BUTTON_ID) {
            return;
        }

        GuiScreen targetGui = item.createGui(this);
        if (targetGui != null) {
            mc.displayGuiScreen(targetGui);
        }
    }

    private void openGitHubProfile() {
        GitHubManager.openGitHubProfile();
    }

    @Override
    protected String getDisplayName(MenuItem item) {
        return item.getDisplayName();
    }

    @Override
    protected List<String> getTooltip(MenuItem item) {
        return null; // Pas de tooltips pour le menu principal
    }

    @Override
    protected String getTitle() {
        return TitleManager.getMainTitle();
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        drawScrollBar();
    }

    private void drawScrollBar() {
        if (allItems.size() > BUTTONS_PER_PAGE) {
            int startY = getMainListStartY();
            int totalHeight = BUTTONS_PER_PAGE * BUTTON_SPACING;
            int scrollOffset = listComponent != null ? listComponent.getScrollOffset() : 0;
            ScrollManager.drawScrollBar(getCenterX() + BUTTON_WIDTH / 2 + SCROLLBAR_MARGIN, startY,
                    totalHeight, allItems.size(), BUTTONS_PER_PAGE, scrollOffset);
        }
    }

    private int getMainListStartY() {
        return getCenterY() - (BUTTONS_PER_PAGE * BUTTON_SPACING) / 2;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BACK_BUTTON_ID) {
            handleBackButton();
            return;
        }

        for (MenuItem item : allItems) {
            if (item.getButtonId() == button.id) {
                onSelect(item);
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == KEY_ESCAPE) {
            handleBackButton();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateCustomButtonsIfNeeded(false);
    }
}
