package com.estudenoah.backend.video;

import com.estudenoah.backend.activity.ActivityGenerationException;
import com.estudenoah.backend.activity.ActivityGenerationRequest;
import com.estudenoah.backend.activity.GeneratedActivity;
import com.estudenoah.backend.activity.PedagogicalActivityPrompt;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GeminiInteractionsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiInteractionsClient.class);
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final GeminiSettings settings;
    private final URI endpoint;
    private final Duration requestTimeout;
    private final JsonNode responseSchema;
    private final JsonNode activitySchema;
    private final Path diagnosticPath;

    GeminiInteractionsClient(HttpClient httpClient, ObjectMapper mapper, GeminiSettings settings,
                             URI endpoint, Duration requestTimeout) {
        this(httpClient, mapper, settings, endpoint, requestTimeout, null);
    }

    GeminiInteractionsClient(HttpClient httpClient, ObjectMapper mapper, GeminiSettings settings,
                             URI endpoint, Duration requestTimeout, Path diagnosticPath) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.settings = settings;
        this.endpoint = endpoint;
        this.requestTimeout = requestTimeout;
        this.diagnosticPath = diagnosticPath;
        try (InputStream input = getClass().getResourceAsStream("/youtube-analysis-schema.json")) {
            if (input == null) throw new IOException("schema resource missing");
            this.responseSchema = mapper.readTree(input);
        } catch (IOException error) {
            throw new IllegalStateException("Não foi possível carregar o schema de análise.", error);
        }
        try (InputStream input = getClass().getResourceAsStream("/activity-generation-schema.json")) {
            if (input == null) throw new IOException("activity schema resource missing");
            this.activitySchema = mapper.readTree(input);
        } catch (IOException error) {
            throw new IllegalStateException("Não foi possível carregar o schema de atividades.", error);
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
            writeDiagnostic(response, "provider_http_error");
            throw providerError(response.statusCode());
        }
        return parse(response, youtubeUrl);
    }

    public GeneratedActivity generateActivity(ActivityGenerationRequest generationRequest) {
        if (settings.apiKey().isBlank()) {
            throw new ActivityGenerationException(ActivityGenerationException.Kind.CONFIGURATION,
                    "A geração de atividades não está configurada.");
        }
        HttpRequest request = activityRequest(generationRequest);
        HttpResponse<String> response;
        try {
            response = send(request);
            if (isTransient(response.statusCode())) response = send(request);
        } catch (GeminiProviderException error) {
            throw activityError(error);
        }
        if (response.statusCode() / 100 != 2) throw activityProviderError(response.statusCode());
        try {
            String output = interactionOutput(response.body());
            if (output == null || output.isBlank()) throw new IllegalArgumentException("missing output");
            return mapper.readValue(output, GeneratedActivity.class);
        } catch (RuntimeException error) {
            throw new ActivityGenerationException(ActivityGenerationException.Kind.INVALID_RESPONSE,
                    "O provedor retornou uma atividade inválida.", error);
        }
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

    private HttpRequest activityRequest(ActivityGenerationRequest generationRequest) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", settings.model());
        root.put("store", false);
        root.putArray("input").addObject().put("type", "text")
                .put("text", PedagogicalActivityPrompt.text(generationRequest,
                        mapper.writeValueAsString(generationRequest)));
        root.putObject("generation_config").put("temperature", 0.2).put("max_output_tokens", 16384);
        root.putObject("response_format").put("type", "text").put("mime_type", "application/json")
                .set("schema", activitySchema);
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

    private VideoAnalysis parse(HttpResponse<String> response, URI normalizedUrl) {
        try {
            JsonNode interaction = mapper.readTree(response.body());
            String status = interaction.path("status").asText("");
            if (!"completed".equals(status)) throw invalidResponse("interaction_status_" + safeToken(status));
            StringBuilder output = new StringBuilder();
            boolean foundModelOutput = false;
            for (JsonNode step : interaction.path("steps")) {
                if ("model_output".equals(step.path("type").asText())) {
                    if (foundModelOutput) output.setLength(0);
                    foundModelOutput = true;
                    for (JsonNode content : step.path("content")) {
                        if ("text".equals(content.path("type").asText()) && content.path("text").isTextual()) {
                            output.append(content.path("text").asText());
                        }
                    }
                }
            }
            if (!foundModelOutput) throw invalidResponse("model_output_absent");
            if (output.toString().isBlank()) throw invalidResponse("output_text_empty");
            VideoAnalysis providerResult;
            try {
                providerResult = mapper.readValue(output.toString(), VideoAnalysis.class);
            } catch (RuntimeException error) {
                throw invalidResponse("analysis_json_deserialization_" + error.getClass().getSimpleName());
            }
            VideoAnalysis result = withAuthoritativeSource(providerResult, normalizedUrl);
            validate(result, normalizedUrl);
            return result;
        } catch (GeminiProviderException error) {
            writeDiagnostic(response, error.getMessage());
            throw error;
        } catch (RuntimeException error) {
            GeminiProviderException invalid = invalidResponse("interaction_json_" + error.getClass().getSimpleName());
            writeDiagnostic(response, invalid.getMessage());
            throw invalid;
        }
    }

    private String interactionOutput(String body) {
        JsonNode interaction = mapper.readTree(body);
        if (!"completed".equals(interaction.path("status").asText())) return null;
        StringBuilder output = new StringBuilder();
        boolean foundModelOutput = false;
        for (JsonNode step : interaction.path("steps")) {
            if (!"model_output".equals(step.path("type").asText())) continue;
            if (foundModelOutput) output.setLength(0);
            foundModelOutput = true;
            for (JsonNode content : step.path("content")) {
                if ("text".equals(content.path("type").asText()) && content.path("text").isTextual()) {
                    output.append(content.path("text").asText());
                }
            }
        }
        return foundModelOutput ? output.toString() : null;
    }

    private static void validate(VideoAnalysis result, URI normalizedUrl) {
        if (result == null) throw invalidResponse("analysis_null");
        if (!"youtube".equals(result.sourceType())) throw invalidResponse("analysis_source_type");
        if (!normalizedUrl.toString().equals(result.sourceUrl())) throw invalidResponse("analysis_source_url");
        if (blank(result.videoTitle())) throw invalidResponse("analysis_video_title");
        if (blank(result.subject())) throw invalidResponse("analysis_subject");
        if (blank(result.summary())) throw invalidResponse("analysis_summary");
        if (result.themes() == null || result.themes().isEmpty()) throw invalidResponse("analysis_themes");
        if (result.warnings() == null) throw invalidResponse("analysis_warnings");
        for (VideoAnalysis.Theme theme : result.themes()) {
            if (theme == null) throw invalidResponse("theme_null");
            if (blank(theme.name())) throw invalidResponse("theme_name");
            if (theme.learningObjectives() == null) throw invalidResponse("theme_learning_objectives");
            if (theme.concepts() == null) throw invalidResponse("theme_concepts");
            if (theme.relationships() == null) throw invalidResponse("theme_relationships");
            if (theme.likelyMisconceptions() == null) throw invalidResponse("theme_likely_misconceptions");
            if (theme.evidence() == null) throw invalidResponse("theme_evidence");
            for (VideoAnalysis.Evidence evidence : theme.evidence()) {
                if (evidence == null) throw invalidResponse("evidence_null");
                if (blank(evidence.description())) throw invalidResponse("evidence_description");
                if (blank(evidence.timestamp())) throw invalidResponse("evidence_timestamp");
            }
        }
    }

    private static VideoAnalysis withAuthoritativeSource(VideoAnalysis result, URI normalizedUrl) {
        if (result == null) return null;
        return new VideoAnalysis("youtube", normalizedUrl.toString(), result.videoTitle(), result.subject(),
                result.summary(), result.themes(), result.warnings());
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static boolean isTransient(int status) { return status == 429 || status == 500 || status == 502 || status == 503 || status == 504; }
    private static GeminiProviderException invalidResponse(String reason) {
        return new GeminiProviderException(GeminiProviderException.Kind.INVALID_RESPONSE,
                reason);
    }

    private void writeDiagnostic(HttpResponse<String> response, String reason) {
        if (diagnosticPath == null) return;
        try {
            JsonNode interaction = safeTree(response.body());
            ObjectNode diagnostic = mapper.createObjectNode();
            diagnostic.put("providerHttpStatus", response.statusCode());
            diagnostic.put("model", settings.model());
            diagnostic.put("providerRequestId", requestId(response, interaction));
            diagnostic.put("interactionStatus", interaction.path("status").asText("absent"));
            diagnostic.put("statusReason", statusReason(interaction));
            ArrayNode fields = diagnostic.putArray("topLevelFields");
            if (interaction.isObject()) interaction.propertyStream().map(java.util.Map.Entry::getKey).sorted().forEach(fields::add);
            OutputMetadata output = outputMetadata(interaction);
            diagnostic.put("modelOutputPresent", output.modelOutputPresent());
            diagnostic.put("textBlockCount", output.textBlockCount());
            diagnostic.put("outputTextPresent", output.textLength() > 0);
            diagnostic.put("outputTextLength", output.textLength());
            diagnostic.put("failureReason", safeReason(reason));
            Path parent = diagnosticPath.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            mapper.writerWithDefaultPrettyPrinter().writeValue(diagnosticPath.toFile(), diagnostic);
            LOGGER.warn("Gemini response rejected: status={}, model={}, requestId={}, reason={}",
                    response.statusCode(), settings.model(), diagnostic.path("providerRequestId").asText(), safeReason(reason));
        } catch (RuntimeException | IOException diagnosticError) {
            LOGGER.warn("Could not write sanitized Gemini diagnostic: {}", diagnosticError.getClass().getSimpleName());
        }
    }

    private JsonNode safeTree(String body) {
        try {
            JsonNode value = mapper.readTree(body == null ? "" : body);
            return value == null ? mapper.createObjectNode() : value;
        } catch (RuntimeException error) {
            return mapper.createObjectNode();
        }
    }

    private static String requestId(HttpResponse<String> response, JsonNode interaction) {
        String bodyId = interaction.path("id").asText("");
        if (!bodyId.isBlank()) return bodyId;
        if (response.headers() == null) return "absent";
        return response.headers().firstValue("x-request-id")
                .or(() -> response.headers().firstValue("x-goog-request-id"))
                .orElse("absent");
    }

    private static String statusReason(JsonNode interaction) {
        for (String field : List.of("status_reason", "finish_reason", "finishReason")) {
            String value = interaction.path(field).asText("");
            if (!value.isBlank()) return safeToken(value);
        }
        return "absent";
    }

    private static OutputMetadata outputMetadata(JsonNode interaction) {
        int blocks = 0;
        int length = 0;
        boolean modelOutput = false;
        for (JsonNode step : interaction.path("steps")) {
            if (!"model_output".equals(step.path("type").asText())) continue;
            modelOutput = true;
            for (JsonNode content : step.path("content")) {
                if ("text".equals(content.path("type").asText()) && content.path("text").isTextual()) {
                    blocks++;
                    length += content.path("text").asText().length();
                }
            }
        }
        return new OutputMetadata(modelOutput, blocks, length);
    }

    private static String safeReason(String value) {
        if (value == null || value.isBlank()) return "unknown";
        String sanitized = value.replaceAll("[^A-Za-z0-9_.-]", "_");
        return sanitized.substring(0, Math.min(sanitized.length(), 160));
    }

    private static String safeToken(String value) {
        return value == null || value.isBlank() ? "absent" : safeReason(value);
    }

    private record OutputMetadata(boolean modelOutputPresent, int textBlockCount, int textLength) { }

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

    private static ActivityGenerationException activityError(GeminiProviderException error) {
        ActivityGenerationException.Kind kind = switch (error.kind()) {
            case CONFIGURATION -> ActivityGenerationException.Kind.CONFIGURATION;
            case AUTHENTICATION, VIDEO_INACCESSIBLE -> ActivityGenerationException.Kind.AUTHENTICATION;
            case QUOTA -> ActivityGenerationException.Kind.QUOTA;
            case TIMEOUT -> ActivityGenerationException.Kind.TIMEOUT;
            case INVALID_RESPONSE -> ActivityGenerationException.Kind.INVALID_RESPONSE;
            case UNAVAILABLE -> ActivityGenerationException.Kind.UNAVAILABLE;
        };
        return new ActivityGenerationException(kind, "Não foi possível gerar a atividade.", error);
    }

    private static ActivityGenerationException activityProviderError(int status) {
        ActivityGenerationException.Kind kind = switch (status) {
            case 401, 403 -> ActivityGenerationException.Kind.AUTHENTICATION;
            case 408, 504 -> ActivityGenerationException.Kind.TIMEOUT;
            case 429 -> ActivityGenerationException.Kind.QUOTA;
            default -> ActivityGenerationException.Kind.UNAVAILABLE;
        };
        return new ActivityGenerationException(kind, "Não foi possível gerar a atividade no provedor.");
    }
}
