package fr.ntgitg.mineglot.ui.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class UIContextState {

    private final Minecraft minecraft;
    private volatile FontRenderer fontRenderer;
    private volatile boolean initialized;

    UIContextState() {
        this.minecraft = Minecraft.getMinecraft();
    }

    boolean initializeIfPossible() {
        if (initialized) {
            return true;
        }
        if (minecraft == null || minecraft.fontRendererObj == null) {
            return false;
        }

        fontRenderer = minecraft.fontRendererObj;
        initialized = true;
        return true;
    }

    FontRenderer getFontRenderer() {
        if (!initialized) {
            initializeIfPossible();
        }
        if (fontRenderer == null && minecraft != null) {
            fontRenderer = minecraft.fontRendererObj;
        }
        return fontRenderer;
    }

    boolean isReady() {
        return minecraft != null && getFontRenderer() != null;
    }

    boolean hasMinecraft() {
        return minecraft != null;
    }

    boolean isInitialized() {
        return initialized;
    }

    ScaledResolution getScaledResolution() {
        if (minecraft == null) {
            return null;
        }
        return new ScaledResolution(minecraft);
    }

    void cleanup() {
        fontRenderer = null;
        initialized = false;
    }
}
