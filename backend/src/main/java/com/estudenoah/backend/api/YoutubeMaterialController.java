package com.estudenoah.backend.api;

import com.estudenoah.backend.video.VideoAnalysis;
import com.estudenoah.backend.video.VideoAnalysisService;
import com.estudenoah.backend.video.YoutubeUrlNormalizer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class YoutubeMaterialController {
    private final YoutubeUrlNormalizer urlNormalizer;
    private final VideoAnalysisService analysisService;

    public YoutubeMaterialController(YoutubeUrlNormalizer urlNormalizer, VideoAnalysisService analysisService) {
        this.urlNormalizer = urlNormalizer;
        this.analysisService = analysisService;
    }

    @PostMapping(
            path = "/v1/materials/youtube/analyze",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public VideoAnalysis analyze(@RequestBody(required = false) YoutubeAnalysisRequest request) {
        return analysisService.analyze(urlNormalizer.normalize(request == null ? null : request.url()));
    }

    public record YoutubeAnalysisRequest(String url) {
    }
}
