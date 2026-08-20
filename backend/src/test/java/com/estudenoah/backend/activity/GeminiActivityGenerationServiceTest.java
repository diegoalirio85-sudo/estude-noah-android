package com.estudenoah.backend.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.estudenoah.backend.api.ApiException;
import com.estudenoah.backend.video.GeminiInteractionsClient;
import com.estudenoah.backend.video.VideoAnalysis;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeminiActivityGenerationServiceTest {
    @Test
    void retriesOnceWithPedagogicalFeedbackAndReturnsSecondResult() {
        var client = mock(GeminiInteractionsClient.class);
        var validator = mock(ActivityValidator.class);
        var request = request();
        var first = activity("primeira");
        var second = activity("segunda");
        when(client.generateActivity(request)).thenReturn(first);
        when(client.generateActivity(request, "faltam aplicações")).thenReturn(second);
        org.mockito.Mockito.doThrow(new ActivityValidationException(ActivityValidationException.Kind.INVALID_RESPONSE,
                "faltam aplicações")).doNothing().when(validator).validateResult(request, first);

        assertThat(new GeminiActivityGenerationService(client, validator).generate(request)).isSameAs(second);
        verify(client).generateActivity(request, "faltam aplicações");
    }

    @Test
    void failsAfterSecondInvalidPedagogicalResult() {
        var client = mock(GeminiInteractionsClient.class);
        var validator = mock(ActivityValidator.class);
        var request = request();
        when(client.generateActivity(request)).thenReturn(activity("primeira"));
        when(client.generateActivity(any(), anyString())).thenReturn(activity("segunda"));
        org.mockito.Mockito.doThrow(new ActivityValidationException(ActivityValidationException.Kind.INVALID_RESPONSE,
                "sem diversidade")).when(validator).validateResult(any(), any());
        assertThatThrownBy(() -> new GeminiActivityGenerationService(client, validator).generate(request))
                .isInstanceOf(ApiException.class);
        verify(client, times(1)).generateActivity(any(), anyString());
    }

    @Test
    void doesNotRetryProviderAuthenticationFailure() {
        var client = mock(GeminiInteractionsClient.class);
        var request = request();
        when(client.generateActivity(request)).thenThrow(new ActivityGenerationException(
                ActivityGenerationException.Kind.AUTHENTICATION, "auth"));
        assertThatThrownBy(() -> new GeminiActivityGenerationService(client, mock(ActivityValidator.class)).generate(request))
                .isInstanceOf(ApiException.class);
        verify(client, never()).generateActivity(any(), anyString());
    }

    private static ActivityGenerationRequest request() {
        var theme = new VideoAnalysis.Theme("Tema", List.of("Objetivo"), List.of("Conceito"), List.of("Relação"),
                List.of("Equívoco"), List.of(new VideoAnalysis.Evidence("Evidência", "00:01")));
        return new ActivityGenerationRequest("4º Ano", "Ciências", new ActivityGenerationRequest.Source("youtube", "Aula", "url"),
                new VideoAnalysis("youtube", "url", "Aula", "Ciências", "Resumo", List.of(theme), List.of()));
    }

    private static GeneratedActivity activity(String warning) {
        return new GeneratedActivity("Ciências", "4º Ano", "TRUE_FALSE", List.of(), List.of(warning));
    }
}
