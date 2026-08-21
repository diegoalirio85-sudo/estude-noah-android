package com.estudenoah.backend.video;

import com.estudenoah.backend.document.DocumentAnalysis;
import com.estudenoah.backend.document.DocumentAnalysisException;
import com.estudenoah.backend.document.DocumentAnalysisProvider;
import com.estudenoah.backend.document.DocumentAnalysisRequest;
import com.estudenoah.backend.document.DocumentPedagogicalAnalysisPrompt;
import com.estudenoah.backend.video.GeminiConfiguration.GeminiSettings;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class GeminiDocumentAnalysisClient implements DocumentAnalysisProvider {
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final GeminiSettings settings;
    private final URI endpoint;
    private final Duration timeout;
    private final JsonNode schema;

    GeminiDocumentAnalysisClient(HttpClient httpClient, ObjectMapper mapper, GeminiSettings settings,
                                 URI endpoint, Duration timeout) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.settings = settings;
        this.endpoint = endpoint;
        this.timeout = timeout;
        try (InputStream input = getClass().getResourceAsStream("/document-analysis-schema.json")) {
            if (input == null) throw new IOException("document schema missing");
            schema = mapper.readTree(input);
        } catch (IOException error) {
            throw new IllegalStateException("Não foi possível carregar o schema documental.", error);
        }
    }

    @Override
    public DocumentAnalysis analyze(DocumentAnalysisRequest source) {
        if (settings.apiKey().isBlank()) throw error(DocumentAnalysisException.Kind.CONFIGURATION, "A análise documental não está configurada.");
        HttpRequest request = request(source);
        HttpResponse<String> response = send(request);
        if (transientStatus(response.statusCode())) response = send(request);
        if (response.statusCode() / 100 != 2) throw providerError(response.statusCode());
        try {
            String output = interactionOutput(response.body());
            if (output == null || output.isBlank()) throw new IllegalArgumentException("empty output");
            DocumentAnalysis model = mapper.readValue(output, DocumentAnalysis.class);
            DocumentAnalysis result = new DocumentAnalysis(source.sourceType(), source.sourceTitle(), source.subject(),
                    model.summary(), model.themes(), model.warnings());
            validate(result);
            return result;
        } catch (DocumentAnalysisException error) {
            throw error;
        } catch (RuntimeException error) {
            throw new DocumentAnalysisException(DocumentAnalysisException.Kind.INVALID_RESPONSE,
                    "O provedor retornou uma análise documental inválida.", error);
        }
    }

    private HttpRequest request(DocumentAnalysisRequest source) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", settings.model());
        root.put("store", false);
        root.putArray("input").addObject().put("type", "text").put("text", DocumentPedagogicalAnalysisPrompt.text(source));
        root.putObject("generation_config").put("temperature", 0.1).put("max_output_tokens", 8192);
        root.putObject("response_format").put("type", "text").put("mime_type", "application/json").set("schema", schema);
        return HttpRequest.newBuilder(endpoint).timeout(timeout).header("Content-Type", "application/json")
                .header("x-goog-api-key", settings.apiKey()).POST(HttpRequest.BodyPublishers.ofString(root.toString())).build();
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException error) {
            throw new DocumentAnalysisException(DocumentAnalysisException.Kind.TIMEOUT, "A análise documental excedeu o tempo limite.", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new DocumentAnalysisException(DocumentAnalysisException.Kind.UNAVAILABLE, "O provedor está indisponível.", error);
        } catch (IOException error) {
            throw new DocumentAnalysisException(DocumentAnalysisException.Kind.UNAVAILABLE, "O provedor está indisponível.", error);
        }
    }

    private String interactionOutput(String body) {
        JsonNode interaction = mapper.readTree(body);
        if (!"completed".equals(interaction.path("status").asText())) return null;
        StringBuilder output = new StringBuilder();
        boolean found = false;
        for (JsonNode step : interaction.path("steps")) {
            if (!"model_output".equals(step.path("type").asText())) continue;
            if (found) output.setLength(0);
            found = true;
            for (JsonNode content : step.path("content"))
                if ("text".equals(content.path("type").asText()) && content.path("text").isTextual()) output.append(content.path("text").asText());
        }
        return found ? output.toString() : null;
    }

    private static void validate(DocumentAnalysis analysis) {
        if (analysis == null || blank(analysis.summary()) || analysis.themes() == null || analysis.themes().isEmpty() || analysis.warnings() == null)
            throw error(DocumentAnalysisException.Kind.INVALID_RESPONSE, "Análise documental incompleta.");
        for (VideoAnalysis.Theme theme : analysis.themes()) {
            if (theme == null || blank(theme.name()) || theme.learningObjectives() == null || theme.learningObjectives().isEmpty()
                    || theme.concepts() == null || theme.concepts().isEmpty() || theme.relationships() == null
                    || theme.likelyMisconceptions() == null || theme.evidence() == null || theme.evidence().isEmpty())
                throw error(DocumentAnalysisException.Kind.INVALID_RESPONSE, "Tema documental incompleto.");
            for (VideoAnalysis.Evidence evidence : theme.evidence())
                if (evidence == null || blank(evidence.description()) || blank(evidence.timestamp()))
                    throw error(DocumentAnalysisException.Kind.INVALID_RESPONSE, "Evidência documental inválida.");
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static boolean transientStatus(int status) { return status == 429 || status == 500 || status == 502 || status == 503 || status == 504; }
    private static DocumentAnalysisException providerError(int status) {
        return error(switch (status) {
            case 401, 403 -> DocumentAnalysisException.Kind.AUTHENTICATION;
            case 408, 504 -> DocumentAnalysisException.Kind.TIMEOUT;
            case 429 -> DocumentAnalysisException.Kind.QUOTA;
            default -> DocumentAnalysisException.Kind.UNAVAILABLE;
        }, "Não foi possível analisar o documento no provedor.");
    }
    private static DocumentAnalysisException error(DocumentAnalysisException.Kind kind, String message) { return new DocumentAnalysisException(kind, message); }
}
