package com.estudenoah.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public final class FirebaseAppCheckFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Firebase-AppCheck";
    private static final Set<String> PROTECTED_POST_PATHS = Set.of(
            "/v1/materials/ppt/extract",
            "/v1/materials/youtube/analyze",
            "/v1/materials/text/analyze",
            "/v1/activities/generate",
            "/v1/activities/from-text",
            "/v1/activities/from-ppt"
    );

    private final AppCheckTokenVerifier verifier;
    private final InMemoryRequestRateLimiter rateLimiter;
    private final boolean enabled;

    public FirebaseAppCheckFilter(AppCheckTokenVerifier verifier,
                                  @Value("${security.app-check.enabled:true}") boolean enabled,
                                  @Value("${security.rate-limit.requests-per-minute:30}") int requestsPerMinute) {
        this.verifier = verifier;
        this.enabled = enabled;
        this.rateLimiter = new InMemoryRequestRateLimiter(requestsPerMinute);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equals(request.getMethod()) || !PROTECTED_POST_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader(HEADER);
        if (token == null || token.isBlank()) {
            reject(response, 401, "app_check_token_missing", "Firebase App Check token is required.");
            return;
        }
        try {
            var verified = verifier.verify(token);
            String clientKey = verified.appId() + ":" + tokenDigest(token) + ":" + request.getRemoteAddr();
            if (!rateLimiter.allow(clientKey)) {
                reject(response, 429, "rate_limit_exceeded", "Too many requests. Try again later.");
                return;
            }
            request.setAttribute(AppCheckTokenVerifier.VerifiedApp.class.getName(), verified);
            chain.doFilter(request, response);
        } catch (AppCheckVerificationException | IllegalArgumentException error) {
            reject(response, 401, "app_check_token_invalid", "Firebase App Check token is invalid.");
        }
    }

    private static String tokenDigest(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}

