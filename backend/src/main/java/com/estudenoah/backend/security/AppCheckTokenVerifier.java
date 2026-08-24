package com.estudenoah.backend.security;

public interface AppCheckTokenVerifier {
    VerifiedApp verify(String token);

    record VerifiedApp(String appId) {
    }
}

