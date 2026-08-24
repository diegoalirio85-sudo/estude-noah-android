package com.estudenoah.backend.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.estudenoah.backend.document.DocumentAnalysisException;
import com.estudenoah.backend.document.DocumentAnalysisRequest;
import com.estudenoah.backend.video.GeminiConfiguration.GeminiSettings;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

class GeminiDocumentAnalysisClientTest {
    @Test
    void usesStructuredOutputAndAuthoritativeDocumentMetadata() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> value = response(200, interaction(validAnalysis()));
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(value);

        var result = client(http).analyze(request());

        assertThat(result.sourceType()).isEqualTo("pdf");
        assertThat(result.sourceTitle()).isEqualTo("Substantivos");
        assertThat(result.subject()).isEqualTo("Língua Portuguesa");
        assertThat(result.themes()).hasSize(1);
        ArgumentCaptor<HttpRequest> sent = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(sent.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(requestBody(sent.getValue())).contains("\"store\":false", "document-analysis-v1",
                "CONTEÚDO DOCUMENTAL", "\"mime_type\":\"application/json\"");
    }

    @Test
    void rejectsStructurallyInvalidAnalysis() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> value = response(200, interaction("{\"sourceType\":\"pdf\"}"));
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(value);
        assertThatThrownBy(() -> client(http).analyze(request())).isInstanceOf(DocumentAnalysisException.class)
                .extracting(error -> ((DocumentAnalysisException) error).kind()).isEqualTo(DocumentAnalysisException.Kind.INVALID_RESPONSE);
    }

    @ParameterizedTest
    @MethodSource("failures")
    void mapsQuotaTimeoutAndProviderErrors(int status, DocumentAnalysisException.Kind kind) throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> value = response(status, "provider detail");
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(value);
        assertThatThrownBy(() -> client(http).analyze(request())).isInstanceOf(DocumentAnalysisException.class)
                .extracting(error -> ((DocumentAnalysisException) error).kind()).isEqualTo(kind);
    }

    static Stream<Arguments> failures() {
        return Stream.of(Arguments.of(401, DocumentAnalysisException.Kind.AUTHENTICATION),
                Arguments.of(429, DocumentAnalysisException.Kind.QUOTA),
                Arguments.of(504, DocumentAnalysisException.Kind.TIMEOUT),
                Arguments.of(503, DocumentAnalysisException.Kind.UNAVAILABLE));
    }

    private static DocumentAnalysisRequest request() {
        return new DocumentAnalysisRequest("pdf", "Substantivos", "Língua Portuguesa", "4º Ano", "conteúdo pedagógico ".repeat(20));
    }
    private static GeminiDocumentAnalysisClient client(HttpClient http) {
        return new GeminiDocumentAnalysisClient(http, new ObjectMapper(), new GeminiSettings("test-key", "gemini-test"),
                URI.create("https://example.test/interactions"), Duration.ofSeconds(2));
    }
    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
    private static String interaction(String output) throws Exception {
        return "{\"status\":\"completed\",\"steps\":[{\"type\":\"model_output\",\"content\":[{\"type\":\"text\",\"text\":"
                + new ObjectMapper().writeValueAsString(output) + "}]}]}";
    }
    private static String validAnalysis() {
        return """
                {"sourceType":"pdf","sourceTitle":"modelo","subject":"modelo","summary":"Síntese fiel.","themes":[{"name":"Substantivos","learningObjectives":["Reconhecer funções"],"concepts":["substantivo"],"relationships":["nomeia seres"],"likelyMisconceptions":["toda palavra é substantivo"],"evidence":[{"description":"O texto define e exemplifica substantivos.","timestamp":"document"}]}],"warnings":[]}
                """;
    }
    private static String requestBody(HttpRequest request) throws Exception {
        var subscriber = new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
            final java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            public void onSubscribe(java.util.concurrent.Flow.Subscription value) { value.request(Long.MAX_VALUE); }
            public void onNext(java.nio.ByteBuffer item) { byte[] bytes = new byte[item.remaining()]; item.get(bytes); output.writeBytes(bytes); }
            public void onError(Throwable throwable) { throw new AssertionError(throwable); }
            public void onComplete() { }
        };
        request.bodyPublisher().orElseThrow().subscribe(subscriber);
        return subscriber.output.toString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
