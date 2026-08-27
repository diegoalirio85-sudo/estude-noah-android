package com.estudenoah.backend.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FirebaseAdminAuthTokenVerifierTest {
    @Test
    void createsFirebaseAppWithExplicitProductionProjectIdWithoutNetwork() {
        String appName = "firebase-auth-test-" + UUID.randomUUID();
        GoogleCredentials fakeCredentials = GoogleCredentials.create(new AccessToken("fake-token", new Date(System.currentTimeMillis() + 60_000)));
        var verifier = new FirebaseAdminAuthTokenVerifier("comestudenoahapp", () -> fakeCredentials, appName);
        var app = verifier.firebaseApp();
        try {
            assertEquals("comestudenoahapp", app.getOptions().getProjectId());
            assertDoesNotThrow(verifier::auth);
        } finally {
            app.delete();
        }
    }

    @Test
    void missingProjectIdFailsClosedBeforeTokenVerification() {
        var verifier = new FirebaseAdminAuthTokenVerifier(" ", GoogleCredentials::getApplicationDefault, "missing-project-test");
        var error = assertThrows(FirebaseAuthConfigurationException.class, verifier::auth);
        assertEquals("FIREBASE_PROJECT_ID is required in firebase_auth mode.", error.getMessage());
    }
}

