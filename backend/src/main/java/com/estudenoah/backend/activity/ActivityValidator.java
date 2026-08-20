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
                    rejectDuplicate(question.explanation(), explanations, "Há explicações idênticas ou muito semelhantes.");
                }
            }
            if (!math && trueCount != 2 && trueCount != 3) {
                throw invalidResponse("O conjunto Verdadeiro/Falso deve ter distribuição equilibrada.");
            }
        }
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
