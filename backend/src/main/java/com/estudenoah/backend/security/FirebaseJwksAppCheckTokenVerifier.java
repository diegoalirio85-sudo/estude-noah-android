package com.estudenoah.backend.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class FirebaseJwksAppCheckTokenVerifier implements AppCheckTokenVerifier {
    private static final URI JWKS_URI = URI.create("https://firebaseappcheck.googleapis.com/v1/jwks");
    private static final long MAX_CACHE_MILLIS = Duration.ofHours(6).toMillis();
    private final String projectNumber;
    private final Set<String> allowedAppIds;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private volatile CachedKeys cachedKeys = new CachedKeys(Map.of(), 0);

    public FirebaseJwksAppCheckTokenVerifier(
            @Value("${firebase.project-number:${FIREBASE_PROJECT_NUMBER:}}") String projectNumber,
            @Value("${firebase.allowed-app-ids:${FIREBASE_APP_IDS:}}") String allowedAppIds,
            ObjectMapper objectMapper) {
        this(projectNumber, parseAllowedIds(allowedAppIds), HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build(), objectMapper, Clock.systemUTC());
    }

    FirebaseJwksAppCheckTokenVerifier(String projectNumber, Set<String> allowedAppIds, HttpClient httpClient,
                                      ObjectMapper objectMapper, Clock clock) {
        this.projectNumber = projectNumber == null ? "" : projectNumber.trim();
        this.allowedAppIds = Set.copyOf(allowedAppIds);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public VerifiedApp verify(String token) {
        try {
            if (projectNumber.isBlank()) throw new IllegalStateException("FIREBASE_PROJECT_NUMBER is not configured.");
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) throw new IllegalArgumentException("Malformed token.");
            JsonNode header = decode(parts[0]);
            JsonNode payload = decode(parts[1]);
            if (!"RS256".equals(header.path("alg").asText()) || !"JWT".equals(header.path("typ").asText()))
                throw new IllegalArgumentException("Unexpected JWT header.");
            String keyId = requiredText(header, "kid");
            RSAPublicKey key = keys().get(keyId);
            if (key == null) {
                cachedKeys = new CachedKeys(Map.of(), 0);
                key = keys().get(keyId);
            }
            if (key == null || !validSignature(key, parts)) throw new IllegalArgumentException("Invalid signature.");
            long now = clock.instant().getEpochSecond();
            if (payload.path("exp").asLong(0) <= now || payload.path("iat").asLong(Long.MAX_VALUE) > now + 60)
                throw new IllegalArgumentException("Expired or future token.");
            String expectedIssuer = "https://firebaseappcheck.googleapis.com/" + projectNumber;
            if (!expectedIssuer.equals(payload.path("iss").asText())) throw new IllegalArgumentException("Invalid issuer.");
            boolean expectedAudience = false;
            for (JsonNode audience : payload.path("aud")) {
                if (("projects/" + projectNumber).equals(audience.asText())) expectedAudience = true;
            }
            if (!expectedAudience) throw new IllegalArgumentException("Invalid audience.");
            String appId = requiredText(payload, "sub");
            if (!allowedAppIds.isEmpty() && !allowedAppIds.contains(appId))
                throw new IllegalArgumentException("App is not allowed.");
            return new VerifiedApp(appId);
        } catch (AppCheckVerificationException error) {
            throw error;
        } catch (Exception error) {
            throw new AppCheckVerificationException(error);
        }
    }

    private JsonNode decode(String value) throws Exception {
        return objectMapper.readTree(Base64.getUrlDecoder().decode(value));
    }

    private Map<String, RSAPublicKey> keys() throws Exception {
        CachedKeys current = cachedKeys;
        if (current.expiresAtMillis > clock.millis() && !current.keys.isEmpty()) return current.keys;
        synchronized (this) {
            current = cachedKeys;
            if (current.expiresAtMillis > clock.millis() && !current.keys.isEmpty()) return current.keys;
            HttpRequest request = HttpRequest.newBuilder(JWKS_URI).timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) throw new IllegalStateException("Firebase JWKS endpoint unavailable.");
            Map<String, RSAPublicKey> parsed = parseKeys(objectMapper.readTree(response.body()));
            cachedKeys = new CachedKeys(parsed, clock.millis() + MAX_CACHE_MILLIS);
            return parsed;
        }
    }

    private static Map<String, RSAPublicKey> parseKeys(JsonNode root) throws Exception {
        Map<String, RSAPublicKey> result = new HashMap<>();
        KeyFactory factory = KeyFactory.getInstance("RSA");
        for (JsonNode item : root.path("keys")) {
            if (!"RSA".equals(item.path("kty").asText()) || !"RS256".equals(item.path("alg").asText())) continue;
            String keyId = requiredText(item, "kid");
            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(requiredText(item, "n")));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(requiredText(item, "e")));
            result.put(keyId, (RSAPublicKey) factory.generatePublic(new RSAPublicKeySpec(modulus, exponent)));
        }
        if (result.isEmpty()) throw new IllegalStateException("Firebase JWKS response contains no usable keys.");
        return Collections.unmodifiableMap(result);
    }

    private static boolean validSignature(RSAPublicKey key, String[] parts) throws Exception {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(key);
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        return verifier.verify(Base64.getUrlDecoder().decode(parts[2]));
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value.isBlank()) throw new IllegalArgumentException("Missing JWT field.");
        return value;
    }

    private static Set<String> parseAllowedIds(String value) {
        if (value == null || value.isBlank()) return Set.of();
        Set<String> result = new HashSet<>();
        for (String item : value.split(",")) if (!item.isBlank()) result.add(item.trim());
        return result;
    }

    private record CachedKeys(Map<String, RSAPublicKey> keys, long expiresAtMillis) {
    }
}

