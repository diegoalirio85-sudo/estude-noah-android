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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "APP_CHECK_ENABLED=true",
        "FIREBASE_PROJECT_NUMBER=1048661265103",
        "FIREBASE_APP_IDS=1:1048661265103:android:78e26aa9cc07930e4c85ba"
})
@AutoConfigureMockMvc
class AppCheckApplicationContextTest {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void startsWithProductionAppCheckConfigurationWithoutFetchingJwks() throws Exception {
        applicationContext.getBean(FirebaseJwksAppCheckTokenVerifier.class);
        applicationContext.getBean(FirebaseAppCheckFilter.class);

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        mockMvc.perform(post("/v1/activities/generate")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("app_check_token_missing"));
    }
}

