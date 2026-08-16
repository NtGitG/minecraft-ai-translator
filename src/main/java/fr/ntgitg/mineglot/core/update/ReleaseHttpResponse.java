package fr.ntgitg.mineglot.core.update;

final class ReleaseHttpResponse {
    private final int statusCode;
    private final String body;

    ReleaseHttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body == null ? "" : body;
    }

    int getStatusCode() {
        return statusCode;
    }

    String getBody() {
        return body;
    }
}
