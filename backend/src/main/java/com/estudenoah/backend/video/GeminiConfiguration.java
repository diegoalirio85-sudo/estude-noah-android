package com.estudenoah.backend.video;

import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class GeminiConfiguration {
    static final URI INTERACTIONS_ENDPOINT = URI.create("https://generativelanguage.googleapis.com/v1beta/interactions");

    @Bean
    GeminiSettings geminiSettings(Environment environment) {
        return new GeminiSettings(
                environment.getProperty("GEMINI_API_KEY", ""),
                environment.getProperty("GEMINI_MODEL", "gemini-3.6-flash")
        );
    }

    @Bean
    GeminiInteractionsClient geminiInteractionsClient(ObjectMapper mapper, GeminiSettings settings) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        String diagnosticFile = environment.getProperty("GEMINI_DIAGNOSTIC_FILE", "").trim();
        Path diagnosticPath = diagnosticFile.isEmpty() ? null : Path.of(diagnosticFile);
        return new GeminiInteractionsClient(httpClient, mapper, settings, INTERACTIONS_ENDPOINT,
                Duration.ofMinutes(3), diagnosticPath);
    }

    record GeminiSettings(String apiKey, String model) {
    }
}
