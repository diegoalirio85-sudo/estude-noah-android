package com.estudenoah.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"BACKEND_AUTH_MODE=firebase_auth", "FIREBASE_PROJECT_ID=comestudenoahapp", "ALLOWED_FIREBASE_UIDS=allowed-uid"})
@AutoConfigureMockMvc
class FirebaseAuthApplicationContextTest {
    @Autowired ApplicationContext applicationContext;
    @Autowired MockMvc mockMvc;
    @MockitoBean FirebaseAuthTokenVerifier verifier;

    @Test void startsWithoutFirebaseNetworkAndKeepsHealthPublic() throws Exception {
        applicationContext.getBean(FirebaseAuthenticationFilter.class);
        mockMvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        mockMvc.perform(post("/v1/activities/generate").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("firebase_auth_token_missing"));
    }
}

