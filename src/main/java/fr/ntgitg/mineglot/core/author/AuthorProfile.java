package fr.ntgitg.mineglot.core.author;

import net.minecraft.util.ResourceLocation;

public class AuthorProfile {

    private static final String AUTHOR_NAME = "Nathan";
    private static final String AUTHOR_USERNAME = "NtGitG";
    private static final String AVATAR_RESOURCE = "mineglot:textures/gui/author_avatar.png";

    private static final AuthorProfile INSTANCE = new AuthorProfile();

    private AuthorProfile() {
    }

    public static AuthorProfile getInstance() {
        return INSTANCE;
    }

    public String getName() {
        return AUTHOR_NAME;
    }

    public String getUsername() {
        return AUTHOR_USERNAME;
    }

    public ResourceLocation getAvatarResource() {
        return new ResourceLocation(AVATAR_RESOURCE);
    }

    public String getAvatarResourcePath() {
        return AVATAR_RESOURCE;
    }

    public boolean hasAvatar() {
        return true; // Toujours disponible car local
    }

    public String getFormattedDescription() {
        return String.format("%s (@%s)", AUTHOR_NAME, AUTHOR_USERNAME);
    }

    public String getAbout() {
        return getFormattedDescription();
    }

    @Override
    public String toString() {
        return String.format("AuthorProfile{name='%s', username='%s'}", AUTHOR_NAME, AUTHOR_USERNAME);
    }
}
