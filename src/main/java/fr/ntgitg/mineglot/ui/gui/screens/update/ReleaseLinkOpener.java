package fr.ntgitg.mineglot.ui.gui.screens.update;

import java.net.URI;

@FunctionalInterface
interface ReleaseLinkOpener {
    boolean open(URI releasePageUri);
}
