package com.estudenoah.backend.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.estudenoah.backend.activity.ActivityGenerationException;
import com.estudenoah.backend.activity.ActivityGenerationRequest;
import com.estudenoah.backend.video.GeminiConfiguration.GeminiSettings;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class GeminiActivityGenerationClientTest {
    @Test
    void generatesStructuredActivityWithSharedStatelessClient() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> providerResponse = response(200, interaction(validActivity()));
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(providerResponse);
        var client = client(http);

        var result = client.generateActivity(request());

        assertThat(result.activityType()).isEqualTo("TRUE_FALSE");
        assertThat(result.themes().getFirst().questions()).hasSize(5);
        ArgumentCaptor<HttpRequest> sent = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(sent.capture(), any(HttpResponse.BodyHandler.class));
        String body = requestBody(sent.getValue());
        assertThat(body).contains("\"store\":false", "c2-v1", "\"mime_type\":\"application/json\"");
        assertThat(sent.getValue().headers().firstValue("x-goog-api-key")).contains("test-key");
    }

    @Test
    void rejectsInvalidGeminiActivityJson() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> providerResponse = response(200, interaction("not-json"));
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(providerResponse);

        assertThatThrownBy(() -> client(http).generateActivity(request()))
                .isInstanceOf(ActivityGenerationException.class)
                .extracting(error -> ((ActivityGenerationException) error).kind())
                .isEqualTo(ActivityGenerationException.Kind.INVALID_RESPONSE);
    }

    private static ActivityGenerationRequest request() {
        var theme = new VideoAnalysis.Theme("Tema", List.of("Compreender"), List.of("Conceito"),
                List.of("Relação"), List.of("Equívoco"), List.of(new VideoAnalysis.Evidence("Trecho", "00:10")));
        var analysis = new VideoAnalysis("youtube", "https://www.youtube.com/watch?v=AbCdEf123_-", "Aula",
                "Português", "Resumo", List.of(theme), List.of());
        return new ActivityGenerationRequest("4º Ano Ensino Fundamental", "Língua Portuguesa",
                new ActivityGenerationRequest.Source("youtube", "Aula", analysis.sourceUrl()), analysis);
    }

    private static GeminiInteractionsClient client(HttpClient http) {
        return new GeminiInteractionsClient(http, new ObjectMapper(), new GeminiSettings("test-key", "gemini-test"),
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

    private static String validActivity() {
        StringBuilder questions = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i > 1) questions.append(',');
            questions.append("{\"statement\":\"Questão ").append(i).append("\",\"answer\":")
                    .append(i % 2 == 1).append(",\"explanation\":\"Explicação ").append(i)
                    .append("\",\"evidence\":[\"Trecho\"],\"theme\":\"Tema\",\"learningObjective\":\"Compreender\",\"difficulty\":\"medium\",\"problem\":null,\"mathAnswer\":null,\"solutionSteps\":null,\"skill\":null}");
        }
        return "{\"subject\":\"Língua Portuguesa\",\"grade\":\"4º Ano Ensino Fundamental\",\"activityType\":\"TRUE_FALSE\",\"themes\":[{\"name\":\"Tema\",\"questions\":["
                + questions + "]}],\"warnings\":[]}";
    }

    private static String requestBody(HttpRequest request) {
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
