package com.estudenoah.backend.activity;

import com.estudenoah.backend.video.VideoAnalysis;

public record ActivityGenerationRequest(
        String grade,
        String subject,
        Source source,
        VideoAnalysis analysis
) {
    public record Source(String type, String title, String url) {
    }
}
