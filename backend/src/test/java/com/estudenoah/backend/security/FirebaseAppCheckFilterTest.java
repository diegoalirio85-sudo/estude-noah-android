package com.estudenoah.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

class FirebaseAppCheckFilterTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AppCheckTokenVerifier fake = token -> {
            if (!"valid-fake-token".equals(token)) throw new AppCheckVerificationException(new IllegalArgumentException());
            return new AppCheckTokenVerifier.VerifiedApp("fake-android-app");
        };
        var filter = new FirebaseAppCheckFilter(fake, true, "app_check", 30);
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController()).addFilters(filter).build();
    }

    @Test
    void healthRemainsPublic() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void protectedEndpointRejectsMissingTokenWithoutLeakingSecrets() throws Exception {
        mockMvc.perform(post("/v1/activities/generate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("app_check_token_missing"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("GEMINI_API_KEY"))));
    }

    @Test
    void protectedEndpointRejectsInvalidToken() throws Exception {
        mockMvc.perform(post("/v1/activities/generate")
                        .header(FirebaseAppCheckFilter.HEADER, "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("app_check_token_invalid"));
    }

    @Test
    void protectedEndpointAllowsValidFakeTokenToProceed() throws Exception {
        mockMvc.perform(post("/v1/activities/generate")
                        .header(FirebaseAppCheckFilter.HEADER, "valid-fake-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("processed"));
    }

    @Test
    void rateLimiterRejectsRequestsBeyondConfiguredWindow() throws Exception {
        AppCheckTokenVerifier fake = token -> new AppCheckTokenVerifier.VerifiedApp("fake-android-app");
        MockMvc limited = MockMvcBuilders.standaloneSetup(new TestController())
                .addFilters(new FirebaseAppCheckFilter(fake, true, "app_check", 1)).build();

        limited.perform(post("/v1/activities/generate").header(FirebaseAppCheckFilter.HEADER, "valid-fake-token"))
                .andExpect(status().isOk());
        limited.perform(post("/v1/activities/generate").header(FirebaseAppCheckFilter.HEADER, "valid-fake-token"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("rate_limit_exceeded"));
    }

    @RestController
    static final class TestController {
        @GetMapping("/health")
        String health() {
            return "ok";
        }

        @PostMapping("/v1/activities/generate")
        String protectedEndpoint() {
            return "processed";
        }
    }
}

