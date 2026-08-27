package com.estudenoah.backend.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class FirebaseAdminAuthTokenVerifier implements FirebaseAuthTokenVerifier {
    private static final String APP_NAME = "estude-noah-firebase-auth";
    private final String projectId;
    private final CredentialsProvider credentialsProvider;
    private final String appName;
    private volatile FirebaseAuth auth;

    @Autowired
    public FirebaseAdminAuthTokenVerifier(@Value("${firebase.project-id:}") String projectId) {
        this(projectId, GoogleCredentials::getApplicationDefault, APP_NAME);
    }

    FirebaseAdminAuthTokenVerifier(String projectId, CredentialsProvider credentialsProvider, String appName) {
        this.projectId = projectId == null ? "" : projectId.trim();
        this.credentialsProvider = credentialsProvider;
        this.appName = appName;
    }

    @Override
    public VerifiedUser verify(String idToken) {
        try {
            var decoded = auth().verifyIdToken(idToken);
            return new VerifiedUser(decoded.getUid(), decoded.getEmail());
        } catch (FirebaseAuthConfigurationException error) {
            throw error;
        } catch (FirebaseAuthException | IllegalArgumentException error) {
            throw new FirebaseAuthVerificationException(error);
        }
    }

    FirebaseAuth auth() {
        var current = auth;
        if (current != null) return current;
        synchronized (this) {
            if (auth == null) {
                auth = FirebaseAuth.getInstance(firebaseApp());
            }
            return auth;
        }
    }

    FirebaseApp firebaseApp() {
        if (projectId.isBlank()) {
            throw new FirebaseAuthConfigurationException("FIREBASE_PROJECT_ID is required in firebase_auth mode.");
        }
        return FirebaseApp.getApps().stream().filter(app -> appName.equals(app.getName())).findFirst().orElseGet(() -> {
            try {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentialsProvider.get())
                        .setProjectId(projectId)
                        .build();
                return FirebaseApp.initializeApp(options, appName);
            } catch (IOException error) {
                throw new FirebaseAuthConfigurationException("Application Default Credentials are unavailable.", error);
            }
        });
    }

    @FunctionalInterface
    interface CredentialsProvider {
        GoogleCredentials get() throws IOException;
    }
}

