package com.estudenoah.backend.security;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Component;

@Component
public final class FirebaseAdminAuthTokenVerifier implements FirebaseAuthTokenVerifier {
    private volatile FirebaseAuth auth;

    @Override
    public VerifiedUser verify(String idToken) {
        try {
            var decoded = auth().verifyIdToken(idToken);
            return new VerifiedUser(decoded.getUid(), decoded.getEmail());
        } catch (FirebaseAuthException | IllegalArgumentException error) {
            throw new FirebaseAuthVerificationException(error);
        }
    }

    private FirebaseAuth auth() {
        var current = auth;
        if (current != null) return current;
        synchronized (this) {
            if (auth == null) {
                FirebaseApp app = FirebaseApp.getApps().stream().findFirst().orElseGet(FirebaseApp::initializeApp);
                auth = FirebaseAuth.getInstance(app);
            }
            return auth;
        }
    }
}

