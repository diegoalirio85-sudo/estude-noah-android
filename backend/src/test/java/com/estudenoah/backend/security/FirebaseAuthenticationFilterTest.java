package com.estudenoah.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

class FirebaseAuthenticationFilterTest {
    private final FirebaseAuthTokenVerifier verifier = token -> {
        if ("allowed-token".equals(token)) return new FirebaseAuthTokenVerifier.VerifiedUser("allowed-uid", "parent@example.com");
        if ("denied-token".equals(token)) return new FirebaseAuthTokenVerifier.VerifiedUser("other-uid", "other@example.com");
        throw new FirebaseAuthVerificationException(new IllegalArgumentException());
    };

    @Test void healthIsPublic() throws Exception { mvc("allowed-uid").perform(get("/health")).andExpect(status().isOk()).andExpect(content().string("ok")); }
    @Test void missingAuthorizationIs401() throws Exception { mvc("allowed-uid").perform(post("/v1/activities/generate")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("firebase_auth_token_missing")); }
    @Test void invalidTokenIs401() throws Exception { mvc("allowed-uid").perform(post("/v1/activities/generate").header("Authorization", "Bearer invalid" )).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("firebase_auth_token_invalid")); }
    @Test void allowedUidProceeds() throws Exception { mvc("allowed-uid").perform(post("/v1/activities/generate").header("Authorization", "Bearer allowed-token")).andExpect(status().isOk()).andExpect(content().string("processed")); }
    @Test void unauthorizedUidIs403() throws Exception { mvc("allowed-uid").perform(post("/v1/activities/generate").header("Authorization", "Bearer denied-token")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("firebase_uid_not_allowed")); }
    @Test void emptyAllowlistFailsClosed() throws Exception { mvc("").perform(post("/v1/activities/generate").header("Authorization", "Bearer allowed-token")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("firebase_uid_allowlist_empty")); }
    @Test void missingProjectConfigurationFailsSafely() throws Exception {
        FirebaseAuthTokenVerifier misconfigured = token -> { throw new FirebaseAuthConfigurationException("missing project"); };
        MockMvc configured = MockMvcBuilders.standaloneSetup(new TestController()).addFilters(new FirebaseAuthenticationFilter(misconfigured, "allowed-uid", "firebase_auth", 30)).build();
        configured.perform(post("/v1/activities/generate").header("Authorization", "Bearer token"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("firebase_auth_configuration_invalid"));
    }

    private MockMvc mvc(String allowlist) {
        return MockMvcBuilders.standaloneSetup(new TestController()).addFilters(new FirebaseAuthenticationFilter(verifier, allowlist, "firebase_auth", 30)).build();
    }

    @RestController static final class TestController {
        @GetMapping("/health") String health() { return "ok"; }
        @PostMapping("/v1/activities/generate") String protectedEndpoint() { return "processed"; }
    }
}

