package fr.ntgitg.mineglot.ui.gui.components.progressbar;

public class HoldToConfirmBar {
    private final ProgressBar bar;
    private final Runnable onConfirm;
    private final int holdDurationMs;
    private long holdStartTime = -1;
    private boolean holding = false;
    private boolean confirmed = false;

    public HoldToConfirmBar(int x, int y, int width, int height, int holdDurationMs,
                            Runnable onConfirm) {
        if (holdDurationMs <= 0) {
            throw new IllegalArgumentException("holdDurationMs must be > 0");
        }
        if (onConfirm == null) {
            throw new IllegalArgumentException("onConfirm cannot be null");
        }
        this.bar = ProgressBar.builder().position(x, y).size(width, height).backgroundColor(0x33FFFFFF)
                .progressColor(0xFFFF0000).build();
        this.holdDurationMs = holdDurationMs;
        this.onConfirm = onConfirm;
    }

    public void startHold() {
        if (!holding) {
            holding = true;
            confirmed = false;
            holdStartTime = System.currentTimeMillis();
        }
    }

    public void stopHold() {
        holding = false;
        holdStartTime = -1;
        bar.setProgress(0f);
    }

    public void update(float deltaTime) {
        if (holding && holdStartTime > 0) {
            long elapsed = System.currentTimeMillis() - holdStartTime;
            float progress = Math.min(1.0f, (float) elapsed / holdDurationMs);
            bar.setProgress(progress);
            bar.update(deltaTime);
            if (progress >= 1.0f && !confirmed) {
                confirmed = true;
                holding = false;
                onConfirm.run();
            }
        }
    }

    public void render() {
        bar.render();
    }

    public int getY() {
        return bar.getY();
    }

    public int getHeight() {
        return bar.getHeight();
    }
}
