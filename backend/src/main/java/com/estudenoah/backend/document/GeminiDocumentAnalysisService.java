package com.estudenoah.backend.document;

import com.estudenoah.backend.api.ApiException;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public final class GeminiDocumentAnalysisService implements DocumentAnalysisService {
    static final int MINIMUM_TEXT_CHARACTERS = 140;
    static final int MAXIMUM_TEXT_CHARACTERS = 60_000;
    private static final Set<String> SOURCE_TYPES = Set.of("pdf", "ppt", "pptx", "doc", "docx", "odt", "text");
    private final DocumentAnalysisProvider provider;

    public GeminiDocumentAnalysisService(DocumentAnalysisProvider provider) {
        this.provider = provider;
    }

    @Override
    public DocumentAnalysis analyze(DocumentAnalysisRequest request) {
        validate(request);
        try {
            return provider.analyze(normalized(request));
        } catch (DocumentAnalysisException error) {
            throw switch (error.kind()) {
                case CONFIGURATION -> api(HttpStatus.SERVICE_UNAVAILABLE, "document_analysis_not_configured", "A análise documental não está configurada no servidor.");
                case AUTHENTICATION -> api(HttpStatus.BAD_GATEWAY, "gemini_authentication_failed", "O provedor recusou a autenticação do servidor.");
                case QUOTA -> api(HttpStatus.TOO_MANY_REQUESTS, "gemini_quota_exceeded", "O limite temporário do provedor foi atingido.");
                case TIMEOUT -> api(HttpStatus.GATEWAY_TIMEOUT, "gemini_timeout", "A análise documental excedeu o tempo limite.");
                case INVALID_RESPONSE -> api(HttpStatus.BAD_GATEWAY, "gemini_invalid_response", "O provedor retornou uma análise documental inválida.");
                case UNAVAILABLE -> api(HttpStatus.SERVICE_UNAVAILABLE, "gemini_unavailable", "O provedor está temporariamente indisponível.");
            };
        }
    }

    static void validate(DocumentAnalysisRequest request) {
        if (request == null) throw api(HttpStatus.BAD_REQUEST, "invalid_document_request", "Envie os dados do documento.");
        String type = request.sourceType() == null ? "" : request.sourceType().trim().toLowerCase(Locale.ROOT);
        if (!SOURCE_TYPES.contains(type)) throw api(HttpStatus.BAD_REQUEST, "unsupported_source_type", "O tipo de documento informado não é aceito.");
        if (blank(request.sourceTitle()) || blank(request.subject()) || blank(request.grade()))
            throw api(HttpStatus.BAD_REQUEST, "invalid_document_request", "Título, disciplina e ano são obrigatórios.");
        if (request.text() == null || request.text().isBlank())
            throw api(HttpStatus.BAD_REQUEST, "empty_document_text", "Envie o texto extraído do documento.");
        int length = request.text().strip().length();
        if (length < MINIMUM_TEXT_CHARACTERS)
            throw api(HttpStatus.UNPROCESSABLE_CONTENT, "insufficient_document_text", "O texto é insuficiente para uma análise pedagógica confiável.");
        if (length > MAXIMUM_TEXT_CHARACTERS)
            throw api(HttpStatus.PAYLOAD_TOO_LARGE, "document_text_too_large", "O texto excede o limite de 60000 caracteres; chunking ainda não está disponível.");
    }

    private static DocumentAnalysisRequest normalized(DocumentAnalysisRequest request) {
        return new DocumentAnalysisRequest(request.sourceType().trim().toLowerCase(Locale.ROOT), request.sourceTitle().trim(),
                request.subject().trim(), request.grade().trim(), request.text().strip());
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static ApiException api(HttpStatus status, String code, String message) { return new ApiException(status, code, message); }
}
