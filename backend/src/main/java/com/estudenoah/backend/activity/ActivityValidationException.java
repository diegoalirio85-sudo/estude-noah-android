package com.estudenoah.backend.activity;

public final class ActivityValidationException extends RuntimeException {
    public enum Kind { INVALID_INPUT, INSUFFICIENT_CONTENT, INVALID_RESPONSE }

    private final Kind kind;

    ActivityValidationException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
