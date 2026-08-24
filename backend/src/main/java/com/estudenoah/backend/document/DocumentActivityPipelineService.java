package com.estudenoah.backend.document;

import com.estudenoah.backend.activity.ActivityGenerationRequest;
import com.estudenoah.backend.activity.ActivityGenerationService;
import com.estudenoah.backend.activity.GeneratedActivity;
import org.springframework.stereotype.Service;

@Service
public final class DocumentActivityPipelineService {
    private final DocumentAnalysisService analysisService;
    private final ActivityGenerationService activityService;

    public DocumentActivityPipelineService(DocumentAnalysisService analysisService, ActivityGenerationService activityService) {
        this.analysisService = analysisService;
        this.activityService = activityService;
    }

    public GeneratedActivity generate(DocumentAnalysisRequest request) {
        DocumentAnalysis analysis = analysisService.analyze(request);
        return activityService.generate(new ActivityGenerationRequest(
                request.grade(), request.subject(),
                new ActivityGenerationRequest.Source(request.sourceType(), request.sourceTitle(), null),
                analysis.asPedagogicalAnalysis()
        ));
    }
}
