package com.estudenoah.backend.document;

public final class DocumentAnalysisException extends RuntimeException {
    private final Kind kind;

    public DocumentAnalysisException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public DocumentAnalysisException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() { return kind; }

    public enum Kind { CONFIGURATION, AUTHENTICATION, QUOTA, TIMEOUT, INVALID_RESPONSE, UNAVAILABLE }
}
