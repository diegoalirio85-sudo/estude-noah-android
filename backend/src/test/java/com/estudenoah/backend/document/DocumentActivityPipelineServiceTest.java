package com.estudenoah.backend.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.estudenoah.backend.activity.ActivityGenerationRequest;
import com.estudenoah.backend.activity.ActivityGenerationService;
import com.estudenoah.backend.activity.GeneratedActivity;
import com.estudenoah.backend.video.VideoAnalysis;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DocumentActivityPipelineServiceTest {
    @Test
    void reusesC21ActivityGenerationService() {
        DocumentAnalysisService analysis = mock(DocumentAnalysisService.class);
        ActivityGenerationService activities = mock(ActivityGenerationService.class);
        when(analysis.analyze(any())).thenReturn(analysis("Ciências"));
        var expected = new GeneratedActivity("Ciências", "4º Ano", "TRUE_FALSE", List.of(), List.of());
        when(activities.generate(any())).thenReturn(expected);

        var result = new DocumentActivityPipelineService(analysis, activities).generate(request("Ciências"));

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<ActivityGenerationRequest> passed = ArgumentCaptor.forClass(ActivityGenerationRequest.class);
        verify(activities).generate(passed.capture());
        assertThat(passed.getValue().analysis().themes()).hasSize(1);
        assertThat(passed.getValue().source().type()).isEqualTo("pdf");
    }

    @Test
    void preservesMathProblemsReturnedByC21() {
        DocumentAnalysisService analysis = mock(DocumentAnalysisService.class);
        ActivityGenerationService activities = mock(ActivityGenerationService.class);
        when(analysis.analyze(any())).thenReturn(analysis("Matemática"));
        var expected = new GeneratedActivity("Matemática", "4º Ano", "MATH_PROBLEMS", List.of(), List.of());
        when(activities.generate(any())).thenReturn(expected);
        assertThat(new DocumentActivityPipelineService(analysis, activities).generate(request("Matemática")).activityType())
                .isEqualTo("MATH_PROBLEMS");
    }

    private static DocumentAnalysisRequest request(String subject) { return new DocumentAnalysisRequest("pdf", "Aula", subject, "4º Ano", "conteúdo ".repeat(30)); }
    private static DocumentAnalysis analysis(String subject) {
        var theme = new VideoAnalysis.Theme("Tema", List.of("Objetivo"), List.of("Conceito"), List.of("Relação"),
                List.of("Equívoco"), List.of(new VideoAnalysis.Evidence("Evidência", "document")));
        return new DocumentAnalysis("pdf", "Aula", subject, "Resumo", List.of(theme), List.of());
    }
}
