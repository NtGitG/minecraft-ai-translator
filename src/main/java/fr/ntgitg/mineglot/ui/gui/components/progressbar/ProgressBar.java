package fr.ntgitg.mineglot.ui.gui.components.progressbar;

import net.minecraft.util.MathHelper;

public class ProgressBar {

    public static final int DEFAULT_BACKGROUND_COLOR = 0xFF333333;
    public static final int DEFAULT_PROGRESS_COLOR = 0xFF00FF00;
    public static final float DEFAULT_ANIMATION_SPEED = 50f;
    public static final int DEFAULT_STRIPE_WIDTH = 30;

    public enum Orientation {
        HORIZONTAL, VERTICAL
    }

    private static final ProgressBarRenderer RENDERER = new ProgressBarRenderer();

    private final int x, y, width, height;
    private float progress;
    private final int backgroundColor, progressColor;
    private final Orientation orientation;
    private final float animationSpeed;
    private final int stripeWidth;
    private float animationOffset;

    private ProgressBar(Builder builder) {
        this.x = builder.x;
        this.y = builder.y;
        this.width = builder.width;
        this.height = builder.height;
        this.backgroundColor = builder.backgroundColor;
        this.progressColor = builder.progressColor;
        this.orientation = builder.orientation;
        this.animationSpeed = builder.animationSpeed;
        this.stripeWidth = builder.stripeWidth;
        this.progress = 0f;
        this.animationOffset = 0f;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setProgress(float progress) {
        this.progress = MathHelper.clamp_float(progress, 0f, 1f);
    }

    public void update(float deltaTime) {
        animationOffset = (animationOffset + deltaTime * animationSpeed) % (stripeWidth * 2);
    }

    public void render() {
        RENDERER.render(this);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getProgress() {
        return progress;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getProgressColor() {
        return progressColor;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public float getAnimationSpeed() {
        return animationSpeed;
    }

    public int getStripeWidth() {
        return stripeWidth;
    }

    public float getAnimationOffset() {
        return animationOffset;
    }

    public static class Builder {
        private int x, y, width, height;
        private int backgroundColor = DEFAULT_BACKGROUND_COLOR;
        private int progressColor = DEFAULT_PROGRESS_COLOR;
        private Orientation orientation = Orientation.HORIZONTAL;
        private float animationSpeed = DEFAULT_ANIMATION_SPEED;
        private int stripeWidth = DEFAULT_STRIPE_WIDTH;

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder backgroundColor(int color) {
            this.backgroundColor = color;
            return this;
        }

        public Builder progressColor(int color) {
            this.progressColor = color;
            return this;
        }

        public Builder orientation(Orientation orientation) {
            this.orientation = orientation;
            return this;
        }

        public Builder animationSpeed(float speed) {
            this.animationSpeed = speed;
            return this;
        }

        public Builder stripeWidth(int width) {
            this.stripeWidth = width;
            return this;
        }

        public ProgressBar build() {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("ProgressBar size must be > 0");
            }
            if (stripeWidth <= 0) {
                throw new IllegalArgumentException("stripeWidth must be > 0");
            }
            if (animationSpeed < 0f) {
                throw new IllegalArgumentException("animationSpeed must be >= 0");
            }
            return new ProgressBar(this);
        }
    }
}
