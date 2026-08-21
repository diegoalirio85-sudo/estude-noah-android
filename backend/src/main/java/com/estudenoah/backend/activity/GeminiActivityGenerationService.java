package com.estudenoah.backend.activity;

import com.estudenoah.backend.api.ApiException;
import com.estudenoah.backend.video.GeminiInteractionsClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public final class GeminiActivityGenerationService implements ActivityGenerationService {
    private final GeminiInteractionsClient client;
    private final ActivityValidator validator;

    GeminiActivityGenerationService(GeminiInteractionsClient client, ActivityValidator validator) {
        this.client = client;
        this.validator = validator;
    }

    @Override
    public GeneratedActivity generate(ActivityGenerationRequest request) {
        try {
            validator.validateRequest(request);
            GeneratedActivity result = client.generateActivity(request);
            try {
                validator.validateResult(request, result);
            } catch (ActivityValidationException first) {
                if (first.kind() != ActivityValidationException.Kind.INVALID_RESPONSE) throw first;
                result = client.generateActivity(request, first.getMessage());
                validator.validateResult(request, result);
            }
            return result;
        } catch (ActivityValidationException error) {
            throw switch (error.kind()) {
                case INVALID_INPUT -> api(HttpStatus.BAD_REQUEST, "invalid_activity_request", error.getMessage());
                case INSUFFICIENT_CONTENT -> api(HttpStatus.UNPROCESSABLE_CONTENT, "insufficient_material", error.getMessage());
                case INVALID_RESPONSE -> api(HttpStatus.BAD_GATEWAY, "invalid_generated_activity", error.getMessage());
            };
        } catch (ActivityGenerationException error) {
            throw switch (error.kind()) {
                case CONFIGURATION -> api(HttpStatus.SERVICE_UNAVAILABLE, "activity_generation_not_configured",
                        "A geração de atividades não está configurada no servidor.");
                case AUTHENTICATION -> api(HttpStatus.BAD_GATEWAY, "gemini_authentication_failed",
                        "O provedor recusou a autenticação do servidor.");
                case QUOTA -> api(HttpStatus.TOO_MANY_REQUESTS, "gemini_quota_exceeded",
                        "O limite temporário do provedor foi atingido.");
                case TIMEOUT -> api(HttpStatus.GATEWAY_TIMEOUT, "gemini_timeout",
                        "A geração da atividade excedeu o tempo limite.");
                case INVALID_RESPONSE -> api(HttpStatus.BAD_GATEWAY, "gemini_invalid_response",
                        "O provedor retornou uma atividade inválida.");
                case UNAVAILABLE -> api(HttpStatus.SERVICE_UNAVAILABLE, "gemini_unavailable",
                        "O provedor está temporariamente indisponível.");
            };
        }
    }

    private static ApiException api(HttpStatus status, String code, String message) {
        return new ApiException(status, code, message);
    }
}
