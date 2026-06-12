package fr.ntgitg.mineglot.core.command.target.services;

import fr.ntgitg.mineglot.core.service.SingletonManager;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TargetPlayerList {
    private final Set<UUID> players = ConcurrentHashMap.newKeySet();

    private TargetPlayerList() {
    }

    public static TargetPlayerList getInstance() {
        return SingletonManager.getInstance(TargetPlayerList.class, TargetPlayerList::new);
    }

    public boolean add(UUID uuid) {
        return players.add(uuid);
    }

    public boolean remove(UUID uuid) {
        return players.remove(uuid);
    }

    public void clear() {
        players.clear();
    }

    public Set<UUID> all() {
        return Collections.unmodifiableSet(players);
    }

    public boolean contains(UUID uuid) {
        return players.contains(uuid);
    }

    public int size() {
        return players.size();
    }
}
