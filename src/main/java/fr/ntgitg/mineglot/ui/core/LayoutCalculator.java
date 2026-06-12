package fr.ntgitg.mineglot.ui.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;

public final class LayoutCalculator {

    private LayoutCalculator() {
    }

    public static class ResolutionInfo {
        private final ScaledResolution resolution;
        private final int width;
        private final int height;
        private final int centerX;
        private final int centerY;

        private ResolutionInfo(ScaledResolution resolution) {
            this.resolution = resolution;
            this.width = resolution.getScaledWidth();
            this.height = resolution.getScaledHeight();
            this.centerX = width / 2;
            this.centerY = height / 2;
        }

        public ScaledResolution getResolution() {
            return resolution;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getCenterX() {
            return centerX;
        }

        public int getCenterY() {
            return centerY;
        }

        public int[] getCenter() {
            return new int[]{centerX, centerY};
        }
    }

    public static class BoundsInfo {
        private final int minX;
        private final int minY;
        private final int maxX;
        private final int maxY;

        public BoundsInfo(int minX, int minY, int maxX, int maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        public int getMinX() {
            return minX;
        }

        public int getMinY() {
            return minY;
        }

        public int getMaxX() {
            return maxX;
        }

        public int getMaxY() {
            return maxY;
        }

        public Point getMaxPoint() {
            return new Point(maxX, maxY);
        }
    }

    public static class ClampedPosition {
        private final int x;
        private final int y;
        private final boolean wasClamped;

        public ClampedPosition(int x, int y, boolean wasClamped) {
            this.x = x;
            this.y = y;
            this.wasClamped = wasClamped;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public boolean wasClamped() {
            return wasClamped;
        }
    }

    public static ResolutionInfo getResolutionInfo() {
        return new ResolutionInfo(new ScaledResolution(Minecraft.getMinecraft()));
    }

    public static ResolutionInfo getResolutionInfo(ScaledResolution resolution) {
        return new ResolutionInfo(resolution);
    }

    public static int[] calculateScreenCenter() {
        ResolutionInfo info = getResolutionInfo();
        return info.getCenter();
    }

    public static int[] calculateCenter(ScaledResolution resolution) {
        ResolutionInfo info = getResolutionInfo(resolution);
        return info.getCenter();
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static ClampedPosition clampPosition(int x, int y, BoundsInfo bounds) {
        int clampedX = clamp(x, bounds.getMinX(), bounds.getMaxX());
        int clampedY = clamp(y, bounds.getMinY(), bounds.getMaxY());
        boolean wasClamped = (clampedX != x) || (clampedY != y);
        return new ClampedPosition(clampedX, clampedY, wasClamped);
    }

    public static BoundsInfo calculateBounds(int elementWidth, int elementHeight) {
        ResolutionInfo info = getResolutionInfo();
        return calculateBounds(elementWidth, elementHeight, info);
    }

    public static BoundsInfo calculateBounds(int elementWidth, int elementHeight,
                                             ResolutionInfo info) {
        int maxX = info.getWidth() - elementWidth;
        int maxY = info.getHeight() - elementHeight;
        return new BoundsInfo(0, 0, Math.max(0, maxX), Math.max(0, maxY));
    }

    public static int getBackButtonY(int centerY, int guiHeight, int buttonHeight) {
        return centerY + guiHeight / 2 - buttonHeight - 15;
    }

    public static int getListStartY(int centerY, int guiHeight) {
        return centerY - guiHeight / 2 + 70;
    }

    public static int getSearchFieldY(int centerY, int guiHeight) {
        return centerY - guiHeight / 2 + 40;
    }

    public static int getListItemY(int startY, int index, int itemHeight, int spacing) {
        return startY + index * (itemHeight + spacing);
    }

    public static int calculateVerticalCenter(int containerY, int containerHeight,
                                              int elementHeight) {
        return containerY + (containerHeight - elementHeight) / 2;
    }

    public static int calculateHorizontalStart(int containerX, int paddingLeft) {
        return containerX + paddingLeft;
    }

    public static int calculateAvailableSpace(int totalSpace, int paddingStart, int paddingEnd) {
        return totalSpace - paddingStart - paddingEnd;
    }
}
