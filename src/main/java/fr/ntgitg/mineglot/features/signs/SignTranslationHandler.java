package fr.ntgitg.mineglot.features.signs;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fr.ntgitg.mineglot.core.translation.TranslationOrchestrator;
import fr.ntgitg.mineglot.core.config.ConfigurationManager;
import fr.ntgitg.mineglot.core.service.SingletonManager;
import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.core.service.thread.ThreadSafeMessageService;
import fr.ntgitg.mineglot.core.validation.ValidationService;
import fr.ntgitg.mineglot.features.AbstractTranslationHandler;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IChatComponent;

import java.util.concurrent.TimeUnit;

public class SignTranslationHandler extends AbstractTranslationHandler<BlockPos> {

    private static final String SIGN_SENDER_KEY = "sign.label";
    private static final String SIGN_LINE_SEPARATOR = " | ";
    private static final long COOLDOWN_MESSAGE_INTERVAL_MS = 2_000L;

    private final Cache<BlockPos, Long> recentSignTranslations = CacheBuilder.newBuilder()
            .expireAfterWrite(TRANSLATION_COOLDOWN_MS, TimeUnit.MILLISECONDS)
            .maximumSize(512)
            .build();
    private final Cache<BlockPos, Long> recentCooldownNotices = CacheBuilder.newBuilder()
            .expireAfterWrite(COOLDOWN_MESSAGE_INTERVAL_MS, TimeUnit.MILLISECONDS)
            .maximumSize(512)
            .build();

    private SignTranslationHandler() {
        super("Sign");
    }

    public static SignTranslationHandler getInstance() {
        return SingletonManager.getInstance(SignTranslationHandler.class, SignTranslationHandler::new);
    }

    public void handleSignInteraction(BlockPos signPos, TileEntitySign sign) {
        if (signPos == null || sign == null) {
            return;
        }

        if (!ConfigurationManager.getInstance().isSignTranslationEnabled()) {
            return;
        }

        if (isOnCooldown(signPos)) {
            if (shouldShowCooldownMessage(signPos)) {
                ThreadSafeMessageService.sendInfo("sign.cooldown");
            }
            return;
        }

        String fullText = buildSignText(sign.signText);
        if (fullText.isEmpty()) {
            ThreadSafeMessageService.sendError("sign.empty");
            return;
        }

        String textToTranslate = ValidationService.truncateSignContent(fullText);

        updateLastTarget(signPos);

        String displayText = ValidationService.truncateDisplayText(textToTranslate);
        ThreadSafeMessageService.sendSuccess("sign.translating", displayText);

        translateSign(textToTranslate);
    }

    private void translateSign(String text) {
        try {
            String senderLabel = I18nManager.getMessage(SIGN_SENDER_KEY);
            TranslationOrchestrator.translateAsync(senderLabel, text, true, false)
                    .exceptionally(throwable -> {
                        ModLogger.error("Erreur lors de la traduction du panneau", throwable);
                        ThreadSafeMessageService.sendError("sign.failed");
                        return null;
                    });
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la traduction du panneau", e);
            ThreadSafeMessageService.sendError("sign.failed");
        }
    }

    public void shutdown() {
        super.shutdown();
        recentSignTranslations.invalidateAll();
        recentCooldownNotices.invalidateAll();
    }

    public void resetLastSign() {
        lastTarget = null;
        recentSignTranslations.invalidateAll();
        recentCooldownNotices.invalidateAll();
    }

    @Override
    protected boolean isOnCooldown(BlockPos current) {
        if (current == null) {
            return false;
        }

        Long lastTranslation = recentSignTranslations.getIfPresent(current);
        return lastTranslation != null
                && System.currentTimeMillis() - lastTranslation < TRANSLATION_COOLDOWN_MS;
    }

    @Override
    protected void updateLastTarget(BlockPos current) {
        if (current == null) {
            return;
        }

        BlockPos key = copyOf(current);
        recentSignTranslations.put(key, System.currentTimeMillis());
        lastTarget = key;
        lastTranslationTime = System.currentTimeMillis();
    }

    @Override
    protected boolean isSame(BlockPos a, BlockPos b) {
        return a != null && a.equals(b);
    }

    @Override
    protected BlockPos copyOf(BlockPos target) {
        return target == null ? null : new BlockPos(target.getX(), target.getY(), target.getZ());
    }

    private boolean shouldShowCooldownMessage(BlockPos signPos) {
        BlockPos key = copyOf(signPos);
        long now = System.currentTimeMillis();
        Long lastNotice = recentCooldownNotices.getIfPresent(key);
        if (lastNotice != null && now - lastNotice < COOLDOWN_MESSAGE_INTERVAL_MS) {
            return false;
        }

        recentCooldownNotices.put(key, now);
        return true;
    }

    static String buildSignText(IChatComponent[] lines) {
        if (lines == null || lines.length == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (IChatComponent line : lines) {
            String text = cleanLine(line);
            if (text.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(SIGN_LINE_SEPARATOR);
            }
            builder.append(text);
        }

        return builder.toString();
    }

    private static String cleanLine(IChatComponent line) {
        if (line == null || line.getUnformattedText() == null) {
            return "";
        }
        return line.getUnformattedText().trim();
    }
}
