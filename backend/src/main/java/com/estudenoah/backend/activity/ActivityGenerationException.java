package com.estudenoah.backend.activity;

public final class ActivityGenerationException extends RuntimeException {
    public enum Kind { CONFIGURATION, AUTHENTICATION, QUOTA, TIMEOUT, INVALID_RESPONSE, UNAVAILABLE }

    private final Kind kind;

    public ActivityGenerationException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public ActivityGenerationException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
