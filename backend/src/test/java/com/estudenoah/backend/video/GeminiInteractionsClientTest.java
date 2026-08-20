package com.estudenoah.backend.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.estudenoah.backend.video.GeminiConfiguration.GeminiSettings;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpHeaders;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

class GeminiInteractionsClientTest {
    private static final URI VIDEO = URI.create("https://www.youtube.com/watch?v=AbCdEf123_-");

    @Test
    void parsesValidStructuredAnalysisAndUsesStatelessMultimodalRequest() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> response = response(200, interaction(validAnalysis()));
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var client = client(http);

        VideoAnalysis result = client.analyze(VIDEO);

        assertThat(result.videoTitle()).isEqualTo("Ciclo da água");
        assertThat(result.themes()).hasSize(1);
        assertThat(result.themes().getFirst().evidence().getFirst().timestamp()).isEqualTo("00:42");
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(request.capture(), any(HttpResponse.BodyHandler.class));
        String body = request.getValue().bodyPublisher().orElseThrow().contentLength() > 0
                ? requestBody(request.getValue()) : "";
        assertThat(body).contains("\"store\":false", "\"type\":\"video\"", VIDEO.toString(),
                "\"mime_type\":\"application/json\"");
        assertThat(request.getValue().headers().firstValue("x-goog-api-key")).contains("test-key");
    }

    @Test
    void concatenatesAllTextBlocksFromLastModelOutputLikeOfficialOutputText() throws Exception {
        HttpClient http = mock(HttpClient.class);
        String analysis = validAnalysis();
        int split = analysis.length() / 2;
        String body = interactionWithTextBlocks(analysis.substring(0, split), analysis.substring(split));
        stubResponse(http, 200, body);

        VideoAnalysis result = client(http).analyze(VIDEO);

        assertThat(result.videoTitle()).isEqualTo("Ciclo da água");
        assertThat(result.themes()).hasSize(1);
    }

    @Test
    void ignoresEarlierModelOutputAndUsesLastOne() throws Exception {
        HttpClient http = mock(HttpClient.class);
        String body = "{\"status\":\"completed\",\"steps\":["
                + "{\"type\":\"model_output\",\"content\":[{\"type\":\"text\",\"text\":\"rascunho\"}]},"
                + modelOutput(validAnalysis()) + "]}";
        stubResponse(http, 200, body);

        assertThat(client(http).analyze(VIDEO).summary()).isEqualTo("Explica as etapas observadas.");
    }

    @ParameterizedTest
    @MethodSource("invalidInteractions")
    void rejectsMissingEmptyMalformedAndAbnormalOutputs(String body) throws Exception {
        HttpClient http = mock(HttpClient.class);
        stubResponse(http, 200, body);

        assertThatThrownBy(() -> client(http).analyze(VIDEO))
                .isInstanceOf(GeminiProviderException.class)
                .extracting(error -> ((GeminiProviderException) error).kind())
                .isEqualTo(GeminiProviderException.Kind.INVALID_RESPONSE);
    }

    static Stream<Arguments> invalidInteractions() throws Exception {
        return Stream.of(
                Arguments.of("{\"status\":\"completed\",\"steps\":[]}"),
                Arguments.of(interaction("")),
                Arguments.of(interaction("{not-json")),
                Arguments.of(interaction("{\"sourceType\":\"youtube\"}")),
                Arguments.of("{\"status\":\"failed\",\"steps\":[],\"status_reason\":\"safety\"}"),
                Arguments.of(interaction(validAnalysis().substring(0, validAnalysis().length() / 2)))
        );
    }

    @Test
    void acceptsEmptyThemeDetailListsWhenRequiredContractFieldsExist() throws Exception {
        HttpClient http = mock(HttpClient.class);
        String analysis = validAnalysis()
                .replace("[\"Relacionar as etapas\"]", "[]")
                .replace("[\"evaporação\"]", "[]")
                .replace("[\"calor favorece evaporação\"]", "[]")
                .replace("[\"água desaparece\"]", "[]")
                .replace("[{\"description\":\"Diagrama e narração mostram evaporação\",\"timestamp\":\"00:42\"}]", "[]");
        stubResponse(http, 200, interaction(analysis));

        VideoAnalysis result = client(http).analyze(VIDEO);

        assertThat(result.themes().getFirst().learningObjectives()).isEmpty();
        assertThat(result.themes().getFirst().evidence()).isEmpty();
    }

    @Test
    void usesServerValidatedCanonicalSourceInsteadOfModelEcho() throws Exception {
        HttpClient http = mock(HttpClient.class);
        String analysis = validAnalysis()
                .replace("https://www.youtube.com/watch?v=AbCdEf123_-", "https://youtu.be/model-echo");
        stubResponse(http, 200, interaction(analysis));

        VideoAnalysis result = client(http).analyze(VIDEO);

        assertThat(result.sourceType()).isEqualTo("youtube");
        assertThat(result.sourceUrl()).isEqualTo(VIDEO.toString());
    }

    @Test
    void writesOnlySanitizedMetadataWhenParsingFails() throws Exception {
        HttpClient http = mock(HttpClient.class);
        Path diagnostic = Files.createTempFile("gemini-diagnostic", ".json");
        Files.deleteIfExists(diagnostic);
        HttpResponse<String> response = response(200, interaction("private educational output that is not json"));
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of("x-request-id", List.of("request-123")), (a, b) -> true));
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var client = new GeminiInteractionsClient(http, new ObjectMapper(),
                new GeminiSettings("test-key", "gemini-3.6-flash"),
                URI.create("https://example.test/interactions"), Duration.ofSeconds(2), diagnostic);

        assertThatThrownBy(() -> client.analyze(VIDEO)).isInstanceOf(GeminiProviderException.class);

        String metadata = Files.readString(diagnostic);
        assertThat(metadata).contains("request-123", "gemini-3.6-flash", "outputTextLength", "failureReason");
        assertThat(metadata).doesNotContain("test-key", "private educational output");
        Files.deleteIfExists(diagnostic);
    }

    @Test
    void rejectsInvalidProviderJson() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> invalid = response(200, interaction("{\"sourceType\":\"youtube\"}"));
        when(http.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(invalid);

        assertThatThrownBy(() -> client(http).analyze(VIDEO))
                .isInstanceOf(GeminiProviderException.class)
                .extracting(error -> ((GeminiProviderException) error).kind())
                .isEqualTo(GeminiProviderException.Kind.INVALID_RESPONSE);
    }

    @ParameterizedTest
    @MethodSource("providerErrors")
    void mapsProviderFailuresWithoutExposingProviderBodies(int status, GeminiProviderException.Kind kind) throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> failure = response(status, "secret provider detail");
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(failure);

        assertThatThrownBy(() -> client(http).analyze(VIDEO))
                .isInstanceOf(GeminiProviderException.class)
                .extracting(error -> ((GeminiProviderException) error).kind())
                .isEqualTo(kind);
    }

    static Stream<Arguments> providerErrors() {
        return Stream.of(
                Arguments.of(400, GeminiProviderException.Kind.VIDEO_INACCESSIBLE),
                Arguments.of(401, GeminiProviderException.Kind.AUTHENTICATION),
                Arguments.of(429, GeminiProviderException.Kind.QUOTA),
                Arguments.of(503, GeminiProviderException.Kind.UNAVAILABLE)
        );
    }

    private static GeminiInteractionsClient client(HttpClient http) {
        return new GeminiInteractionsClient(http, new ObjectMapper(),
                new GeminiSettings("test-key", "gemini-test"), URI.create("https://example.test/interactions"), Duration.ofSeconds(2));
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    @SuppressWarnings("unchecked")
    private static void stubResponse(HttpClient http, int status, String body) throws Exception {
        HttpResponse<String> value = response(status, body);
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(value);
    }

    private static String interaction(String output) throws Exception {
        return "{\"status\":\"completed\",\"steps\":[" + modelOutput(output) + "]}";
    }

    private static String interactionWithTextBlocks(String... outputs) throws Exception {
        StringBuilder content = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();
        for (String output : outputs) {
            if (!content.isEmpty()) content.append(',');
            content.append("{\"type\":\"text\",\"text\":")
                    .append(mapper.writeValueAsString(output)).append('}');
        }
        return "{\"status\":\"completed\",\"steps\":[{\"type\":\"model_output\",\"content\":[" + content + "]}]}";
    }

    private static String modelOutput(String output) throws Exception {
        String encoded = new ObjectMapper().writeValueAsString(output);
        return "{\"type\":\"model_output\",\"content\":[{\"type\":\"text\",\"text\":" + encoded + "}]}";
    }

    private static String validAnalysis() {
        return """
                {"sourceType":"youtube","sourceUrl":"https://www.youtube.com/watch?v=AbCdEf123_-","videoTitle":"Ciclo da água","subject":"Ciências","summary":"Explica as etapas observadas.","themes":[{"name":"Mudanças de estado","learningObjectives":["Relacionar as etapas"],"concepts":["evaporação"],"relationships":["calor favorece evaporação"],"likelyMisconceptions":["água desaparece"],"evidence":[{"description":"Diagrama e narração mostram evaporação","timestamp":"00:42"}]}],"warnings":[]}
                """;
    }

    private static String requestBody(HttpRequest request) throws Exception {
        var subscriber = new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
            final java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            java.util.concurrent.Flow.Subscription subscription;
            public void onSubscribe(java.util.concurrent.Flow.Subscription value) { subscription = value; value.request(Long.MAX_VALUE); }
            public void onNext(java.nio.ByteBuffer item) { byte[] bytes = new byte[item.remaining()]; item.get(bytes); output.writeBytes(bytes); }
            public void onError(Throwable throwable) { throw new AssertionError(throwable); }
            public void onComplete() { }
        };
        request.bodyPublisher().orElseThrow().subscribe(subscriber);
        return subscriber.output.toString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
