package fr.ntgitg.mineglot.core.update;

import java.io.IOException;

@FunctionalInterface
interface LatestReleaseSource {
    ReleaseInfo fetchLatestRelease() throws IOException;
}
