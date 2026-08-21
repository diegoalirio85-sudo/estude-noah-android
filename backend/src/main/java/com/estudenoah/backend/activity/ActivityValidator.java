package com.estudenoah.backend.activity;

import com.estudenoah.backend.video.VideoAnalysis;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class ActivityValidator {
    private static final Set<String> DIFFICULTIES = Set.of("easy", "medium", "hard");
    private static final Set<String> COGNITIVE_DEMANDS = Set.of("understanding", "application", "analysis");
    private static final Set<String> CONSTRUCTION_TYPES = Set.of("concept", "application", "relation", "misconception", "source_example");

    public void validateRequest(ActivityGenerationRequest request) {
        if (request == null || blank(request.grade()) || blank(request.subject()) || request.source() == null
                || blank(request.source().type()) || request.analysis() == null) {
            throw invalidInput("Informe ano, disciplina, fonte e análise pedagógica.");
        }
        VideoAnalysis analysis = request.analysis();
        if (analysis.themes() == null || analysis.themes().isEmpty()) {
            throw insufficient("A análise não contém temas suficientes para gerar uma atividade.");
        }
        for (VideoAnalysis.Theme theme : analysis.themes()) {
            if (theme == null || blank(theme.name()) || empty(theme.learningObjectives())
                    || empty(theme.concepts()) || empty(theme.evidence())) {
                throw insufficient("A análise não contém objetivos, conceitos e evidências suficientes.");
            }
        }
    }

    public void validateResult(ActivityGenerationRequest request, GeneratedActivity result) {
        boolean math = isMath(request.subject());
        String expectedType = math ? "MATH_PROBLEMS" : "TRUE_FALSE";
        if (result == null || !request.subject().equals(result.subject()) || !request.grade().equals(result.grade())
                || !expectedType.equals(result.activityType()) || result.themes() == null || result.themes().isEmpty()
                || result.warnings() == null) {
            throw invalidResponse("A atividade gerada não respeita o contrato solicitado.");
        }

        Set<String> statements = new HashSet<>();
        Set<String> explanations = new HashSet<>();
        for (GeneratedActivity.Theme theme : result.themes()) {
            if (theme == null || blank(theme.name()) || theme.questions() == null || theme.questions().size() != 5) {
                throw invalidResponse("Cada tema deve conter exatamente cinco questões válidas.");
            }
            int trueCount = 0;
            int easyCount = 0;
            int applicationCount = 0;
            int sourceExampleCount = 0;
            int misconceptionFalseCount = 0;
            VideoAnalysis.Theme sourceTheme = findTheme(request.analysis(), theme.name());
            for (GeneratedActivity.Question question : theme.questions()) {
                validateCommon(question, theme.name());
                String prompt = math ? question.problem() : question.statement();
                rejectDuplicate(prompt, statements, "Há questões idênticas ou muito semelhantes.");
                if (math) {
                    if (question.answer() != null || !blank(question.statement()) || blank(question.problem())
                            || blank(question.mathAnswer()) || empty(question.solutionSteps()) || blank(question.skill())) {
                        throw invalidResponse("Problemas de Matemática devem ter resposta e solução, sem Verdadeiro/Falso.");
                    }
                } else {
                    if (blank(question.statement()) || question.answer() == null || blank(question.explanation())
                            || empty(question.evidence()) || !blank(question.problem()) || question.mathAnswer() != null
                            || (question.solutionSteps() != null && !question.solutionSteps().isEmpty())) {
                        throw invalidResponse("Questões conceituais devem conter afirmação, resposta, explicação e evidência.");
                    }
                    if (question.answer()) trueCount++;
                    if ("easy".equals(question.difficulty())) easyCount++;
                    if (!COGNITIVE_DEMANDS.contains(question.cognitiveDemand())
                            || !CONSTRUCTION_TYPES.contains(question.constructionType())) {
                        throw invalidResponse("Toda questão conceitual deve declarar demanda cognitiva e tipo de construção válidos.");
                    }
                    if ("application".equals(question.constructionType()) || "relation".equals(question.constructionType())) {
                        applicationCount++;
                    }
                    if ("source_example".equals(question.constructionType()) || tooLiteral(question.statement(), sourceTheme)) {
                        sourceExampleCount++;
                    }
                    if (!question.answer() && "misconception".equals(question.constructionType())) misconceptionFalseCount++;
                    if (!question.answer() && mechanicalInversion(question.statement(), sourceTheme)) {
                        throw invalidResponse("Uma falsa foi criada por inversão mecânica de um exemplo da fonte.");
                    }
                    rejectDuplicate(question.explanation(), explanations, "Há explicações idênticas ou muito semelhantes.");
                }
            }
            if (!math && trueCount != 2 && trueCount != 3) {
                throw invalidResponse("O conjunto Verdadeiro/Falso deve ter distribuição equilibrada.");
            }
            if (!math && easyCount > 1) throw invalidResponse("Há mais de uma questão easy no conjunto.");
            if (!math && sourceExampleCount > 2) throw invalidResponse("Há mais de duas questões excessivamente literais.");
            if (!math && applicationCount < 2) throw invalidResponse("Faltam pelo menos duas questões de aplicação ou relação.");
            if (!math && sourceTheme != null && !empty(sourceTheme.likelyMisconceptions()) && misconceptionFalseCount < 1) {
                throw invalidResponse("Falta uma questão falsa baseada em misconception plausível.");
            }
        }
    }

    private static VideoAnalysis.Theme findTheme(VideoAnalysis analysis, String name) {
        if (analysis == null || analysis.themes() == null) return null;
        return analysis.themes().stream().filter(theme -> theme != null && name.equals(theme.name())).findFirst().orElse(null);
    }

    private static boolean tooLiteral(String statement, VideoAnalysis.Theme theme) {
        if (theme == null || theme.evidence() == null) return false;
        return theme.evidence().stream().filter(java.util.Objects::nonNull)
                .map(VideoAnalysis.Evidence::description).filter(value -> !blank(value))
                .anyMatch(value -> tokenContainment(normalize(statement), normalize(value)) >= 0.82);
    }

    private static boolean mechanicalInversion(String statement, VideoAnalysis.Theme theme) {
        if (theme == null || theme.evidence() == null) return false;
        String normalized = normalize(statement);
        for (VideoAnalysis.Evidence evidence : theme.evidence()) {
            if (evidence == null || blank(evidence.description())) continue;
            String source = normalize(evidence.description());
            if (tokenContainment(normalized, source) >= 0.70
                    && ((source.contains("crescente") && normalized.contains("decrescente"))
                    || (source.contains("decrescente") && normalized.contains("crescente")))) return true;
        }
        return false;
    }

    private static double tokenContainment(String left, String right) {
        Set<String> a = new HashSet<>(List.of(left.split(" ")));
        Set<String> b = new HashSet<>(List.of(right.split(" ")));
        a.removeIf(String::isBlank);
        b.removeIf(String::isBlank);
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        return (double) intersection.size() / Math.min(a.size(), b.size());
    }

    private static void validateCommon(GeneratedActivity.Question question, String themeName) {
        if (question == null || blank(question.theme()) || !themeName.equals(question.theme())
                || blank(question.learningObjective()) || !DIFFICULTIES.contains(question.difficulty())
                || empty(question.evidence())) {
            throw invalidResponse("Uma questão está vazia ou sem tema, objetivo, evidência ou dificuldade válida.");
        }
    }

    private static void rejectDuplicate(String value, Set<String> prior, String message) {
        if (blank(value)) throw invalidResponse("Uma questão está vazia.");
        String normalized = normalize(value);
        for (String existing : prior) {
            if (normalized.equals(existing) || similarity(normalized, existing) >= 0.86) {
                throw invalidResponse(message);
            }
        }
        prior.add(normalized);
    }

    private static double similarity(String left, String right) {
        Set<String> a = new HashSet<>(List.of(left.split(" ")));
        Set<String> b = new HashSet<>(List.of(right.split(" ")));
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 1 : (double) intersection.size() / union.size();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean isMath(String subject) {
        String normalized = normalize(subject);
        return normalized.equals("matematica") || normalized.startsWith("matematica ");
    }

    private static boolean empty(List<?> values) { return values == null || values.isEmpty(); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static ActivityValidationException invalidInput(String message) {
        return new ActivityValidationException(ActivityValidationException.Kind.INVALID_INPUT, message);
    }
    private static ActivityValidationException insufficient(String message) {
        return new ActivityValidationException(ActivityValidationException.Kind.INSUFFICIENT_CONTENT, message);
    }
    private static ActivityValidationException invalidResponse(String message) {
        return new ActivityValidationException(ActivityValidationException.Kind.INVALID_RESPONSE, message);
    }
}
