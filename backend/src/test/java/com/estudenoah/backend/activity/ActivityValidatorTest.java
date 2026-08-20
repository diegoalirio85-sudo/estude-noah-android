package com.estudenoah.backend.activity;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.estudenoah.backend.video.VideoAnalysis;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ActivityValidatorTest {
    private final ActivityValidator validator = new ActivityValidator();

    @ParameterizedTest
    @ValueSource(strings = {"Língua Portuguesa", "História", "Ciências", "Geografia"})
    void acceptsBalancedConceptualActivityForSupportedSubjects(String subject) {
        var request = request(subject, analysis(theme("Tema")));
        assertThatNoException().isThrownBy(() -> validator.validateResult(request, trueFalse(subject, "Tema")));
    }

    @Test
    void acceptsMultipleThemesWithFiveQuestionsEach() {
        var analysis = analysis(theme("Tema A"), theme("Tema B"));
        var result = new GeneratedActivity("Ciências", "4º Ano Ensino Fundamental", "TRUE_FALSE",
                List.of(vfTheme("Tema A"), vfTheme("Tema B")), List.of());
        assertThatNoException().isThrownBy(() -> validator.validateResult(request("Ciências", analysis), result));
    }

    @Test
    void acceptsMathProblemsWithSolutionsAndNoTrueFalse() {
        var request = request("Matemática", analysis(theme("Multiplicação")));
        var result = new GeneratedActivity("Matemática", request.grade(), "MATH_PROBLEMS",
                List.of(new GeneratedActivity.Theme("Multiplicação", mathQuestions())), List.of());
        assertThatNoException().isThrownBy(() -> validator.validateResult(request, result));
    }

    @Test
    void rejectsQuestionCountDifferentFromFive() {
        var result = trueFalse("História", "Tema");
        var shortTheme = new GeneratedActivity.Theme("Tema", result.themes().getFirst().questions().subList(0, 4));
        assertInvalid(request("História", analysis(theme("Tema"))),
                new GeneratedActivity("História", result.grade(), result.activityType(), List.of(shortTheme), List.of()));
    }

    @Test
    void rejectsUnbalancedTrueFalseAnswers() {
        List<GeneratedActivity.Question> questions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) questions.add(vf("Tema", i, true));
        assertInvalid(request("Geografia", analysis(theme("Tema"))),
                new GeneratedActivity("Geografia", "4º Ano Ensino Fundamental", "TRUE_FALSE",
                        List.of(new GeneratedActivity.Theme("Tema", questions)), List.of()));
    }

    @Test
    void rejectsDuplicateAndNearDuplicateStatements() {
        List<GeneratedActivity.Question> questions = new ArrayList<>(vfTheme("Tema").questions());
        questions.set(1, new GeneratedActivity.Question(
                questions.getFirst().statement() + "!", false, "Explicação exclusiva dois", List.of("Evidência 2"),
                "Tema", "Compreender 2", "medium", null, null, null, null));
        assertInvalid(request("Ciências", analysis(theme("Tema"))),
                new GeneratedActivity("Ciências", "4º Ano Ensino Fundamental", "TRUE_FALSE",
                        List.of(new GeneratedActivity.Theme("Tema", questions)), List.of()));
    }

    @Test
    void rejectsEmptyExplanation() {
        List<GeneratedActivity.Question> questions = new ArrayList<>(vfTheme("Tema").questions());
        var q = questions.getFirst();
        questions.set(0, new GeneratedActivity.Question(q.statement(), q.answer(), "", q.evidence(), q.theme(),
                q.learningObjective(), q.difficulty(), null, null, null, null));
        assertInvalid(request("Português", analysis(theme("Tema"))), activity("Português", "TRUE_FALSE", "Tema", questions));
    }

    @Test
    void rejectsInvalidDifficulty() {
        List<GeneratedActivity.Question> questions = new ArrayList<>(vfTheme("Tema").questions());
        var q = questions.getFirst();
        questions.set(0, new GeneratedActivity.Question(q.statement(), q.answer(), q.explanation(), q.evidence(), q.theme(),
                q.learningObjective(), "extreme", null, null, null, null));
        assertInvalid(request("Português", analysis(theme("Tema"))), activity("Português", "TRUE_FALSE", "Tema", questions));
    }

    @Test
    void rejectsMathContainingTrueFalseFields() {
        List<GeneratedActivity.Question> questions = new ArrayList<>(mathQuestions());
        var q = questions.getFirst();
        questions.set(0, new GeneratedActivity.Question("Afirmação indevida", true, q.explanation(), q.evidence(), q.theme(),
                q.learningObjective(), q.difficulty(), q.problem(), q.mathAnswer(), q.solutionSteps(), q.skill()));
        assertInvalid(request("Matemática", analysis(theme("Multiplicação"))),
                activity("Matemática", "MATH_PROBLEMS", "Multiplicação", questions));
    }

    @Test
    void rejectsMathWithoutSolution() {
        List<GeneratedActivity.Question> questions = new ArrayList<>(mathQuestions());
        var q = questions.getFirst();
        questions.set(0, new GeneratedActivity.Question(null, null, null, q.evidence(), q.theme(), q.learningObjective(),
                q.difficulty(), q.problem(), q.mathAnswer(), List.of(), q.skill()));
        assertInvalid(request("Matemática", analysis(theme("Multiplicação"))),
                activity("Matemática", "MATH_PROBLEMS", "Multiplicação", questions));
    }

    @Test
    void rejectsInsufficientThemeBeforeCallingProvider() {
        var weak = new VideoAnalysis.Theme("Tema", List.of(), List.of(), List.of(), List.of(), List.of());
        assertThatThrownBy(() -> validator.validateRequest(request("Ciências", analysis(weak))))
                .isInstanceOf(ActivityValidationException.class)
                .extracting(error -> ((ActivityValidationException) error).kind())
                .isEqualTo(ActivityValidationException.Kind.INSUFFICIENT_CONTENT);
    }

    @Test
    void preservesWarningsOnValidResult() {
        var result = new GeneratedActivity("Ciências", "4º Ano Ensino Fundamental", "TRUE_FALSE",
                List.of(vfTheme("Tema")), List.of("Tema pouco desenvolvido."));
        assertThatNoException().isThrownBy(() -> validator.validateResult(request("Ciências", analysis(theme("Tema"))), result));
    }

    private void assertInvalid(ActivityGenerationRequest request, GeneratedActivity result) {
        assertThatThrownBy(() -> validator.validateResult(request, result))
                .isInstanceOf(ActivityValidationException.class);
    }

    private static ActivityGenerationRequest request(String subject, VideoAnalysis analysis) {
        return new ActivityGenerationRequest("4º Ano Ensino Fundamental", subject,
                new ActivityGenerationRequest.Source("youtube", "Aula", "https://www.youtube.com/watch?v=AbCdEf123_-"), analysis);
    }

    private static VideoAnalysis analysis(VideoAnalysis.Theme... themes) {
        return new VideoAnalysis("youtube", "https://www.youtube.com/watch?v=AbCdEf123_-", "Aula", "Ciências",
                "Resumo sustentado pelo material.", List.of(themes), List.of());
    }

    private static VideoAnalysis.Theme theme(String name) {
        return new VideoAnalysis.Theme(name, List.of("Compreender relações"), List.of("conceito"),
                List.of("relação"), List.of("equívoco"), List.of(new VideoAnalysis.Evidence("Trecho da aula", "00:10")));
    }

    private static GeneratedActivity trueFalse(String subject, String theme) {
        return new GeneratedActivity(subject, "4º Ano Ensino Fundamental", "TRUE_FALSE", List.of(vfTheme(theme)), List.of());
    }

    private static GeneratedActivity.Theme vfTheme(String theme) {
        return new GeneratedActivity.Theme(theme, List.of(vf(theme, 1, true), vf(theme, 2, false), vf(theme, 3, true),
                vf(theme, 4, false), vf(theme, 5, true)));
    }

    private static GeneratedActivity.Question vf(String theme, int index, boolean answer) {
        return new GeneratedActivity.Question("Relação conceitual de " + theme + " número " + index, answer,
                "Explicação pedagógica de " + theme + " número " + index, List.of("Evidência " + index), theme,
                "Compreender objetivo " + index, index == 5 ? "hard" : "medium", null, null, null, null);
    }

    private static List<GeneratedActivity.Question> mathQuestions() {
        List<GeneratedActivity.Question> questions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            questions.add(new GeneratedActivity.Question(null, null, null, List.of("Evidência " + i), "Multiplicação",
                    "Resolver situação " + i, "medium", "Problema matemático novo número " + i,
                    String.valueOf(i * 2), List.of("Identificar os dados", "Calcular " + i + " vezes 2"), "Multiplicar"));
        }
        return questions;
    }

    private static GeneratedActivity activity(String subject, String type, String theme,
                                               List<GeneratedActivity.Question> questions) {
        return new GeneratedActivity(subject, "4º Ano Ensino Fundamental", type,
                List.of(new GeneratedActivity.Theme(theme, questions)), List.of());
    }
}
