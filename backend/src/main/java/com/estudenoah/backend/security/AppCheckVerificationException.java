package com.estudenoah.backend.security;

public final class AppCheckVerificationException extends RuntimeException {
    public AppCheckVerificationException(Throwable cause) {
        super("Firebase App Check token verification failed.", cause);
    }
}

