package com.estudenoah.backend.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.estudenoah.backend.api.ApiException;
import com.estudenoah.backend.video.VideoAnalysis;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class GeminiDocumentAnalysisServiceTest {
    @ParameterizedTest
    @MethodSource("sourceTypes")
    void acceptsEveryDocumentTextSource(String sourceType) {
        DocumentAnalysisProvider provider = mock(DocumentAnalysisProvider.class);
        when(provider.analyze(any())).thenReturn(analysis(sourceType));
        var result = new GeminiDocumentAnalysisService(provider).analyze(request(sourceType, "conteúdo ".repeat(30)));
        assertThat(result.sourceType()).isEqualTo(sourceType);
    }

    static Stream<String> sourceTypes() { return Stream.of("pdf", "ppt", "pptx", "doc", "docx", "odt", "text"); }

    @Test
    void rejectsEmptyInsufficientAndOversizedTextWithoutCallingGemini() {
        var service = new GeminiDocumentAnalysisService(mock(DocumentAnalysisProvider.class));
        assertCode(service, request("pdf", ""), "empty_document_text");
        assertCode(service, request("pdf", "curto"), "insufficient_document_text");
        assertCode(service, request("pdf", "x".repeat(60_001)), "document_text_too_large");
    }

    @ParameterizedTest
    @MethodSource("providerFailures")
    void mapsGeminiFailures(DocumentAnalysisException.Kind kind, String code) {
        DocumentAnalysisProvider provider = mock(DocumentAnalysisProvider.class);
        when(provider.analyze(any())).thenThrow(new DocumentAnalysisException(kind, "provider secret"));
        assertCode(new GeminiDocumentAnalysisService(provider), request("pdf", "conteúdo ".repeat(30)), code);
    }

    static Stream<Object[]> providerFailures() {
        return Stream.of(
                new Object[]{DocumentAnalysisException.Kind.INVALID_RESPONSE, "gemini_invalid_response"},
                new Object[]{DocumentAnalysisException.Kind.QUOTA, "gemini_quota_exceeded"},
                new Object[]{DocumentAnalysisException.Kind.TIMEOUT, "gemini_timeout"},
                new Object[]{DocumentAnalysisException.Kind.UNAVAILABLE, "gemini_unavailable"}
        );
    }

    private static void assertCode(GeminiDocumentAnalysisService service, DocumentAnalysisRequest request, String code) {
        assertThatThrownBy(() -> service.analyze(request)).isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code()).isEqualTo(code);
    }

    private static DocumentAnalysisRequest request(String type, String text) { return new DocumentAnalysisRequest(type, "Aula", "Ciências", "4º Ano", text); }
    private static DocumentAnalysis analysis(String type) {
        var theme = new VideoAnalysis.Theme("Tema", List.of("Objetivo"), List.of("Conceito"), List.of("Relação"),
                List.of("Equívoco"), List.of(new VideoAnalysis.Evidence("Evidência", "document")));
        return new DocumentAnalysis(type, "Aula", "Ciências", "Resumo", List.of(theme), List.of());
    }
}
