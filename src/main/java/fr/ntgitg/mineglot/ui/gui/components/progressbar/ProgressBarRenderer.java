package fr.ntgitg.mineglot.ui.gui.components.progressbar;

import fr.ntgitg.mineglot.ui.gui.rendering.GuiRenderUtils;
import fr.ntgitg.mineglot.utils.log.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public class ProgressBarRenderer {
    public void render(ProgressBar bar) {
        try {
            GlStateManager.pushMatrix();
            GlStateManager.pushAttrib();
            renderBackground(bar);
            renderProgress(bar);
        } catch (Exception e) {
            ModLogger.error("Erreur OpenGL lors du rendu de la barre de progression", e);
            throw new RuntimeException("Echec du rendu OpenGL de la barre de progression", e);
        } finally {
            try {
                GlStateManager.popAttrib();
                GlStateManager.popMatrix();
            } catch (Exception e) {
                ModLogger.error("Erreur lors de la restauration de l'etat OpenGL", e);
            }
        }
    }

    private void renderBackground(ProgressBar bar) {
        GuiRenderUtils.drawRect(bar.getX(), bar.getY(), bar.getX() + bar.getWidth(),
                bar.getY() + bar.getHeight(), bar.getBackgroundColor());
    }

    private void renderProgress(ProgressBar bar) {
        if (bar.getProgress() <= 0) {
            return;
        }
        try {
            int progressLength = calculateProgressLength(bar);
            if (progressLength <= 0) {
                return;
            }
            setupScissorTest(bar, progressLength);
            renderAnimatedStripes(bar, progressLength);
            renderMainProgress(bar, progressLength);
        } finally {
            try {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            } catch (Exception e) {
                ModLogger.error("Erreur lors de la desactivation du scissor test", e);
            }
        }
    }

    private int calculateProgressLength(ProgressBar bar) {
        if (bar.getOrientation() == ProgressBar.Orientation.HORIZONTAL) {
            return Math.min(bar.getWidth(), (int) (bar.getWidth() * bar.getProgress()));
        } else {
            return Math.min(bar.getHeight(), (int) (bar.getHeight() * bar.getProgress()));
        }
    }

    private void setupScissorTest(ProgressBar bar, int progressLength) {
        int x = bar.getX();
        int y = bar.getY();
        int w = (bar.getOrientation() == ProgressBar.Orientation.HORIZONTAL) ? progressLength
                : bar.getWidth();
        int h = (bar.getOrientation() == ProgressBar.Orientation.HORIZONTAL) ? bar.getHeight()
                : progressLength;
        if (bar.getOrientation() == ProgressBar.Orientation.VERTICAL) {
            y = bar.getY() + (bar.getHeight() - progressLength);
        }
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) {
                return;
            }
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            int scaleFactor = scaledResolution.getScaleFactor();

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(x * scaleFactor, mc.displayHeight - (y + h) * scaleFactor,
                    w * scaleFactor, h * scaleFactor);
        } catch (Exception e) {
            ModLogger.error("Erreur lors de la configuration du scissor test", e);
            throw new RuntimeException("Echec de la configuration du scissor test", e);
        }
    }

    private void renderAnimatedStripes(ProgressBar bar, int progressLength) {
        if (progressLength <= 0) {
            return;
        }
        int stripeWidth = bar.getStripeWidth();
        float offset = bar.getAnimationOffset();
        int x = bar.getX();
        int y = bar.getY();
        int w = (bar.getOrientation() == ProgressBar.Orientation.HORIZONTAL) ? progressLength
                : bar.getWidth();
        int h = (bar.getOrientation() == ProgressBar.Orientation.HORIZONTAL) ? bar.getHeight()
                : progressLength;
        if (w <= 0 || h <= 0) {
            ModLogger.warn("Dimensions de rayures invalides: w={}, h={}", w, h);
            return;
        }
        if (bar.getOrientation() == ProgressBar.Orientation.VERTICAL) {
            y = bar.getY() + (bar.getHeight() - progressLength);
        }
        int brightColor = bar.getProgressColor();
        for (int i =
             -stripeWidth; i < (bar.getOrientation() == ProgressBar.Orientation.HORIZONTAL ? w : h)
                + stripeWidth; i += stripeWidth) {
            try {
                if (bar.getOrientation() == ProgressBar.Orientation.HORIZONTAL) {
                    int startX = x + i + (int) offset;
                    int stripeHeight = Math.max(1, h);
                    GuiRenderUtils.drawRect(startX, y, startX + stripeWidth / 2, y + stripeHeight,
                            brightColor);
                } else {
                    int startY = y + i + (int) offset;
                    int stripeHeight = Math.max(1, w);
                    GuiRenderUtils.drawRect(x, startY, x + stripeHeight, startY + stripeWidth / 2,
                            brightColor);
                }
            } catch (Exception e) {
                ModLogger.error("Erreur lors du rendu des rayures animees", e);
            }
        }
    }

    private void renderMainProgress(ProgressBar bar, int progressLength) {
        if (progressLength <= 0) {
            return;
        }
        int x = bar.getX();
        int y = bar.getY();
        int w = (bar.getOrientation() == ProgressBar.Orientation.HORIZONTAL) ? progressLength
                : bar.getWidth();
        int h = (bar.getOrientation() == ProgressBar.Orientation.HORIZONTAL) ? bar.getHeight()
                : progressLength;
        if (w <= 0 || h <= 0) {
            ModLogger.warn("Dimensions de progression invalides: w={}, h={}", w, h);
            return;
        }
        if (bar.getOrientation() == ProgressBar.Orientation.VERTICAL) {
            y = bar.getY() + (bar.getHeight() - progressLength);
        }
        try {
            GuiRenderUtils.drawRect(x, y, x + w, y + h, bar.getProgressColor());
        } catch (Exception e) {
            ModLogger.error("Erreur lors du rendu de la barre principale", e);
            throw new RuntimeException("Echec du rendu de la barre principale", e);
        }
    }
}
