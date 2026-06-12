package fr.ntgitg.mineglot.ui.gui.utils.button;

import fr.ntgitg.mineglot.ui.core.UIManager;
import fr.ntgitg.mineglot.ui.gui.utils.sound.ISoundProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.util.Objects;

public class CustomButton extends GuiButton {
    private final ResourceLocation texture;
    private final int textureX, textureY;
    private final int textureWidth, textureHeight;
    private ButtonListener eventHandler;
    private final ISoundProvider soundProvider;

    private final UIManager uiManager = UIManager.getInstance();

    public CustomButton(int buttonId, int x, int y, int width, int height, ResourceLocation texture,
                        int textureX, int textureY, int textureWidth, int textureHeight,
                        ISoundProvider soundProvider) {
        super(buttonId, x, y, width, height, "");
        this.texture = Objects.requireNonNull(texture, "texture ne peut pas être null");
        this.textureX = textureX;
        this.textureY = textureY;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.soundProvider =
                Objects.requireNonNull(soundProvider, "soundProvider ne peut pas être null");
    }

    public CustomButton withEventHandler(ButtonListener handler) {
        this.eventHandler = java.util.Objects.requireNonNull(handler, "handler ne peut pas être null");
        return this;
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!enabled || !visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (eventHandler != null) {
            eventHandler.onButtonClick(this);
        }
        soundProvider.playClick(1.0F);
        return true;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible || !enabled) {
            return;
        }
        if (texture != null) {
            bindTexture(mc);
            drawBackground();
        }
        drawLabel(mc);
    }

    private boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width
                && mouseY < yPosition + height;
    }

    private void bindTexture(Minecraft mc) {
        mc.getTextureManager().bindTexture(texture);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private void drawBackground() {
        Gui.drawScaledCustomSizeModalRect(xPosition, yPosition, textureX, textureY, textureWidth,
                textureHeight, width, height, textureWidth, textureHeight);
    }

    private void drawLabel(Minecraft mc) {
        if (displayString == null || displayString.isEmpty()) {
            return;
        }

        int centerX = xPosition + width / 2;
        int centerY = yPosition + (height - 8) / 2;

        uiManager.drawButtonText(displayString, centerX, centerY, enabled);
    }

    @Override
    public void playPressSound(net.minecraft.client.audio.SoundHandler soundHandler) {
    }
}
