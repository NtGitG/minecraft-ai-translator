package fr.ntgitg.mineglot.core.author;

import fr.ntgitg.mineglot.core.service.i18n.I18nManager;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;

public class AuthorManager {

    private static final String[] ABOUT_LINE_KEYS = {
            "author.profile_about_line1",
            "author.profile_about_line2",
            "author.profile_about_line3"
    };

    private static final AuthorManager INSTANCE = new AuthorManager();

    private AuthorManager() {
    }

    public static AuthorManager getInstance() {
        return INSTANCE;
    }

    public void displayAuthorInfo(ICommandSender sender, boolean isGuiMode) {
        AuthorProfile profile = AuthorProfile.getInstance();

        if (!isGuiMode) {
            displayAuthorInfoToPlayer(sender, profile);
        }

        ModLogger.info("Informations auteur affichées pour: {}", sender.getName());
    }

    public AuthorProfile getProfile() {
        return AuthorProfile.getInstance();
    }

    public ResourceLocation getAvatarResource() {
        return AuthorProfile.getInstance().getAvatarResource();
    }

    public boolean isOperational() {
        return true; // Toujours opérationnel car local
    }

    public boolean isLoading() {
        return false; // Pas de chargement car local
    }

    public String getAbout() {
        return AuthorProfile.getInstance().getAbout();
    }

    private void displayAuthorInfoToPlayer(ICommandSender sender, AuthorProfile profile) {
        if (sender instanceof EntityPlayer) {
            displayAuthorInfoToPlayer((EntityPlayer) sender, profile);
            return;
        }

        sendChatLine(sender, "author.profile_banner");
        sendChatLine(sender, "author.profile_name", profile.getName());
        sendChatLine(sender, "author.profile_username", profile.getUsername());
        sendChatLine(sender, "author.profile_about_title");
        for (String lineKey : ABOUT_LINE_KEYS) {
            sendChatLine(sender, lineKey);
        }
        sendChatLine(sender, "author.profile_footer");
    }

    private void displayAuthorInfoToPlayer(EntityPlayer player, AuthorProfile profile) {
        sendChatLine(player, "author.profile_banner");
        sendChatLine(player, "author.profile_name", profile.getName());
        sendChatLine(player, "author.profile_username", profile.getUsername());
        sendChatLine(player, "author.profile_about_title");
        for (String lineKey : ABOUT_LINE_KEYS) {
            sendChatLine(player, lineKey);
        }
        sendChatLine(player, "author.profile_footer");
    }

    private void sendChatLine(ICommandSender sender, String messageKey, Object... args) {
        sender.addChatMessage(new ChatComponentText(I18nManager.getMessage(messageKey, args)));
    }

    private void sendChatLine(EntityPlayer player, String messageKey, Object... args) {
        sendChatLine((ICommandSender) player, messageKey, args);
    }
}
