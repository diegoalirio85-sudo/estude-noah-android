package com.estudenoah.backend.document;

import com.estudenoah.backend.video.VideoAnalysis;
import java.util.List;

public record DocumentAnalysis(
        String sourceType,
        String sourceTitle,
        String subject,
        String summary,
        List<VideoAnalysis.Theme> themes,
        List<String> warnings
) {
    public DocumentAnalysis {
        themes = themes == null ? null : List.copyOf(themes);
        warnings = warnings == null ? null : List.copyOf(warnings);
    }

    public VideoAnalysis asPedagogicalAnalysis() {
        return new VideoAnalysis(sourceType, "", sourceTitle, subject, summary, themes, warnings);
    }
}
