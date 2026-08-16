package fr.ntgitg.mineglot.ui.gui.utils.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

public class SoundManager {
    private static final ResourceLocation SOUND_CLICK = new ResourceLocation("mineglot", "click");
    private static final ResourceLocation SOUND_HOVER = new ResourceLocation("mineglot", "hover");
    private static final ResourceLocation SOUND_SUCCESS = new ResourceLocation("mineglot", "success");
    private static final ResourceLocation SOUND_ERROR = new ResourceLocation("mineglot", "error");

    private static final float DEFAULT_VOLUME = 1.0f;

    public static void playClick() {
        playSound(SOUND_CLICK, DEFAULT_VOLUME);
    }

    public static void playHover() {
        playSound(SOUND_HOVER, DEFAULT_VOLUME);
    }

    public static void playSuccess() {
        playSound(SOUND_SUCCESS, DEFAULT_VOLUME);
    }

    public static void playError() {
        playSound(SOUND_ERROR, DEFAULT_VOLUME);
    }

    private static void playSound(ResourceLocation sound, float volume) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.getSoundHandler() != null) {
            mc.getSoundHandler().playSound(PositionedSoundRecord.create(sound, volume));
        }
    }
}
