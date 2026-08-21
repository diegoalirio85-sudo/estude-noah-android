package com.estudenoah.backend.document;

public record DocumentAnalysisRequest(
        String sourceType,
        String sourceTitle,
        String subject,
        String grade,
        String text
) {
}
