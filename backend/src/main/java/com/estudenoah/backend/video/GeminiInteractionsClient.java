package com.estudenoah.backend.video;

import com.estudenoah.backend.video.GeminiConfiguration.GeminiSettings;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class GeminiInteractionsClient {
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final GeminiSettings settings;
    private final URI endpoint;
    private final Duration requestTimeout;
    private final JsonNode responseSchema;

    GeminiInteractionsClient(HttpClient httpClient, ObjectMapper mapper, GeminiSettings settings,
                             URI endpoint, Duration requestTimeout) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.settings = settings;
        this.endpoint = endpoint;
        this.requestTimeout = requestTimeout;
        try (InputStream input = getClass().getResourceAsStream("/youtube-analysis-schema.json")) {
            if (input == null) throw new IOException("schema resource missing");
            this.responseSchema = mapper.readTree(input);
        } catch (IOException error) {
            throw new IllegalStateException("Não foi possível carregar o schema de análise.", error);
        }
    }

    VideoAnalysis analyze(URI youtubeUrl) {
        if (settings.apiKey().isBlank()) {
            throw new GeminiProviderException(GeminiProviderException.Kind.CONFIGURATION,
                    "A análise de vídeo não está configurada.");
        }
        HttpRequest request = request(youtubeUrl);
        HttpResponse<String> response = send(request);
        if (isTransient(response.statusCode())) {
            response = send(request);
        }
        if (response.statusCode() / 100 != 2) {
            throw providerError(response.statusCode());
        }
        return parse(response.body(), youtubeUrl);
    }

    private HttpRequest request(URI youtubeUrl) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", settings.model());
        root.put("store", false);
        ArrayNode input = root.putArray("input");
        input.addObject().put("type", "video").put("uri", youtubeUrl.toString());
        input.addObject().put("type", "text").put("text", YoutubePedagogicalAnalysisPrompt.text());
        root.putObject("generation_config").put("temperature", 0.1).put("max_output_tokens", 8192);
        ObjectNode format = root.putObject("response_format");
        format.put("type", "text").put("mime_type", "application/json").set("schema", responseSchema);

        return HttpRequest.newBuilder(endpoint)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", settings.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                .build();
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException error) {
            throw new GeminiProviderException(GeminiProviderException.Kind.TIMEOUT,
                    "A análise do vídeo excedeu o tempo limite.", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new GeminiProviderException(GeminiProviderException.Kind.UNAVAILABLE,
                    "O provedor de análise está indisponível.", error);
        } catch (IOException error) {
            throw new GeminiProviderException(GeminiProviderException.Kind.UNAVAILABLE,
                    "O provedor de análise está indisponível.", error);
        }
    }

    private VideoAnalysis parse(String body, URI normalizedUrl) {
        try {
            JsonNode interaction = mapper.readTree(body);
            if (!"completed".equals(interaction.path("status").asText())) throw invalidResponse();
            String output = null;
            for (JsonNode step : interaction.path("steps")) {
                if ("model_output".equals(step.path("type").asText())) {
                    for (JsonNode content : step.path("content")) {
                        if ("text".equals(content.path("type").asText())) output = content.path("text").asText(null);
                    }
                }
            }
            if (output == null || output.isBlank()) throw invalidResponse();
            VideoAnalysis result = mapper.readValue(output, VideoAnalysis.class);
            validate(result, normalizedUrl);
            return result;
        } catch (GeminiProviderException error) {
            throw error;
        } catch (RuntimeException error) {
            throw new GeminiProviderException(GeminiProviderException.Kind.INVALID_RESPONSE,
                    "O provedor retornou uma análise inválida.", error);
        }
    }

    private static void validate(VideoAnalysis result, URI normalizedUrl) {
        if (result == null || !"youtube".equals(result.sourceType())
                || !normalizedUrl.toString().equals(result.sourceUrl())
                || blank(result.videoTitle()) || blank(result.subject()) || blank(result.summary())
                || result.themes() == null || result.themes().isEmpty() || result.warnings() == null) {
            throw invalidResponse();
        }
        for (VideoAnalysis.Theme theme : result.themes()) {
            if (theme == null || blank(theme.name()) || anyNull(theme.learningObjectives(), theme.concepts(),
                    theme.relationships(), theme.likelyMisconceptions(), theme.evidence())) throw invalidResponse();
            for (VideoAnalysis.Evidence evidence : theme.evidence()) {
                if (evidence == null || blank(evidence.description()) || blank(evidence.timestamp())) throw invalidResponse();
            }
        }
    }

    private static boolean anyNull(Object... values) {
        for (Object value : values) if (value == null) return true;
        return false;
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static boolean isTransient(int status) { return status == 429 || status == 500 || status == 502 || status == 503 || status == 504; }
    private static GeminiProviderException invalidResponse() {
        return new GeminiProviderException(GeminiProviderException.Kind.INVALID_RESPONSE,
                "O provedor retornou uma análise inválida.");
    }

    private static GeminiProviderException providerError(int status) {
        GeminiProviderException.Kind kind = switch (status) {
            case 400, 404, 422 -> GeminiProviderException.Kind.VIDEO_INACCESSIBLE;
            case 401, 403 -> GeminiProviderException.Kind.AUTHENTICATION;
            case 408, 504 -> GeminiProviderException.Kind.TIMEOUT;
            case 429 -> GeminiProviderException.Kind.QUOTA;
            default -> GeminiProviderException.Kind.UNAVAILABLE;
        };
        return new GeminiProviderException(kind, "Não foi possível analisar o vídeo no provedor.");
    }
}
