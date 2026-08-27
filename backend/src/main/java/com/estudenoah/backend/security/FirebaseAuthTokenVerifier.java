package com.estudenoah.backend.security;

public interface FirebaseAuthTokenVerifier {
    VerifiedUser verify(String idToken);

    record VerifiedUser(String uid, String email) {}
}

