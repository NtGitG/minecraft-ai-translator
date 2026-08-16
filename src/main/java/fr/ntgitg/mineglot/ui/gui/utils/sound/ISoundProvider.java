package fr.ntgitg.mineglot.ui.gui.utils.sound;

public interface ISoundProvider {

    void playClick(float volume);

    void playHover(float volume);

    void playSuccess(float volume);

    void playError(float volume);
}
