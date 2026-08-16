package com.estudenoah.backend.video;

import java.util.List;

public record VideoAnalysis(
        String sourceType,
        String sourceUrl,
        String videoTitle,
        String subject,
        String summary,
        List<Theme> themes,
        List<String> warnings
) {
    public VideoAnalysis {
        themes = List.copyOf(themes);
        warnings = List.copyOf(warnings);
    }

    public record Theme(
            String name,
            List<String> learningObjectives,
            List<String> concepts,
            List<String> relationships,
            List<String> likelyMisconceptions,
            List<Evidence> evidence
    ) {
        public Theme {
            learningObjectives = List.copyOf(learningObjectives);
            concepts = List.copyOf(concepts);
            relationships = List.copyOf(relationships);
            likelyMisconceptions = List.copyOf(likelyMisconceptions);
            evidence = List.copyOf(evidence);
        }
    }

    public record Evidence(String description, String timestamp) {
    }
}
