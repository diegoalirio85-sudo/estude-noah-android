package com.estudenoah.backend.video;

import com.estudenoah.backend.api.ApiException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public final class GeminiVideoAnalysisService implements VideoAnalysisService {
    private final GeminiInteractionsClient client;

    GeminiVideoAnalysisService(GeminiInteractionsClient client) {
        this.client = client;
    }

    @Override
    public VideoAnalysis analyze(URI youtubeUrl) {
        try {
            return client.analyze(youtubeUrl);
        } catch (GeminiProviderException error) {
            throw switch (error.kind()) {
                case CONFIGURATION -> api(HttpStatus.SERVICE_UNAVAILABLE, "youtube_analysis_not_configured",
                        "A análise de vídeo não está configurada no servidor.");
                case VIDEO_INACCESSIBLE -> api(HttpStatus.UNPROCESSABLE_ENTITY, "youtube_video_inaccessible",
                        "O vídeo não existe, não é público ou não pode ser analisado.");
                case AUTHENTICATION -> api(HttpStatus.BAD_GATEWAY, "gemini_authentication_failed",
                        "O provedor de análise recusou a autenticação do servidor.");
                case QUOTA -> api(HttpStatus.TOO_MANY_REQUESTS, "gemini_quota_exceeded",
                        "O limite temporário do provedor de análise foi atingido.");
                case TIMEOUT -> api(HttpStatus.GATEWAY_TIMEOUT, "gemini_timeout",
                        "A análise do vídeo excedeu o tempo limite.");
                case INVALID_RESPONSE -> api(HttpStatus.BAD_GATEWAY, "gemini_invalid_response",
                        "O provedor retornou uma análise inválida.");
                case UNAVAILABLE -> api(HttpStatus.SERVICE_UNAVAILABLE, "gemini_unavailable",
                        "O provedor de análise está temporariamente indisponível.");
            };
        }
    }

    private static ApiException api(HttpStatus status, String code, String message) {
        return new ApiException(status, code, message);
    }
}
