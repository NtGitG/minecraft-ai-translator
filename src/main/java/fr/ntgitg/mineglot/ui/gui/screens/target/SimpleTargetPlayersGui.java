package fr.ntgitg.mineglot.ui.gui.screens.target;

import fr.ntgitg.mineglot.core.target.TargetPlayerCoordinator;
import fr.ntgitg.mineglot.core.command.target.services.TargetPlayerList;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.core.service.message.MessageService;
import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.ui.gui.base.AbstractScrollableListGui;
import fr.ntgitg.mineglot.ui.gui.base.ScrollableListComponent.SelectionMode;
import fr.ntgitg.mineglot.ui.gui.utils.button.ButtonType;
import fr.ntgitg.mineglot.ui.gui.utils.button.CustomButtonFactory;
import fr.ntgitg.mineglot.ui.gui.utils.title.TitleManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SimpleTargetPlayersGui extends AbstractScrollableListGui<String> {
    private static final int CLEAR_BUTTON_MARGIN = 5;

    public SimpleTargetPlayersGui(GuiScreen parentScreen) {
        super(parentScreen, new ArrayList<>(), 3);
    }

    @Override
    public void initGui() {
        loadPlayerList();

        super.initGui();

        int backY = getUIManager().getBackButtonY(getCenterY(), GUI_HEIGHT, BUTTON_HEIGHT);
        GuiButton clearBtn = CustomButtonFactory.createClearCache(getCenterX() - BUTTON_WIDTH / 2,
                backY - BUTTON_HEIGHT - CLEAR_BUTTON_MARGIN, I18nManager.getMessage("gui.target.reset"), btn -> {
                });
        buttonList.add(clearBtn);
    }

    private void loadPlayerList() {
        if (!areServicesAvailable()) {
            ModLogger.warn("Services non disponibles pour la mise a jour de la liste des joueurs");
            allItems.clear();
            return;
        }

        try {
            List<String> onlinePlayers = getAvailablePlayersViaService();

            allItems.clear();
            allItems.addAll(onlinePlayers);
            allItems.sort(String::compareToIgnoreCase);

        } catch (Exception e) {
            handlePlayerError(e, "chargement de la liste des joueurs");
            allItems.clear();
        }
    }

    private void forceReloadPlayerList() {
        loadPlayerList();
        refreshListSafely("rechargement de la liste des joueurs");
    }

    private List<String> getAvailablePlayersViaService() {
        List<String> availablePlayers = new ArrayList<>();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return availablePlayers;
        }

        EntityPlayerSP self = minecraft.thePlayer;
        if (self == null) {
            return availablePlayers;
        }

        getPlayerNameManager().markDirty();
        List<String> rawPlayerNames = getPlayerNameManager().getRawOnlinePlayerNames();
        String currentPlayerName = self.getName();

        for (String playerName : rawPlayerNames) {
            if (isValidTargetPlayer(playerName, currentPlayerName)) {
                availablePlayers.add(playerName);
            }
        }

        if (availablePlayers.isEmpty()) {
            availablePlayers.addAll(getPlayersFromWorld(currentPlayerName));
        }

        return availablePlayers;
    }

    private boolean isValidTargetPlayer(String playerName, String currentPlayerName) {
        if (playerName.equals(currentPlayerName)) {
            return false;
        }

        if (!ValidationService.isValidPlayerNameSimple(playerName)) {
            return false;
        }

        return !ValidationService.isLikelyBot(playerName);
    }

    private List<String> getPlayersFromWorld(String currentPlayerName) {
        List<String> worldPlayers = new ArrayList<>();
        WorldClient world = Minecraft.getMinecraft().theWorld;
        if (world == null) {
            return worldPlayers;
        }

        for (EntityPlayer player : world.playerEntities) {
            String playerName = player.getName();
            if (isValidTargetPlayer(playerName, currentPlayerName)) {
                worldPlayers.add(playerName);
            }
        }

        return worldPlayers;
    }

    private boolean areServicesAvailable() {
        boolean configAvailable = getConfigService().isOperational();
        boolean playerManagerAvailable = (getPlayerNameManager() != null);

        if (!configAvailable) {
            ModLogger.warn("ConfigService non operationnel");
        }

        return configAvailable && playerManagerAvailable;
    }

    @Override
    protected String getDisplayName(String player) {
        return player;
    }

    @Override
    protected void onSelect(String playerName) {
        playButtonSound();

        if (!areServicesAvailable()) {
            MessageService.sendError(mc.thePlayer, "service.unavailable");
            return;
        }

        try {
            UUID playerId = getPlayerNameManager().getPlayerUuidByName(playerName);
            if (playerId == null) {
                MessageService.sendError(mc.thePlayer, "player.not_found", playerName);
                return;
            }

            boolean wasInTargetList = TargetPlayerList.getInstance().contains(playerId);

            if (wasInTargetList) {
                removePlayerFromTargetList(playerName);
            } else {
                addPlayerToTargetList(playerName);
            }

            refreshListSafely("selection du joueur");

        } catch (Exception e) {
            handlePlayerError(e, "selection du joueur: " + playerName);
        }
    }

    private void addPlayerToTargetList(String playerName) {
        int maxTargetedPlayers = getConfigService().getModConfig().getMaxTargetedPlayers();
        if (TargetPlayerList.getInstance().size() >= maxTargetedPlayers) {
            MessageService.sendError(mc.thePlayer, "target.limit_reached");
            return;
        }

        TargetPlayerCoordinator.addPlayer(playerName,
                name -> handleTargetMutationSuccess(name, true),
                errorKey -> MessageService.sendError(mc.thePlayer, errorKey, playerName));
    }

    private void removePlayerFromTargetList(String playerName) {
        TargetPlayerCoordinator.removePlayer(playerName,
                name -> handleTargetMutationSuccess(name, false),
                errorKey -> MessageService.sendError(mc.thePlayer, errorKey, playerName));
    }

    private void handleTargetMutationSuccess(String playerName, boolean added) {
        if (added) {
            MessageService.sendSuccess(mc.thePlayer, "target.player_added", playerName);
            ModLogger.info("Joueur ajoute a la liste cible: {}", playerName);
        } else {
            MessageService.sendSuccess(mc.thePlayer, "target.player_removed", playerName);
            ModLogger.info("Joueur retire de la liste cible: {}", playerName);
        }

        refreshListSafely("mise a jour des joueurs cibles");
    }

    private void refreshListSafely(String context) {
        if (listComponent != null) {
            refreshList();
            return;
        }
        ModLogger.warn("listComponent est null - impossible de refresh ({})", context);
    }

    @Override
    protected String getSelectedItem() {
        return null;
    }

    @Override
    protected SelectionMode getSelectionMode() {
        return SelectionMode.MULTIPLE;
    }

    @Override
    protected Set<Integer> getInitialSelectedIndices() {
        Set<Integer> selectedIndices = new HashSet<>();

        if (allItems == null || allItems.isEmpty()) {
            return selectedIndices;
        }

        try {
            for (int i = 0; i < allItems.size(); i++) {
                String playerName = allItems.get(i);
                UUID playerId = getPlayerNameManager().getPlayerUuidByName(playerName);
                if (playerId != null && TargetPlayerList.getInstance().contains(playerId)) {
                    selectedIndices.add(i);
                }
            }
        } catch (Exception e) {
            ModLogger.warn("Impossible de recuperer les selections actuelles - liste vide utilisee");
        }

        return selectedIndices;
    }

    @Override
    protected String getTitle() {
        return TitleManager.getTargetTitle();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ButtonType.CLEAR_CACHE.getId()) {
            handleClearTargetList();
        } else if (button.id == BACK_BUTTON_ID) {
            handleBackButton();
        }
    }

    private void handleClearTargetList() {
        TargetPlayerCoordinator.clearTargets(count -> {
            MessageService.sendSuccess(mc.thePlayer, "target.list_cleared", count);
            ModLogger.info("Liste des joueurs cibles nettoyee: {} joueurs supprimes", count);
            forceReloadPlayerList();
        }, errorKey -> MessageService.sendError(mc.thePlayer, errorKey));
    }

    @Override
    protected List<String> getTooltip(String player) {
        return null;
    }
}
