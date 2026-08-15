package com.estudenoah.backend.video;

import java.util.List;

public record VideoAnalysis(
        String sourceType,
        String sourceUrl,
        String title,
        String subject,
        String summary,
        List<Theme> themes
) {
    public VideoAnalysis {
        themes = List.copyOf(themes);
    }

    public record Theme(
            String name,
            List<String> learningObjectives,
            List<String> concepts,
            List<String> relationships,
            List<String> likelyMisconceptions,
            List<String> evidence
    ) {
        public Theme {
            learningObjectives = List.copyOf(learningObjectives);
            concepts = List.copyOf(concepts);
            relationships = List.copyOf(relationships);
            likelyMisconceptions = List.copyOf(likelyMisconceptions);
            evidence = List.copyOf(evidence);
        }
    }
}
