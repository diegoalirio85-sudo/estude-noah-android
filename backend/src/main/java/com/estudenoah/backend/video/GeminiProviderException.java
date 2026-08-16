package com.estudenoah.backend.video;

final class GeminiProviderException extends RuntimeException {
    enum Kind { CONFIGURATION, VIDEO_INACCESSIBLE, AUTHENTICATION, QUOTA, TIMEOUT, INVALID_RESPONSE, UNAVAILABLE }

    private final Kind kind;

    GeminiProviderException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    GeminiProviderException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    Kind kind() {
        return kind;
    }
}
