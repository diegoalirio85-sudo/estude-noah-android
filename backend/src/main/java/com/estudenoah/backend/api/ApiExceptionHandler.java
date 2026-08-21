package com.estudenoah.backend.api;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MissingServletRequestParameterException;

@RestControllerAdvice
public final class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException error) {
        return ResponseEntity.status(error.status())
                .body(new ErrorResponse(error.code(), error.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException error) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("missing_file", "O campo multipart 'file' é obrigatório.", Instant.now()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException error) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("missing_parameter", "Informe os metadados obrigatórios do material.", Instant.now()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException error) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse("file_too_large", "O arquivo excede o limite configurado.", Instant.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableJson(HttpMessageNotReadableException error) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("invalid_json", "Envie um corpo JSON válido.", Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception error) {
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("internal_error", "Não foi possível concluir o processamento.", Instant.now()));
    }
}
