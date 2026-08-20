package com.estudenoah.backend.activity;

import java.util.List;

public record GeneratedActivity(
        String subject,
        String grade,
        String activityType,
        List<Theme> themes,
        List<String> warnings
) {
    public GeneratedActivity {
        themes = themes == null ? null : List.copyOf(themes);
        warnings = warnings == null ? null : List.copyOf(warnings);
    }

    public record Theme(String name, List<Question> questions) {
        public Theme {
            questions = questions == null ? null : List.copyOf(questions);
        }
    }

    public record Question(
            String statement,
            Boolean answer,
            String explanation,
            List<String> evidence,
            String theme,
            String learningObjective,
            String difficulty,
            String problem,
            String mathAnswer,
            List<String> solutionSteps,
            String skill,
            String cognitiveDemand,
            String constructionType
    ) {
        public Question(String statement, Boolean answer, String explanation, List<String> evidence, String theme,
                        String learningObjective, String difficulty, String problem, String mathAnswer,
                        List<String> solutionSteps, String skill) {
            this(statement, answer, explanation, evidence, theme, learningObjective, difficulty, problem, mathAnswer,
                    solutionSteps, skill, problem == null ? "application" : null,
                    problem == null ? "application" : null);
        }
        public Question {
            evidence = evidence == null ? null : List.copyOf(evidence);
            solutionSteps = solutionSteps == null ? null : List.copyOf(solutionSteps);
        }
    }
}
