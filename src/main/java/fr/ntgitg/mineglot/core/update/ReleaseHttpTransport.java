package fr.ntgitg.mineglot.core.update;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@FunctionalInterface
interface ReleaseHttpTransport {
    ReleaseHttpResponse get(URI uri, Map<String, String> headers) throws IOException;
}
