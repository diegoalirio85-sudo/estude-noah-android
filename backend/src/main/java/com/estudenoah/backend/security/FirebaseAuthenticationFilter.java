package com.estudenoah.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public final class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    public static final String AUTHORIZATION = "Authorization";
    private static final Set<String> PROTECTED_POST_PATHS = Set.of(
            "/v1/materials/ppt/extract", "/v1/materials/youtube/analyze", "/v1/materials/text/analyze",
            "/v1/activities/generate", "/v1/activities/from-text", "/v1/activities/from-ppt");

    private final FirebaseAuthTokenVerifier verifier;
    private final Set<String> allowedUids;
    private final String authMode;
    private final InMemoryRequestRateLimiter rateLimiter;

    public FirebaseAuthenticationFilter(FirebaseAuthTokenVerifier verifier,
            @Value("${security.firebase.allowed-uids:}") String allowedUids,
            @Value("${security.auth.mode:firebase_auth}") String authMode,
            @Value("${security.rate-limit.requests-per-minute:30}") int requestsPerMinute) {
        this.verifier = verifier;
        this.allowedUids = Arrays.stream(allowedUids.split(",")).map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toUnmodifiableSet());
        this.authMode = authMode;
        this.rateLimiter = new InMemoryRequestRateLimiter(requestsPerMinute);
    }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"firebase_auth".equals(authMode) || !"POST".equals(request.getMethod()) || !PROTECTED_POST_PATHS.contains(request.getRequestURI());
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (allowedUids.isEmpty()) { reject(response, 503, "firebase_uid_allowlist_empty", "Firebase UID allowlist is not configured."); return; }
        String header = request.getHeader(AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ") || header.substring(7).isBlank()) { reject(response, 401, "firebase_auth_token_missing", "Firebase ID token is required."); return; }
        try {
            var user = verifier.verify(header.substring(7));
            if (!allowedUids.contains(user.uid())) { reject(response, 403, "firebase_uid_not_allowed", "Firebase user is not authorized."); return; }
            if (!rateLimiter.allow(user.uid() + ":" + request.getRemoteAddr())) { reject(response, 429, "rate_limit_exceeded", "Too many requests. Try again later."); return; }
            request.setAttribute(FirebaseAuthTokenVerifier.VerifiedUser.class.getName(), user);
            chain.doFilter(request, response);
        } catch (FirebaseAuthConfigurationException error) {
            reject(response, 503, "firebase_auth_configuration_invalid", "Firebase Authentication is not configured.");
        } catch (FirebaseAuthVerificationException | IllegalArgumentException error) {
            reject(response, 401, "firebase_auth_token_invalid", "Firebase ID token is invalid.");
        }
    }

    private static void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status); response.setContentType("application/json"); response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}

