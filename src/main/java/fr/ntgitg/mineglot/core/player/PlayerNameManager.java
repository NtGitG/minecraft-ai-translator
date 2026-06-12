package fr.ntgitg.mineglot.core.player;

import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.error.ErrorManager;
import fr.ntgitg.mineglot.core.service.error.ErrorType;
import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.utils.extractor.PlayerNameExtractor;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SideOnly(Side.CLIENT)
public final class PlayerNameManager {

    private static final long PLAYER_CACHE_TTL_MS = 1000L;

    private final ConcurrentMap<String, NetworkPlayerInfo> infoMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> rawNameByUuid = new ConcurrentHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(true);
    private final Object cacheLock = new Object();

    private volatile OnlinePlayerNameSnapshot onlinePlayerNameSnapshot =
            OnlinePlayerNameSnapshot.empty();
    private volatile long lastCacheRefreshMs;

    PlayerNameManager() {
    }

    public static PlayerNameManager getInstance() {
        return SingletonManager.getInstance(PlayerNameManager.class, PlayerNameManager::new);
    }

    public void markDirty() {
        dirty.set(true);
    }

    private void updateCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (!dirty.get() && now - lastCacheRefreshMs < PLAYER_CACHE_TTL_MS) {
            return;
        }

        synchronized (cacheLock) {
            now = System.currentTimeMillis();
            if (!dirty.get() && now - lastCacheRefreshMs < PLAYER_CACHE_TTL_MS) {
                return;
            }

            if (rebuildCache(now)) {
                dirty.set(false);
            }
        }
    }

    private boolean rebuildCache(long refreshTimeMs) {
        ConcurrentMap<String, NetworkPlayerInfo> nextInfoMap = new ConcurrentHashMap<>();
        ConcurrentMap<UUID, String> nextRawNameByUuid = new ConcurrentHashMap<>();
        List<String> rawNames = new ArrayList<>();

        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getNetHandler() == null) {
                infoMap.clear();
                rawNameByUuid.clear();
                onlinePlayerNameSnapshot = OnlinePlayerNameSnapshot.empty();
                lastCacheRefreshMs = refreshTimeMs;
                return true;
            }

            mc.getNetHandler().getPlayerInfoMap().forEach(info -> {
                String displayName =
                        info.getDisplayName() != null ? info.getDisplayName().getUnformattedText()
                                : info.getGameProfile().getName();
                String profileName = info.getGameProfile().getName();
                String visibleName = resolveVisiblePlayerName(displayName, profileName);
                String visibleKey = normalizeLookupKey(visibleName);
                if (!visibleKey.isEmpty()) {
                    nextInfoMap.put(visibleKey, info);
                    addUniquePlayerName(rawNames, visibleName);

                    UUID uuid = info.getGameProfile().getId();
                    if (uuid != null) {
                        nextRawNameByUuid.put(uuid, visibleName);
                    }
                }

                String profileKey = normalizeValidProfileKey(profileName);
                if (!profileKey.isEmpty()) {
                    nextInfoMap.put(profileKey, info);
                }
            });

            infoMap.clear();
            infoMap.putAll(nextInfoMap);
            rawNameByUuid.clear();
            rawNameByUuid.putAll(nextRawNameByUuid);
            onlinePlayerNameSnapshot = OnlinePlayerNameSnapshot.from(rawNames);
            lastCacheRefreshMs = refreshTimeMs;
            return true;
        } catch (Exception e) {
            ErrorManager.handleError(e, ErrorType.PLAYER, null);
            return false;
        }
    }

    @Nullable
    public String getRealPlayerName(String formattedName) {
        if (formattedName == null)
            return null;
        updateCacheIfNeeded();
        String clean = PlayerNameExtractor.extractBaseName(formattedName).toLowerCase(Locale.ROOT);
        NetworkPlayerInfo info = infoMap.get(clean);
        if (info == null) {
            return null;
        }

        UUID uuid = info.getGameProfile().getId();
        if (uuid != null) {
            String visibleName = rawNameByUuid.get(uuid);
            if (visibleName != null && !visibleName.isEmpty()) {
                return visibleName;
            }
        }

        String displayName = info.getDisplayName() != null
                ? info.getDisplayName().getUnformattedText()
                : null;
        return resolveVisiblePlayerName(displayName, info.getGameProfile().getName());
    }

    public boolean isPlayerOnline(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        updateCacheIfNeeded();
        String clean = PlayerNameExtractor.extractBaseName(playerName).toLowerCase(Locale.ROOT);
        return infoMap.containsKey(clean);
    }

    @Nullable
    public UUID getPlayerUuidByName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return null;
        }
        updateCacheIfNeeded();
        String clean = PlayerNameExtractor.extractBaseName(playerName).toLowerCase(Locale.ROOT);
        NetworkPlayerInfo info = infoMap.get(clean);
        return info != null ? info.getGameProfile().getId() : null;
    }

    @Nullable
    public EntityPlayer getPlayerByName(String playerName) {
        if (playerName == null || playerName.isEmpty())
            return null;
        updateCacheIfNeeded();
        String clean = PlayerNameExtractor.extractBaseName(playerName).toLowerCase(Locale.ROOT);
        NetworkPlayerInfo info = infoMap.get(clean);
        if (info == null)
            return null;
        return getPlayerByUUID(info.getGameProfile().getId());
    }

    @Nullable
    public EntityPlayer getPlayerByUUID(UUID uuid) {
        if (uuid == null)
            return null;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) {
            ModLogger.warn("getPlayerByUUID called with no world loaded");
            return null;
        }
        EntityPlayer player = mc.theWorld.getPlayerEntityByUUID(uuid);
        if (player != null)
            return player;
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (uuid.equals(p.getUniqueID())) {
                return p;
            }
        }
        return null;
    }

    @Nullable
    public String getPlayerNameByUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        updateCacheIfNeeded();
        return rawNameByUuid.get(uuid);
    }

    public int getPlayerCount() {
        updateCacheIfNeeded();
        return infoMap.size();
    }

    public List<String> getRawOnlinePlayerNames() {
        updateCacheIfNeeded();
        return new ArrayList<>(onlinePlayerNameSnapshot.getRawNames());
    }

    public OnlinePlayerNameSnapshot getOnlinePlayerNameSnapshot() {
        updateCacheIfNeeded();
        return onlinePlayerNameSnapshot;
    }

    static String resolveVisiblePlayerName(String displayName, String profileName) {
        String displayBaseName = PlayerNameExtractor.extractBaseName(displayName);
        if (isUsablePlayerName(displayBaseName)) {
            return displayBaseName;
        }

        String trimmedProfileName = profileName != null ? profileName.trim() : "";
        if (isUsablePlayerName(trimmedProfileName)) {
            return trimmedProfileName;
        }

        return "";
    }

    private static String normalizeLookupKey(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return "";
        }

        String clean = PlayerNameExtractor.extractBaseName(playerName);
        return clean != null ? clean.toLowerCase(Locale.ROOT) : "";
    }

    private static String normalizeValidProfileKey(String profileName) {
        if (!isUsablePlayerName(profileName)) {
            return "";
        }
        return profileName.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isUsablePlayerName(String playerName) {
        if (playerName == null) {
            return false;
        }

        String trimmed = playerName.trim();
        return !trimmed.isEmpty()
                && ValidationService.isValidPlayerNameSimple(trimmed)
                && !PlayerNameExtractor.isBotName(trimmed);
    }

    private static void addUniquePlayerName(List<String> names, String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return;
        }

        for (String existing : names) {
            if (existing.equalsIgnoreCase(playerName)) {
                return;
            }
        }
        names.add(playerName);
    }

    public static final class OnlinePlayerNameSnapshot {
        private final List<String> rawNames;
        private final Set<String> rawNameSet;

        private OnlinePlayerNameSnapshot(List<String> rawNames, Set<String> rawNameSet) {
            this.rawNames = rawNames;
            this.rawNameSet = rawNameSet;
        }

        private static OnlinePlayerNameSnapshot empty() {
            return new OnlinePlayerNameSnapshot(Collections.emptyList(), Collections.emptySet());
        }

        private static OnlinePlayerNameSnapshot from(List<String> names) {
            List<String> rawNames = Collections.unmodifiableList(new ArrayList<>(names));
            Set<String> rawNameSet = Collections.unmodifiableSet(new HashSet<>(names));
            return new OnlinePlayerNameSnapshot(rawNames, rawNameSet);
        }

        public List<String> getRawNames() {
            return rawNames;
        }

        public Set<String> getRawNameSet() {
            return rawNameSet;
        }
    }
}
