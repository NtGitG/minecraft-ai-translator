package fr.ntgitg.mineglot.ui.gui.utils.sound;

public class SoundManagerAdapter implements ISoundProvider {

    @Override
    public void playClick(float volume) {
        SoundManager.playClick();
    }

    @Override
    public void playHover(float volume) {
        SoundManager.playHover();
    }

    @Override
    public void playSuccess(float volume) {
        SoundManager.playSuccess();
    }

    @Override
    public void playError(float volume) {
        SoundManager.playError();
    }
}
