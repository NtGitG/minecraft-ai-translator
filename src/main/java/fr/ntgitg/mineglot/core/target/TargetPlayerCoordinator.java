package fr.ntgitg.mineglot.core.target;

import fr.ntgitg.mineglot.core.command.target.services.TargetPlayerList;
import fr.ntgitg.mineglot.core.player.PlayerNameManager;
import fr.ntgitg.mineglot.core.validation.ValidationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class TargetPlayerCoordinator {

    private static final PlayerNameManager playerNameManager = PlayerNameManager.getInstance();

    public static void addPlayer(String playerName, Consumer<String> onSuccess,
                                 Consumer<String> onError) {
        playerNameManager.markDirty();
        UUID uuid = playerNameManager.getPlayerUuidByName(playerName);
        if (uuid == null) {
            if (onError != null)
                onError.accept("player.not_found");
            return;
        }
        if (TargetPlayerList.getInstance().contains(uuid)) {
            if (onError != null)
                onError.accept("target.already_targeted");
            return;
        }
        boolean added = TargetPlayerList.getInstance().add(uuid);
        if (added) {
            if (onSuccess != null)
                onSuccess.accept(playerName);
        } else {
            if (onError != null)
                onError.accept("target.add_failed");
        }
    }

    public static void removePlayer(String playerName, Consumer<String> onSuccess,
                                    Consumer<String> onError) {
        playerNameManager.markDirty();
        UUID uuid = playerNameManager.getPlayerUuidByName(playerName);
        if (uuid == null) {
            if (onError != null)
                onError.accept("player.not_found");
            return;
        }
        if (!TargetPlayerList.getInstance().contains(uuid)) {
            if (onError != null)
                onError.accept("target.not_targeted");
            return;
        }
        boolean removed = TargetPlayerList.getInstance().remove(uuid);
        if (removed) {
            if (onSuccess != null)
                onSuccess.accept(playerName);
        } else {
            if (onError != null)
                onError.accept("target.remove_failed");
        }
    }

    public static void clearTargets(Consumer<Integer> onSuccess, Consumer<String> onError) {
        int count = TargetPlayerList.getInstance().size();
        if (count == 0) {
            if (onError != null)
                onError.accept("target.list_empty");
            return;
        }
        TargetPlayerList.getInstance().clear();
        if (onSuccess != null)
            onSuccess.accept(count);
    }

    public static List<String> getTargetedPlayerNames() {
        List<String> names = new ArrayList<>();
        Set<UUID> uuids = TargetPlayerList.getInstance().all();
        for (UUID uuid : uuids) {
            String playerName = playerNameManager.getPlayerNameByUuid(uuid);
            if (playerName != null && !playerName.isEmpty()) {
                names.add(playerName);
            }
        }
        return names;
    }

    public static boolean isPlayerTargeted(String playerName) {
        UUID uuid = playerNameManager.getPlayerUuidByName(playerName);
        return uuid != null && TargetPlayerList.getInstance().contains(uuid);
    }

    public static boolean isValidTargetPlayer(String playerName, String currentPlayerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }

        if (currentPlayerName == null || currentPlayerName.trim().isEmpty()) {
            return false;
        }

        if (playerName.equals(currentPlayerName))
            return false;
        if (!ValidationService.isValidPlayerNameSimple(playerName))
            return false;
        if (ValidationService.isLikelyBot(playerName))
            return false;
        return true;
    }

    private TargetPlayerCoordinator() {
    }
}
